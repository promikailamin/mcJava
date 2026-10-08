package net.fabricmc.loader.impl.lib.classtweaker.api.visitor;

import org.jetbrains.annotations.Nullable;

public interface ClassTweakerVisitor {
   default void visitHeader(String namespace) {
   }

   @Nullable
   default AccessWidenerVisitor visitAccessWidener(String owner) {
      return null;
   }

   default void visitInjectedInterface(String owner, String iface, boolean transitive) {
   }

   default void visitEnumExtension(String owner, String addedConstant, boolean transitive) {
   }

   default void visitLineNumber(int lineNumber) {
   }
}
