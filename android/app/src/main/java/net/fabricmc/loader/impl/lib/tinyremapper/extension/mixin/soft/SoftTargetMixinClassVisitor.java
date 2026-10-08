package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxClass;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.MixinAnnotationVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class SoftTargetMixinClassVisitor extends ClassVisitor {
   private final CommonData data;
   private MxClass _class;
   private final List<String> targets = new ArrayList<>();

   public SoftTargetMixinClassVisitor(CommonData data, ClassVisitor delegate) {
      super(589824, delegate);
      this.data = Objects.requireNonNull(data);
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this._class = new MxClass(name);
      super.visit(version, access, name, signature, superName, interfaces);
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
      if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) {
         av = new MixinAnnotationVisitor(this.data, av, this.targets);
      }

      return av;
   }

   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
      MxMember method = this._class.getMethod(name, descriptor);
      return this.targets.isEmpty() ? mv : new SoftTargetMixinMethodVisitor(this.data, mv, method, Collections.unmodifiableList(this.targets));
   }
}
