package net.fabricmc.loader.impl.discovery;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweaker;
import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweakerReader;
import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweakerWriter;
import net.fabricmc.loader.impl.lib.classtweaker.visitors.ClassTweakerRemapperVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.InputTag;
import net.fabricmc.loader.impl.lib.tinyremapper.NonClassCopyMode;
import net.fabricmc.loader.impl.lib.tinyremapper.OutputConsumerPath;
import net.fabricmc.loader.impl.lib.tinyremapper.TinyRemapper;
import net.fabricmc.loader.impl.lib.tinyremapper.TinyUtils;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.MixinExtension;
import net.fabricmc.loader.impl.util.FileSystemUtil;
import net.fabricmc.loader.impl.util.ManifestUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.TinyRemapperLoggerAdapter;
import org.objectweb.asm.commons.Remapper;

public final class RuntimeModRemapper {
   private static final String REMAP_TYPE_MANIFEST_KEY = "Fabric-Loom-Mixin-Remap-Type";
   private static final String REMAP_TYPE_MIXIN = "mixin";
   private static final String REMAP_TYPE_STATIC = "static";

   public static void remap(Collection<ModCandidateImpl> modCandidates, Path tmpDir, Path outputDir) {
      List<ModCandidateImpl> modsToRemap = new ArrayList<>();
      Set<InputTag> remapMixins = new HashSet<>();

      for (ModCandidateImpl mod : modCandidates) {
         if (mod.getRequiresRemap()) {
            modsToRemap.add(mod);
         }
      }

      if (!modsToRemap.isEmpty()) {
         MappingConfiguration config = FabricLauncherBase.getLauncher().getMappingConfiguration();
         String modNs = config.getDefaultModDistributionNamespace();
         String runtimeNs = config.getRuntimeNamespace();
         if (!modNs.equals(runtimeNs) && config.hasAnyMappings()) {
            Map<ModCandidateImpl, RuntimeModRemapper.RemapInfo> infoMap = new HashMap<>();
            TinyRemapper remapper = null;

            try {
               FabricLauncher launcher = FabricLauncherBase.getLauncher();
               ClassTweaker mergedClassTweaker = ClassTweaker.newInstance();
               mergedClassTweaker.visitHeader(modNs);

               for (ModCandidateImpl mod : modsToRemap) {
                  RuntimeModRemapper.RemapInfo info = new RuntimeModRemapper.RemapInfo();
                  infoMap.put(mod, info);
                  if (mod.hasPath()) {
                     List<Path> paths = mod.getPaths();
                     if (paths.size() != 1) {
                        throw new UnsupportedOperationException("multiple path for " + mod);
                     }

                     info.inputPath = paths.get(0);
                  } else {
                     info.inputPath = mod.copyToDir(tmpDir, true);
                     info.inputIsTemp = true;
                  }

                  info.outputPath = outputDir.resolve(mod.getDefaultFileName());
                  Files.deleteIfExists(info.outputPath);
                  String classTweaker = mod.getMetadata().getClassTweaker();
                  if (classTweaker != null) {
                     info.classTweakerPath = classTweaker;

                     try {
                        FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(info.inputPath, false);

                        try {
                           FileSystem fs = jarFs.get();
                           info.classTweaker = Files.readAllBytes(fs.getPath(classTweaker));
                        } catch (Throwable var40) {
                           if (jarFs != null) {
                              try {
                                 jarFs.close();
                              } catch (Throwable var37) {
                                 var40.addSuppressed(var37);
                              }
                           }

                           throw var40;
                        }

                        if (jarFs != null) {
                           jarFs.close();
                        }
                     } catch (Throwable t) {
                        throw new RuntimeException("Error reading class tweaker for mod '" + mod.getId() + "'!", t);
                     }

                     ClassTweakerReader.create(mergedClassTweaker).read(info.classTweaker, modNs);
                  }
               }

               remapper = TinyRemapper.newRemapper(new TinyRemapperLoggerAdapter(LogCategory.MOD_REMAP))
                  .withMappings(TinyUtils.createMappingProvider(launcher.getMappingConfiguration().getMappings(), modNs, runtimeNs))
                  .renameInvalidLocals(false)
                  .extension(new MixinExtension(remapMixins::contains))
                  .extraAnalyzeVisitor((mrjVersion, className, next) -> mergedClassTweaker.createClassVisitor(589824, next, null))
                  .build();

               try {
                  remapper.readClassPathAsync(getRemapClasspath().toArray(new Path[0]));
               } catch (IOException e) {
                  throw new RuntimeException("Failed to populate remap classpath", e);
               }

               String defaultMixinRemapType = System.getProperty("fabric.defaultMixinRemapType", "mixin");

               for (ModCandidateImpl mod : modsToRemap) {
                  RuntimeModRemapper.RemapInfo info = infoMap.get(mod);
                  InputTag tag = remapper.createInputTag();
                  info.tag = tag;
                  if (requiresMixinRemap(info.inputPath, defaultMixinRemapType)) {
                     remapMixins.add(tag);
                  }

                  remapper.readInputsAsync(tag, info.inputPath);
               }

               for (ModCandidateImpl mod : modsToRemap) {
                  RuntimeModRemapper.RemapInfo info = infoMap.get(mod);
                  OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(info.outputPath).build();
                  FileSystemUtil.FileSystemDelegate delegate = FileSystemUtil.getJarFileSystem(info.inputPath, false);
                  if (delegate.get() == null) {
                     throw new RuntimeException("Could not open JAR file " + info.inputPath.getFileName() + " for NIO reading!");
                  }

                  Path inputJar = delegate.get().getRootDirectories().iterator().next();
                  outputConsumer.addNonClassFiles(inputJar, NonClassCopyMode.FIX_META_INF, remapper);
                  info.outputConsumerPath = outputConsumer;
                  remapper.apply(outputConsumer, info.tag);
               }

               for (ModCandidateImpl mod : modsToRemap) {
                  RuntimeModRemapper.RemapInfo info = infoMap.get(mod);
                  if (info.classTweaker != null) {
                     info.classTweaker = remapClassTweaker(info.classTweaker, remapper.getEnvironment().getRemapper(), modNs, runtimeNs);
                  }
               }

               remapper.finish();

               for (ModCandidateImpl mod : modsToRemap) {
                  RuntimeModRemapper.RemapInfo info = infoMap.get(mod);
                  info.outputConsumerPath.close();
                  if (info.classTweakerPath != null) {
                     FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(info.outputPath, false);

                     try {
                        FileSystem fs = jarFs.get();
                        Files.delete(fs.getPath(info.classTweakerPath));
                        Files.write(fs.getPath(info.classTweakerPath), info.classTweaker);
                     } catch (Throwable var39) {
                        if (jarFs != null) {
                           try {
                              jarFs.close();
                           } catch (Throwable var36) {
                              var39.addSuppressed(var36);
                           }
                        }

                        throw var39;
                     }

                     if (jarFs != null) {
                        jarFs.close();
                     }
                  }

                  mod.setPaths(Collections.singletonList(info.outputPath));
               }
            } catch (Throwable t) {
               if (remapper != null) {
                  remapper.finish();
               }

               for (RuntimeModRemapper.RemapInfo info : infoMap.values()) {
                  if (info.outputPath != null) {
                     try {
                        Files.deleteIfExists(info.outputPath);
                     } catch (IOException e) {
                        Log.warn(LogCategory.MOD_REMAP, "Error deleting failed output jar %s", info.outputPath, e);
                     }
                  }
               }

               throw new FormattedException("Failed to remap mods!", t);
            } finally {
               for (RuntimeModRemapper.RemapInfo info : infoMap.values()) {
                  try {
                     if (info.inputIsTemp) {
                        Files.deleteIfExists(info.inputPath);
                     }
                  } catch (IOException e) {
                     Log.warn(LogCategory.MOD_REMAP, "Error deleting temporary input jar %s", info.inputIsTemp, e);
                  }
               }
            }
         }
      }
   }

   private static byte[] remapClassTweaker(byte[] input, Remapper remapper, String modNs, String runtimeNs) {
      ClassTweakerWriter writer = ClassTweakerWriter.create(4);
      ClassTweakerRemapperVisitor remappingDecorator = new ClassTweakerRemapperVisitor(writer, remapper, modNs, runtimeNs);
      ClassTweakerReader reader = ClassTweakerReader.create(remappingDecorator);
      reader.read(input, modNs);
      return writer.getOutput();
   }

   private static List<Path> getRemapClasspath() throws IOException {
      String remapClasspathFile = System.getProperty("fabric.remapClasspathFile");
      if (remapClasspathFile == null) {
         throw new RuntimeException("No remapClasspathFile provided");
      }

      String content = new String(Files.readAllBytes(Paths.get(remapClasspathFile)), StandardCharsets.UTF_8);
      return Arrays.stream(content.split(File.pathSeparator)).map(x$0 -> Paths.get(x$0)).collect(Collectors.toList());
   }

   private static boolean requiresMixinRemap(Path inputPath, String defaultMixinRemapType) throws IOException, URISyntaxException {
      Manifest manifest = ManifestUtil.readManifest(inputPath);
      if (manifest == null) {
         return false;
      }

      Attributes mainAttributes = manifest.getMainAttributes();
      String remapType = mainAttributes.getValue("Fabric-Loom-Mixin-Remap-Type");
      if (remapType == null) {
         remapType = defaultMixinRemapType;
      }

      return "static".equalsIgnoreCase(remapType);
   }

   private static class RemapInfo {
      InputTag tag;
      Path inputPath;
      Path outputPath;
      boolean inputIsTemp;
      OutputConsumerPath outputConsumerPath;
      String classTweakerPath;
      byte[] classTweaker;

      private RemapInfo() {
      }
   }
}
