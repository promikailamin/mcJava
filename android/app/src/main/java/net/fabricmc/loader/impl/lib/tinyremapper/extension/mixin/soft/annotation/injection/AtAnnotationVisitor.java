package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.data.MemberInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.AnnotationNode;

class AtAnnotationVisitor extends AnnotationNode {
   private final CommonData data;
   private final AnnotationVisitor delegate;
   private final List<String> targets;
   private String value;

   AtAnnotationVisitor(CommonData data, AnnotationVisitor delegate, List<String> targets) {
      super(589824, "Lorg/spongepowered/asm/mixin/injection/At;");
      this.data = Objects.requireNonNull(data);
      this.delegate = Objects.requireNonNull(delegate);
      this.targets = Objects.requireNonNull(targets);
   }

   public void visit(String name, Object value) {
      if (name.equals("value")) {
         this.value = Objects.requireNonNull((String)value);
      }

      super.visit(name, value);
   }

   public void visitEnd() {
      this.accept(new AtAnnotationVisitor.AtSecondPassAnnotationVisitor(this.data, this.delegate, this.value, this.targets));
      super.visitEnd();
   }

   private static class AtConstructorMappable {
      private final CommonData data;
      private final MemberInfo info;

      AtConstructorMappable(CommonData data, MemberInfo info) {
         this.data = Objects.requireNonNull(data);
         this.info = Objects.requireNonNull(info);
      }

      public MemberInfo result() {
         if (this.info.getDesc().isEmpty()) {
            return new MemberInfo(this.data.mapper.asTrRemapper().map(this.info.getOwner()), this.info.getName(), this.info.getQuantifier(), "");
         } else {
            return this.info.getDesc().endsWith(")V")
               ? new MemberInfo(
                  this.data.mapper.asTrRemapper().map(this.info.getOwner()),
                  this.info.getName(),
                  this.info.getQuantifier(),
                  this.data.mapper.asTrRemapper().mapMethodDesc(this.info.getDesc())
               )
               : new MemberInfo(
                  this.info.getOwner(), this.info.getName(), this.info.getQuantifier(), this.data.mapper.asTrRemapper().mapMethodDesc(this.info.getDesc())
               );
         }
      }
   }

   private static class AtSecondPassAnnotationVisitor extends AnnotationVisitor {
      private final CommonData data;
      private final String value;
      private final List<String> targets;

      AtSecondPassAnnotationVisitor(CommonData data, AnnotationVisitor delegate, String value, List<String> targets) {
         super(589824, delegate);
         this.data = Objects.requireNonNull(data);
         this.value = Objects.requireNonNull(value);
         this.targets = Objects.requireNonNull(targets);
      }

      public void visit(String name, Object value) {
         if (name.equals("target")) {
            MemberInfo info = MemberInfo.parse(((String)Objects.requireNonNull((String)value)).replaceAll("\\s", ""));
            if (info != null) {
               if (this.value.equals("NEW")) {
                  value = new AtAnnotationVisitor.AtConstructorMappable(this.data, info).result().toString();
               } else {
                  value = new AtMemberMappable(this.data, info).result().toString();
               }
            }
         }

         super.visit(name, value);
      }

      public AnnotationVisitor visitAnnotation(String name, String descriptor) {
         AnnotationVisitor av = super.visitAnnotation(name, descriptor);
         if (name.equals("desc")) {
            if (!descriptor.equals("Lorg/spongepowered/asm/mixin/injection/Desc;")) {
               throw new RuntimeException("Unexpected annotation " + descriptor);
            }

            av = new DescAnnotationVisitor(this.targets, this.data, av, this.value.equals("FIELD") ? TrMember.MemberType.FIELD : TrMember.MemberType.METHOD);
         }

         return av;
      }

      public AnnotationVisitor visitArray(String name) {
         AnnotationVisitor av = super.visitArray(name);
         if (name.equals("args") && this.value.equals("NEW")) {
            String prefix = "class=";
            av = new AnnotationVisitor(589824, av) {
               public void visit(String name, Object value) {
                  String argument = Objects.requireNonNull((String)value);
                  MemberInfo info;
                  if (argument.startsWith("class=") && (info = MemberInfo.parse(argument.substring("class=".length()).replaceAll("\\s", ""))) != null) {
                     value = "class=" + new AtAnnotationVisitor.AtConstructorMappable(AtSecondPassAnnotationVisitor.this.data, info).result().toString();
                  }

                  super.visit(name, value);
               }
            };
         }

         return av;
      }
   }
}
