package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.annotation.ShadowAnnotationVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;

class HardTargetMixinFieldVisitor extends FieldVisitor {
   private final Collection<Consumer<CommonData>> tasks;
   private final MxMember field;
   private final List<String> targets;

   HardTargetMixinFieldVisitor(Collection<Consumer<CommonData>> tasks, FieldVisitor delegate, MxMember field, List<String> targets) {
      super(589824, delegate);
      this.tasks = Objects.requireNonNull(tasks);
      this.field = Objects.requireNonNull(field);
      this.targets = Objects.requireNonNull(targets);
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
      if ("Lorg/spongepowered/asm/mixin/Shadow;".equals(descriptor)) {
         av = new ShadowAnnotationVisitor(this.tasks, av, this.field, this.targets);
      }

      return av;
   }
}
