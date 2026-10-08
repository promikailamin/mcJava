package net.fabricmc.loader.impl.game.minecraft;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.ObjectShare;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.LibClassifier;
import net.fabricmc.loader.impl.game.minecraft.patch.BrandingPatch;
import net.fabricmc.loader.impl.game.minecraft.patch.EntrypointPatch;
import net.fabricmc.loader.impl.game.minecraft.patch.EntrypointPatchFML125;
import net.fabricmc.loader.impl.game.minecraft.patch.TinyFDPatch;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ModDependencyImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.LogHandler;

public class MinecraftGameProvider implements GameProvider {
   private static final String[] ALLOWED_EARLY_CLASS_PREFIXES = new String[]{"org.apache.logging.log4j.", "com.mojang.util."};
   private static final Set<String> SENSITIVE_ARGS = new HashSet<>(
      Arrays.asList("accesstoken", "clientid", "profileproperties", "proxypass", "proxyuser", "username", "userproperties", "uuid", "xuid")
   );
   private EnvType envType;
   private String entrypoint;
   private Arguments arguments;
   private final List<Path> gameJars = new ArrayList<>(2);
   private Path realmsJar;
   private final Set<Path> logJars = new HashSet<>();
   private boolean log4jAvailable;
   private boolean slf4jAvailable;
   private final List<Path> miscGameLibraries = new ArrayList<>();
   private Collection<Path> validParentClassPath;
   private McVersion versionData;
   private boolean hasModLoader = false;
   private final GameTransformer transformer = new GameTransformer(
      new EntrypointPatch(this), new BrandingPatch(), new EntrypointPatchFML125(), new TinyFDPatch()
   );
   private static final Set<GameProvider.BuiltinTransform> TRANSFORM_WIDENALL_STRIPENV_CLASSTWEAKS = EnumSet.of(
      GameProvider.BuiltinTransform.WIDEN_ALL_PACKAGE_ACCESS, GameProvider.BuiltinTransform.STRIP_ENVIRONMENT, GameProvider.BuiltinTransform.CLASS_TWEAKS
   );
   private static final Set<GameProvider.BuiltinTransform> TRANSFORM_WIDENALL_CLASSTWEAKS = EnumSet.of(
      GameProvider.BuiltinTransform.WIDEN_ALL_PACKAGE_ACCESS, GameProvider.BuiltinTransform.CLASS_TWEAKS
   );
   private static final Set<GameProvider.BuiltinTransform> TRANSFORM_STRIPENV = EnumSet.of(GameProvider.BuiltinTransform.STRIP_ENVIRONMENT);

   @Override
   public String getGameId() {
      return "minecraft";
   }

   @Override
   public String getGameName() {
      return "Minecraft";
   }

   @Override
   public String getRawGameVersion() {
      return this.versionData.getRaw();
   }

   @Override
   public String getNormalizedGameVersion() {
      return this.versionData.getNormalized();
   }

   @Override
   public Collection<GameProvider.BuiltinMod> getBuiltinMods() {
      BuiltinModMetadata.Builder metadata = new BuiltinModMetadata.Builder(this.getGameId(), this.getNormalizedGameVersion()).setName(this.getGameName());
      if (this.versionData.getClassVersion().isPresent()) {
         int version = this.versionData.getClassVersion().getAsInt() - 44;

         try {
            metadata.addDependency(
               new ModDependencyImpl(ModDependency.Kind.DEPENDS, "java", Collections.singletonList(String.format(Locale.ENGLISH, ">=%d", version)))
            );
         } catch (VersionParsingException e) {
            throw new RuntimeException(e);
         }
      }

      return Collections.singletonList(new GameProvider.BuiltinMod(this.gameJars, metadata.build()));
   }

   public Path getGameJar() {
      return this.gameJars.get(0);
   }

   @Override
   public String getEntrypoint() {
      return this.entrypoint;
   }

   @Override
   public Path getLaunchDirectory() {
      return this.arguments == null ? Paths.get(".") : getLaunchDirectory(this.arguments);
   }

   @Override
   public boolean requiresUrlClassLoader() {
      return this.hasModLoader;
   }

   @Override
   public Set<GameProvider.BuiltinTransform> getBuiltinTransforms(String className) {
      boolean isMinecraftClass = className.startsWith("net.minecraft.")
         || className.startsWith("com.mojang.minecraft.")
         || className.startsWith("com.mojang.rubydung.")
         || className.startsWith("com.mojang.blaze3d.")
         || className.startsWith("com.mojang.renderpearl.")
         || className.startsWith("com.mojang.math.")
         || className.startsWith("com.mojang.realmsclient.")
         || className.indexOf(46) < 0;
      if (isMinecraftClass) {
         return FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment() ? TRANSFORM_WIDENALL_STRIPENV_CLASSTWEAKS : TRANSFORM_WIDENALL_CLASSTWEAKS;
      } else {
         return TRANSFORM_STRIPENV;
      }
   }

   @Override
   public boolean isEnabled() {
      return !SystemProperties.isSet("fabric.skipMcProvider");
   }

   @Override
   public boolean locateGame(FabricLauncher launcher, String[] args) {
      this.envType = launcher.getEnvironmentType();
      this.arguments = new Arguments();
      this.arguments.parse(args);

      try {
         LibClassifier<McLibrary> classifier = new LibClassifier<>(McLibrary.class, this.envType, this);
         McLibrary envGameLib = this.envType == EnvType.CLIENT ? McLibrary.MC_CLIENT : McLibrary.MC_SERVER;
         Path commonGameJar = GameProviderHelper.getCommonGameJar();
         Path envGameJar = GameProviderHelper.getEnvGameJar(this.envType);
         boolean commonGameJarDeclared = commonGameJar != null;
         if (commonGameJarDeclared) {
            if (envGameJar != null) {
               classifier.process(envGameJar, McLibrary.MC_COMMON);
            }

            classifier.process(commonGameJar);
         } else if (envGameJar != null) {
            classifier.process(envGameJar);
         }

         classifier.process(launcher.getClassPath());
         if (classifier.has(McLibrary.MC_BUNDLER)) {
            BundlerProcessor.process(classifier);
         }

         envGameJar = classifier.getOrigin(envGameLib);
         if (envGameJar == null) {
            return false;
         }

         commonGameJar = classifier.getOrigin(McLibrary.MC_COMMON);
         if (commonGameJarDeclared && commonGameJar == null) {
            Log.warn(LogCategory.GAME_PROVIDER, "The declared common game jar didn't contain any of the expected classes!");
         }

         this.gameJars.add(envGameJar);
         if (commonGameJar != null && !commonGameJar.equals(envGameJar)) {
            this.gameJars.add(commonGameJar);
         }

         Path assetsJar = classifier.getOrigin(McLibrary.MC_ASSETS_ROOT);
         if (assetsJar != null && !assetsJar.equals(commonGameJar) && !assetsJar.equals(envGameJar)) {
            this.gameJars.add(assetsJar);
         }

         this.entrypoint = classifier.getClassName(envGameLib);
         this.realmsJar = classifier.getOrigin(McLibrary.REALMS);
         this.hasModLoader = classifier.has(McLibrary.MODLOADER);
         this.log4jAvailable = classifier.has(McLibrary.LOG4J_API) && classifier.has(McLibrary.LOG4J_CORE);
         this.slf4jAvailable = classifier.has(McLibrary.SLF4J_API) && classifier.has(McLibrary.SLF4J_CORE);
         boolean hasLogLib = this.log4jAvailable || this.slf4jAvailable;
         Log.configureBuiltin(hasLogLib, !hasLogLib);

         for (McLibrary lib : McLibrary.LOGGING) {
            Path path = classifier.getOrigin(lib);
            if (path != null) {
               if (hasLogLib) {
                  this.logJars.add(path);
               } else if (!this.gameJars.contains(path)) {
                  this.miscGameLibraries.add(path);
               }
            }
         }

         this.miscGameLibraries.addAll(classifier.getUnmatchedOrigins());
         this.validParentClassPath = classifier.getSystemLibraries();
      } catch (IOException e) {
         throw ExceptionUtil.wrap(e);
      }

      ObjectShare share = FabricLoaderImpl.INSTANCE.getObjectShare();
      share.put("fabric-loader:inputGameJar", this.gameJars.get(0));
      share.put("fabric-loader:inputGameJars", Collections.unmodifiableList(new ArrayList<>(this.gameJars)));
      if (this.realmsJar != null) {
         share.put("fabric-loader:inputRealmsJar", this.realmsJar);
      }

      String version = this.arguments.remove("fabric.gameVersion");
      if (version == null) {
         version = System.getProperty("fabric.gameVersion");
      }

      this.versionData = McVersionLookup.getVersion(this.gameJars, this.entrypoint, version);
      processArgumentMap(this.arguments, this.envType);
      return true;
   }

   private static void processArgumentMap(Arguments argMap, EnvType envType) {
      switch (envType) {
         case CLIENT:
            if (!argMap.containsKey("accessToken")) {
               argMap.put("accessToken", "FabricMC");
            }

            if (!argMap.containsKey("version")) {
               argMap.put("version", "Fabric");
            }

            String versionType = "";
            if (argMap.containsKey("versionType") && !argMap.get("versionType").equalsIgnoreCase("release")) {
               versionType = argMap.get("versionType") + "/";
            }

            argMap.put("versionType", versionType + "Fabric");
            if (!argMap.containsKey("gameDir")) {
               argMap.put("gameDir", getLaunchDirectory(argMap).toAbsolutePath().normalize().toString());
            }
            break;
         case SERVER:
            argMap.remove("version");
            argMap.remove("gameDir");
            argMap.remove("assetsDir");
      }
   }

   private static Path getLaunchDirectory(Arguments argMap) {
      return Paths.get(argMap.getOrDefault("gameDir", "."));
   }

   @Override
   public void initialize(FabricLauncher launcher) {
      launcher.setValidParentClassPath(this.validParentClassPath);
      MappingConfiguration config = launcher.getMappingConfiguration();
      String runtimeNs = config.getRuntimeNamespace();
      String gameNs = System.getProperty("fabric.gameMappingNamespace");
      if (gameNs == null) {
         gameNs = "official";
         if (config.hasAnyMappings()) {
            List<String> mappingNamespaces = config.getNamespaces();
            if (mappingNamespaces != null) {
               if (launcher.isDevelopment() && mappingNamespaces.contains("named")) {
                  gameNs = "named";
               } else if (!mappingNamespaces.contains("official")) {
                  gameNs = this.envType == EnvType.CLIENT ? "clientOfficial" : "serverOfficial";
               }
            }
         }
      }

      Log.debug(
         LogCategory.GAME_PROVIDER,
         "namespace detection result: game=%s runtime=%s mod-default=%s",
         gameNs,
         runtimeNs,
         config.getDefaultModDistributionNamespace()
      );
      if (!gameNs.equals(runtimeNs)) {
         Map<String, Path> obfJars = new HashMap<>(3);
         String[] names = new String[this.gameJars.size()];

         for (int i = 0; i < this.gameJars.size(); i++) {
            String name;
            if (i == 0) {
               name = this.envType.name().toLowerCase(Locale.ENGLISH);
            } else if (i == 1) {
               name = "common";
            } else {
               name = String.format(Locale.ENGLISH, "extra-%d", i - 2);
            }

            obfJars.put(name, this.gameJars.get(i));
            names[i] = name;
         }

         if (this.realmsJar != null) {
            obfJars.put("realms", this.realmsJar);
         }

         obfJars = GameProviderHelper.deobfuscate(obfJars, gameNs, this.getGameId(), this.getNormalizedGameVersion(), this.getLaunchDirectory(), launcher);

         for (int i = 0; i < this.gameJars.size(); i++) {
            Path newJar = obfJars.get(names[i]);
            Path oldJar = this.gameJars.set(i, newJar);
            if (this.logJars.remove(oldJar)) {
               this.logJars.add(newJar);
            }
         }

         this.realmsJar = obfJars.get("realms");
      }

      if (!this.logJars.isEmpty() && !Boolean.getBoolean("fabric.unitTest")) {
         for (Path jar : this.logJars) {
            if (this.gameJars.contains(jar)) {
               launcher.addToClassPath(jar, ALLOWED_EARLY_CLASS_PREFIXES);
            } else {
               launcher.addToClassPath(jar);
            }
         }
      }

      this.setupLogHandler(launcher, true);
      this.transformer.locateEntrypoints(launcher, this.gameJars);
   }

   private void setupLogHandler(FabricLauncher launcher, boolean useTargetCl) {
      System.setProperty("log4j2.formatMsgNoLookups", "true");

      try {
         String logHandlerClsName;
         if (this.log4jAvailable) {
            logHandlerClsName = "net.fabricmc.loader.impl.game.minecraft.Log4jLogHandler";
         } else {
            if (!this.slf4jAvailable) {
               return;
            }

            logHandlerClsName = "net.fabricmc.loader.impl.game.minecraft.Slf4jLogHandler";
         }

         ClassLoader prevCl = Thread.currentThread().getContextClassLoader();
         Class<?> logHandlerCls;
         if (useTargetCl) {
            Thread.currentThread().setContextClassLoader(launcher.getTargetClassLoader());
            logHandlerCls = launcher.loadIntoTarget(logHandlerClsName);
         } else {
            logHandlerCls = Class.forName(logHandlerClsName);
         }

         Log.init((LogHandler)logHandlerCls.getConstructor().newInstance());
         Thread.currentThread().setContextClassLoader(prevCl);
      } catch (ReflectiveOperationException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public Arguments getArguments() {
      return this.arguments;
   }

   @Override
   public String[] getLaunchArguments(boolean sanitize) {
      if (this.arguments == null) {
         return new String[0];
      }

      String[] ret = this.arguments.toArray();
      if (!sanitize) {
         return ret;
      }

      int writeIdx = 0;

      for (int i = 0; i < ret.length; i++) {
         String arg = ret[i];
         if (i + 1 < ret.length && arg.startsWith("--") && SENSITIVE_ARGS.contains(arg.substring(2).toLowerCase(Locale.ENGLISH))) {
            i++;
         } else {
            ret[writeIdx++] = arg;
         }
      }

      if (writeIdx < ret.length) {
         ret = Arrays.copyOf(ret, writeIdx);
      }

      return ret;
   }

   @Override
   public GameTransformer getEntrypointTransformer() {
      return this.transformer;
   }

   @Override
   public boolean canOpenErrorGui() {
      if (this.arguments != null && this.envType != EnvType.CLIENT) {
         List<String> extras = this.arguments.getExtraArgs();
         return !extras.contains("nogui") && !extras.contains("--nogui");
      } else {
         return true;
      }
   }

   @Override
   public boolean hasAwtSupport() {
      return !LoaderUtil.hasMacOs();
   }

   @Override
   public void unlockClassPath(FabricLauncher launcher) {
      for (Path gameJar : this.gameJars) {
         if (this.logJars.contains(gameJar)) {
            launcher.setAllowedPrefixes(gameJar);
         } else {
            launcher.addToClassPath(gameJar);
         }
      }

      if (this.realmsJar != null) {
         launcher.addToClassPath(this.realmsJar);
      }

      for (Path lib : this.miscGameLibraries) {
         launcher.addToClassPath(lib);
      }
   }

   @Override
   public void launch(ClassLoader loader) {
      String targetClass = this.entrypoint;
      if (this.envType == EnvType.CLIENT && targetClass.contains("Applet")) {
         targetClass = "net.fabricmc.loader.impl.game.minecraft.applet.AppletMain";
      }

      MethodHandle invoker;
      try {
         Class<?> c = loader.loadClass(targetClass);
         invoker = MethodHandles.lookup().findStatic(c, "main", MethodType.methodType(void.class, String[].class));
      } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
         throw FormattedException.ofLocalized("exception.minecraft.invokeFailure", e);
      }

      try {
         invoker.invokeExact(this.arguments.toArray());
      } catch (Throwable t) {
         throw FormattedException.ofLocalized("exception.minecraft.generic", t);
      }
   }
}
