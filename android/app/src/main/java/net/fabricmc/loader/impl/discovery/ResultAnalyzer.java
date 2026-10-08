package net.fabricmc.loader.impl.discovery;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.impl.util.Localization;
import net.fabricmc.loader.impl.util.StringUtil;
import net.fabricmc.loader.impl.util.version.VersionIntervalImpl;

final class ResultAnalyzer {
   private static final boolean SHOW_PATH_INFO = false;
   private static final boolean SHOW_INACTIVE = false;

   static String gatherErrors(
      ModSolver.Result result,
      Map<String, ModCandidateImpl> selectedMods,
      Map<String, List<ModCandidateImpl>> modsById,
      Map<String, Set<ModCandidateImpl>> envDisabledMods,
      EnvType envType
   ) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);

      try {
         String prefix = "";
         boolean suggestFix = true;
         if (result.fix != null) {
            pw.printf("\n%s", Localization.format("resolution.solutionHeader"));
            formatFix(result.fix, result, selectedMods, modsById, envDisabledMods, envType, pw);
            pw.printf("\n%s", Localization.format("resolution.depListHeader"));
            prefix = "\t";
            suggestFix = false;
         }

         List<ModCandidateImpl> matches = new ArrayList<>();

         for (Explanation explanation : result.reason) {
            assert explanation.error.isDependencyError;
            ModDependency dep = explanation.dep;
            ModCandidateImpl selected = selectedMods.get(dep.getModId());
            if (selected != null) {
               matches.add(selected);
            } else {
               List<ModCandidateImpl> candidates = modsById.get(dep.getModId());
               if (candidates != null) {
                  matches.addAll(candidates);
               }
            }

            addErrorToList(explanation.mod, explanation.dep, matches, envDisabledMods.containsKey(dep.getModId()), suggestFix, prefix, pw);
            matches.clear();
         }
      } catch (Throwable var16) {
         try {
            pw.close();
         } catch (Throwable var15) {
            var16.addSuppressed(var15);
         }

         throw var16;
      }

      pw.close();
      return sw.toString();
   }

   private static void formatFix(
      ModSolver.Fix fix,
      ModSolver.Result result,
      Map<String, ModCandidateImpl> selectedMods,
      Map<String, List<ModCandidateImpl>> modsById,
      Map<String, Set<ModCandidateImpl>> envDisabledMods,
      EnvType envType,
      PrintWriter pw
   ) {
      for (ModSolver.AddModVar mod : fix.modsToAdd) {
         Set<ModCandidateImpl> envDisabledAlternatives = envDisabledMods.get(mod.getId());
         if (envDisabledAlternatives == null) {
            pw.printf("\n\t - %s", Localization.format("resolution.solution.addMod", mod.getId(), formatVersionRequirements(mod.getVersionIntervals())));
         } else {
            String envKey = String.format("environment.%s", envType.name().toLowerCase(Locale.ENGLISH));
            pw.printf(
               "\n\t - %s",
               Localization.format(
                  "resolution.solution.replaceModEnvDisabled",
                  formatOldMods(envDisabledAlternatives),
                  mod.getId(),
                  formatVersionRequirements(mod.getVersionIntervals()),
                  Localization.format(envKey)
               )
            );
         }
      }

      for (ModCandidateImpl mod : fix.modsToRemove) {
         pw.printf("\n\t - %s", Localization.format("resolution.solution.removeMod", getName(mod), getVersion(mod), mod.getLocalPath()));
      }

      for (Entry<ModSolver.AddModVar, List<ModCandidateImpl>> entry : fix.modReplacements.entrySet()) {
         ModSolver.AddModVar newMod = entry.getKey();
         List<ModCandidateImpl> oldMods = entry.getValue();
         String oldModsFormatted = formatOldMods(oldMods);
         if (oldMods.size() == 1 && oldMods.get(0).getId().equals(newMod.getId())) {
            ModCandidateImpl oldMod = oldMods.get(0);
            boolean hasOverlap = !VersionInterval.and(
                  newMod.getVersionIntervals(), Collections.singletonList(new VersionIntervalImpl(oldMod.getVersion(), true, oldMod.getVersion(), true))
               )
               .isEmpty();
            if (!hasOverlap) {
               pw.printf(
                  "\n\t - %s",
                  Localization.format("resolution.solution.replaceModVersion", oldModsFormatted, formatVersionRequirements(newMod.getVersionIntervals()))
               );
            } else {
               pw.printf(
                  "\n\t - %s",
                  Localization.format(
                     "resolution.solution.replaceModVersionDifferent", oldModsFormatted, formatVersionRequirements(newMod.getVersionIntervals())
                  )
               );
               boolean foundAny = false;

               for (ModDependency dep : oldMod.getDependencies()) {
                  if (!dep.getKind().isSoft()) {
                     ModCandidateImpl mod = fix.activeMods.get(dep.getModId());
                     if (mod == null) {
                        for (ModSolver.AddModVar addMod : fix.modReplacements.keySet()) {
                           if (addMod.getId().equals(dep.getModId())) {
                              pw.printf(
                                 "\n\t\t - %s",
                                 Localization.format(
                                    "resolution.solution.replaceModVersionDifferent.reqSupportedModVersions",
                                    addMod.getId(),
                                    formatVersionRequirements(addMod.getVersionIntervals())
                                 )
                              );
                              foundAny = true;
                              break;
                           }
                        }
                     } else if (dep.matches(mod.getVersion()) != dep.getKind().isPositive()) {
                        pw.printf(
                           "\n\t\t - %s",
                           Localization.format("resolution.solution.replaceModVersionDifferent.reqSupportedModVersion", mod.getId(), getVersion(mod))
                        );
                        foundAny = true;
                     }
                  }
               }

               if (!foundAny) {
                  pw.printf("\n\t\t - %s", Localization.format("resolution.solution.replaceModVersionDifferent.unknown"));
               }
            }
         } else {
            String newModName = newMod.getId();
            ModCandidateImpl alt = selectedMods.get(newMod.getId());
            if (alt != null) {
               newModName = getName(alt);
            } else {
               List<ModCandidateImpl> alts = modsById.get(newMod.getId());
               if (alts != null && !alts.isEmpty()) {
                  newModName = getName(alts.get(0));
               }
            }

            pw.printf(
               "\n\t - %s",
               Localization.format("resolution.solution.replaceMod", oldModsFormatted, newModName, formatVersionRequirements(newMod.getVersionIntervals()))
            );
         }
      }
   }

   static String gatherWarnings(
      List<ModCandidateImpl> uniqueSelectedMods,
      Map<String, ModCandidateImpl> selectedMods,
      Map<String, Set<ModCandidateImpl>> envDisabledMods,
      EnvType envType
   ) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);

      try {
         for (ModCandidateImpl mod : uniqueSelectedMods) {
            for (ModDependency dep : mod.getDependencies()) {
               switch (dep.getKind()) {
                  case RECOMMENDS:
                     ModCandidateImpl depModx = selectedMods.get(dep.getModId());
                     if (depModx == null || !dep.matches(depModx.getVersion())) {
                        addErrorToList(mod, dep, toList(depModx), envDisabledMods.containsKey(dep.getModId()), true, "", pw);
                     }
                     break;
                  case CONFLICTS:
                     ModCandidateImpl depMod = selectedMods.get(dep.getModId());
                     if (depMod != null && dep.matches(depMod.getVersion())) {
                        addErrorToList(mod, dep, toList(depMod), false, true, "", pw);
                     }
               }
            }
         }
      } catch (Throwable var12) {
         try {
            pw.close();
         } catch (Throwable var11) {
            var12.addSuppressed(var11);
         }

         throw var12;
      }

      pw.close();
      return sw.getBuffer().length() == 0 ? null : sw.toString();
   }

   private static List<ModCandidateImpl> toList(ModCandidateImpl mod) {
      return mod != null ? Collections.singletonList(mod) : Collections.emptyList();
   }

   private static void addErrorToList(
      ModCandidateImpl mod, ModDependency dep, List<ModCandidateImpl> matches, boolean presentForOtherEnv, boolean suggestFix, String prefix, PrintWriter pw
   ) {
      Object[] args = new Object[]{
         getName(mod),
         getVersion(mod),
         matches.isEmpty() ? dep.getModId() : getName(matches.get(0)),
         formatVersionRequirements(dep.getVersionIntervals()),
         getVersions(matches),
         matches.size()
      };
      String reason;
      if (!matches.isEmpty()) {
         boolean present;
         if (dep.getKind().isPositive()) {
            present = false;

            for (ModCandidateImpl match : matches) {
               if (dep.matches(match.getVersion())) {
                  present = true;
                  break;
               }
            }
         } else {
            present = true;
         }

         reason = present ? "invalid" : "mismatch";
      } else if (presentForOtherEnv && dep.getKind().isPositive()) {
         reason = "envDisabled";
      } else {
         reason = "missing";
      }

      String key = String.format("resolution.%s.%s", dep.getKind().getKey(), reason);
      pw.printf("\n%s - %s", prefix, StringUtil.capitalize(Localization.format(key, args)));
      if (suggestFix) {
         key = String.format("resolution.%s.suggestion", dep.getKind().getKey());
         pw.printf("\n%s\t - %s", prefix, StringUtil.capitalize(Localization.format(key, args)));
      }
   }

   private static void appendJijInfo(ModCandidateImpl mod, String prefix, boolean mentionMod, PrintWriter pw) {
      String loc;
      String path;
      if (mod.getMetadata().getType().equals("builtin")) {
         loc = "builtin";
         path = null;
      } else if (mod.isRoot()) {
         loc = "root";
         path = mod.getLocalPath();
      } else {
         loc = "normal";
         List<ModCandidateImpl> paths = new ArrayList<>();
         paths.add(mod);
         ModCandidateImpl cur = mod;

         do {
            ModCandidateImpl best = null;
            int maxDiff = 0;

            for (ModCandidateImpl parent : cur.getParentMods()) {
               int diff = cur.getMinNestLevel() - parent.getMinNestLevel();
               if (diff > maxDiff) {
                  best = parent;
                  maxDiff = diff;
               }
            }

            if (best == null) {
               break;
            }

            paths.add(best);
            cur = best;
         } while (!cur.isRoot());

         StringBuilder pathSb = new StringBuilder();

         for (int i = paths.size() - 1; i >= 0; i--) {
            ModCandidateImpl m = paths.get(i);
            if (pathSb.length() > 0) {
               pathSb.append(" -> ");
            }

            pathSb.append(m.getLocalPath());
         }

         path = pathSb.toString();
      }

      String key = String.format("resolution.jij.%s%s", loc, mentionMod ? "" : "NoMention");
      String text;
      if (mentionMod) {
         if (path == null) {
            text = Localization.format(key, getName(mod), getVersion(mod));
         } else {
            text = Localization.format(key, getName(mod), getVersion(mod), path);
         }
      } else if (path == null) {
         text = Localization.format(key);
      } else {
         text = Localization.format(key, path);
      }

      pw.printf("\n%s\t - %s", prefix, StringUtil.capitalize(text));
   }

   private static String formatOldMods(Collection<ModCandidateImpl> mods) {
      List<ModCandidateImpl> modsSorted = new ArrayList<>(mods);
      modsSorted.sort(ModCandidateImpl.ID_VERSION_COMPARATOR);
      List<String> ret = new ArrayList<>(modsSorted.size());

      for (ModCandidateImpl m : modsSorted) {
         ret.add(Localization.format("resolution.solution.replaceMod.oldModNoPath", getName(m), getVersion(m)));
      }

      return formatEnumeration(ret, true);
   }

   private static String getName(ModCandidateImpl candidate) {
      String typePrefix;
      switch (candidate.getMetadata().getType()) {
         case "fabric":
            typePrefix = String.format("%s ", Localization.format("resolution.type.mod"));
            break;
         case "builtin":
         default:
            typePrefix = "";
      }

      return String.format("%s'%s' (%s)", typePrefix, candidate.getMetadata().getName(), candidate.getId());
   }

   private static String getVersion(ModCandidateImpl candidate) {
      return candidate.getVersion().getFriendlyString();
   }

   private static String getVersions(Collection<ModCandidateImpl> candidates) {
      return candidates.stream().map(ResultAnalyzer::getVersion).collect(Collectors.joining("/"));
   }

   private static String formatVersionRequirements(Collection<VersionInterval> intervals) {
      List<String> ret = new ArrayList<>();

      for (VersionInterval interval : intervals) {
         if (interval != null) {
            String str;
            if (interval.getMin() == null) {
               if (interval.getMax() == null) {
                  return Localization.format("resolution.version.any");
               }

               if (interval.isMaxInclusive()) {
                  str = Localization.format("resolution.version.lessEqual", interval.getMax());
               } else {
                  str = Localization.format("resolution.version.less", interval.getMax());
               }
            } else if (interval.getMax() == null) {
               if (interval.isMinInclusive()) {
                  str = Localization.format("resolution.version.greaterEqual", interval.getMin());
               } else {
                  str = Localization.format("resolution.version.greater", interval.getMin());
               }
            } else if (interval.getMin().equals(interval.getMax())) {
               if (!interval.isMinInclusive() || !interval.isMaxInclusive()) {
                  continue;
               }

               str = Localization.format("resolution.version.equal", interval.getMin());
            } else if (isWildcard(interval, 0)) {
               SemanticVersion version = (SemanticVersion)interval.getMin();
               str = Localization.format("resolution.version.major", version.getVersionComponent(0));
            } else if (isWildcard(interval, 1)) {
               SemanticVersion version = (SemanticVersion)interval.getMin();
               str = Localization.format("resolution.version.majorMinor", version.getVersionComponent(0), version.getVersionComponent(1));
            } else {
               String key = String.format(
                  "resolution.version.rangeMin%sMax%s", interval.isMinInclusive() ? "Inc" : "Exc", interval.isMaxInclusive() ? "Inc" : "Exc"
               );
               str = Localization.format(key, interval.getMin(), interval.getMax());
            }

            ret.add(str);
         }
      }

      return ret.isEmpty() ? Localization.format("resolution.version.none") : formatEnumeration(ret, false);
   }

   private static boolean isWildcard(VersionInterval interval, int incrementedComponent) {
      if (interval != null
         && interval.getMin() != null
         && interval.getMax() != null
         && interval.isMinInclusive()
         && !interval.isMaxInclusive()
         && interval.isSemantic()) {
         SemanticVersion min = (SemanticVersion)interval.getMin();
         SemanticVersion max = (SemanticVersion)interval.getMax();
         if ("".equals(min.getPrereleaseKey().orElse(null)) && "".equals(max.getPrereleaseKey().orElse(null))) {
            if (max.getVersionComponent(incrementedComponent) != min.getVersionComponent(incrementedComponent) + 1) {
               return false;
            }

            int i = incrementedComponent + 1;

            for (int m = Math.max(min.getVersionComponentCount(), max.getVersionComponentCount()); i < m; i++) {
               if (min.getVersionComponent(i) != 0 || max.getVersionComponent(i) != 0) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static String formatEnumeration(Collection<?> elements, boolean isAnd) {
      String keyPrefix = isAnd ? "enumerationAnd." : "enumerationOr.";
      Iterator<?> it = elements.iterator();
      switch (elements.size()) {
         case 0:
            return "";
         case 1:
            return Objects.toString(it.next());
         case 2:
            return Localization.format(keyPrefix + "2", it.next(), it.next());
         case 3:
            return Localization.format(keyPrefix + "3", it.next(), it.next(), it.next());
         default:
            String ret = Localization.format(keyPrefix + "nPrefix", it.next());

            do {
               Object next = it.next();
               ret = Localization.format(it.hasNext() ? keyPrefix + "n" : keyPrefix + "nSuffix", ret, next);
            } while (it.hasNext());

            return ret;
      }
   }
}
