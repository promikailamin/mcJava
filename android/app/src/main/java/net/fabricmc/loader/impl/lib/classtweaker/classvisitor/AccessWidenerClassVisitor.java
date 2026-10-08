package net.fabricmc.loader.impl.lib.classtweaker.classvisitor;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.impl.lib.classtweaker.api.AccessWidener;
import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweaker;
import net.fabricmc.loader.impl.lib.classtweaker.utils.EntryTriple;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.tree.MethodNode;

public final class AccessWidenerClassVisitor extends ClassVisitor {
   private final ClassTweaker classTweaker;
   private String className;
   private int classAccess;
   private AccessWidener accessWidener = null;
   private boolean shouldDeferRecordMethods;
   private final StringBuilder recordDescriptor = new StringBuilder("(");
   private final List<MethodNode> recordMethods = new ArrayList<>();

   public AccessWidenerClassVisitor(int api, ClassVisitor classVisitor, ClassTweaker classTweaker) {
      super(api, classVisitor);
      this.classTweaker = classTweaker;
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.className = name;
      this.classAccess = access;
      this.accessWidener = this.classTweaker.getAccessWidener(name);
      this.shouldDeferRecordMethods = (access & 65536) != 0;
      super.visit(version, this.accessWidener.getClassAccess().apply(access, name, this.classAccess), name, signature, superName, interfaces);
   }

   public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
      this.recordDescriptor.append(descriptor);
      return super.visitRecordComponent(name, descriptor, signature);
   }

   public void visitPermittedSubclass(String permittedSubclass) {
      AccessWidener.Access access = this.accessWidener.getClassAccess();
      if (!access.isExtendable()) {
         super.visitPermittedSubclass(permittedSubclass);
      }
   }

   public void visitInnerClass(String name, String outerName, String innerName, int access) {
      super.visitInnerClass(name, outerName, innerName, this.classTweaker.getAccessWidener(name).getClassAccess().apply(access, name, this.classAccess));
   }

   public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
      return super.visitField(
         this.accessWidener.getFieldAccess(new EntryTriple(this.className, name, descriptor)).apply(access, name, this.classAccess),
         name,
         descriptor,
         signature,
         value
      );
   }

   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      if (this.shouldDeferRecordMethods) {
         MethodNode constructor = new MethodNode(access, name, descriptor, signature, exceptions);
         this.recordMethods.add(constructor);
         return constructor;
      } else {
         return new AccessWidenerClassVisitor.AccessWidenerMethodVisitor(
            super.visitMethod(
               this.accessWidener.getMethodAccess(new EntryTriple(this.className, name, descriptor)).apply(access, name, this.classAccess),
               name,
               descriptor,
               signature,
               exceptions
            )
         );
      }
   }

   public void visitEnd() {
      if (!this.shouldDeferRecordMethods) {
         super.visitEnd();
      } else {
         this.shouldDeferRecordMethods = false;
         String canonicalDesc = this.recordDescriptor.append(")V").toString();

         for (MethodNode constructor : this.recordMethods) {
            if (constructor.desc.equals(canonicalDesc)) {
               constructor.access = this.accessWidener.getCanonicalConstructorAccess().apply(constructor.access, constructor.name, this.classAccess);
            }

            constructor.accept(this);
         }

         super.visitEnd();
      }
   }

   private class AccessWidenerMethodVisitor extends MethodVisitor {
      AccessWidenerMethodVisitor(MethodVisitor methodVisitor) {
         super(AccessWidenerClassVisitor.this.api, methodVisitor);
      }

      public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
         if (opcode == 183 && this.isTargetMethod(owner, name, descriptor)) {
            opcode = 182;
         }

         super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      }

      public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
         for (int i = 0; i < bootstrapMethodArguments.length; i++) {
            if (bootstrapMethodArguments[i] instanceof Handle) {
               Handle handle = (Handle)bootstrapMethodArguments[i];
               if (handle.getTag() == 7 && this.isTargetMethod(handle.getOwner(), handle.getName(), handle.getDesc())) {
                  bootstrapMethodArguments[i] = new Handle(5, handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
               }
            }
         }

         super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
      }

      private boolean isTargetMethod(String owner, String name, String descriptor) {
         return owner.equals(AccessWidenerClassVisitor.this.className)
            && !name.equals("<init>")
            && AccessWidenerClassVisitor.this.accessWidener.getMethodAccess(new EntryTriple(owner, name, descriptor)).isChanged();
      }
   }
}
