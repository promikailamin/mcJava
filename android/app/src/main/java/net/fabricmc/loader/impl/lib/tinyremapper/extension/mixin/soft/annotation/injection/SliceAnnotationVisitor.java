package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import org.objectweb.asm.AnnotationVisitor;

public class SliceAnnotationVisitor extends AnnotationVisitor {
   private final CommonData data;
   private final List<String> targets;

   public SliceAnnotationVisitor(CommonData data, AnnotationVisitor delegate, List<String> targets) {
      super(589824, delegate);
      this.data = Objects.requireNonNull(data);
      this.targets = Objects.requireNonNull(targets);
   }

   public AnnotationVisitor visitAnnotation(String name, String descriptor) {
      AnnotationVisitor av = super.visitAnnotation(name, descriptor);
      if (name.equals("from") || name.equals("to")) {
         if (!descriptor.equals("Lorg/spongepowered/asm/mixin/injection/At;")) {
            throw new RuntimeException("Unexpected annotation " + descriptor);
         }

         av = new AtAnnotationVisitor(this.data, av, this.targets);
      }

      return av;
   }
}
