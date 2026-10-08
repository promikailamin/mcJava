package net.fabricmc.loader.impl.discovery;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class ArgumentModCandidateFinder implements ModCandidateFinder {
   private final boolean requiresRemap;

   public ArgumentModCandidateFinder(boolean requiresRemap) {
      this.requiresRemap = requiresRemap;
   }

   @Override
   public void findCandidates(ModCandidateFinder.ModCandidateConsumer out) {
      String list = System.getProperty("fabric.addMods");
      if (list != null) {
         this.addMods(list, "system property", out);
      }

      list = FabricLoaderImpl.INSTANCE.getGameProvider().getArguments().remove("fabric.addMods");
      if (list != null) {
         this.addMods(list, "argument", out);
      }
   }

   private void addMods(String list, String source, ModCandidateFinder.ModCandidateConsumer out) {
      for (String pathStr : list.split(File.pathSeparator)) {
         if (!pathStr.isEmpty()) {
            if (!pathStr.startsWith("@")) {
               this.addMod(pathStr, source, out);
            } else {
               Path path = Paths.get(pathStr.substring(1));
               if (!Files.isRegularFile(path)) {
                  Log.warn(LogCategory.DISCOVERY, "Skipping missing/invalid %s provided mod list file %s", source, path);
               } else {
                  try {
                     BufferedReader reader = Files.newBufferedReader(path);

                     try {
                        String fileSource = String.format("%s file %s", source, path);

                        String line;
                        while ((line = reader.readLine()) != null) {
                           line = line.trim();
                           if (!line.isEmpty()) {
                              this.addMod(line, fileSource, out);
                           }
                        }
                     } catch (Throwable var13) {
                        if (reader != null) {
                           try {
                              reader.close();
                           } catch (Throwable var12) {
                              var13.addSuppressed(var12);
                           }
                        }

                        throw var13;
                     }

                     if (reader != null) {
                        reader.close();
                     }
                  } catch (IOException e) {
                     throw new RuntimeException(String.format("Error reading %s provided mod list file %s", source, path), e);
                  }
               }
            }
         }
      }
   }

   private void addMod(String pathStr, String source, final ModCandidateFinder.ModCandidateConsumer out) {
      final Path path = LoaderUtil.normalizePath(Paths.get(pathStr));
      if (!Files.exists(path)) {
         Log.warn(LogCategory.DISCOVERY, "Skipping missing %s provided mod path %s", source, path);
      } else if (Files.isDirectory(path)) {
         if (isHidden(path)) {
            Log.warn(LogCategory.DISCOVERY, "Ignoring hidden %s provided mod path %s", source, path);
            return;
         }

         if (Files.exists(path.resolve("fabric.mod.json"))) {
            out.accept(path, this.requiresRemap);
         } else {
            try {
               final List<String> skipped = new ArrayList<>();
               Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                     if (DirectoryModCandidateFinder.isValidFile(file)) {
                        out.accept(file, ArgumentModCandidateFinder.this.requiresRemap);
                     } else {
                        skipped.add(path.relativize(file).toString());
                     }

                     return FileVisitResult.CONTINUE;
                  }

                  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                     return ArgumentModCandidateFinder.isHidden(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                  }
               });
               if (!skipped.isEmpty()) {
                  Log.warn(
                     LogCategory.DISCOVERY,
                     "Incompatible files in %s provided mod directory %s (non-jar or hidden): %s",
                     source,
                     path,
                     String.join(", ", skipped)
                  );
               }
            } catch (IOException e) {
               Log.warn(LogCategory.DISCOVERY, "Error processing %s provided mod path %s: %s", source, path, e);
            }
         }
      } else if (!DirectoryModCandidateFinder.isValidFile(path)) {
         Log.warn(LogCategory.DISCOVERY, "Incompatible file in %s provided mod path %s (non-jar or hidden)", source, path);
      } else {
         out.accept(path, this.requiresRemap);
      }
   }

   private static boolean isHidden(Path path) {
      try {
         return path.getFileName().toString().startsWith(".") || Files.isHidden(path);
      } catch (IOException e) {
         Log.warn(LogCategory.DISCOVERY, "Error determining whether %s is hidden: %s", path, e);
         return true;
      }
   }
}
