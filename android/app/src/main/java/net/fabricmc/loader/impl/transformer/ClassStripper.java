package net.fabricmc.loader.impl.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ClassStripper extends ClassVisitor {
   private final Collection<String> stripInterfaces;
   private final Collection<String> stripFields;
   private final Collection<String> stripMethods;
   private String className;

   public ClassStripper(int api, ClassVisitor classVisitor, Collection<String> stripInterfaces, Collection<String> stripFields, Collection<String> stripMethods) {
      super(api, classVisitor);
      this.stripInterfaces = stripInterfaces;
      this.stripFields = stripFields;
      this.stripMethods = stripMethods;
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.className = name;
      if (!this.stripInterfaces.isEmpty()) {
         List<String> interfacesList = new ArrayList<>();

         for (String itf : interfaces) {
            if (!this.stripInterfaces.contains(itf)) {
               interfacesList.add(itf);
            }
         }

         interfaces = interfacesList.toArray(new String[0]);
      }

      super.visit(version, access, name, signature, superName, interfaces);
   }

   public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
      return this.stripFields.contains(name + descriptor) ? null : super.visitField(access, name, descriptor, signature, value);
   }

   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      if (this.stripMethods.contains(name + descriptor)) {
         return null;
      }

      MethodVisitor ret = super.visitMethod(access, name, descriptor, signature, exceptions);
      if (this.stripFields.isEmpty()) {
         return ret;
      }

      final int opcodeToStrip;
      switch (name) {
         case "<clinit>":
            opcodeToStrip = 179;
            break;
         case "<init>":
            opcodeToStrip = 181;
            break;
         default:
            return ret;
      }

      return new MethodVisitor(this.api, ret) {
         public void visitFieldInsn(int opcode, String owner, String namex, String descriptorx) {
            if (opcode == opcodeToStrip && owner.equals(ClassStripper.this.className) && ClassStripper.this.stripFields.contains(namex + descriptorx)) {
               super.visitInsn(Type.getType(descriptorx).getSize() == 2 ? 88 : 87);
               if (opcode == 181) {
                  super.visitInsn(87);
               }
            } else {
               super.visitFieldInsn(opcode, owner, namex, descriptorx);
            }
         }
      };
   }
}
