package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.ResolveUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.data.MemberInfo;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.util.LocalsMapper;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.AnnotationNode;

public class ModifyVariableAnnotationVisitor extends AnnotationNode {
   private final CommonData data;
   private final AnnotationVisitor delegate;
   private final List<String> targets;
   private final Set<MemberInfo> knownTargetMethods;
   private final List<String> rawMethods = new ArrayList<>();

   public ModifyVariableAnnotationVisitor(CommonData data, AnnotationVisitor delegate, List<String> targets, Set<MemberInfo> knownTargetMethods) {
      super(589824, "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;");
      this.data = Objects.requireNonNull(data);
      this.delegate = Objects.requireNonNull(delegate);
      this.targets = Objects.requireNonNull(targets);
      this.knownTargetMethods = Objects.requireNonNull(knownTargetMethods);
   }

   public AnnotationVisitor visitArray(String name) {
      AnnotationVisitor av = super.visitArray(name);
      return name.equals("method") ? new AnnotationVisitor(589824, av) {
         public void visit(String name, Object value) {
            if (value != null) {
               ModifyVariableAnnotationVisitor.this.rawMethods.add((String)value);
            }

            super.visit(name, value);
         }
      } : av;
   }

   public void visitEnd() {
      this.accept(
         new ModifyVariableAnnotationVisitor.ModifyVariableSecondPassAnnotationVisitor(
            this.data, this.delegate, this.targets, this.rawMethods, this.knownTargetMethods
         )
      );
      super.visitEnd();
   }

   private static class ModifyVariableSecondPassAnnotationVisitor extends CommonInjectionAnnotationVisitor {
      private final List<String> rawMethods;
      private final List<TrClass> targets;
      private boolean visitedMethods = false;

      ModifyVariableSecondPassAnnotationVisitor(
         CommonData data, AnnotationVisitor delegate, List<String> targets, List<String> rawMethods, Set<MemberInfo> knownTargetMethods
      ) {
         super(data, delegate, targets, knownTargetMethods);
         this.rawMethods = rawMethods;
         this.targets = Objects.requireNonNull(targets)
            .stream()
            .map(data.resolver::resolveClass)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
      }

      @Override
      public AnnotationVisitor visitArray(String name) {
         if (name.equals("method")) {
            if (this.visitedMethods) {
               return null;
            }

            this.visitedMethods = true;
         }

         if (name.equals("name") && !this.visitedMethods) {
            AnnotationVisitor annotationVisitor = this.visitArray("method");
            if (annotationVisitor != null) {
               for (String rawMethod : this.rawMethods) {
                  annotationVisitor.visit(null, rawMethod);
               }

               annotationVisitor.visitEnd();
            }

            this.visitedMethods = true;
         }

         AnnotationVisitor av = super.visitArray(name);
         return name.equals("name")
            ? new AnnotationVisitor(589824, av) {
               public void visit(String name, Object value) {
                  String localName = ((String)Objects.requireNonNull((String)value)).replaceAll("\\s", "");
                  List<TrMethod> targetMethods = ModifyVariableSecondPassAnnotationVisitor.this.knownTargetMethods
                     .stream()
                     .map(
                        memberInfo -> ModifyVariableSecondPassAnnotationVisitor.this.data
                           .resolver
                           .resolveMethod(memberInfo.getOwner(), memberInfo.getName(), memberInfo.getDesc(), ResolveUtility.FLAG_UNIQUE)
                     )
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .collect(Collectors.toList());
                  List<String> collection = targetMethods.stream()
                     .map(m -> LocalsMapper.mapLocal(ModifyVariableSecondPassAnnotationVisitor.this.data, m, localName))
                     .sorted()
                     .distinct()
                     .collect(Collectors.toList());
                  if (collection.size() > 1) {
                     ModifyVariableSecondPassAnnotationVisitor.this.data.getLogger().error("Conflict mapping detected, %s -> %s.", localName, collection);
                  } else if (collection.isEmpty()) {
                     ModifyVariableSecondPassAnnotationVisitor.this.data
                        .getLogger()
                        .warn("Cannot remap %s because it does not exist in any of the targets %s", localName, targetMethods);
                  }

                  super.visit(name, collection.stream().findFirst().orElse(localName));
               }
            }
            : av;
      }
   }
}
