package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.Pair;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.data.MemberInfo;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.util.RegexMatcher;
import org.objectweb.asm.AnnotationVisitor;

class CommonInjectionAnnotationVisitor extends AnnotationVisitor {
   protected final CommonData data;
   protected final List<String> targets;
   protected final Set<MemberInfo> knownTargetMethods;

   CommonInjectionAnnotationVisitor(CommonData data, AnnotationVisitor delegate, List<String> targets, Set<MemberInfo> knownTargetMethods) {
      super(589824, Objects.requireNonNull(delegate));
      this.data = Objects.requireNonNull(data);
      this.targets = Objects.requireNonNull(targets);
      this.knownTargetMethods = Objects.requireNonNull(knownTargetMethods);
   }

   public AnnotationVisitor visitAnnotation(String name, String descriptor) {
      AnnotationVisitor av = super.visitAnnotation(name, descriptor);
      if (name.equals("at")) {
         if (!descriptor.equals("Lorg/spongepowered/asm/mixin/injection/At;")) {
            throw new RuntimeException("Unexpected annotation " + descriptor);
         }

         av = new AtAnnotationVisitor(this.data, av, this.targets);
      } else if (name.equals("slice")) {
         if (!descriptor.equals("Lorg/spongepowered/asm/mixin/injection/Slice;")) {
            throw new RuntimeException("Unexpected annotation " + descriptor);
         }

         av = new SliceAnnotationVisitor(this.data, av, this.targets);
      }

      return av;
   }

   public AnnotationVisitor visitArray(String name) {
      AnnotationVisitor av = super.visitArray(name);
      if (name.equals("method")) {
         return new AnnotationVisitor(589824, av) {
            public void visit(String name, Object value) {
               String string = Objects.requireNonNull((String)value);
               if (string.endsWith("/")) {
                  List<String> resolved = CommonInjectionAnnotationVisitor.this.resolveAndRemapMixinRegex(string);
                  if (resolved == null) {
                     super.visit(name, value);
                  } else {
                     for (String remappedSelector : resolved) {
                        super.visit(name, remappedSelector);
                     }
                  }
               } else {
                  MemberInfo info = MemberInfo.parse(string.replaceAll("\\s", ""));
                  if (info == null) {
                     super.visit(name, value);
                  } else {
                     List<MemberInfo> resolved = new CommonInjectionAnnotationVisitor.InjectMethodMappable(
                           CommonInjectionAnnotationVisitor.this.data,
                           info,
                           CommonInjectionAnnotationVisitor.this.targets,
                           CommonInjectionAnnotationVisitor.this.knownTargetMethods
                        )
                        .result();
                     if (resolved.isEmpty()) {
                        throw new RuntimeException("InjectMethodMappable should never resolve to zero entries");
                     }

                     for (MemberInfo remappedInfo : resolved) {
                        super.visit(name, remappedInfo.toString());
                     }
                  }
               }
            }
         };
      } else if (name.equals("target")) {
         return new AnnotationVisitor(589824, av) {
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
               if (!descriptor.equals("Lorg/spongepowered/asm/mixin/injection/Desc;")) {
                  throw new RuntimeException("Unexpected annotation " + descriptor);
               }

               AnnotationVisitor av1 = super.visitAnnotation(name, descriptor);
               return new DescAnnotationVisitor(
                  CommonInjectionAnnotationVisitor.this.targets, CommonInjectionAnnotationVisitor.this.data, av1, TrMember.MemberType.METHOD
               );
            }
         };
      } else if (name.equals("at")) {
         return new AnnotationVisitor(589824, av) {
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
               if (!descriptor.equals("Lorg/spongepowered/asm/mixin/injection/At;")) {
                  throw new RuntimeException("Unexpected annotation " + descriptor);
               }

               AnnotationVisitor av1 = super.visitAnnotation(name, descriptor);
               return new AtAnnotationVisitor(CommonInjectionAnnotationVisitor.this.data, av1, CommonInjectionAnnotationVisitor.this.targets);
            }
         };
      } else {
         return name.equals("slice") ? new AnnotationVisitor(589824, av) {
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
               if (!descriptor.equals("Lorg/spongepowered/asm/mixin/injection/Slice;")) {
                  throw new RuntimeException("Unexpected annotation " + descriptor);
               }

               AnnotationVisitor av1 = super.visitAnnotation(name, descriptor);
               return new SliceAnnotationVisitor(CommonInjectionAnnotationVisitor.this.data, av1, CommonInjectionAnnotationVisitor.this.targets);
            }
         } : av;
      }
   }

   private List<String> resolveAndRemapMixinRegex(String input) {
      RegexMatcher matcher;
      try {
         matcher = RegexMatcher.parse(input);
      } catch (RegexMatcher.ParsingException e) {
         this.data.getLogger().warn("Error parsing mixin regex %s: %s", input, e.toString());
         return null;
      }

      List<? extends TrMethod> matchedMethods = Objects.requireNonNull(this.targets)
         .stream()
         .map(this.data.resolver::resolveClass)
         .filter(Optional::isPresent)
         .map(Optional::get)
         .flatMap(trClass -> trClass.getMethods().stream())
         .filter(trMethod -> matcher.matches(trMethod.getOwner().getName(), trMethod.getName(), trMethod.getDesc()))
         .sorted(
            Comparator.<TrMethod, String>comparing(trMethod -> trMethod.getOwner().getName()).thenComparing(TrMember::getName).thenComparing(TrMember::getDesc)
         )
         .collect(Collectors.toList());
      List<String> result = new ArrayList<>();

      for (TrMethod matchedMethod : matchedMethods) {
         String mappedOwner = this.data.mapper.mapName(matchedMethod.getOwner());
         String mappedName = this.data.mapper.mapName(matchedMethod);
         String mappedDesc = this.data.mapper.mapDesc(matchedMethod);
         result.add(String.format("L%s;%s%s", mappedOwner, mappedName, mappedDesc));
         this.knownTargetMethods.add(new MemberInfo(matchedMethod.getOwner().getName(), matchedMethod.getName(), "", matchedMethod.getDesc()));
      }

      return result;
   }

   private static Pair<Integer, Integer> parseQuantifier(CommonData data, String quantifier) {
      if (quantifier == null || quantifier.isEmpty()) {
         return Pair.of(0, 1);
      }

      if (quantifier.equals("*")) {
         return Pair.of(0, Integer.MAX_VALUE);
      }

      if (quantifier.equals("+")) {
         return Pair.of(1, Integer.MAX_VALUE);
      }

      if (quantifier.startsWith("{") && quantifier.endsWith("}") && quantifier.length() >= 3) {
         String inner = quantifier.substring(1, quantifier.length() - 1).trim();
         if (inner.isEmpty()) {
            data.getLogger().error("Unable to parse quantifier %s, remap behaviour may be incorrect", quantifier);
            return Pair.of(0, 0);
         }

         String strMin = inner;
         String strMax = inner;
         int comma = inner.indexOf(44);
         if (comma > -1) {
            strMin = inner.substring(0, comma).trim();
            strMax = inner.substring(comma + 1).trim();
         }

         try {
            int min = !strMin.isEmpty() ? Integer.parseInt(strMin) : 0;
            int max = !strMax.isEmpty() ? Integer.parseInt(strMax) : Integer.MAX_VALUE;
            return Pair.of(min, Math.max(min, max));
         } catch (NumberFormatException ex) {
            data.getLogger().error("Unable to parse quantifier %s, remap behaviour may be incorrect", quantifier);
            return Pair.of(0, 0);
         }
      } else {
         data.getLogger().error("Unable to parse quantifier %s, remap behaviour may be incorrect", quantifier);
         return Pair.of(0, 0);
      }
   }

   private static class InjectMethodMappable {
      private final CommonData data;
      private final MemberInfo info;
      private final List<TrClass> targets;
      protected final Set<MemberInfo> knownTargetMethods;

      InjectMethodMappable(CommonData data, MemberInfo info, List<String> targets, Set<MemberInfo> knownTargetMethods) {
         this.data = Objects.requireNonNull(data);
         this.info = Objects.requireNonNull(info);
         this.knownTargetMethods = Objects.requireNonNull(knownTargetMethods);
         if (info.getOwner().isEmpty()) {
            this.targets = Objects.requireNonNull(targets)
               .stream()
               .map(data.resolver::resolveClass)
               .filter(Optional::isPresent)
               .map(Optional::get)
               .collect(Collectors.toList());
         } else {
            this.targets = data.resolver.resolveClass(info.getOwner()).map(Collections::singletonList).orElse(Collections.emptyList());
         }
      }

      private List<TrMethod> resolvePartials(TrClass owner, String name, String desc) {
         Objects.requireNonNull(owner);
         name = name.isEmpty() ? null : name;
         desc = desc.isEmpty() ? null : desc;
         Collection<TrMethod> col = owner.resolveMethods(name, desc, false, null, null);
         return col instanceof List ? (List)col : new ArrayList<>(col);
      }

      public List<MemberInfo> result() {
         String mappedOwner = this.info.getOwner();
         if (!mappedOwner.isEmpty()) {
            mappedOwner = this.data.mapper.asTrRemapper().map(mappedOwner);
         }

         Pair<Integer, Integer> parsedQuantifier = CommonInjectionAnnotationVisitor.parseQuantifier(this.data, this.info.getQuantifier());
         int quantifierMin = parsedQuantifier.first();
         int methodsPerTarget = parsedQuantifier.second();
         if (!this.targets.isEmpty() && !this.info.getName().isEmpty() && methodsPerTarget > 0) {
            Map<Pair<String, String>, Set<TrClass>> fullMethodToTarget = new HashMap<>();
            SortedMap<String, SortedSet<String>> namesToDesc = new TreeMap<>();

            for (TrClass target : this.targets) {
               List<TrMethod> methods = this.resolvePartials(target, this.info.getName(), this.info.getDesc());
               int matchedCount = Math.min(methods.size(), methodsPerTarget);

               for (int i = 0; i < matchedCount; i++) {
                  TrMember method = methods.get(i);
                  String mappedName = this.data.mapper.mapName(method);
                  String mappedDesc = this.data.mapper.mapDesc(method);
                  fullMethodToTarget.computeIfAbsent(Pair.of(mappedName, mappedDesc), k -> new HashSet<>()).add(target);
                  namesToDesc.computeIfAbsent(mappedName, k -> new TreeSet<>()).add(mappedDesc);
                  this.knownTargetMethods.add(new MemberInfo(target.getName(), method.getName(), "", method.getDesc()));
               }
            }

            if (fullMethodToTarget.isEmpty()) {
               this.data.getLogger().warn("Cannot remap %s because it does not exist in any of the targets %s", this.info.toString(), this.targets);
               return Collections.singletonList(this.info);
            }

            List<MemberInfo> list = new ArrayList<>();
            boolean explicitDesc = !this.info.getDesc().isEmpty();

            for (Entry<String, SortedSet<String>> entry : namesToDesc.entrySet()) {
               String mappedName = entry.getKey();
               SortedSet<String> mappedDescriptors = entry.getValue();
               if (!explicitDesc && this.canInject(mappedName, methodsPerTarget, fullMethodToTarget)) {
                  list.add(new MemberInfo(mappedOwner, mappedName, this.info.getQuantifier(), ""));
               } else {
                  for (String mappedDesc : mappedDescriptors) {
                     if (this.canInject(mappedName, mappedDesc, fullMethodToTarget)) {
                        String quantifier = this.info.getQuantifier();
                        if (!explicitDesc) {
                           if (quantifierMin > 0) {
                              this.data
                                 .getLogger()
                                 .error("Quantifier min was provided, but due to conflicts %s after remap had to use descriptor", this.info.toString());
                           }

                           quantifier = "";
                        }

                        list.add(new MemberInfo(mappedOwner, mappedName, quantifier, mappedDesc));
                     } else {
                        this.data
                           .getLogger()
                           .error(
                              "Unable to fully remap %s, the method %s%s could not be targeted without conflicts", this.info.toString(), mappedName, mappedDesc
                           );
                     }
                  }
               }
            }

            return list.isEmpty() ? Collections.singletonList(this.info) : list;
         } else {
            String desc = this.info.getDesc();
            if (!desc.isEmpty()) {
               desc = this.data.mapper.asTrRemapper().mapDesc(desc);
            }

            return Collections.singletonList(new MemberInfo(mappedOwner, this.info.getName(), this.info.getQuantifier(), desc));
         }
      }

      private boolean canInject(String mappedName, int methodsPerTarget, Map<Pair<String, String>, Set<TrClass>> fullMethodToTarget) {
         if (methodsPerTarget <= 0) {
            throw new IllegalArgumentException();
         }

         for (TrClass target : this.targets) {
            int toCheck = methodsPerTarget;

            for (TrMethod method : target.getMethods()) {
               String otherName = this.data.mapper.mapName(method);
               if (otherName.equals(mappedName)) {
                  String otherDesc = this.data.mapper.mapDesc(method);
                  Pair<String, String> pair = Pair.of(otherName, otherDesc);
                  Set<TrClass> validClasses = fullMethodToTarget.get(pair);
                  if (validClasses == null || !validClasses.contains(target)) {
                     return false;
                  }

                  if (--toCheck <= 0 && methodsPerTarget > 1) {
                     break;
                  }
               }
            }
         }

         return true;
      }

      private boolean canInject(String mappedName, String mappedDesc, Map<Pair<String, String>, Set<TrClass>> fullMethodToTarget) {
         Pair<String, String> pair = Pair.of(mappedName, mappedDesc);
         Set<TrClass> validClasses = fullMethodToTarget.get(pair);
         if (validClasses != null && !validClasses.isEmpty()) {
            for (TrClass target : this.targets) {
               TrMethod method = target.getMethod(mappedName, mappedDesc);
               if (method != null && !validClasses.contains(target)) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }
}
