package net.fabricmc.loader.impl.launch.knot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.FileSystemUtil;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.ManifestUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.UrlConversionException;
import net.fabricmc.loader.impl.util.UrlUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

final class KnotClassDelegate<T extends ClassLoader & KnotClassDelegate.ClassLoaderAccess> implements KnotClassLoaderInterface {
   private static final boolean LOG_CLASS_LOAD = SystemProperties.isSet("fabric.debug.logClassLoad");
   private static final boolean LOG_CLASS_LOAD_ERRORS = LOG_CLASS_LOAD || SystemProperties.isSet("fabric.debug.logClassLoadErrors");
   private static final boolean LOG_TRANSFORM_ERRORS = SystemProperties.isSet("fabric.debug.logTransformErrors");
   private static final boolean DISABLE_ISOLATION = SystemProperties.isSet("fabric.debug.disableClassPathIsolation");
   private static final ClassLoader PLATFORM_CLASS_LOADER = getPlatformClassLoader();
   private final Map<Path, KnotClassDelegate.Metadata> metadataCache = new ConcurrentHashMap<>();
   private final T classLoader;
   private final ClassLoader parentClassLoader;
   private final GameProvider provider;
   private final boolean isDevelopment;
   private final EnvType envType;
   private IMixinTransformer mixinTransformer;
   private boolean transformInitialized = false;
   private volatile Set<Path> codeSources = Collections.emptySet();
   private volatile Set<Path> validParentCodeSources = null;
   private final Map<Path, String[]> allowedPrefixes = new ConcurrentHashMap<>();
   private final Set<String> parentSourcedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private static final Collection<Path> JVM_NATIVE_DIRS = computeJvmNativeDirs();
   private static final Map<String, String> PROCESSED_NATIVES = new HashMap<>();

   KnotClassDelegate(boolean isDevelopment, EnvType envType, T classLoader, ClassLoader parentClassLoader, GameProvider provider) {
      this.isDevelopment = isDevelopment;
      this.envType = envType;
      this.classLoader = classLoader;
      this.parentClassLoader = parentClassLoader;
      this.provider = provider;
   }

   @Override
   public ClassLoader getClassLoader() {
      return this.classLoader;
   }

   @Override
   public void initializeTransformers() {
      if (this.transformInitialized) {
         throw new IllegalStateException("Cannot initialize KnotClassDelegate twice!");
      }

      this.mixinTransformer = MixinServiceKnot.getTransformer();
      if (this.mixinTransformer == null) {
         try {
            Constructor<IMixinTransformer> ctor = (Constructor<IMixinTransformer>)Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer")
               .getConstructor();
            ctor.setAccessible(true);
            this.mixinTransformer = ctor.newInstance();
         } catch (ReflectiveOperationException e) {
            Log.debug(LogCategory.KNOT, "Can't create Mixin transformer through reflection (only applicable for 0.8-0.8.2): %s", e);
            throw new IllegalStateException("mixin transformer unavailable?");
         }
      }

      this.transformInitialized = true;
   }

   private IMixinTransformer getMixinTransformer() {
      assert this.mixinTransformer != null;
      return this.mixinTransformer;
   }

   @Override
   public void addCodeSource(Path path) {
      path = LoaderUtil.normalizeExistingPath(path);
      synchronized (this) {
         Set<Path> codeSources = this.codeSources;
         if (codeSources.contains(path)) {
            return;
         }

         Set<Path> newCodeSources = new HashSet<>(codeSources.size() + 1, 1.0F);
         newCodeSources.addAll(codeSources);
         newCodeSources.add(path);
         this.codeSources = newCodeSources;
      }

      try {
         this.classLoader.addUrlFwd(UrlUtil.asUrl(path));
      } catch (MalformedURLException e) {
         throw new RuntimeException(e);
      }

      if (LOG_CLASS_LOAD_ERRORS) {
         Log.info(LogCategory.KNOT, "added code source %s", path);
      }
   }

   @Override
   public void setAllowedPrefixes(Path codeSource, String... prefixes) {
      codeSource = LoaderUtil.normalizeExistingPath(codeSource);
      if (prefixes.length == 0) {
         this.allowedPrefixes.remove(codeSource);
      } else {
         this.allowedPrefixes.put(codeSource, prefixes);
      }
   }

   @Override
   public void setValidParentClassPath(Collection<Path> paths) {
      Set<Path> validPaths = new HashSet<>(paths.size(), 1.0F);

      for (Path path : paths) {
         validPaths.add(LoaderUtil.normalizeExistingPath(path));
      }

      this.validParentCodeSources = validPaths;
   }

   @Override
   public Manifest getManifest(Path codeSource) {
      return this.getMetadata(LoaderUtil.normalizeExistingPath(codeSource)).manifest;
   }

   @Override
   public boolean isClassLoaded(String name) {
      synchronized (this.classLoader.getClassLoadingLockFwd(name)) {
         return this.classLoader.findLoadedClassFwd(name) != null;
      }
   }

   @Override
   public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
      synchronized (this.classLoader.getClassLoadingLockFwd(name)) {
         Class<?> c = this.classLoader.findLoadedClassFwd(name);
         if (c == null) {
            c = this.tryLoadClass(name, true);
            if (c == null) {
               throw new ClassNotFoundException("can't find class " + name);
            }

            if (LOG_CLASS_LOAD) {
               Log.info(LogCategory.KNOT, "loaded class %s into target", name);
            }
         }

         this.classLoader.resolveClassFwd(c);
         return c;
      }
   }

   Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      synchronized (this.classLoader.getClassLoadingLockFwd(name)) {
         Class<?> c = this.classLoader.findLoadedClassFwd(name);
         if (c == null) {
            if (name.startsWith("java.")) {
               c = PLATFORM_CLASS_LOADER.loadClass(name);
            } else {
               c = this.tryLoadClass(name, false);
               if (c == null) {
                  String fileName = LoaderUtil.getClassFileName(name);
                  URL url = this.parentClassLoader.getResource(fileName);
                  if (url == null) {
                     try {
                        c = PLATFORM_CLASS_LOADER.loadClass(name);
                        if (LOG_CLASS_LOAD) {
                           Log.info(LogCategory.KNOT, "loaded resources-less class %s from platform class loader");
                        }
                     } catch (ClassNotFoundException e) {
                        if (LOG_CLASS_LOAD_ERRORS) {
                           Log.warn(LogCategory.KNOT, "can't find class %s", name);
                        }

                        throw e;
                     }
                  } else {
                     if (!this.isValidParentUrl(url, fileName)) {
                        String msg = String.format(
                           "can't load class %s at %s as it hasn't been exposed to the game (yet? The system property fabric.classPathGroups may not be set correctly in-dev)",
                           name,
                           getCodeSource(url, fileName)
                        );
                        if (LOG_CLASS_LOAD_ERRORS) {
                           Log.warn(LogCategory.KNOT, msg);
                        }

                        throw new ClassNotFoundException(msg);
                     }

                     if (LOG_CLASS_LOAD) {
                        Log.info(LogCategory.KNOT, "loading class %s using the parent class loader", name);
                     }

                     c = this.parentClassLoader.loadClass(name);
                  }
               } else if (LOG_CLASS_LOAD) {
                  Log.info(LogCategory.KNOT, "loaded class %s", name);
               }
            }
         }

         if (resolve) {
            this.classLoader.resolveClassFwd(c);
         }

         return c;
      }
   }

   private boolean isValidParentUrl(URL url, String fileName) {
      if (url == null) {
         return false;
      }

      if (DISABLE_ISOLATION) {
         return true;
      }

      if (!hasRegularCodeSource(url)) {
         return true;
      }

      Path codeSource = getCodeSource(url, fileName);
      Set<Path> validParentCodeSources = this.validParentCodeSources;
      return validParentCodeSources == null
         ? !this.codeSources.contains(codeSource)
         : validParentCodeSources.contains(codeSource) || PLATFORM_CLASS_LOADER.getResource(fileName) != null;
   }

   Class<?> tryLoadClass(String name, boolean allowFromParent) throws ClassNotFoundException {
      if (name.startsWith("java.")) {
         return null;
      }

      if (!this.allowedPrefixes.isEmpty() && !DISABLE_ISOLATION) {
         String fileName = LoaderUtil.getClassFileName(name);
         URL url = this.classLoader.getResource(fileName);
         if (url != null && hasRegularCodeSource(url)) {
            Path codeSource = getCodeSource(url, fileName);
            String[] prefixes = this.allowedPrefixes.get(codeSource);
            if (prefixes != null) {
               assert prefixes.length > 0;
               boolean found = false;

               for (String prefix : prefixes) {
                  if (name.startsWith(prefix)) {
                     found = true;
                     break;
                  }
               }

               if (!found) {
                  String msg = "class " + name + " is currently restricted from being loaded";
                  if (LOG_CLASS_LOAD_ERRORS) {
                     Log.warn(LogCategory.KNOT, msg);
                  }

                  throw new ClassNotFoundException(msg);
               }
            }
         }
      }

      if (!allowFromParent && !this.parentSourcedClasses.isEmpty()) {
         int pos = name.length();

         while ((pos = name.lastIndexOf(36, pos - 1)) > 0) {
            if (this.parentSourcedClasses.contains(name.substring(0, pos))) {
               allowFromParent = true;
               break;
            }
         }
      }

      byte[] input = this.getPostMixinClassByteArray(name, allowFromParent);
      if (input == null) {
         return null;
      }

      Class<?> existingClass = this.classLoader.findLoadedClassFwd(name);
      if (existingClass != null) {
         return existingClass;
      }

      if (allowFromParent) {
         this.parentSourcedClasses.add(name);
      }

      KnotClassDelegate.Metadata metadata = this.getMetadata(name);
      int pkgDelimiterPos = name.lastIndexOf(46);
      if (pkgDelimiterPos > 0) {
         String pkgString = name.substring(0, pkgDelimiterPos);
         if (this.classLoader.getPackageFwd(pkgString) == null) {
            try {
               this.classLoader.definePackageFwd(pkgString, null, null, null, null, null, null, null);
            } catch (IllegalArgumentException e) {
               if (this.classLoader.getPackageFwd(pkgString) == null) {
                  throw e;
               }
            }
         }
      }

      return this.classLoader.defineClassFwd(name, input, 0, input.length, metadata.codeSource);
   }

   private KnotClassDelegate.Metadata getMetadata(String name) {
      String fileName = LoaderUtil.getClassFileName(name);
      URL url = this.classLoader.getResource(fileName);
      return url != null && hasRegularCodeSource(url) ? this.getMetadata(getCodeSource(url, fileName)) : KnotClassDelegate.Metadata.EMPTY;
   }

   private KnotClassDelegate.Metadata getMetadata(Path codeSource) {
      return this.metadataCache.computeIfAbsent(codeSource, path -> {
         Manifest manifest = null;
         CodeSource cs = null;
         Certificate[] certificates = null;

         try {
            if (Files.isDirectory(path)) {
               manifest = ManifestUtil.readManifestFromBasePath(path);
            } else {
               URLConnection connection = new URL("jar:" + path.toUri().toString() + "!/").openConnection();
               if (connection instanceof JarURLConnection) {
                  manifest = ((JarURLConnection)connection).getManifest();
                  certificates = ((JarURLConnection)connection).getCertificates();
               }

               if (manifest == null) {
                  FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(path, false);

                  try {
                     manifest = ManifestUtil.readManifestFromBasePath(jarFs.get().getRootDirectories().iterator().next());
                  } catch (Throwable t$) {
                     if (jarFs != null) {
                        try {
                           jarFs.close();
                        } catch (Throwable x2) {
                           t$.addSuppressed(x2);
                        }
                     }

                     throw t$;
                  }

                  if (jarFs != null) {
                     jarFs.close();
                  }
               }
            }
         } catch (IOException | FileSystemNotFoundException e) {
            if (FabricLauncherBase.getLauncher().isDevelopment()) {
               Log.warn(LogCategory.KNOT, "Failed to load manifest", e);
            }
         }

         if (cs == null) {
            try {
               cs = new CodeSource(UrlUtil.asUrl(path), certificates);
            } catch (MalformedURLException e) {
               throw new RuntimeException(e);
            }
         }

         return new KnotClassDelegate.Metadata(manifest, cs);
      });
   }

   private byte[] getPostMixinClassByteArray(String name, boolean allowFromParent) {
      byte[] transformedClassArray = this.getPreMixinClassByteArray(name, allowFromParent);
      if (this.transformInitialized && canTransformClass(name)) {
         try {
            return this.getMixinTransformer().transformClassBytes(name, name, transformedClassArray);
         } catch (Throwable t) {
            String msg = String.format("Mixin transformation of %s failed", name);
            if (LOG_TRANSFORM_ERRORS) {
               Log.warn(LogCategory.KNOT, msg, t);
            }

            throw new RuntimeException(msg, t);
         }
      } else {
         return transformedClassArray;
      }
   }

   @Override
   public byte[] getPreMixinClassBytes(String name) {
      return this.getPreMixinClassByteArray(name, true);
   }

   private byte[] getPreMixinClassByteArray(String name, boolean allowFromParent) {
      name = name.replace('/', '.');
      if (this.transformInitialized && canTransformClass(name)) {
         byte[] input = this.provider.getEntrypointTransformer().transform(name);
         if (input == null) {
            try {
               input = this.getRawClassByteArray(name, allowFromParent);
            } catch (IOException e) {
               throw new RuntimeException("Failed to load class file for '" + name + "'!", e);
            }
         }

         return input != null ? FabricTransformer.transform(this.isDevelopment, this.envType, name, input) : null;
      } else {
         try {
            return this.getRawClassByteArray(name, allowFromParent);
         } catch (IOException e) {
            throw new RuntimeException("Failed to load class file for '" + name + "'!", e);
         }
      }
   }

   private static boolean canTransformClass(String name) {
      name = name.replace('/', '.');
      return !name.startsWith("org.apache.logging.log4j");
   }

   @Override
   public byte[] getRawClassBytes(String name) throws IOException {
      return this.getRawClassByteArray(name, true);
   }

   private byte[] getRawClassByteArray(String name, boolean allowFromParent) throws IOException {
      name = LoaderUtil.getClassFileName(name);
      URL url = this.classLoader.findResourceFwd(name);
      if (url == null) {
         if (!allowFromParent) {
            return null;
         }

         url = this.parentClassLoader.getResource(name);
         if (!this.isValidParentUrl(url, name)) {
            if (LOG_CLASS_LOAD) {
               Log.info(LogCategory.KNOT, "refusing to load class %s at %s from parent class loader", name, url != null ? getCodeSource(url, name) : "null");
            }

            return null;
         }
      }

      InputStream inputStream = url.openStream();

      byte[] var9;
      try {
         int a = inputStream.available();
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream(a < 32 ? 32768 : a);
         byte[] buffer = new byte[8192];

         int len;
         while ((len = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, len);
         }

         var9 = outputStream.toByteArray();
      } catch (Throwable var11) {
         if (inputStream != null) {
            try {
               inputStream.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }
         }

         throw var11;
      }

      if (inputStream != null) {
         inputStream.close();
      }

      return var9;
   }

   private static boolean hasRegularCodeSource(URL url) {
      return url.getProtocol().equals("file") || url.getProtocol().equals("jar");
   }

   private static Path getCodeSource(URL url, String fileName) {
      try {
         return LoaderUtil.normalizeExistingPath(UrlUtil.getCodeSource(url, fileName));
      } catch (UrlConversionException e) {
         throw ExceptionUtil.wrap(e);
      }
   }

   private static ClassLoader getPlatformClassLoader() {
      try {
         return (ClassLoader)ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
      } catch (NoSuchMethodException e) {
         return new ClassLoader(null) {};
      } catch (ReflectiveOperationException e) {
         throw new RuntimeException(e);
      }
   }

   private static Collection<Path> computeJvmNativeDirs() {
      Set<Path> ret = new HashSet<>();
      String[] libPathProperties = new String[]{"sun.boot.library.path", "java.library.path"};

      for (String libPathProperty : libPathProperties) {
         String value = System.getProperty(libPathProperty);
         if (value != null && !value.isEmpty()) {
            for (String pathStr : value.split(File.pathSeparator)) {
               try {
                  Path path = Paths.get(pathStr);
                  if (Files.exists(path)) {
                     ret.add(path);
                  }
               } catch (InvalidPathException e) {
                  Log.warn(LogCategory.KNOT, "Ignoring invalid library path %s", pathStr);
               }
            }
         }
      }

      return ret;
   }

   synchronized String findLibrary(String libname) {
      String ret = PROCESSED_NATIVES.get(libname);
      if (ret != null) {
         return ret;
      }

      String fileName = System.mapLibraryName(libname);

      for (Path dir : JVM_NATIVE_DIRS) {
         Path file = dir.resolve(fileName);
         if (Files.exists(file)) {
            return null;
         }
      }

      URL url = this.classLoader.getResource(fileName);
      if (url == null) {
         return null;
      }

      Path codeSource;
      try {
         codeSource = UrlUtil.getCodeSource(url, fileName);
      } catch (UrlConversionException e) {
         throw new RuntimeException(e);
      }

      Path libFile;
      if (Files.isDirectory(codeSource)) {
         libFile = codeSource.resolve(fileName);
      } else {
         Path cacheDir = null;

         try {
            cacheDir = FabricLoaderImpl.INSTANCE.getGameDir().resolve(".fabric").resolve("natives");
            assert cacheDir.isAbsolute();
            Files.createDirectories(cacheDir);
         } catch (IllegalStateException e) {
            return null;
         } catch (IOException e) {
            Log.warn(LogCategory.KNOT, "Error creating natives cache directory %s", cacheDir, e);
            return null;
         }

         libFile = cacheDir.resolve(fileName);
         Log.debug(LogCategory.KNOT, "Extracting native %s from class path %s to %s", libname, url, libFile);

         try {
            copyZipEntryIfDistinct(codeSource, fileName, libFile);
         } catch (IOException e) {
            Log.warn(LogCategory.KNOT, "Error extracting native %s to %s", url, cacheDir, e);
            return null;
         }
      }

      ret = libFile.toString();
      PROCESSED_NATIVES.put(libname, ret);
      Log.debug(LogCategory.KNOT, "Supplying native %s from class path (%s)", libname, ret);
      return ret;
   }

   private static void copyZipEntryIfDistinct(Path zipFile, String fileName, Path output) throws IOException {
      ZipFile zf = new ZipFile(zipFile.toFile());

      label64: {
         try {
            ZipEntry entry = zf.getEntry(fileName);
            if (entry == null) {
               throw new FileNotFoundException(String.format("zip file %s doesn't contain %s", zipFile, fileName));
            }

            if (Files.exists(output)) {
               long expectedSize = entry.getSize();
               long expectedCrc = entry.getCrc();
               if (Files.size(output) == expectedSize) {
                  CRC32 crc = new CRC32();
                  byte[] buffer = new byte[16384];
                  InputStream is = Files.newInputStream(output);

                  int len;
                  try {
                     while ((len = is.read(buffer)) >= 0) {
                        crc.update(buffer, 0, len);
                     }
                  } catch (Throwable var16) {
                     if (is != null) {
                        try {
                           is.close();
                        } catch (Throwable var15) {
                           var16.addSuppressed(var15);
                        }
                     }

                     throw var16;
                  }

                  if (is != null) {
                     is.close();
                  }

                  if (crc.getValue() == expectedCrc) {
                     break label64;
                  }
               }
            }

            Files.copy(zf.getInputStream(entry), output, StandardCopyOption.REPLACE_EXISTING);
         } catch (Throwable var17) {
            try {
               zf.close();
            } catch (Throwable var14) {
               var17.addSuppressed(var14);
            }

            throw var17;
         }

         zf.close();
         return;
      }

      zf.close();
   }

   interface ClassLoaderAccess {
      void addUrlFwd(URL var1);

      URL findResourceFwd(String var1);

      Package getPackageFwd(String var1);

      Package definePackageFwd(String var1, String var2, String var3, String var4, String var5, String var6, String var7, URL var8) throws IllegalArgumentException;

      Object getClassLoadingLockFwd(String var1);

      Class<?> findLoadedClassFwd(String var1);

      Class<?> defineClassFwd(String var1, byte[] var2, int var3, int var4, CodeSource var5);

      void resolveClassFwd(Class<?> var1);
   }

   static final class Metadata {
      static final KnotClassDelegate.Metadata EMPTY = new KnotClassDelegate.Metadata(null, null);
      final Manifest manifest;
      final CodeSource codeSource;

      Metadata(Manifest manifest, CodeSource codeSource) {
         this.manifest = manifest;
         this.codeSource = codeSource;
      }
   }
}
