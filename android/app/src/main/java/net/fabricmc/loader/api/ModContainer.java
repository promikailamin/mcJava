package net.fabricmc.loader.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;

public interface ModContainer {
   ModMetadata getMetadata();

   List<Path> getRootPaths();

   default Optional<Path> findPath(String file) {
      for (Path root : this.getRootPaths()) {
         Path path = root.resolve(file.replace("/", root.getFileSystem().getSeparator()));
         if (Files.exists(path)) {
            return Optional.of(path);
         }
      }

      return Optional.empty();
   }

   ModOrigin getOrigin();

   Optional<ModContainer> getContainingMod();

   Collection<ModContainer> getContainedMods();

   @Deprecated
   default Path getRoot() {
      return this.getRootPath();
   }

   @Deprecated
   Path getRootPath();

   @Deprecated
   Path getPath(String var1);
}
