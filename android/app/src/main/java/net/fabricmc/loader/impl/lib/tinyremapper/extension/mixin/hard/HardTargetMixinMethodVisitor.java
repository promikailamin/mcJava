package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.annotation.OverwriteAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.annotation.ShadowAnnotationVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

class HardTargetMixinMethodVisitor extends MethodVisitor {
   private final Collection<Consumer<CommonData>> data;
   private final MxMember method;
   private final List<String> targets;

   HardTargetMixinMethodVisitor(Collection<Consumer<CommonData>> data, MethodVisitor delegate, MxMember method, List<String> targets) {
      super(589824, delegate);
      this.data = Objects.requireNonNull(data);
      this.method = Objects.requireNonNull(method);
      this.targets = Objects.requireNonNull(targets);
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
      if ("Lorg/spongepowered/asm/mixin/Shadow;".equals(descriptor)) {
         av = new ShadowAnnotationVisitor(this.data, av, this.method, this.targets);
      } else if ("Lorg/spongepowered/asm/mixin/Overwrite;".equals(descriptor)) {
         av = new OverwriteAnnotationVisitor(this.data, av, this.method, this.targets);
      }

      return av;
   }
}
