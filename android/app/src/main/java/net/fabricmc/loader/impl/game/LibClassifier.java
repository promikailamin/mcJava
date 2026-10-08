package net.fabricmc.loader.impl.game;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.ManifestUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.UrlUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public final class LibClassifier<L extends Enum<L> & LibClassifier.LibraryType> {
   private static final boolean DEBUG = SystemProperties.isSet("fabric.debug.logLibClassification");
   private final List<L> libs;
   private final Map<L, Path> origins;
   private final Map<L, String> localPaths;
   private final Set<Path> systemLibraries = new HashSet<>();
   private final List<Path> unmatchedOrigins = new ArrayList<>();

   public LibClassifier(Class<L> cls, EnvType env, GameProvider gameProvider) throws IOException {
      L[] libs = cls.getEnumConstants();
      this.libs = new ArrayList<>(libs.length);
      this.origins = new EnumMap<>(cls);
      this.localPaths = new EnumMap<>(cls);

      for (L lib : libs) {
         if (lib.isApplicable(env)) {
            this.libs.add(lib);
         }
      }

      StringBuilder sb = DEBUG ? new StringBuilder() : null;
      List<Path> systemLibs = GameProviderHelper.getLibraries("fabric.systemLibraries");
      if (systemLibs != null) {
         for (Path lib : systemLibs) {
            assert lib.equals(LoaderUtil.normalizeExistingPath(lib));
            if (this.systemLibraries.add(lib) && DEBUG) {
               sb.append(String.format("\ud83c\uddf8 %s%n", lib));
            }
         }
      }

      boolean junitRun = SystemProperties.isSet("fabric.unitTest");

      for (LoaderLibrary lib : LoaderLibrary.values()) {
         if (lib.isApplicable(env, junitRun)) {
            if (lib.path != null) {
               Path path = LoaderUtil.normalizeExistingPath(lib.path);
               this.systemLibraries.add(path);
               if (DEBUG) {
                  sb.append(String.format("✅ %s %s%n", lib.name(), path));
               }
            } else if (DEBUG) {
               sb.append(String.format("❎ %s%n", lib.name()));
            }
         }
      }

      Path gameProviderPath = UrlUtil.getCodeSource(gameProvider.getClass());
      if (gameProviderPath != null) {
         gameProviderPath = LoaderUtil.normalizeExistingPath(gameProviderPath);
         if (this.systemLibraries.add(gameProviderPath) && DEBUG) {
            sb.append(String.format("✅ gameprovider %s%n", gameProviderPath));
         }
      } else if (DEBUG) {
         sb.append("❎ gameprovider");
      }

      if (DEBUG) {
         Log.info(LogCategory.LIB_CLASSIFICATION, "Loader/system libraries:%n%s", sb);
      }

      List<Path> gameLibs = GameProviderHelper.getLibraries("fabric.gameLibraries");
      if (gameLibs != null) {
         this.process(gameLibs, (L[])(new Enum[0]));
      }

      this.processManifestClassPath(LoaderLibrary.SERVER_LAUNCH, env, junitRun);
   }

   private void processManifestClassPath(LoaderLibrary lib, EnvType env, boolean junitRun) throws IOException {
      if (lib.path != null && lib.isApplicable(env, junitRun) && Files.isRegularFile(lib.path)) {
         ZipFile zf = new ZipFile(lib.path.toFile());

         label52: {
            Manifest manifest;
            try {
               ZipEntry entry = zf.getEntry("META-INF/MANIFEST.MF");
               if (entry == null) {
                  break label52;
               }

               manifest = new Manifest(zf.getInputStream(entry));
            } catch (Throwable var9) {
               try {
                  zf.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            zf.close();
            List<URL> cp = ManifestUtil.getClassPath(manifest, lib.path);
            if (cp == null) {
               return;
            }

            for (URL url : cp) {
               this.process(url);
            }

            return;
         }

         zf.close();
      }
   }

   public void process(URL url) throws IOException {
      this.process(UrlUtil.asPath(url), (L[])(new Enum[0]));
   }

   @SafeVarargs
   public final void process(Iterable<Path> paths, L... excludedLibs) throws IOException {
      Set<L> excluded = makeSet(excludedLibs);

      for (Path path : paths) {
         this.process(path, excluded);
      }
   }

   @SafeVarargs
   public final void process(Path path, L... excludedLibs) throws IOException {
      this.process(path, makeSet(excludedLibs));
   }

   private static <L extends Enum<L>> Set<L> makeSet(L[] libs) {
      if (libs.length == 0) {
         return Collections.emptySet();
      }

      Set<L> ret = EnumSet.of(libs[0]);

      for (int i = 1; i < libs.length; i++) {
         ret.add(libs[i]);
      }

      return ret;
   }

   private void process(Path path, Set<L> excludedLibs) throws IOException {
      path = LoaderUtil.normalizeExistingPath(path);
      if (!this.systemLibraries.contains(path)) {
         boolean matched = false;
         if (Files.isDirectory(path)) {
            for (L lib : this.libs) {
               if (!excludedLibs.contains(lib) && !this.origins.containsKey(lib)) {
                  for (String p : lib.getPaths()) {
                     if (Files.exists(path.resolve(p))) {
                        matched = true;
                        this.addLibrary(lib, path, p);
                        break;
                     }
                  }
               }
            }
         } else {
            try {
               ZipFile zf = new ZipFile(path.toFile());

               try {
                  for (L lib : this.libs) {
                     if (!excludedLibs.contains(lib) && !this.origins.containsKey(lib)) {
                        for (String p : lib.getPaths()) {
                           if (zf.getEntry(p) != null) {
                              matched = true;
                              this.addLibrary(lib, path, p);
                              break;
                           }
                        }
                     }
                  }
               } catch (Throwable var12) {
                  try {
                     zf.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }

                  throw var12;
               }

               zf.close();
            } catch (ZipError | IOException e) {
               throw new IOException("error reading " + path, e);
            }
         }

         if (!matched) {
            this.unmatchedOrigins.add(path);
            if (DEBUG) {
               Log.info(LogCategory.LIB_CLASSIFICATION, "unmatched %s", path);
            }
         }
      }
   }

   private void addLibrary(L lib, Path originPath, String localPath) {
      Path prev = this.origins.put(lib, originPath);
      if (prev != null) {
         throw new IllegalStateException("lib " + lib + " was already added");
      }

      this.localPaths.put(lib, localPath);
      if (DEBUG) {
         Log.info(LogCategory.LIB_CLASSIFICATION, "%s %s (%s)", lib.name(), originPath, localPath);
      }
   }

   @SafeVarargs
   public final boolean is(Path path, L... libs) {
      for (L lib : libs) {
         if (path.equals(this.origins.get(lib))) {
            return true;
         }
      }

      return false;
   }

   public boolean has(L lib) {
      return this.origins.containsKey(lib);
   }

   public Path getOrigin(L lib) {
      return this.origins.get(lib);
   }

   public String getLocalPath(L lib) {
      return this.localPaths.get(lib);
   }

   public String getClassName(L lib) {
      String localPath = this.localPaths.get(lib);
      return localPath != null && localPath.endsWith(".class") ? localPath.substring(0, localPath.length() - 6).replace('/', '.') : null;
   }

   public List<Path> getUnmatchedOrigins() {
      return this.unmatchedOrigins;
   }

   public Collection<Path> getSystemLibraries() {
      return this.systemLibraries;
   }

   public boolean remove(Path path) {
      if (this.unmatchedOrigins.remove(path)) {
         return true;
      }

      boolean ret = false;
      Iterator<Entry<L, Path>> it = this.origins.entrySet().iterator();

      while (it.hasNext()) {
         Entry<L, Path> entry = it.next();
         if (entry.getValue().equals(path)) {
            this.localPaths.remove(entry.getKey());
            it.remove();
            ret = true;
         }
      }

      return ret;
   }

   public interface LibraryType {
      boolean isApplicable(EnvType var1);

      String[] getPaths();
   }
}
