package net.fabricmc.loader.impl.game;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import net.fabricmc.loader.impl.lib.tinyremapper.InputTag;
import net.fabricmc.loader.impl.lib.tinyremapper.NonClassCopyMode;
import net.fabricmc.loader.impl.lib.tinyremapper.OutputConsumerPath;
import net.fabricmc.loader.impl.lib.tinyremapper.TinyRemapper;
import net.fabricmc.loader.impl.lib.tinyremapper.TinyUtils;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.UrlConversionException;
import net.fabricmc.loader.impl.util.UrlUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.TinyRemapperLoggerAdapter;
import org.jetbrains.annotations.Nullable;

public final class GameProviderHelper {
   private static boolean emittedInfo = false;

   private GameProviderHelper() {
   }

   public static Path getCommonGameJar() {
      return getGameJar("fabric.gameJarPath");
   }

   public static Path getEnvGameJar(EnvType env) {
      return getGameJar(env == EnvType.CLIENT ? "fabric.gameJarPath.client" : "fabric.gameJarPath.server");
   }

   private static Path getGameJar(String property) {
      String val = System.getProperty(property);
      if (val == null) {
         return null;
      } else {
         Path path = Paths.get(val);
         if (!Files.exists(path)) {
            throw new RuntimeException(
               "Game jar " + path + " (" + LoaderUtil.normalizePath(path) + ") configured through " + property + " system property doesn't exist"
            );
         } else {
            return LoaderUtil.normalizeExistingPath(path);
         }
      }
   }

   @Nullable
   public static List<Path> getLibraries(String property) {
      String value = System.getProperty(property);
      if (value == null) {
         return null;
      }

      List<Path> ret = new ArrayList<>();

      for (String pathStr : value.split(File.pathSeparator)) {
         if (!pathStr.isEmpty()) {
            if (!pathStr.startsWith("@")) {
               addLibrary(pathStr, ret);
            } else {
               Path path = Paths.get(pathStr.substring(1));
               if (!Files.isRegularFile(path)) {
                  Log.warn(LogCategory.GAME_PROVIDER, "Skipping missing/invalid library list file %s", path);
               } else {
                  try {
                     BufferedReader reader = Files.newBufferedReader(path);

                     String line;
                     try {
                        while ((line = reader.readLine()) != null) {
                           line = line.trim();
                           if (!line.isEmpty()) {
                              addLibrary(line, ret);
                           }
                        }
                     } catch (Throwable var12) {
                        if (reader != null) {
                           try {
                              reader.close();
                           } catch (Throwable var11) {
                              var12.addSuppressed(var11);
                           }
                        }

                        throw var12;
                     }

                     if (reader != null) {
                        reader.close();
                     }
                  } catch (IOException e) {
                     throw new RuntimeException(String.format("Error reading library list file %s", path), e);
                  }
               }
            }
         }
      }

      return ret;
   }

   public static void addLibrary(String pathStr, List<Path> out) {
      Path path = LoaderUtil.normalizePath(Paths.get(pathStr));
      if (!Files.exists(path)) {
         Log.warn(LogCategory.GAME_PROVIDER, "Skipping missing library path %s", path);
      } else {
         out.add(path);
      }
   }

   public static Optional<Path> getSource(ClassLoader loader, String filename) {
      URL url;
      if ((url = loader.getResource(filename)) != null) {
         try {
            return Optional.of(UrlUtil.getCodeSource(url, filename));
         } catch (UrlConversionException e) {
            e.printStackTrace();
         }
      }

      return Optional.empty();
   }

   public static List<Path> getSources(ClassLoader loader, String filename) {
      try {
         Enumeration<URL> urls = loader.getResources(filename);
         List<Path> paths = new ArrayList<>();

         while (urls.hasMoreElements()) {
            URL url = urls.nextElement();

            try {
               paths.add(UrlUtil.getCodeSource(url, filename));
            } catch (UrlConversionException e) {
               e.printStackTrace();
            }
         }

         return paths;
      } catch (IOException e) {
         e.printStackTrace();
         return Collections.emptyList();
      }
   }

   public static GameProviderHelper.FindResult findFirst(List<Path> paths, Map<Path, ZipFile> zipFiles, boolean isClassName, String... names) {
      for (String name : names) {
         String file = isClassName ? LoaderUtil.getClassFileName(name) : name;

         for (Path path : paths) {
            if (Files.isDirectory(path)) {
               if (Files.exists(path.resolve(file))) {
                  return new GameProviderHelper.FindResult(name, path);
               }
            } else {
               ZipFile zipFile = zipFiles.get(path);
               if (zipFile == null) {
                  try {
                     zipFile = new ZipFile(path.toFile());
                     zipFiles.put(path, zipFile);
                  } catch (IOException e) {
                     throw new RuntimeException("Error reading " + path, e);
                  }
               }

               if (zipFile.getEntry(file) != null) {
                  return new GameProviderHelper.FindResult(name, path);
               }
            }
         }
      }

      return null;
   }

   public static Map<String, Path> deobfuscate(
      Map<String, Path> inputFileMap, String sourceNamespace, String gameId, String gameVersion, Path gameDir, FabricLauncher launcher
   ) {
      Log.debug(LogCategory.GAME_REMAP, "Requesting deobfuscation of %s", inputFileMap);
      MappingConfiguration mappingConfig = launcher.getMappingConfiguration();
      String targetNamespace = mappingConfig.getRuntimeNamespace();
      if (sourceNamespace.equals(targetNamespace)) {
         return inputFileMap;
      }

      if (!mappingConfig.matches(gameId, gameVersion)) {
         String mappingsGameId = mappingConfig.getGameId();
         String mappingsGameVersion = mappingConfig.getGameVersion();
         throw new FormattedException(
            "Incompatible mappings",
            String.format(
               "Supplied mappings for %s %s are incompatible with %s %s, this is likely caused by launcher misbehavior",
               mappingsGameId != null ? mappingsGameId : "(unknown)",
               mappingsGameVersion != null ? mappingsGameVersion : "(unknown)",
               gameId,
               gameVersion
            )
         );
      }

      List<String> namespaces = mappingConfig.getNamespaces();
      if (namespaces != null && namespaces.contains(sourceNamespace) && namespaces.contains(targetNamespace)) {
         if (namespaces.contains(targetNamespace) && namespaces.contains(sourceNamespace)) {
            String mappingName = mappingConfig.getMappingName();
            Path deobfJarDir = getDeobfJarDir(gameDir, gameId, gameVersion);
            List<Path> inputFiles = new ArrayList<>(inputFileMap.size());
            List<Path> outputFiles = new ArrayList<>(inputFileMap.size());
            List<Path> tmpFiles = new ArrayList<>(inputFileMap.size());
            Map<String, Path> ret = new HashMap<>(inputFileMap.size());
            boolean anyMissing = false;

            for (Entry<String, Path> entry : inputFileMap.entrySet()) {
               String name = entry.getKey();
               Path inputFile = entry.getValue();
               String deobfJarFilename = mappingName == null
                  ? String.format("%s-%s.jar", name, targetNamespace)
                  : String.format("%s-%s-%s.jar", name, targetNamespace, mappingName);
               Path outputFile = deobfJarDir.resolve(deobfJarFilename);
               Path tmpFile = deobfJarDir.resolve(deobfJarFilename + ".tmp");
               if (Files.exists(tmpFile)) {
                  Log.warn(
                     LogCategory.GAME_REMAP,
                     "Incomplete remapped file found! This means that the remapping process failed on the previous launch. If this persists, make sure to let us at Fabric know!"
                  );

                  try {
                     Files.deleteIfExists(outputFile);
                     Files.deleteIfExists(tmpFile);
                  } catch (IOException e) {
                     throw new RuntimeException("can't delete incompletely remapped files", e);
                  }
               }

               inputFiles.add(inputFile);
               outputFiles.add(outputFile);
               tmpFiles.add(tmpFile);
               ret.put(name, outputFile);
               if (!anyMissing && !Files.exists(outputFile)) {
                  anyMissing = true;
               }
            }

            if (!anyMissing) {
               Log.debug(LogCategory.GAME_REMAP, "Remapped files exist already, reusing them");
               return ret;
            }

            Log.debug(LogCategory.GAME_REMAP, "Fabric mapping file detected, applying...");
            if (!emittedInfo) {
               Log.info(LogCategory.GAME_REMAP, "Fabric is preparing JARs on first launch, this may take a few seconds...");
               emittedInfo = true;
            }

            try {
               Files.createDirectories(deobfJarDir);
               deobfuscate0(inputFiles, outputFiles, tmpFiles, mappingConfig.getMappings(), sourceNamespace, targetNamespace, launcher);
               return ret;
            } catch (IOException e) {
               throw new RuntimeException("error remapping game jars " + inputFiles, e);
            }
         } else {
            Log.debug(LogCategory.GAME_REMAP, "Missing namespace in mappings, using input files");
            return inputFileMap;
         }
      } else {
         Log.debug(LogCategory.GAME_REMAP, "No mappings, using input files");
         return inputFileMap;
      }
   }

   private static Path getDeobfJarDir(Path gameDir, String gameId, String gameVersion) {
      Path ret = gameDir.resolve(".fabric").resolve("remappedJars");
      StringBuilder versionDirName = new StringBuilder();
      if (!gameId.isEmpty()) {
         versionDirName.append(gameId);
      }

      if (!gameVersion.isEmpty()) {
         if (versionDirName.length() > 0) {
            versionDirName.append('-');
         }

         versionDirName.append(gameVersion);
      }

      if (versionDirName.length() > 0) {
         versionDirName.append('-');
      }

      versionDirName.append("0.19.5");
      return ret.resolve(versionDirName.toString().replaceAll("[^\\w\\-\\. ]+", "_"));
   }

   private static void deobfuscate0(
      List<Path> inputFiles,
      List<Path> outputFiles,
      List<Path> tmpFiles,
      MappingTree mappings,
      String sourceNamespace,
      String targetNamespace,
      FabricLauncher launcher
   ) throws IOException {
      TinyRemapper remapper = TinyRemapper.newRemapper(new TinyRemapperLoggerAdapter(LogCategory.GAME_REMAP))
         .withMappings(TinyUtils.createMappingProvider(mappings, sourceNamespace, targetNamespace))
         .rebuildSourceFilenames(true)
         .build();
      Set<Path> depPaths = new HashSet<>();
      if (SystemProperties.isSet("fabric.debug.deobfuscateWithClasspath")) {
         for (Path path : launcher.getClassPath()) {
            if (!inputFiles.contains(path)) {
               depPaths.add(path);
               Log.debug(LogCategory.GAME_REMAP, "Appending '%s' to remapper classpath", path);
               remapper.readClassPathAsync(path);
            }
         }
      }

      List<OutputConsumerPath> outputConsumers = new ArrayList<>(inputFiles.size());
      List<InputTag> inputTags = new ArrayList<>(inputFiles.size());

      try {
         for (int i = 0; i < inputFiles.size(); i++) {
            Path inputFile = inputFiles.get(i);
            Path tmpFile = tmpFiles.get(i);
            InputTag inputTag = remapper.createInputTag();
            OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(tmpFile).assumeArchive(true).build();
            outputConsumers.add(outputConsumer);
            inputTags.add(inputTag);
            outputConsumer.addNonClassFiles(inputFile, NonClassCopyMode.FIX_META_INF, remapper);
            remapper.readInputsAsync(inputTag, inputFile);
         }

         for (int i = 0; i < inputFiles.size(); i++) {
            remapper.apply(outputConsumers.get(i), inputTags.get(i));
         }
      } finally {
         for (OutputConsumerPath outputConsumer : outputConsumers) {
            outputConsumer.close();
         }

         remapper.finish();
      }

      depPaths.addAll(tmpFiles);

      for (Path p : depPaths) {
         try {
            p.getFileSystem().close();
         } catch (Exception var28) {
         }

         try {
            FileSystems.getFileSystem(new URI("jar:" + p.toUri())).close();
         } catch (Exception var27) {
         }
      }

      List<Path> missing = new ArrayList<>();

      for (int i = 0; i < inputFiles.size(); i++) {
         Path inputFile = inputFiles.get(i);
         Path tmpFile = tmpFiles.get(i);
         Path outputFile = outputFiles.get(i);
         JarFile jar = new JarFile(tmpFile.toFile());

         boolean found;
         try {
            found = jar.stream().anyMatch(e -> e.getName().endsWith(".class"));
         } catch (Throwable var26) {
            try {
               jar.close();
            } catch (Throwable var25) {
               var26.addSuppressed(var25);
            }

            throw var26;
         }

         jar.close();
         if (!found) {
            missing.add(inputFile);
            Files.delete(tmpFile);
         } else {
            Files.move(tmpFile, outputFile);
         }
      }

      if (!missing.isEmpty()) {
         throw new RuntimeException("Generated deobfuscated JARs contain no classes: " + missing);
      }
   }

   public static final class FindResult {
      public final String name;
      public final Path path;

      FindResult(String name, Path path) {
         this.name = name;
         this.path = path;
      }
   }
}
