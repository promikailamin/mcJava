package net.fabricmc.loader.impl.lib.classtweaker.api.visitor;

public interface AccessWidenerVisitor {
   default void visitClass(AccessWidenerVisitor.AccessType access, boolean transitive) {
   }

   default void visitMethod(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
   }

   default void visitField(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
   }

   enum AccessType {
      ACCESSIBLE("accessible"),
      EXTENDABLE("extendable"),
      MUTABLE("mutable");

      private final String id;

      AccessType(String id) {
         this.id = id;
      }

      @Override
      public String toString() {
         return this.id;
      }
   }
}
