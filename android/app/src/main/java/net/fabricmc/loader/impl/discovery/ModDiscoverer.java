package net.fabricmc.loader.impl.discovery;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.MetadataVerifier;
import net.fabricmc.loader.impl.metadata.ModMetadataParser;
import net.fabricmc.loader.impl.metadata.NestedJarEntry;
import net.fabricmc.loader.impl.metadata.ParseMetadataException;
import net.fabricmc.loader.impl.metadata.VersionOverrides;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public final class ModDiscoverer {
   private final VersionOverrides versionOverrides;
   private final DependencyOverrides depOverrides;
   private final List<ModCandidateFinder> candidateFinders = new ArrayList<>();
   private final EnvType envType = FabricLoaderImpl.INSTANCE.getEnvironmentType();
   private final Map<Long, ModDiscoverer.ModScanTask> jijDedupMap = new ConcurrentHashMap<>();
   private final List<ModDiscoverer.NestedModInitData> nestedModInitDatas = Collections.synchronizedList(new ArrayList<>());
   private final List<Path> nonFabricMods = Collections.synchronizedList(new ArrayList<>());

   public ModDiscoverer(VersionOverrides versionOverrides, DependencyOverrides depOverrides) {
      this.versionOverrides = versionOverrides;
      this.depOverrides = depOverrides;
   }

   public void addCandidateFinder(ModCandidateFinder f) {
      this.candidateFinders.add(f);
   }

   public List<ModCandidateImpl> discoverMods(FabricLoaderImpl loader, Map<String, Set<ModCandidateImpl>> envDisabledModsOut) throws ModResolutionException {
      long startTime = System.nanoTime();
      ForkJoinPool pool = new ForkJoinPool();
      Set<Path> processedPaths = new HashSet<>();
      List<Future<ModCandidateImpl>> futures = new ArrayList<>();
      ModCandidateFinder.ModCandidateConsumer taskSubmitter = (paths, requiresRemap) -> {
         List<Path> pendingPaths = new ArrayList<>(paths.size());

         for (Path path : paths) {
            assert path.equals(LoaderUtil.normalizeExistingPath(path));
            if (processedPaths.add(path)) {
               pendingPaths.add(path);
            }
         }

         if (!pendingPaths.isEmpty()) {
            futures.add(pool.submit(new ModDiscoverer.ModScanTask(pendingPaths, requiresRemap)));
         }
      };

      for (ModCandidateFinder finder : this.candidateFinders) {
         finder.findCandidates(taskSubmitter);
      }

      List<ModCandidateImpl> candidates = new ArrayList<>();

      for (GameProvider.BuiltinMod mod : loader.getGameProvider().getBuiltinMods()) {
         if (!(mod.metadata.getVersion() instanceof SemanticVersion)) {
            String error = String.format(
               "%s uses the non-semantic version %s, which doesn't support range comparisons and may cause mod dependencies against it to fail unexpectedly. Consider updating Fabric Loader or explicitly specifying the game version with the fabric.gameVersion system property.",
               mod.metadata.getId(),
               mod.metadata.getVersion()
            );
            if (loader.isDevelopmentEnvironment()) {
               throw new FormattedException("Invalid game version", error);
            }

            Log.warn(LogCategory.GENERAL, error);
         }

         ModCandidateImpl candidate = ModCandidateImpl.createBuiltin(mod, this.versionOverrides, this.depOverrides);
         candidates.add(MetadataVerifier.verifyIndev(candidate, loader.isDevelopmentEnvironment()));
      }

      candidates.add(MetadataVerifier.verifyIndev(this.createJavaMod(), loader.isDevelopmentEnvironment()));
      ModResolutionException exception = null;
      int timeout = Integer.getInteger("fabric.debug.discoveryTimeout", 60);
      if (timeout <= 0) {
         timeout = Integer.MAX_VALUE;
      }

      try {
         pool.shutdown();
         pool.awaitTermination(timeout, TimeUnit.SECONDS);

         for (Future<ModCandidateImpl> future : futures) {
            if (!future.isDone()) {
               throw new TimeoutException();
            }

            try {
               ModCandidateImpl candidate = future.get();
               if (candidate != null) {
                  candidates.add(candidate);
               }
            } catch (ExecutionException e) {
               exception = ExceptionUtil.gatherExceptions(e, exception, exc -> new ModResolutionException("Mod discovery failed!", exc));
            }
         }

         for (ModDiscoverer.NestedModInitData data : this.nestedModInitDatas) {
            for (Future<ModCandidateImpl> future : data.futures) {
               if (!future.isDone()) {
                  throw new TimeoutException();
               }

               try {
                  ModCandidateImpl candidate = future.get();
                  if (candidate != null) {
                     data.target.add(candidate);
                  }
               } catch (ExecutionException e) {
                  exception = ExceptionUtil.gatherExceptions(e, exception, exc -> new ModResolutionException("Mod discovery failed!", exc));
               }
            }
         }
      } catch (TimeoutException e) {
         throw new FormattedException(
            "Mod discovery took too long!",
            "Analyzing the mod folder contents took longer than %d seconds. This may be caused by unusually slow hardware, pathological antivirus interference or other issues. The timeout can be changed with the system property %s (-D%<s=<desired timeout in seconds>).",
            timeout,
            "fabric.debug.discoveryTimeout"
         );
      } catch (InterruptedException e) {
         throw new FormattedException("Mod discovery interrupted!", e);
      }

      if (exception != null) {
         throw exception;
      }

      Set<String> disabledModIds = findDisabledModIds();
      Set<ModCandidateImpl> ret = Collections.newSetFromMap(new IdentityHashMap<>(candidates.size() * 2));
      Queue<ModCandidateImpl> queue = new ArrayDeque<>(candidates);

      ModCandidateImpl mod;
      while ((mod = queue.poll()) != null) {
         if (!mod.getMetadata().loadsInEnvironment(this.envType)) {
            envDisabledModsOut.computeIfAbsent(mod.getId(), ignore -> Collections.newSetFromMap(new IdentityHashMap<>())).add(mod);
         } else if (disabledModIds.contains(mod.getId())) {
            Log.info(LogCategory.DISCOVERY, "Skipping disabled mod %s", mod.getId());
         } else if (ret.add(mod)) {
            for (ModCandidateImpl child : mod.getNestedMods()) {
               if (child.addParent(mod)) {
                  queue.add(child);
               }
            }
         }
      }

      long endTime = System.nanoTime();
      Log.debug(LogCategory.DISCOVERY, "Mod discovery time: %.1f ms", (endTime - startTime) * 1.0E-6);
      return new ArrayList<>(ret);
   }

   public List<Path> getNonFabricMods() {
      return Collections.unmodifiableList(this.nonFabricMods);
   }

   private static Set<String> findDisabledModIds() {
      String modIdList = System.getProperty("fabric.debug.disableModIds");
      if (modIdList == null) {
         return Collections.emptySet();
      }

      Set<String> disabledModIds = Arrays.stream(modIdList.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
      Log.debug(LogCategory.DISCOVERY, "Disabled mod ids: %s", disabledModIds);
      return disabledModIds;
   }

   private ModCandidateImpl createJavaMod() {
      ModMetadata metadata = new BuiltinModMetadata.Builder("java", System.getProperty("java.specification.version").replaceFirst("^1\\.", ""))
         .setName(System.getProperty("java.vm.name"))
         .build();
      GameProvider.BuiltinMod builtinMod = new GameProvider.BuiltinMod(Collections.singletonList(Paths.get(System.getProperty("java.home"))), metadata);
      return ModCandidateImpl.createBuiltin(builtinMod, this.versionOverrides, this.depOverrides);
   }

   private static boolean isValidNestedJarEntry(ZipEntry entry) {
      return entry != null && !entry.isDirectory() && entry.getName().endsWith(".jar");
   }

   static ByteBuffer readMod(InputStream is) throws IOException {
      int available = is.available();
      boolean availableGood = available > 1;
      byte[] buffer = new byte[availableGood ? available : 30000];
      int offset = 0;

      int len;
      while ((len = is.read(buffer, offset, buffer.length - offset)) >= 0) {
         offset += len;
         if (offset == buffer.length) {
            if (availableGood) {
               int val = is.read();
               if (val < 0) {
                  break;
               }

               availableGood = false;
               buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, 30000));
               buffer[offset++] = (byte)val;
            } else {
               buffer = Arrays.copyOf(buffer, buffer.length * 2);
            }
         }
      }

      return ByteBuffer.wrap(buffer, 0, offset);
   }

   final class ModScanTask extends RecursiveTask<ModCandidateImpl> {
      private final List<Path> paths;
      private final String localPath;
      private final ModDiscoverer.RewindableInputStream is;
      private final long hash;
      private final boolean requiresRemap;
      private final List<String> parentPaths;

      ModScanTask(List<Path> paths, boolean requiresRemap) {
         this(paths, null, null, -1L, requiresRemap, Collections.emptyList());
      }

      private ModScanTask(
         List<Path> paths, String localPath, ModDiscoverer.RewindableInputStream is, long hash, boolean requiresRemap, List<String> parentPaths
      ) {
         this.paths = paths;
         this.localPath = localPath != null ? localPath : paths.get(0).toString();
         this.is = is;
         this.hash = hash;
         this.requiresRemap = requiresRemap;
         this.parentPaths = parentPaths;
      }

      protected ModCandidateImpl compute() {
         if (this.is != null) {
            try {
               return this.computeJarStream();
            } catch (ParseMetadataException e) {
               throw ExceptionUtil.wrap(e);
            } catch (Throwable t) {
               throw new RuntimeException(String.format("Error analyzing nested jar %s from %s: %s", this.localPath, this.parentPaths, t), t);
            }
         } else {
            try {
               for (Path path : this.paths) {
                  ModCandidateImpl candidate;
                  if (Files.isDirectory(path)) {
                     candidate = this.computeDir(path);
                  } else {
                     candidate = this.computeJarFile(path);
                  }

                  if (candidate != null) {
                     return candidate;
                  }
               }

               return null;
            } catch (ParseMetadataException e) {
               throw ExceptionUtil.wrap(e);
            } catch (Throwable t) {
               throw new RuntimeException(String.format("Error analyzing %s: %s", this.paths, t), t);
            }
         }
      }

      private ModCandidateImpl computeDir(Path path) throws IOException, ParseMetadataException {
         Path modJson = path.resolve("fabric.mod.json");
         if (!Files.exists(modJson)) {
            return null;
         }

         InputStream is = Files.newInputStream(modJson);

         LoaderModMetadata metadata;
         try {
            metadata = this.parseMetadata(is, path.toString());
         } catch (Throwable var8) {
            if (is != null) {
               try {
                  is.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (is != null) {
            is.close();
         }

         return ModCandidateImpl.createPlain(this.paths, metadata, this.requiresRemap, Collections.emptyList());
      }

      private ModCandidateImpl computeJarFile(Path path) throws IOException, ParseMetadataException {
         final ZipFile zf = new ZipFile(path.toFile());

         Object var12;
         label72: {
            ModCandidateImpl var14;
            label71: {
               ModCandidateImpl var7;
               try {
                  ZipEntry entry = zf.getEntry("fabric.mod.json");
                  if (entry == null) {
                     ModDiscoverer.this.nonFabricMods.add(path);
                     var12 = null;
                     break label72;
                  }

                  InputStream is = zf.getInputStream(entry);

                  try {
                     metadata = this.parseMetadata(is, this.localPath);
                  } catch (Throwable var10) {
                     if (is != null) {
                        try {
                           is.close();
                        } catch (Throwable var9) {
                           var10.addSuppressed(var9);
                        }
                     }

                     throw var10;
                  }

                  if (is != null) {
                     is.close();
                  }

                  if (!metadata.loadsInEnvironment(ModDiscoverer.this.envType)) {
                     var14 = ModCandidateImpl.createPlain(this.paths, metadata, this.requiresRemap, Collections.emptyList());
                     break label71;
                  }

                  List<ModDiscoverer.ModScanTask> nestedModTasks;
                  if (metadata.getJars().isEmpty()) {
                     nestedModTasks = Collections.emptyList();
                  } else {
                     final Set<NestedJarEntry> nestedJarPaths = new HashSet<>(metadata.getJars());
                     nestedModTasks = this.computeNestedMods(new ModDiscoverer.ZipEntrySource() {
                        private final Iterator<NestedJarEntry> jarIt = nestedJarPaths.iterator();
                        private ZipEntry currentEntry;

                        @Override
                        public ZipEntry getNextEntry() throws IOException {
                           while (this.jarIt.hasNext()) {
                              NestedJarEntry jar = this.jarIt.next();
                              ZipEntry ret = zf.getEntry(jar.getFile());
                              if (ModDiscoverer.isValidNestedJarEntry(ret)) {
                                 this.currentEntry = ret;
                                 this.jarIt.remove();
                                 return ret;
                              }
                           }

                           this.currentEntry = null;
                           return null;
                        }

                        @Override
                        public ModDiscoverer.RewindableInputStream getInputStream() throws IOException {
                           InputStream isx = zf.getInputStream(this.currentEntry);

                           ModDiscoverer.RewindableInputStream var2;
                           try {
                              var2 = new ModDiscoverer.RewindableInputStream(isx);
                           } catch (Throwable var5) {
                              if (isx != null) {
                                 try {
                                    isx.close();
                                 } catch (Throwable var4) {
                                    var5.addSuppressed(var4);
                                 }
                              }

                              throw var5;
                           }

                           if (isx != null) {
                              isx.close();
                           }

                           return var2;
                        }
                     });
                     if (!nestedJarPaths.isEmpty() && FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment()) {
                        Log.warn(LogCategory.METADATA, "Mod %s %s references missing nested jars: %s", metadata.getId(), metadata.getVersion(), nestedJarPaths);
                     }
                  }

                  List<ModCandidateImpl> nestedMods;
                  if (nestedModTasks.isEmpty()) {
                     nestedMods = Collections.emptyList();
                  } else {
                     nestedMods = new ArrayList<>();
                     ModDiscoverer.this.nestedModInitDatas.add(new ModDiscoverer.NestedModInitData(nestedModTasks, nestedMods));
                  }

                  var7 = ModCandidateImpl.createPlain(this.paths, metadata, this.requiresRemap, nestedMods);
               } catch (Throwable var11) {
                  try {
                     zf.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }

                  throw var11;
               }

               zf.close();
               return var7;
            }

            zf.close();
            return var14;
         }

         zf.close();
         return (ModCandidateImpl)var12;
      }

      private ModCandidateImpl computeJarStream() throws IOException, ParseMetadataException {
         LoaderModMetadata metadata = null;
         ZipInputStream zis = new ZipInputStream(this.is);

         ZipEntry entry;
         try {
            while ((entry = zis.getNextEntry()) != null) {
               if (entry.getName().equals("fabric.mod.json")) {
                  metadata = this.parseMetadata(zis, this.localPath);
                  break;
               }
            }
         } catch (Throwable var12) {
            try {
               zis.close();
            } catch (Throwable var10) {
               var12.addSuppressed(var10);
            }

            throw var12;
         }

         zis.close();
         if (metadata == null) {
            return null;
         }

         if (!metadata.loadsInEnvironment(ModDiscoverer.this.envType)) {
            return ModCandidateImpl.createNested(this.localPath, this.hash, metadata, this.requiresRemap, Collections.emptyList());
         }

         Collection<NestedJarEntry> nestedJars = metadata.getJars();
         List<ModDiscoverer.ModScanTask> nestedModTasks;
         if (nestedJars.isEmpty()) {
            nestedModTasks = Collections.emptyList();
         } else {
            final Set<String> nestedJarPaths = new HashSet<>(nestedJars.size());

            for (NestedJarEntry nestedJar : nestedJars) {
               nestedJarPaths.add(nestedJar.getFile());
            }

            this.is.rewind();
            final ZipInputStream zisx = new ZipInputStream(this.is);

            try {
               nestedModTasks = this.computeNestedMods(new ModDiscoverer.ZipEntrySource() {
                  private ModDiscoverer.RewindableInputStream is;

                  @Override
                  public ZipEntry getNextEntry() throws IOException {
                     if (nestedJarPaths.isEmpty()) {
                        return null;
                     }

                     ZipEntry ret;
                     while ((ret = zis.getNextEntry()) != null) {
                        if (ModDiscoverer.isValidNestedJarEntry(ret) && nestedJarPaths.remove(ret.getName())) {
                           this.is = new ModDiscoverer.RewindableInputStream(zis);
                           return ret;
                        }
                     }

                     return null;
                  }

                  @Override
                  public ModDiscoverer.RewindableInputStream getInputStream() throws IOException {
                     return this.is;
                  }
               });
            } catch (Throwable var11) {
               try {
                  zisx.close();
               } catch (Throwable var9) {
                  var11.addSuppressed(var9);
               }

               throw var11;
            }

            zisx.close();
            if (!nestedJarPaths.isEmpty() && FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment()) {
               Log.warn(LogCategory.METADATA, "Mod %s %s references missing nested jars: %s", metadata.getId(), metadata.getVersion(), nestedJarPaths);
            }
         }

         List<ModCandidateImpl> nestedMods;
         if (nestedModTasks.isEmpty()) {
            nestedMods = Collections.emptyList();
         } else {
            nestedMods = new ArrayList<>();
            ModDiscoverer.this.nestedModInitDatas.add(new ModDiscoverer.NestedModInitData(nestedModTasks, nestedMods));
         }

         ModCandidateImpl ret = ModCandidateImpl.createNested(this.localPath, this.hash, metadata, this.requiresRemap, nestedMods);
         ret.setData(this.is.getBuffer());
         return ret;
      }

      private List<ModDiscoverer.ModScanTask> computeNestedMods(ModDiscoverer.ZipEntrySource entrySource) throws IOException {
         List<String> parentPaths = new ArrayList<>(this.parentPaths.size() + 1);
         parentPaths.addAll(this.parentPaths);
         parentPaths.add(this.localPath);
         List<ModDiscoverer.ModScanTask> tasks = new ArrayList<>(5);
         ModDiscoverer.ModScanTask localTask = null;

         ZipEntry entry;
         while ((entry = entrySource.getNextEntry()) != null) {
            long hash = ModCandidateImpl.hash(entry);
            ModDiscoverer.ModScanTask task = ModDiscoverer.this.jijDedupMap.get(hash);
            if (task == null) {
               task = ModDiscoverer.this.new ModScanTask(null, entry.getName(), entrySource.getInputStream(), hash, this.requiresRemap, parentPaths);
               ModDiscoverer.ModScanTask prev = ModDiscoverer.this.jijDedupMap.putIfAbsent(hash, task);
               if (prev != null) {
                  task = prev;
               } else if (localTask == null) {
                  localTask = task;
               } else {
                  task.fork();
               }
            }

            tasks.add(task);
         }

         if (tasks.isEmpty()) {
            return Collections.emptyList();
         }

         if (localTask != null) {
            localTask.invoke();
         }

         return tasks;
      }

      private LoaderModMetadata parseMetadata(InputStream is, String localPath) throws ParseMetadataException {
         return ModMetadataParser.parseMetadata(
            is,
            localPath,
            this.parentPaths,
            ModDiscoverer.this.versionOverrides,
            ModDiscoverer.this.depOverrides,
            FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment()
         );
      }
   }

   private static class NestedModInitData {
      final List<? extends Future<ModCandidateImpl>> futures;
      final List<ModCandidateImpl> target;

      NestedModInitData(List<? extends Future<ModCandidateImpl>> futures, List<ModCandidateImpl> target) {
         this.futures = futures;
         this.target = target;
      }
   }

   private static final class RewindableInputStream extends InputStream {
      private final ByteBuffer buffer;
      private int pos;

      RewindableInputStream(InputStream parent) throws IOException {
         this.buffer = ModDiscoverer.readMod(parent);
         assert this.buffer.hasArray() && this.buffer.arrayOffset() == 0 && this.buffer.position() == 0;
      }

      public ByteBuffer getBuffer() {
         return this.buffer;
      }

      public void rewind() {
         this.pos = 0;
      }

      @Override
      public int read() throws IOException {
         return this.pos >= this.buffer.limit() ? -1 : this.buffer.get(this.pos++) & 0xFF;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
         int rem = this.buffer.limit() - this.pos;
         if (rem <= 0) {
            return -1;
         }

         len = Math.min(len, rem);
         System.arraycopy(this.buffer.array(), this.pos, b, off, len);
         this.pos += len;
         return len;
      }
   }

   private interface ZipEntrySource {
      ZipEntry getNextEntry() throws IOException;

      ModDiscoverer.RewindableInputStream getInputStream() throws IOException;
   }
}
