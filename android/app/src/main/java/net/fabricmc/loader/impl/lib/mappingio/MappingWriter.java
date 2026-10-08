package net.fabricmc.loader.impl.lib.mappingio;

import java.io.Closeable;
import java.io.IOException;

public interface MappingWriter extends Closeable, MappingVisitor {
   @Override
   default boolean visitEnd() throws IOException {
      this.close();
      return true;
   }
}
