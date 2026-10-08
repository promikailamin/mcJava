package net.fabricmc.loader.impl.lib.tinyremapper;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public abstract class VisitTrackingClassRemapper extends ClassRemapper {
   public VisitTrackingClassRemapper(ClassVisitor classVisitor, Remapper remapper) {
      super(classVisitor, remapper);
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.INITIAL);
      super.visit(version, access, name, signature, superName, interfaces);
   }

   public void visitSource(String source, String debug) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.SOURCE);
      super.visitSource(source, debug);
   }

   public ModuleVisitor visitModule(String name, int access, String version) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.MODULE);
      return super.visitModule(name, access, version);
   }

   public void visitNestHost(String nestHost) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.NEST_HOST);
      super.visitNestHost(nestHost);
   }

   public void visitPermittedSubclass(String permittedSubclass) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.PERMITTED_SUBCLASS);
      super.visitPermittedSubclass(permittedSubclass);
   }

   public void visitOuterClass(String owner, String name, String descriptor) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.OUTER_CLASS);
      super.visitOuterClass(owner, name, descriptor);
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.ANNOTATION);
      return super.visitAnnotation(descriptor, visible);
   }

   public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.TYPE_ANNOTATION);
      return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
   }

   public void visitAttribute(Attribute attribute) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.ATTRIBUTE);
      super.visitAttribute(attribute);
   }

   public void visitNestMember(String nestMember) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.NEST_MEMBER);
      super.visitNestMember(nestMember);
   }

   public void visitInnerClass(String name, String outerName, String innerName, int access) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.INNER_CLASS);
      super.visitInnerClass(name, outerName, innerName, access);
   }

   public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.RECORD_COMPONENT);
      return super.visitRecordComponent(name, descriptor, signature);
   }

   public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.FIELD);
      return super.visitField(access, name, descriptor, signature, value);
   }

   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.METHOD);
      return super.visitMethod(access, name, descriptor, signature, exceptions);
   }

   public void visitEnd() {
      this.onVisit(VisitTrackingClassRemapper.VisitKind.END);
      super.visitEnd();
   }

   protected abstract void onVisit(VisitTrackingClassRemapper.VisitKind var1);

   protected enum VisitKind {
      INITIAL,
      SOURCE,
      MODULE,
      NEST_HOST,
      PERMITTED_SUBCLASS,
      OUTER_CLASS,
      ANNOTATION,
      TYPE_ANNOTATION,
      ATTRIBUTE,
      NEST_MEMBER,
      INNER_CLASS,
      RECORD_COMPONENT,
      FIELD,
      METHOD,
      END;
   }
}
