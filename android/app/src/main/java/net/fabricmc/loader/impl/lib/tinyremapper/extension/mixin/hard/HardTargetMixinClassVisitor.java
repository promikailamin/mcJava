package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.MapUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxClass;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.annotation.ImplementsAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.annotation.MixinAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.data.SoftInterface;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class HardTargetMixinClassVisitor extends ClassVisitor {
   private final Collection<Consumer<CommonData>> tasks;
   private MxClass _class;
   private final List<String> targets = new ArrayList<>();
   private final List<SoftInterface> interfaces = new ArrayList<>();

   public HardTargetMixinClassVisitor(Collection<Consumer<CommonData>> tasks, ClassVisitor delegate) {
      super(589824, delegate);
      this.tasks = Objects.requireNonNull(tasks);
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this._class = new MxClass(name);
      super.visit(version, access, name, signature, superName, interfaces);
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
      if ("Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) {
         av = new MixinAnnotationVisitor(av, this.targets);
      } else if ("Lorg/spongepowered/asm/mixin/Implements;".equals(descriptor)) {
         av = new ImplementsAnnotationVisitor(av, this.interfaces);
      }

      return av;
   }

   public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
      FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
      MxMember field = this._class.getField(name, descriptor);
      return this.targets.isEmpty() ? fv : new HardTargetMixinFieldVisitor(this.tasks, fv, field, Collections.unmodifiableList(this.targets));
   }

   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
      MxMember method = this._class.getMethod(name, descriptor);
      if (!this.interfaces.isEmpty() && !MapUtility.IGNORED_NAME.contains(name)) {
         ImplementsAnnotationVisitor.visitMethod(this.tasks, method, this.interfaces);
      }

      return this.targets.isEmpty() ? mv : new HardTargetMixinMethodVisitor(this.tasks, mv, method, Collections.unmodifiableList(this.targets));
   }
}
