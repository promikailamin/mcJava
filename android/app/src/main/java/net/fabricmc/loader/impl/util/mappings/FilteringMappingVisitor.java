package net.fabricmc.loader.impl.util.mappings;

import java.io.IOException;
import net.fabricmc.loader.impl.lib.mappingio.MappedElementKind;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import net.fabricmc.loader.impl.lib.mappingio.adapter.ForwardingMappingVisitor;
import org.jetbrains.annotations.Nullable;

public class FilteringMappingVisitor extends ForwardingMappingVisitor {
   public FilteringMappingVisitor(MappingVisitor next) {
      super(next);
   }

   @Override
   public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
      return false;
   }

   @Override
   public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
   }
}
