package net.fabricmc.loader.impl.discovery;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class DirectoryModCandidateFinder implements ModCandidateFinder {
   private final Path path;
   private final boolean requiresRemap;

   public DirectoryModCandidateFinder(Path path, boolean requiresRemap) {
      this.path = LoaderUtil.normalizePath(path);
      this.requiresRemap = requiresRemap;
   }

   @Override
   public void findCandidates(final ModCandidateFinder.ModCandidateConsumer out) {
      if (!Files.exists(this.path)) {
         try {
            Files.createDirectory(this.path);
         } catch (IOException e) {
            throw new RuntimeException("Could not create directory " + this.path, e);
         }
      } else {
         if (!Files.isDirectory(this.path)) {
            throw new RuntimeException(this.path + " is not a directory!");
         }

         try {
            Files.walkFileTree(this.path, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new SimpleFileVisitor<Path>() {
               public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                  if (DirectoryModCandidateFinder.isValidFile(file)) {
                     out.accept(file, DirectoryModCandidateFinder.this.requiresRemap);
                  }

                  return FileVisitResult.CONTINUE;
               }
            });
         } catch (IOException e) {
            throw new RuntimeException("Exception while searching for mods in '" + this.path + "'!", e);
         }
      }
   }

   static boolean isValidFile(Path path) {
      if (!Files.isRegularFile(path)) {
         return false;
      }

      try {
         if (Files.isHidden(path)) {
            return false;
         }
      } catch (IOException e) {
         Log.warn(LogCategory.DISCOVERY, "Error checking if file %s is hidden", path, e);
         return false;
      }

      String fileName = path.getFileName().toString();
      return fileName.endsWith(".jar") && !fileName.startsWith(".");
   }
}
