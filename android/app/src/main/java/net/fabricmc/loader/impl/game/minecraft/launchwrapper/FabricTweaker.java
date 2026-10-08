package net.fabricmc.loader.impl.game.minecraft.launchwrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.FabricMixinBootstrap;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.ManifestUtil;
import net.fabricmc.loader.impl.util.UrlUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Side;
import org.spongepowered.asm.mixin.transformer.Proxy;

public abstract class FabricTweaker extends FabricLauncherBase implements ITweaker {
   private static final LogCategory LOG_CATEGORY = LogCategory.create("GameProvider", "Tweaker");
   protected Arguments arguments;
   private LaunchClassLoader launchClassLoader;
   private final List<Path> classPath = new ArrayList<>();
   private final boolean isPrimaryTweaker = ((List)Launch.blackboard.get("Tweaks")).isEmpty();

   @Override
   public String getEntrypoint() {
      return this.getLaunchTarget();
   }

   public void acceptOptions(List<String> localArgs, File gameDir, File assetsDir, String profile) {
      this.arguments = new Arguments();
      this.arguments.parse(localArgs);
      if (!this.arguments.containsKey("gameDir") && gameDir != null) {
         this.arguments.put("gameDir", gameDir.getAbsolutePath());
      }

      if (this.getEnvironmentType() == EnvType.CLIENT && !this.arguments.containsKey("assetsDir") && assetsDir != null) {
         this.arguments.put("assetsDir", assetsDir.getAbsolutePath());
      }
   }

   public void injectIntoClassLoader(LaunchClassLoader launchClassLoader) {
      Launch.blackboard.put("fabric.development", IS_DEVELOPMENT);
      setProperties(Launch.blackboard);
      this.launchClassLoader = launchClassLoader;
      launchClassLoader.addClassLoaderExclusion("org.objectweb.asm.");
      launchClassLoader.addClassLoaderExclusion("org.spongepowered.asm.");
      launchClassLoader.addClassLoaderExclusion("net.fabricmc.loader.");
      launchClassLoader.addClassLoaderExclusion("net.fabricmc.api.Environment");
      launchClassLoader.addClassLoaderExclusion("net.fabricmc.api.EnvType");
      launchClassLoader.addClassLoaderExclusion("net.fabricmc.api.ModInitializer");
      launchClassLoader.addClassLoaderExclusion("net.fabricmc.api.ClientModInitializer");
      launchClassLoader.addClassLoaderExclusion("net.fabricmc.api.DedicatedServerModInitializer");

      try {
         this.init();
      } catch (FormattedException e) {
         handleFormattedException(e);
      }
   }

   private void init() {
      setupUncaughtExceptionHandler();
      this.classPath.clear();

      for (URL url : this.launchClassLoader.getSources()) {
         Path path = UrlUtil.asPath(url);
         if (Files.exists(path)) {
            this.classPath.add(LoaderUtil.normalizeExistingPath(path));
         }
      }

      GameProvider provider = new MinecraftGameProvider();
      if (provider.isEnabled() && provider.locateGame(this, this.arguments.toArray())) {
         Log.finishBuiltinConfig();
         this.arguments = null;
         FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
         loader.setGameProvider(provider);
         provider.initialize(this);
         loader.load();
         loader.freeze();
         this.launchClassLoader.registerTransformer(FabricClassTransformer.class.getName());
         FabricLoaderImpl.INSTANCE.loadClassTweakers();
         MixinBootstrap.init();
         FabricMixinBootstrap.init(this.getEnvironmentType(), FabricLoaderImpl.INSTANCE);
         MixinEnvironment.getDefaultEnvironment().setSide(this.getEnvironmentType() == EnvType.CLIENT ? Side.CLIENT : Side.SERVER);
         provider.unlockClassPath(this);

         try {
            loader.invokeEntrypoints("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
         } catch (RuntimeException e) {
            throw FormattedException.ofLocalized("exception.initializerFailure", e);
         }
      } else {
         throw new RuntimeException("Could not locate Minecraft: provider locate failed");
      }
   }

   public String[] getLaunchArguments() {
      return this.isPrimaryTweaker ? FabricLoaderImpl.INSTANCE.getGameProvider().getLaunchArguments(false) : new String[0];
   }

   @Override
   public void addToClassPath(Path path, String... allowedPrefixes) {
      try {
         this.launchClassLoader.addURL(UrlUtil.asUrl(path));
      } catch (MalformedURLException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public void setAllowedPrefixes(Path path, String... prefixes) {
   }

   @Override
   public void setValidParentClassPath(Collection<Path> paths) {
   }

   @Override
   public List<Path> getClassPath() {
      return this.classPath;
   }

   @Override
   public boolean isClassLoaded(String name) {
      throw new RuntimeException("TODO isClassLoaded/launchwrapper");
   }

   @Override
   public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
      return this.launchClassLoader.loadClass(name);
   }

   @Override
   public InputStream getResourceAsStream(String name) {
      return this.launchClassLoader.getResourceAsStream(name);
   }

   @Override
   public ClassLoader getTargetClassLoader() {
      return this.launchClassLoader;
   }

   @Override
   public byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
      String transformedName = name.replace('/', '.');
      byte[] classBytes = this.launchClassLoader.getClassBytes(name);
      if (runTransformers) {
         for (IClassTransformer transformer : this.launchClassLoader.getTransformers()) {
            if (!(transformer instanceof Proxy)) {
               classBytes = transformer.transform(name, transformedName, classBytes);
            }
         }
      }

      return classBytes;
   }

   @Override
   public Manifest getManifest(Path originPath) {
      try {
         return ManifestUtil.readManifest(originPath);
      } catch (IOException e) {
         Log.warn(LOG_CATEGORY, "Error reading Manifest", e);
         return null;
      }
   }

   private void preloadRemappedJar(Path remappedJarFile) throws IOException {
      Map<String, byte[]> resourceCache = null;

      try {
         Field f = LaunchClassLoader.class.getDeclaredField("resourceCache");
         f.setAccessible(true);
         resourceCache = (Map<String, byte[]>)f.get(this.launchClassLoader);
      } catch (Exception e) {
         e.printStackTrace();
      }

      if (resourceCache == null) {
         Log.warn(LOG_CATEGORY, "Resource cache not pre-populated - this will probably cause issues...");
      } else {
         FileInputStream jarFileStream = new FileInputStream(remappedJarFile.toFile());

         try {
            JarInputStream jarStream = new JarInputStream(jarFileStream);

            JarEntry entry;
            try {
               while ((entry = jarStream.getNextJarEntry()) != null) {
                  if (!entry.getName().startsWith("net/minecraft/class_") && entry.getName().endsWith(".class")) {
                     String className = entry.getName();
                     className = className.substring(0, className.length() - 6).replace('/', '.');
                     Log.debug(LOG_CATEGORY, "Appending %s to resource cache...", className);
                     resourceCache.put(className, this.toByteArray(jarStream));
                  }
               }
            } catch (Throwable var10) {
               try {
                  jarStream.close();
               } catch (Throwable var8) {
                  var10.addSuppressed(var8);
               }

               throw var10;
            }

            jarStream.close();
         } catch (Throwable var11) {
            try {
               jarFileStream.close();
            } catch (Throwable var7) {
               var11.addSuppressed(var7);
            }

            throw var11;
         }

         jarFileStream.close();
      }
   }

   private byte[] toByteArray(InputStream inputStream) throws IOException {
      int estimate = inputStream.available();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream(estimate < 32 ? 32768 : estimate);
      byte[] buffer = new byte[8192];

      int len;
      while ((len = inputStream.read(buffer)) > 0) {
         outputStream.write(buffer, 0, len);
      }

      return outputStream.toByteArray();
   }
}
