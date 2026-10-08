package net.fabricmc.loader.impl.lib.tinyremapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum NonClassCopyMode {
   UNCHANGED(),
   FIX_META_INF(MetaInfFixer.INSTANCE),
   SKIP_META_INF(MetaInfRemover.INSTANCE);

   public final List<OutputConsumerPath.ResourceRemapper> remappers;

   NonClassCopyMode(OutputConsumerPath.ResourceRemapper... remappers) {
      this.remappers = Collections.unmodifiableList(Arrays.asList(remappers));
   }
}
