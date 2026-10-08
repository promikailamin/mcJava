package net.fabricmc.loader.impl.lib.mappingio;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public interface MappingVisitor {
   default Set<MappingFlag> getFlags() {
      return MappingFlag.NONE;
   }

   default void reset() {
      throw new UnsupportedOperationException();
   }

   default boolean visitHeader() throws IOException {
      return true;
   }

   void visitNamespaces(String var1, List<String> var2) throws IOException;

   default void visitMetadata(String key, @Nullable String value) throws IOException {
   }

   default boolean visitContent() throws IOException {
      return true;
   }

   boolean visitClass(String var1) throws IOException;

   boolean visitField(String var1, @Nullable String var2) throws IOException;

   boolean visitMethod(String var1, @Nullable String var2) throws IOException;

   boolean visitMethodArg(int var1, int var2, @Nullable String var3) throws IOException;

   boolean visitMethodVar(int var1, int var2, int var3, int var4, @Nullable String var5) throws IOException;

   default boolean visitEnd() throws IOException {
      return true;
   }

   void visitDstName(MappedElementKind var1, int var2, String var3) throws IOException;

   default void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
   }

   default boolean visitElementContent(MappedElementKind targetKind) throws IOException {
      return true;
   }

   void visitComment(MappedElementKind var1, String var2) throws IOException;
}
