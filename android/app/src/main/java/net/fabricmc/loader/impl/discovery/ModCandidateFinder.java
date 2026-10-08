package net.fabricmc.loader.impl.discovery;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@FunctionalInterface
interface ModCandidateFinder {
   void findCandidates(ModCandidateFinder.ModCandidateConsumer var1);

   interface ModCandidateConsumer {
      default void accept(Path path, boolean requiresRemap) {
         this.accept(Collections.singletonList(path), requiresRemap);
      }

      void accept(List<Path> var1, boolean var2);
   }
}
