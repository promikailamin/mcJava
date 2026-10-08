package net.fabricmc.loader.impl.lib.mappingio;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public interface FlatMappingVisitor {
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

   boolean visitClass(String var1, @Nullable String[] var2) throws IOException;

   void visitClassComment(String var1, @Nullable String[] var2, String var3) throws IOException;

   boolean visitField(String var1, String var2, @Nullable String var3, @Nullable String[] var4, @Nullable String[] var5, @Nullable String[] var6) throws IOException;

   void visitFieldComment(
      String var1, String var2, @Nullable String var3, @Nullable String[] var4, @Nullable String[] var5, @Nullable String[] var6, String var7
   ) throws IOException;

   boolean visitMethod(String var1, String var2, @Nullable String var3, @Nullable String[] var4, @Nullable String[] var5, @Nullable String[] var6) throws IOException;

   void visitMethodComment(
      String var1, String var2, @Nullable String var3, @Nullable String[] var4, @Nullable String[] var5, @Nullable String[] var6, String var7
   ) throws IOException;

   boolean visitMethodArg(
      String var1,
      String var2,
      @Nullable String var3,
      int var4,
      int var5,
      @Nullable String var6,
      @Nullable String[] var7,
      @Nullable String[] var8,
      @Nullable String[] var9,
      String[] var10
   ) throws IOException;

   void visitMethodArgComment(
      String var1,
      String var2,
      @Nullable String var3,
      int var4,
      int var5,
      @Nullable String var6,
      @Nullable String[] var7,
      @Nullable String[] var8,
      @Nullable String[] var9,
      @Nullable String[] var10,
      String var11
   ) throws IOException;

   boolean visitMethodVar(
      String var1,
      String var2,
      @Nullable String var3,
      int var4,
      int var5,
      int var6,
      int var7,
      @Nullable String var8,
      @Nullable String[] var9,
      @Nullable String[] var10,
      @Nullable String[] var11,
      String[] var12
   ) throws IOException;

   void visitMethodVarComment(
      String var1,
      String var2,
      @Nullable String var3,
      int var4,
      int var5,
      int var6,
      int var7,
      @Nullable String var8,
      @Nullable String[] var9,
      @Nullable String[] var10,
      @Nullable String[] var11,
      @Nullable String[] var12,
      String var13
   ) throws IOException;

   default boolean visitEnd() throws IOException {
      return true;
   }
}
