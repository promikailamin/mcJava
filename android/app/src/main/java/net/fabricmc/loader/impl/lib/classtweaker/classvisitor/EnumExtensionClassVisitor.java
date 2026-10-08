package net.fabricmc.loader.impl.lib.classtweaker.classvisitor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweaker;
import net.fabricmc.loader.impl.lib.classtweaker.api.EnumExtension;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class EnumExtensionClassVisitor extends ClassVisitor {
   private final ClassTweaker classTweaker;
   private final Set<String> addedConstants = new LinkedHashSet<>();
   private final List<FieldNode> existingConstants = new ArrayList<>();
   private final List<Runnable> postVisitTasks = new ArrayList<>();
   private Type currentType;

   public EnumExtensionClassVisitor(int api, ClassVisitor classVisitor, ClassTweaker classTweaker) {
      super(api, classVisitor);
      this.classTweaker = classTweaker;
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.currentType = Type.getObjectType(name);
      List<EnumExtension> enumExtensions = this.classTweaker.getEnumExtensions(name);
      if (enumExtensions.isEmpty()) {
         super.visit(version, access, name, signature, superName, interfaces);
      } else {
         for (EnumExtension extension : enumExtensions) {
            this.addedConstants.add(extension.getAddedConstant());
         }

         super.visit(version, access, name, signature, superName, interfaces);
      }
   }

   public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
      if (this.currentType.getDescriptor().equals(descriptor)) {
         this.addedConstants.remove(name);
      }

      FieldNode node = new FieldNode(access, name, descriptor, signature, value);
      if ((access & 16384) != 0) {
         this.existingConstants.add(node);
      } else {
         this.postVisitTasks.add(() -> node.accept(this.cv));
      }

      return node;
   }

   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      MethodNode node = new MethodNode(access, name, descriptor, signature, exceptions);
      this.postVisitTasks.add(() -> node.accept(this.cv));
      return node;
   }

   public void visitEnd() {
      if (this.cv != null) {
         for (FieldNode existingConstant : this.existingConstants) {
            existingConstant.accept(this.cv);
         }

         for (String addedConstant : this.addedConstants) {
            FieldVisitor visitor = super.visitField(16409, addedConstant, this.currentType.getDescriptor(), null, null);
            visitor.visitAttribute(new EnumExtensionClassVisitor.StubEnumConstantAttribute());
            visitor.visitEnd();
         }

         for (Runnable task : this.postVisitTasks) {
            task.run();
         }

         super.visitEnd();
      }
   }

   private static final class StubEnumConstantAttribute extends Attribute {
      StubEnumConstantAttribute() {
         super("org.spongepowered.asm.mixin.StubEnumConstant");
      }

      protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength, int maxStack, int maxLocals) {
         return new ByteVector();
      }
   }
}
