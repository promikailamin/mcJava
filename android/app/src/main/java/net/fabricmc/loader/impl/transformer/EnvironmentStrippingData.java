package net.fabricmc.loader.impl.transformer;

import java.util.Collection;
import java.util.HashSet;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvironmentInterface;
import net.fabricmc.api.EnvironmentInterfaces;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class EnvironmentStrippingData extends ClassVisitor {
   private static final String ENVIRONMENT_DESCRIPTOR = Type.getDescriptor(Environment.class);
   private static final String ENVIRONMENT_INTERFACE_DESCRIPTOR = Type.getDescriptor(EnvironmentInterface.class);
   private static final String ENVIRONMENT_INTERFACES_DESCRIPTOR = Type.getDescriptor(EnvironmentInterfaces.class);
   private final String envType;
   private boolean stripEntireClass = false;
   private final Collection<String> stripInterfaces = new HashSet<>();
   private final Collection<String> stripFields = new HashSet<>();
   private final Collection<String> stripMethods = new HashSet<>();

   private AnnotationVisitor visitMemberAnnotation(String descriptor, boolean visible, Runnable onEnvMismatch) {
      return ENVIRONMENT_DESCRIPTOR.equals(descriptor) ? new EnvironmentStrippingData.EnvironmentAnnotationVisitor(this.api, onEnvMismatch) : null;
   }

   public EnvironmentStrippingData(int api, String envType) {
      super(api);
      this.envType = envType;
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      if (ENVIRONMENT_DESCRIPTOR.equals(descriptor)) {
         return new EnvironmentStrippingData.EnvironmentAnnotationVisitor(this.api, () -> this.stripEntireClass = true);
      } else if (ENVIRONMENT_INTERFACE_DESCRIPTOR.equals(descriptor)) {
         return new EnvironmentStrippingData.EnvironmentInterfaceAnnotationVisitor(this.api);
      } else {
         return ENVIRONMENT_INTERFACES_DESCRIPTOR.equals(descriptor) ? new AnnotationVisitor(this.api) {
            public AnnotationVisitor visitArray(String name) {
               return "value".equals(name) ? new AnnotationVisitor(this.api) {
                  public AnnotationVisitor visitAnnotation(String name, String descriptorx) {
                     return EnvironmentStrippingData.this.new EnvironmentInterfaceAnnotationVisitor(this.api);
                  }
               } : null;
            }
         } : null;
      }
   }

   public FieldVisitor visitField(int access, final String name, final String descriptor, String signature, Object value) {
      return new FieldVisitor(this.api) {
         public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
            return EnvironmentStrippingData.this.visitMemberAnnotation(
               annotationDescriptor, visible, () -> EnvironmentStrippingData.this.stripFields.add(name + descriptor)
            );
         }
      };
   }

   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      final String methodId = name + descriptor;
      return new MethodVisitor(this.api) {
         public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return EnvironmentStrippingData.this.visitMemberAnnotation(descriptor, visible, () -> EnvironmentStrippingData.this.stripMethods.add(methodId));
         }
      };
   }

   public boolean stripEntireClass() {
      return this.stripEntireClass;
   }

   public Collection<String> getStripInterfaces() {
      return this.stripInterfaces;
   }

   public Collection<String> getStripFields() {
      return this.stripFields;
   }

   public Collection<String> getStripMethods() {
      return this.stripMethods;
   }

   public boolean isEmpty() {
      return this.stripInterfaces.isEmpty() && this.stripFields.isEmpty() && this.stripMethods.isEmpty();
   }

   private class EnvironmentAnnotationVisitor extends AnnotationVisitor {
      private final Runnable onEnvMismatch;

      private EnvironmentAnnotationVisitor(int api, Runnable onEnvMismatch) {
         super(api);
         this.onEnvMismatch = onEnvMismatch;
      }

      public void visitEnum(String name, String descriptor, String value) {
         if ("value".equals(name) && !EnvironmentStrippingData.this.envType.equals(value)) {
            this.onEnvMismatch.run();
         }
      }
   }

   private class EnvironmentInterfaceAnnotationVisitor extends AnnotationVisitor {
      private boolean envMismatch;
      private Type itf;

      private EnvironmentInterfaceAnnotationVisitor(int api) {
         super(api);
      }

      public void visitEnum(String name, String descriptor, String value) {
         if ("value".equals(name) && !EnvironmentStrippingData.this.envType.equals(value)) {
            this.envMismatch = true;
         }
      }

      public void visit(String name, Object value) {
         if ("itf".equals(name)) {
            this.itf = (Type)value;
         }
      }

      public void visitEnd() {
         if (this.envMismatch) {
            EnvironmentStrippingData.this.stripInterfaces.add(this.itf.getInternalName());
         }
      }
   }
}
