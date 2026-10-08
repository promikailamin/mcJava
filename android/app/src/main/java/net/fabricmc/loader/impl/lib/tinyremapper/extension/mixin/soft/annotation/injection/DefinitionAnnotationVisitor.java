package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.data.MemberInfo;
import org.objectweb.asm.AnnotationVisitor;

public class DefinitionAnnotationVisitor extends AnnotationVisitor {
   private final CommonData data;

   public DefinitionAnnotationVisitor(CommonData data, AnnotationVisitor delegate) {
      super(589824, Objects.requireNonNull(delegate));
      this.data = Objects.requireNonNull(data);
   }

   public AnnotationVisitor visitArray(String name) {
      AnnotationVisitor av = super.visitArray(name);
      switch (name) {
         case "method":
         case "field":
            return new DefinitionAnnotationVisitor.MemberRemappingVisitor(this.data, av);
         default:
            return av;
      }
   }

   private static class MemberRemappingVisitor extends AnnotationVisitor {
      private final CommonData data;

      MemberRemappingVisitor(CommonData data, AnnotationVisitor delegate) {
         super(589824, Objects.requireNonNull(delegate));
         this.data = Objects.requireNonNull(data);
      }

      public void visit(String name, Object value) {
         MemberInfo info = MemberInfo.parse(Objects.requireNonNull((String)value));
         if (info != null) {
            value = new AtMemberMappable(this.data, info).result().toString();
         }

         super.visit(name, value);
      }
   }
}
