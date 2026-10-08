package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import org.objectweb.asm.AnnotationVisitor;

public class DefinitionsAnnotationVisitor extends AnnotationVisitor {
   private final CommonData data;

   public DefinitionsAnnotationVisitor(CommonData data, AnnotationVisitor delegate) {
      super(589824, Objects.requireNonNull(delegate));
      this.data = Objects.requireNonNull(data);
   }

   public AnnotationVisitor visitArray(String name) {
      AnnotationVisitor av = super.visitArray(name);
      return name.equals("value") ? new DefinitionsAnnotationVisitor.DefinitionRemappingVisitor(this.data, av) : av;
   }

   private static class DefinitionRemappingVisitor extends AnnotationVisitor {
      private final CommonData data;

      DefinitionRemappingVisitor(CommonData data, AnnotationVisitor delegate) {
         super(589824, Objects.requireNonNull(delegate));
         this.data = Objects.requireNonNull(data);
      }

      public AnnotationVisitor visitAnnotation(String name, String descriptor) {
         return new DefinitionAnnotationVisitor(this.data, super.visitAnnotation(name, descriptor));
      }
   }
}
