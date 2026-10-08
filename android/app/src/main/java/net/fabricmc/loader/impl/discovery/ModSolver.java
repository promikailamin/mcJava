package net.fabricmc.loader.impl.discovery;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.function.Function;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.fabricmc.loader.impl.lib.sat4j.pb.IPBSolver;
import net.fabricmc.loader.impl.lib.sat4j.pb.SolverFactory;
import net.fabricmc.loader.impl.lib.sat4j.pb.tools.DependencyHelper;
import net.fabricmc.loader.impl.lib.sat4j.pb.tools.INegator;
import net.fabricmc.loader.impl.lib.sat4j.pb.tools.WeightedObject;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.TimeoutException;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.version.SemanticVersionImpl;
import net.fabricmc.loader.impl.util.version.VersionPredicateParser;

final class ModSolver {
   static long solverPrepTime;
   static long solveTime;
   static long solutionFetchTime;
   static long solutionAnalyzeTime;
   static long fixSetupTime;
   static long fixSolveTime;
   private static final BigInteger TWO = BigInteger.valueOf(2L);
   private static final INegator negator = new INegator() {
      @Override
      public Object unNegate(Object thing) {
         return ((ModSolver.NegatedDomainObject)thing).obj;
      }

      @Override
      public boolean isNegated(Object thing) {
         return thing instanceof ModSolver.NegatedDomainObject;
      }
   };

   static ModSolver.Result solve(
      List<ModCandidateImpl> allModsSorted,
      Map<String, List<ModCandidateImpl>> modsById,
      Map<String, ModCandidateImpl> selectedMods,
      List<ModCandidateImpl> uniqueSelectedMods
   ) throws ContradictionException, TimeoutException, ModResolutionException {
      Map<ModCandidateImpl, Integer> priorities = new IdentityHashMap<>(allModsSorted.size());

      for (int i = 0; i < allModsSorted.size(); i++) {
         priorities.put(allModsSorted.get(i), i);
      }

      solverPrepTime = System.nanoTime();
      IPBSolver solver = SolverFactory.newDefaultOptimizer();
      int timeout = Integer.getInteger("fabric.debug.resolutionTimeout", 60);
      if (timeout > 0) {
         solver.setTimeout(timeout);
      }

      DependencyHelper<DomainObject, Explanation> dependencyHelper = createDepHelper(solver);
      setupSolver(allModsSorted, modsById, priorities, selectedMods, uniqueSelectedMods, false, null, false, dependencyHelper);
      solveTime = System.nanoTime();
      boolean hasSolution = dependencyHelper.hasASolution();
      solutionFetchTime = System.nanoTime();
      if (hasSolution) {
         Collection<DomainObject> solution = dependencyHelper.getASolution();
         solutionAnalyzeTime = System.nanoTime();

         for (DomainObject obj : solution) {
            if (obj instanceof ModCandidateImpl) {
               ModResolver.selectMod((ModCandidateImpl)obj, selectedMods, uniqueSelectedMods);
            } else {
               assert obj instanceof ModSolver.OptionalDepVar;
            }
         }

         dependencyHelper.reset();
         return ModSolver.Result.createSuccess();
      } else {
         Set<Explanation> reason = dependencyHelper.why();
         Set<ModDependency> failedDeps = Collections.newSetFromMap(new IdentityHashMap<>());
         List<Explanation> failedExplanations = new ArrayList<>();
         computeFailureCausesOptional(
            allModsSorted, modsById, priorities, selectedMods, uniqueSelectedMods, reason, dependencyHelper, failedDeps, failedExplanations
         );
         fixSetupTime = System.nanoTime();
         ModSolver.Fix fix = computeFix(uniqueSelectedMods, allModsSorted, modsById, priorities, selectedMods, failedDeps, dependencyHelper);
         dependencyHelper.reset();
         return ModSolver.Result.createFailure(reason, failedExplanations, fix);
      }
   }

   private static void computeFailureCausesOptional(
      List<ModCandidateImpl> allModsSorted,
      Map<String, List<ModCandidateImpl>> modsById,
      Map<ModCandidateImpl, Integer> priorities,
      Map<String, ModCandidateImpl> selectedMods,
      List<ModCandidateImpl> uniqueSelectedMods,
      Set<Explanation> reason,
      DependencyHelper<DomainObject, Explanation> dependencyHelper,
      Set<ModDependency> failedDeps,
      List<Explanation> failedExplanations
   ) throws ContradictionException, TimeoutException {
      dependencyHelper.reset();
      dependencyHelper = createDepHelper(dependencyHelper.getSolver());
      setupSolver(allModsSorted, modsById, priorities, selectedMods, uniqueSelectedMods, true, null, false, dependencyHelper);
      if (dependencyHelper.hasASolution()) {
         Collection<DomainObject> solution = dependencyHelper.getASolution();
         Set<ModDependency> disabledDeps = new HashSet<>();

         for (DomainObject obj : solution) {
            if (obj instanceof ModSolver.DisableDepVar) {
               disabledDeps.add(((ModSolver.DisableDepVar)obj).dep);
            } else {
               assert obj instanceof ModCandidateImpl;
            }
         }

         for (DomainObject obj : solution) {
            if (obj instanceof ModCandidateImpl) {
               ModCandidateImpl mod = (ModCandidateImpl)obj;

               for (ModDependency dep : mod.getDependencies()) {
                  if (disabledDeps.contains(dep)) {
                     assert dep.getKind() == ModDependency.Kind.DEPENDS || dep.getKind() == ModDependency.Kind.BREAKS;
                     failedDeps.add(dep);
                     failedExplanations.add(
                        new Explanation(
                           dep.getKind() == ModDependency.Kind.DEPENDS ? Explanation.ErrorKind.HARD_DEP : Explanation.ErrorKind.NEG_HARD_DEP, mod, dep
                        )
                     );
                  }
               }
            }
         }
      }
   }

   private static ModSolver.Fix computeFix(
      List<ModCandidateImpl> uniqueSelectedMods,
      List<ModCandidateImpl> allModsSorted,
      Map<String, List<ModCandidateImpl>> modsById,
      Map<ModCandidateImpl, Integer> priorities,
      Map<String, ModCandidateImpl> selectedMods,
      Set<ModDependency> failedDeps,
      DependencyHelper<DomainObject, Explanation> dependencyHelper
   ) throws ContradictionException, TimeoutException {
      Map<String, Set<Collection<VersionPredicate>>> depsById = new HashMap<>();

      for (ModDependency dep : failedDeps) {
         if (dep.getKind() == ModDependency.Kind.DEPENDS) {
            depsById.computeIfAbsent(dep.getModId(), ignore -> new HashSet<>()).add(dep.getVersionRequirements());
         }
      }

      Set<String> modsWithOnlyOutboundDepFailures = new HashSet<>();

      for (ModCandidateImpl mod : allModsSorted) {
         if (!mod.getDependencies().isEmpty() && !depsById.containsKey(mod.getId()) && !Collections.disjoint(mod.getDependencies(), failedDeps)) {
            depsById.computeIfAbsent(mod.getId(), ignore -> new HashSet<>()).add(Collections.singleton(VersionPredicateParser.getAny()));
            modsWithOnlyOutboundDepFailures.add(mod.getId());
         }
      }

      for (ModCandidateImpl mod : allModsSorted) {
         for (ModDependency dep : mod.getDependencies()) {
            if (dep.getKind() == ModDependency.Kind.DEPENDS) {
               Set<Collection<VersionPredicate>> predicates = depsById.get(dep.getModId());
               if (predicates != null) {
                  predicates.add(dep.getVersionRequirements());
               }
            }
         }
      }

      Map<String, List<ModSolver.AddModVar>> installableMods = new HashMap<>();

      for (Entry<String, Set<Collection<VersionPredicate>>> entry : depsById.entrySet()) {
         String id = entry.getKey();
         boolean hadOnlyOutboundDepFailures = modsWithOnlyOutboundDepFailures.contains(id);
         Set<VersionInterval> allIntervals = new HashSet<>();

         for (Collection<VersionPredicate> versionPredicates : entry.getValue()) {
            List<VersionInterval> intervals = Collections.emptyList();

            for (VersionPredicate v : versionPredicates) {
               intervals = VersionInterval.or(intervals, v.getInterval());
            }

            allIntervals.addAll(intervals);
         }

         if (!allIntervals.isEmpty()) {
            VersionInterval commonInterval = null;
            boolean commonVersionInitialized = false;
            Set<Version> versions = new HashSet<>();

            for (VersionInterval interval : allIntervals) {
               if (commonInterval == null) {
                  if (!commonVersionInitialized) {
                     commonInterval = interval;
                     commonVersionInitialized = true;
                  }
               } else {
                  commonInterval = interval.and(commonInterval);
               }

               versions.add(deriveVersion(interval));
            }

            List<ModSolver.AddModVar> out = installableMods.computeIfAbsent(id, ignore -> new ArrayList<>());
            if (commonInterval != null) {
               out.add(new ModSolver.AddModVar(id, deriveVersion(commonInterval), hadOnlyOutboundDepFailures));
            } else {
               for (Version version : versions) {
                  out.add(new ModSolver.AddModVar(id, version, hadOnlyOutboundDepFailures));
               }
            }

            out.sort(Comparator.comparing(ModSolver.AddModVar::getVersion).reversed());
         }
      }

      fixSolveTime = System.nanoTime();
      dependencyHelper.reset();
      dependencyHelper = createDepHelper(dependencyHelper.getSolver());
      setupSolver(allModsSorted, modsById, priorities, selectedMods, uniqueSelectedMods, false, installableMods, true, dependencyHelper);
      if (!dependencyHelper.hasASolution()) {
         Log.warn(LogCategory.RESOLUTION, "Unable to find a solution to fix the mod set, reason: %s", dependencyHelper.why());
         return null;
      }

      Map<String, ModCandidateImpl> activeMods = new HashMap<>();
      Map<ModCandidateImpl, ModSolver.InactiveReason> inactiveMods = new IdentityHashMap<>(allModsSorted.size());
      List<ModSolver.AddModVar> modsToAdd = new ArrayList<>();
      List<ModCandidateImpl> modsToRemove = new ArrayList<>();
      Map<ModSolver.AddModVar, List<ModCandidateImpl>> modReplacements = new HashMap<>();

      for (ModCandidateImpl mod : allModsSorted) {
         inactiveMods.put(mod, ModSolver.InactiveReason.UNKNOWN);
      }

      for (DomainObject obj : dependencyHelper.getASolution()) {
         if (obj instanceof ModCandidateImpl) {
            ModCandidateImpl mod = (ModCandidateImpl)obj;
            activeMods.put(mod.getId(), mod);
            inactiveMods.remove(mod);
         } else if (obj instanceof ModSolver.AddModVar) {
            ModSolver.AddModVar mod = (ModSolver.AddModVar)obj;
            List<ModCandidateImpl> replaced = new ArrayList<>();
            ModCandidateImpl selectedMod = selectedMods.get(obj.getId());
            if (selectedMod != null) {
               replaced.add(selectedMod);
            }

            List<ModCandidateImpl> mods = modsById.get(obj.getId());
            if (mods != null) {
               replaced.addAll(mods);
            }

            if (replaced.isEmpty()) {
               modsToAdd.add(mod);
            } else {
               modReplacements.put(mod, replaced);

               for (ModCandidateImpl m : replaced) {
                  inactiveMods.put(m, ModSolver.InactiveReason.TO_REPLACE);
               }
            }
         } else if (obj instanceof ModSolver.RemoveModVar) {
            boolean found = false;
            ModCandidateImpl mod = selectedMods.get(obj.getId());
            if (mod != null) {
               modsToRemove.add(mod);
               inactiveMods.put(mod, ModSolver.InactiveReason.TO_REMOVE);
               found = true;
            }

            List<ModCandidateImpl> mods = modsById.get(obj.getId());
            if (mods != null) {
               for (ModCandidateImpl m : mods) {
                  if (m.isRoot()) {
                     modsToRemove.add(m);
                     inactiveMods.put(m, ModSolver.InactiveReason.TO_REMOVE);
                     found = true;
                  }
               }
            }

            assert found;
         } else {
            assert false : obj;
         }
      }

      for (Collection<ModSolver.AddModVar> mods : Arrays.asList(modsToAdd, modReplacements.keySet())) {
         for (ModSolver.AddModVar mod : mods) {
            List<VersionInterval> intervals = Collections.singletonList(VersionInterval.INFINITE);

            for (ModCandidateImpl m : activeMods.values()) {
               for (ModDependency dep : m.getDependencies()) {
                  if (dep.getModId().equals(mod.getId()) && !dep.getKind().isSoft()) {
                     if (dep.getKind().isPositive()) {
                        intervals = VersionInterval.and(intervals, dep.getVersionIntervals());
                     } else {
                        intervals = VersionInterval.and(intervals, VersionInterval.not(dep.getVersionIntervals()));
                     }
                  }
               }
            }

            mod.setVersionIntervals(intervals);
         }
      }

      for (Entry<ModCandidateImpl, ModSolver.InactiveReason> entry : inactiveMods.entrySet()) {
         if (entry.getValue() == ModSolver.InactiveReason.UNKNOWN) {
            ModCandidateImpl mod = entry.getKey();
            ModCandidateImpl active = activeMods.get(mod.getId());
            if (active != null) {
               if (allModsSorted.indexOf(mod) > allModsSorted.indexOf(active)) {
                  if (mod.getVersion().equals(active.getVersion())) {
                     entry.setValue(ModSolver.InactiveReason.SAME_ACTIVE);
                  } else {
                     assert mod.getVersion().compareTo(active.getVersion()) < 0;
                     entry.setValue(ModSolver.InactiveReason.NEWER_ACTIVE);
                  }
               } else {
                  entry.setValue(ModSolver.InactiveReason.INCOMPATIBLE);
               }
            } else if (!mod.getParentMods().isEmpty()) {
               boolean found = false;
               Iterator var70 = mod.getParentMods().iterator();

               while (true) {
                  if (var70.hasNext()) {
                     ModCandidateImpl m = (ModCandidateImpl)var70.next();
                     if (activeMods.get(m.getId()) != m) {
                        continue;
                     }

                     found = true;
                  }

                  if (!found) {
                     entry.setValue(ModSolver.InactiveReason.INACTIVE_PARENT);
                  }
                  break;
               }
            }
         }
      }

      return new ModSolver.Fix(modsToAdd, modsToRemove, modReplacements, activeMods, inactiveMods);
   }

   private static Version deriveVersion(VersionInterval interval) {
      if (!interval.isSemantic()) {
         return interval.getMin() != null ? interval.getMin() : interval.getMax();
      }

      SemanticVersion v = (SemanticVersion)interval.getMin();
      if (v != null) {
         if (!interval.isMinInclusive()) {
            String pr = v.getPrereleaseKey().orElse(null);
            int[] comps = ((SemanticVersionImpl)v).getVersionComponents();
            if (pr != null) {
               pr = pr.isEmpty() ? "0" : pr.concat(".0");
            } else {
               if (comps.length < 3) {
                  comps = Arrays.copyOf(comps, comps.length + 1);
               }

               comps[comps.length - 1]++;
               pr = "";
            }

            v = new SemanticVersionImpl(comps, pr, null);
         }
      } else if ((v = (SemanticVersion)interval.getMax()) != null) {
         if (!interval.isMaxInclusive()) {
            String pr = v.getPrereleaseKey().orElse(null);
            int[] comps = ((SemanticVersionImpl)v).getVersionComponents();
            if (pr == null) {
               pr = "zzzzzzzz";
            } else if (!pr.isEmpty()) {
               int pos = pr.lastIndexOf(46) + 1;
               String suffix = pr.substring(pos);
               int val;
               if (suffix.matches("\\d+") && (val = Integer.parseInt(suffix)) > 0) {
                  pr = pr.substring(0, pos) + (val - 1);
               } else {
                  char c;
                  if (suffix.length() > 0 && ((c = suffix.charAt(suffix.length() - 1)) != '0' || suffix.length() >= 2)) {
                     pr = pr.substring(0, pr.length() - 1);
                     if (c == 'a') {
                        pr = pr + 'Z';
                     } else if (c == 'A') {
                        pr = pr + '9';
                     } else if (c != '0') {
                        pr = pr + (c - 1);
                     }
                  } else {
                     pr = pos > 0 ? pr.substring(0, pos - 1) : "";
                  }
               }
            } else {
               pr = null;
               if (comps.length < 3) {
                  comps = Arrays.copyOf(comps, 3);
               }

               for (int i = 2; i >= 0; i--) {
                  if (comps[i] > 0) {
                     comps[i]--;
                     break;
                  }

                  comps[i] = 9999;
               }
            }

            v = new SemanticVersionImpl(comps, pr, null);
         }
      } else {
         v = new SemanticVersionImpl(new int[]{1}, null, null);
      }

      return v;
   }

   private static void setupSolver(
      List<ModCandidateImpl> allModsSorted,
      Map<String, List<ModCandidateImpl>> modsById,
      Map<ModCandidateImpl, Integer> priorities,
      Map<String, ModCandidateImpl> selectedMods,
      List<ModCandidateImpl> uniqueSelectedMods,
      boolean depDisableSim,
      Map<String, List<ModSolver.AddModVar>> installableMods,
      boolean removalSim,
      DependencyHelper<DomainObject, Explanation> dependencyHelper
   ) throws ContradictionException {
      Map<String, DomainObject> dummies = new HashMap<>();
      Map<ModDependency, Entry<DomainObject, Integer>> disabledDeps = depDisableSim ? new HashMap<>() : null;
      List<WeightedObject<DomainObject>> weightedObjects = new ArrayList<>();
      generatePreselectConstraints(
         uniqueSelectedMods,
         modsById,
         priorities,
         selectedMods,
         depDisableSim,
         installableMods,
         removalSim,
         dummies,
         disabledDeps,
         dependencyHelper,
         weightedObjects
      );
      generateMainConstraints(
         allModsSorted,
         modsById,
         priorities,
         selectedMods,
         depDisableSim,
         installableMods,
         removalSim,
         dummies,
         disabledDeps,
         dependencyHelper,
         weightedObjects
      );
      if (depDisableSim) {
         applyDisableDepVarWeights(disabledDeps, priorities.size(), weightedObjects);
      }

      WeightedObject<DomainObject>[] weights = weightedObjects.toArray(new WeightedObject[0]);
      dependencyHelper.setObjectiveFunction(weights);
   }

   private static void generatePreselectConstraints(
      List<ModCandidateImpl> uniqueSelectedMods,
      Map<String, List<ModCandidateImpl>> modsById,
      Map<ModCandidateImpl, Integer> priorities,
      Map<String, ModCandidateImpl> selectedMods,
      boolean depDisableSim,
      Map<String, List<ModSolver.AddModVar>> installableMods,
      boolean removalSim,
      Map<String, DomainObject> dummyMods,
      Map<ModDependency, Entry<DomainObject, Integer>> disabledDeps,
      DependencyHelper<DomainObject, Explanation> dependencyHelper,
      List<WeightedObject<DomainObject>> weightedObjects
   ) throws ContradictionException {
      boolean enableOptional = !depDisableSim && installableMods == null && !removalSim;
      List<DomainObject> suitableMods = new ArrayList<>();

      for (ModCandidateImpl mod : uniqueSelectedMods) {
         for (ModDependency dep : mod.getDependencies()) {
            if ((enableOptional || !dep.getKind().isSoft()) && !selectedMods.containsKey(dep.getModId())) {
               List<? extends DomainObject.Mod> availableMods = modsById.get(dep.getModId());
               if (availableMods != null) {
                  for (DomainObject.Mod m : availableMods) {
                     if (dep.matches(m.getVersion())) {
                        suitableMods.add(m);
                     }
                  }
               }

               if (installableMods != null) {
                  availableMods = installableMods.get(dep.getModId());
                  if (availableMods != null) {
                     for (DomainObject.Mod m : availableMods) {
                        if (dep.matches(m.getVersion())) {
                           suitableMods.add(m);
                        }
                     }
                  }
               }

               if (!suitableMods.isEmpty() || depDisableSim) {
                  switch (dep.getKind()) {
                     case DEPENDS:
                        if (depDisableSim) {
                           suitableMods.add(getCreateDisableDepVar(dep, disabledDeps));
                        }

                        dependencyHelper.clause(new Explanation(Explanation.ErrorKind.PRESELECT_HARD_DEP, mod, dep), suitableMods.toArray(new DomainObject[0]));
                        break;
                     case RECOMMENDS:
                        suitableMods.removeIf(m -> ((ModCandidateImpl)m).getLoadCondition().ordinal() > ModLoadCondition.IF_RECOMMENDED.ordinal());
                        if (!suitableMods.isEmpty()) {
                           suitableMods.add(getCreateDummy(dep.getModId(), ModSolver.OptionalDepVar::new, dummyMods, priorities.size(), weightedObjects));
                           dependencyHelper.clause(
                              new Explanation(Explanation.ErrorKind.PRESELECT_SOFT_DEP, mod, dep), suitableMods.toArray(new DomainObject[0])
                           );
                        }
                        break;
                     case BREAKS:
                        if (depDisableSim) {
                           dependencyHelper.setTrue(
                              getCreateDisableDepVar(dep, disabledDeps), new Explanation(Explanation.ErrorKind.PRESELECT_NEG_HARD_DEP, mod, dep)
                           );
                        } else {
                           for (DomainObject match : suitableMods) {
                              dependencyHelper.setFalse(match, new Explanation(Explanation.ErrorKind.PRESELECT_NEG_HARD_DEP, mod, dep));
                           }
                        }
                     case CONFLICTS:
                  }

                  suitableMods.clear();
               }
            }
         }

         if (removalSim) {
            int prio = priorities.size() + 10;
            if (installableMods != null) {
               prio += installableMods.getOrDefault(mod.getId(), Collections.emptyList()).size();
               List<ModSolver.AddModVar> installable = installableMods.get(mod.getId());
               if (installable != null) {
                  suitableMods.addAll(installable);
               }
            }

            suitableMods.add(getCreateDummy(mod.getId(), ModSolver.RemoveModVar::new, dummyMods, prio, weightedObjects));
            suitableMods.add(mod);
            dependencyHelper.clause(new Explanation(Explanation.ErrorKind.PRESELECT_FORCELOAD, mod.getId()), suitableMods.toArray(new DomainObject[0]));
            suitableMods.clear();
         }
      }
   }

   private static void generateMainConstraints(
      List<ModCandidateImpl> allModsSorted,
      Map<String, List<ModCandidateImpl>> modsById,
      Map<ModCandidateImpl, Integer> priorities,
      Map<String, ModCandidateImpl> selectedMods,
      boolean depDisableSim,
      Map<String, List<ModSolver.AddModVar>> installableMods,
      boolean removalSim,
      Map<String, DomainObject> dummyMods,
      Map<ModDependency, Entry<DomainObject, Integer>> disabledDeps,
      DependencyHelper<DomainObject, Explanation> dependencyHelper,
      List<WeightedObject<DomainObject>> weightedObjects
   ) throws ContradictionException {
      boolean enableOptional = !depDisableSim && installableMods == null && !removalSim;
      List<DomainObject> suitableMods = new ArrayList<>();

      for (ModCandidateImpl mod : allModsSorted) {
         for (ModDependency dep : mod.getDependencies()) {
            if (enableOptional || !dep.getKind().isSoft()) {
               ModCandidateImpl selectedMod = selectedMods.get(dep.getModId());
               if (selectedMod != null) {
                  if (!removalSim) {
                     if (!dep.getKind().isSoft() && dep.matches(selectedMod.getVersion()) != dep.getKind().isPositive()) {
                        if (depDisableSim) {
                           dependencyHelper.setTrue(getCreateDisableDepVar(dep, disabledDeps), new Explanation(Explanation.ErrorKind.HARD_DEP, mod, dep));
                        } else {
                           dependencyHelper.setFalse(mod, new Explanation(Explanation.ErrorKind.HARD_DEP_INCOMPATIBLE_PRESELECTED, mod, dep));
                        }
                     }
                     continue;
                  }

                  if (dep.matches(selectedMod.getVersion())) {
                     suitableMods.add(selectedMod);
                  }
               }

               List<? extends DomainObject.Mod> availableMods = modsById.get(dep.getModId());
               if (availableMods != null) {
                  for (DomainObject.Mod m : availableMods) {
                     if (dep.matches(m.getVersion())) {
                        suitableMods.add(m);
                     }
                  }
               }

               if (installableMods != null) {
                  availableMods = installableMods.get(dep.getModId());
                  if (availableMods != null) {
                     for (DomainObject.Mod m : availableMods) {
                        if (dep.matches(m.getVersion())) {
                           suitableMods.add(m);
                        }
                     }
                  }
               }

               switch (dep.getKind()) {
                  case DEPENDS:
                     if (depDisableSim) {
                        suitableMods.add(getCreateDisableDepVar(dep, disabledDeps));
                     }

                     if (suitableMods.isEmpty()) {
                        dependencyHelper.setFalse(mod, new Explanation(Explanation.ErrorKind.HARD_DEP_NO_CANDIDATE, mod, dep));
                     } else {
                        dependencyHelper.implication(mod)
                           .implies(suitableMods.toArray(new DomainObject[0]))
                           .named(new Explanation(Explanation.ErrorKind.HARD_DEP, mod, dep));
                     }
                     break;
                  case RECOMMENDS:
                     suitableMods.removeIf(m -> ((ModCandidateImpl)m).getLoadCondition().ordinal() > ModLoadCondition.IF_RECOMMENDED.ordinal());
                     if (!suitableMods.isEmpty()) {
                        suitableMods.add(getCreateDummy(dep.getModId(), ModSolver.OptionalDepVar::new, dummyMods, priorities.size(), weightedObjects));
                        dependencyHelper.implication(mod)
                           .implies(suitableMods.toArray(new DomainObject[0]))
                           .named(new Explanation(Explanation.ErrorKind.SOFT_DEP, mod, dep));
                     }
                     break;
                  case BREAKS:
                     if (!suitableMods.isEmpty()) {
                        if (depDisableSim) {
                           DomainObject var = getCreateDisableDepVar(dep, disabledDeps);

                           for (DomainObject match : suitableMods) {
                              dependencyHelper.implication(mod)
                                 .implies(new ModSolver.NegatedDomainObject(match), var)
                                 .named(new Explanation(Explanation.ErrorKind.NEG_HARD_DEP, mod, dep));
                           }
                        } else {
                           for (DomainObject match : suitableMods) {
                              dependencyHelper.implication(mod).impliesNot(match).named(new Explanation(Explanation.ErrorKind.NEG_HARD_DEP, mod, dep));
                           }
                        }
                     }
                  case CONFLICTS:
               }

               suitableMods.clear();
            }
         }

         if (!mod.isRoot()) {
            ModLoadCondition loadCondition = mod.getLoadCondition();
            if (loadCondition == ModLoadCondition.ALWAYS) {
               Explanation explanation = new Explanation(Explanation.ErrorKind.NESTED_FORCELOAD, mod.getParentMods().iterator().next(), mod.getId());
               DomainObject[] siblings = modsById.get(mod.getId()).toArray(new DomainObject[0]);
               if (isAnyParentSelected(mod, selectedMods)) {
                  dependencyHelper.clause(explanation, siblings);
               } else {
                  for (ModCandidateImpl parent : mod.getParentMods()) {
                     dependencyHelper.implication(parent).implies(siblings).named(explanation);
                  }
               }
            }

            if (!isAnyParentSelected(mod, selectedMods)) {
               dependencyHelper.implication(mod)
                  .implies(mod.getParentMods().toArray(new DomainObject[0]))
                  .named(new Explanation(Explanation.ErrorKind.NESTED_REQ_PARENT, mod));
            }
         }

         if (!mod.isRoot() || mod.getLoadCondition() != ModLoadCondition.ALWAYS || modsById.get(mod.getId()).size() > 1) {
            int prio = priorities.get(mod);
            BigInteger weight;
            if (mod.getLoadCondition().ordinal() >= ModLoadCondition.IF_RECOMMENDED.ordinal()) {
               weight = TWO.pow(prio + 1);
            } else {
               weight = TWO.pow(allModsSorted.size() - prio).negate();
            }

            weightedObjects.add(WeightedObject.newWO(mod, weight));
         }
      }

      for (List<ModCandidateImpl> variants : modsById.values()) {
         ModCandidateImpl firstMod = variants.get(0);
         String id = firstMod.getId();
         if (variants.size() != 1 || removalSim) {
            boolean isRequired = false;

            for (ModCandidateImpl mod : variants) {
               if (mod.isRoot() && mod.getLoadCondition() == ModLoadCondition.ALWAYS) {
                  isRequired = true;
                  break;
               }
            }

            if (isRequired) {
               if (removalSim) {
                  int prio = priorities.size() + 10;
                  if (installableMods != null) {
                     prio += installableMods.getOrDefault(id, Collections.emptyList()).size();
                  }

                  suitableMods.add(getCreateDummy(id, ModSolver.RemoveModVar::new, dummyMods, prio, weightedObjects));
               }

               if (installableMods != null) {
                  List<ModSolver.AddModVar> installable = installableMods.get(id);
                  if (installable != null) {
                     suitableMods.addAll(installable);
                  }
               }

               suitableMods.addAll(variants);
               dependencyHelper.clause(new Explanation(Explanation.ErrorKind.ROOT_FORCELOAD, id), suitableMods.toArray(new DomainObject[0]));
               suitableMods.clear();
            }
         } else if (firstMod.isRoot() && firstMod.getLoadCondition() == ModLoadCondition.ALWAYS) {
            dependencyHelper.setTrue(firstMod, new Explanation(Explanation.ErrorKind.ROOT_FORCELOAD_SINGLE, firstMod));
         }

         suitableMods.addAll(variants);
         if (installableMods != null) {
            List<ModSolver.AddModVar> installable = installableMods.get(id);
            if (installable != null && !installable.isEmpty()) {
               suitableMods.addAll(installable);
               ModCandidateImpl mod = selectedMods.get(id);
               if (mod != null) {
                  suitableMods.add(mod);
               }
            }
         }

         if (suitableMods.size() > 1 || enableOptional && firstMod.getLoadCondition() == ModLoadCondition.IF_POSSIBLE) {
            dependencyHelper.atMost(1, suitableMods.toArray(new DomainObject[0])).named(new Explanation(Explanation.ErrorKind.UNIQUE_ID, id));
         }

         suitableMods.clear();
      }

      if (installableMods != null) {
         for (List<ModSolver.AddModVar> variants : installableMods.values()) {
            String id = variants.get(0).getId();
            boolean isReplacement = modsById.containsKey(id);
            if (!isReplacement) {
               suitableMods.addAll(variants);
               ModCandidateImpl selectedMod = selectedMods.get(id);
               if (selectedMod != null) {
                  suitableMods.add(selectedMod);
               }

               if (suitableMods.size() > 1) {
                  dependencyHelper.atMost(1, suitableMods.toArray(new DomainObject[0])).named(new Explanation(Explanation.ErrorKind.UNIQUE_ID, id));
               }

               suitableMods.clear();
            }

            for (int i = 0; i < variants.size(); i++) {
               ModSolver.AddModVar mod = variants.get(i);
               int weight = priorities.size() + 4 + i;
               if (isReplacement) {
                  weight += 3;
               }

               if (mod.hadOnlyOutboundDepFailures) {
                  weight++;
               }

               weightedObjects.add(WeightedObject.newWO(mod, TWO.pow(weight)));
            }
         }
      }
   }

   private static DependencyHelper<DomainObject, Explanation> createDepHelper(IPBSolver solver) {
      DependencyHelper<DomainObject, Explanation> ret = new DependencyHelper<>(solver);
      ret.setNegator(negator);
      return ret;
   }

   private static DomainObject getCreateDummy(
      String id,
      Function<String, DomainObject> supplier,
      Map<String, DomainObject> duplicateMap,
      int modCount,
      List<WeightedObject<DomainObject>> weightedObjects
   ) {
      DomainObject ret = duplicateMap.get(id);
      if (ret != null) {
         return ret;
      }

      ret = supplier.apply(id);
      int weight = modCount + 2;
      weightedObjects.add(WeightedObject.newWO(ret, TWO.pow(weight)));
      return ret;
   }

   private static DomainObject getCreateDisableDepVar(ModDependency dep, Map<ModDependency, Entry<DomainObject, Integer>> duplicateMap) {
      Entry<DomainObject, Integer> entry = duplicateMap.computeIfAbsent(dep, d -> new SimpleEntry<>(new ModSolver.DisableDepVar(d), 0));
      entry.setValue(entry.getValue() + 1);
      return entry.getKey();
   }

   private static void applyDisableDepVarWeights(
      Map<ModDependency, Entry<DomainObject, Integer>> map, int modCount, List<WeightedObject<DomainObject>> weightedObjects
   ) {
      BigInteger baseWeight = TWO.pow(modCount + 3);

      for (Entry<DomainObject, Integer> entry : map.values()) {
         int count = entry.getValue();
         weightedObjects.add(WeightedObject.newWO(entry.getKey(), count > 1 ? baseWeight.multiply(BigInteger.valueOf(count)) : baseWeight));
      }
   }

   static boolean isAnyParentSelected(ModCandidateImpl mod, Map<String, ModCandidateImpl> selectedMods) {
      for (ModCandidateImpl parentMod : mod.getParentMods()) {
         if (selectedMods.get(parentMod.getId()) == parentMod) {
            return true;
         }
      }

      return false;
   }

   static boolean hasAllDepsSatisfied(ModCandidateImpl mod, Map<String, ModCandidateImpl> mods) {
      for (ModDependency dep : mod.getDependencies()) {
         if (dep.getKind() == ModDependency.Kind.DEPENDS) {
            ModCandidateImpl m = mods.get(dep.getModId());
            if (m == null || !dep.matches(m.getVersion())) {
               return false;
            }
         } else if (dep.getKind() == ModDependency.Kind.BREAKS) {
            ModCandidateImpl m = mods.get(dep.getModId());
            if (m != null && dep.matches(m.getVersion())) {
               return false;
            }
         }
      }

      return true;
   }

   static final class AddModVar implements DomainObject.Mod {
      private final String id;
      private final Version version;
      final boolean hadOnlyOutboundDepFailures;
      private List<VersionInterval> versionIntervals;

      AddModVar(String id, Version version, boolean hadOnlyOutboundDepFailures) {
         this.id = id;
         this.version = version;
         this.hadOnlyOutboundDepFailures = hadOnlyOutboundDepFailures;
      }

      @Override
      public String getId() {
         return this.id;
      }

      @Override
      public Version getVersion() {
         return this.version;
      }

      public List<VersionInterval> getVersionIntervals() {
         return this.versionIntervals;
      }

      void setVersionIntervals(List<VersionInterval> versionIntervals) {
         this.versionIntervals = versionIntervals;
      }

      @Override
      public String toString() {
         return String.format("add:%s %s (%s)", this.id, this.version, this.versionIntervals);
      }
   }

   private static final class DisableDepVar implements DomainObject {
      final ModDependency dep;

      DisableDepVar(ModDependency dep) {
         this.dep = dep;
      }

      @Override
      public String getId() {
         return this.dep.getModId();
      }

      @Override
      public String toString() {
         return "disableDep:" + this.dep;
      }
   }

   static class Fix {
      final Collection<ModSolver.AddModVar> modsToAdd;
      final Collection<ModCandidateImpl> modsToRemove;
      final Map<ModSolver.AddModVar, List<ModCandidateImpl>> modReplacements;
      final Map<String, ModCandidateImpl> activeMods;
      final Map<ModCandidateImpl, ModSolver.InactiveReason> inactiveMods;

      Fix(
         Collection<ModSolver.AddModVar> modsToAdd,
         Collection<ModCandidateImpl> modsToRemove,
         Map<ModSolver.AddModVar, List<ModCandidateImpl>> modReplacements,
         Map<String, ModCandidateImpl> activeMods,
         Map<ModCandidateImpl, ModSolver.InactiveReason> inactiveMods
      ) {
         this.modsToAdd = modsToAdd;
         this.modsToRemove = modsToRemove;
         this.modReplacements = modReplacements;
         this.activeMods = activeMods;
         this.inactiveMods = inactiveMods;
      }
   }

   enum InactiveReason {
      INACTIVE_PARENT("inactive_parent"),
      INCOMPATIBLE("incompatible"),
      NEWER_ACTIVE("newer_active"),
      SAME_ACTIVE("same_active"),
      TO_REMOVE("to_remove"),
      TO_REPLACE("to_replace"),
      UNKNOWN("unknown"),
      WRONG_ENVIRONMENT("wrong_environment");

      final String id;

      InactiveReason(String id) {
         this.id = id;
      }
   }

   private static final class NegatedDomainObject implements DomainObject {
      private final DomainObject obj;

      NegatedDomainObject(DomainObject obj) {
         this.obj = obj;
      }

      @Override
      public String getId() {
         return this.obj.getId();
      }

      @Override
      public String toString() {
         return "!" + this.obj;
      }
   }

   private static final class OptionalDepVar implements DomainObject {
      private final String id;

      OptionalDepVar(String id) {
         this.id = id;
      }

      @Override
      public String getId() {
         return this.id;
      }

      @Override
      public String toString() {
         return "optionalDep:" + this.getId();
      }
   }

   private static final class RemoveModVar implements DomainObject {
      private final String id;

      RemoveModVar(String id) {
         this.id = id;
      }

      @Override
      public String getId() {
         return this.id;
      }

      @Override
      public String toString() {
         return "remove:" + this.getId();
      }
   }

   static class Result {
      final boolean success;
      final Collection<Explanation> immediateReason;
      final Collection<Explanation> reason;
      final ModSolver.Fix fix;

      static ModSolver.Result createSuccess() {
         return new ModSolver.Result(true, null, null, null);
      }

      static ModSolver.Result createFailure(Collection<Explanation> immediateReason, Collection<Explanation> reason, ModSolver.Fix fix) {
         return new ModSolver.Result(false, immediateReason, reason, fix);
      }

      private Result(boolean success, Collection<Explanation> immediateReason, Collection<Explanation> reason, ModSolver.Fix fix) {
         this.success = success;
         this.immediateReason = immediateReason;
         this.reason = reason;
         this.fix = fix;
      }
   }
}
