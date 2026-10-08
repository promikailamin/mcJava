package net.fabricmc.loader.impl.transformer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class PackageAccessFixer extends ClassVisitor {
   private static int modAccess(int access) {
      return (access & 7) != 2 ? access & -8 | 1 : access;
   }

   public PackageAccessFixer(int api, ClassVisitor classVisitor) {
      super(api, classVisitor);
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      super.visit(version, modAccess(access), name, signature, superName, interfaces);
   }

   public void visitInnerClass(String name, String outerName, String innerName, int access) {
      super.visitInnerClass(name, outerName, innerName, modAccess(access));
   }

   public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
      return super.visitField(modAccess(access), name, descriptor, signature, value);
   }

   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      return super.visitMethod(modAccess(access), name, descriptor, signature, exceptions);
   }
}
