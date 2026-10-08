package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrField;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.ResolveUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

class DescAnnotationVisitor extends AnnotationVisitor {
   private final List<String> targets;
   private final CommonData data;
   private final TrMember.MemberType expectedType;
   private List<Type> args;
   private Type owner;
   private Type ret;
   private String value;

   DescAnnotationVisitor(List<String> targets, CommonData data, AnnotationVisitor annotationVisitor, TrMember.MemberType expectedType) {
      super(589824, annotationVisitor);
      this.targets = targets;
      this.data = data;
      this.expectedType = Objects.requireNonNull(expectedType);
   }

   public void visit(String name, Object value) {
      if (name.equals("owner")) {
         this.owner = Objects.requireNonNull((Type)value);
         super.visit(name, value);
      } else if (name.equals("ret")) {
         this.ret = Objects.requireNonNull((Type)value);
         super.visit(name, value);
      } else if (name.equals("value")) {
         this.value = Objects.requireNonNull((String)value);
      } else {
         super.visit(name, value);
      }
   }

   public AnnotationVisitor visitArray(String name) {
      return name.equals("args") ? new AnnotationVisitor(589824, super.visitArray(name)) {
         private final List<Type> argArray = new ArrayList<>();

         public void visit(String name, Object value) {
            this.argArray.add(Objects.requireNonNull((Type)value));
            super.visit(name, value);
         }

         public void visitEnd() {
            DescAnnotationVisitor.this.args = Collections.unmodifiableList(this.argArray);
            super.visitEnd();
         }
      } : super.visitArray(name);
   }

   public void visitEnd() {
      Objects.requireNonNull(this.value);
      List<String> potentialOwners = this.targets;
      if (this.owner != null) {
         potentialOwners = Collections.singletonList(this.owner.getInternalName());
      }

      if (this.expectedType == TrMember.MemberType.METHOD) {
         String desc = "(";
         if (this.args != null) {
            for (Type arg : this.args) {
               desc = desc + arg.getDescriptor();
            }
         }

         desc = desc + ")";
         if (this.ret != null) {
            desc = desc + this.ret.getDescriptor();
         } else {
            desc = desc + "V";
         }

         String proposedName = null;

         for (String owner : potentialOwners) {
            Optional<TrMethod> resolved = this.data.resolver.resolveMethod(owner, this.value, desc, ResolveUtility.FLAG_RECURSIVE | ResolveUtility.FLAG_UNIQUE);
            if (resolved.isPresent()) {
               String remapped = this.data.mapper.mapName(resolved.get());
               if (proposedName == null) {
                  proposedName = remapped;
               } else if (!proposedName.equals(remapped)) {
                  this.data
                     .getLogger()
                     .error(
                        "Multiple conflicting mapping choices found for %s, which can be remapped to %s or %s. Such issues can be resolved by using fully qualified selectors.",
                        this.value + desc,
                        owner + "." + remapped + desc,
                        proposedName + desc
                     );
               }
            }
         }

         if (proposedName != null) {
            super.visit("value", proposedName);
         } else {
            super.visit("value", this.value);
         }
      } else if (this.expectedType == TrMember.MemberType.FIELD) {
         if (this.ret == null) {
            this.data.getLogger().warn("%s is not fully qualified.", this.owner + "." + this.value);
            super.visit("value", this.value);
            super.visitEnd();
            return;
         }

         String proposedName = null;
         String desc = this.ret.getDescriptor();

         for (String owner : potentialOwners) {
            Optional<TrField> resolved = this.data.resolver.resolveField(owner, this.value, desc, ResolveUtility.FLAG_RECURSIVE | ResolveUtility.FLAG_UNIQUE);
            if (resolved.isPresent()) {
               String remapped = this.data.mapper.mapName(resolved.get());
               if (proposedName == null) {
                  proposedName = remapped;
               } else if (!proposedName.equals(remapped)) {
                  this.data
                     .getLogger()
                     .error(
                        "Multiple conflicting mapping choices found for %s, which can be remapped to %s or %s. Such issues can be resolved by using fully qualified selectors.",
                        this.value + desc,
                        owner + "." + remapped + desc,
                        proposedName + desc
                     );
               }
            }
         }

         if (proposedName != null) {
            super.visit("value", proposedName);
         } else {
            super.visit("value", this.value);
         }
      }

      super.visitEnd();
   }
}
