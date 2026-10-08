package net.fabricmc.loader.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.ObjectShare;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.impl.discovery.ArgumentModCandidateFinder;
import net.fabricmc.loader.impl.discovery.ClasspathModCandidateFinder;
import net.fabricmc.loader.impl.discovery.DirectoryModCandidateFinder;
import net.fabricmc.loader.impl.discovery.ModCandidateImpl;
import net.fabricmc.loader.impl.discovery.ModDiscoverer;
import net.fabricmc.loader.impl.discovery.ModResolutionException;
import net.fabricmc.loader.impl.discovery.ModResolver;
import net.fabricmc.loader.impl.discovery.RuntimeModRemapper;
import net.fabricmc.loader.impl.entrypoint.EntrypointStorage;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweaker;
import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweakerReader;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.EntrypointMetadata;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.VersionOverrides;
import net.fabricmc.loader.impl.util.DefaultLanguageAdapter;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.jetbrains.annotations.VisibleForTesting;

public final class FabricLoaderImpl extends FabricLoader {
   public static final FabricLoaderImpl INSTANCE = FabricLoaderImpl.InitHelper.get();
   public static final int ASM_VERSION = 589824;
   public static final String VERSION = "0.19.5";
   public static final String MOD_ID = "fabricloader";
   public static final String CACHE_DIR_NAME = ".fabric";
   private static final String PROCESSED_MODS_DIR_NAME = "processedMods";
   public static final String REMAPPED_JARS_DIR_NAME = "remappedJars";
   private static final String TMP_DIR_NAME = "tmp";
   protected final Map<String, ModContainerImpl> modMap = new HashMap<>();
   private List<ModCandidateImpl> modCandidates;
   protected List<ModContainerImpl> mods = new ArrayList<>();
   private final Map<String, LanguageAdapter> adapterMap = new HashMap<>();
   private final EntrypointStorage entrypointStorage = new EntrypointStorage();
   private final ClassTweaker classTweaker = ClassTweaker.newInstance();
   private final ObjectShare objectShare = new ObjectShareImpl();
   private boolean frozen = false;
   private Object gameInstance;
   private MappingResolver mappingResolver;
   private GameProvider provider;
   private Path gameDir;
   private Path configDir;

   private FabricLoaderImpl() {
   }

   public void freeze() {
      if (this.frozen) {
         throw new IllegalStateException("Already frozen!");
      }

      this.frozen = true;
      this.finishModLoading();
   }

   public GameProvider getGameProvider() {
      if (this.provider == null) {
         throw new IllegalStateException("game provider not set (yet)");
      } else {
         return this.provider;
      }
   }

   public GameProvider tryGetGameProvider() {
      return this.provider;
   }

   public void setGameProvider(GameProvider provider) {
      this.provider = provider;
      this.setGameDir(provider.getLaunchDirectory());
   }

   private void setGameDir(Path gameDir) {
      this.gameDir = gameDir.toAbsolutePath().normalize();
      this.configDir = gameDir.resolve("config");
   }

   @Override
   public String getRawGameVersion() {
      return this.provider.getRawGameVersion();
   }

   @Override
   public Object getGameInstance() {
      return this.gameInstance;
   }

   @Override
   public EnvType getEnvironmentType() {
      return FabricLauncherBase.getLauncher().getEnvironmentType();
   }

   @Override
   public Path getGameDir() {
      if (this.gameDir == null) {
         throw new IllegalStateException("invoked too early?");
      } else {
         return this.gameDir;
      }
   }

   @Deprecated
   @Override
   public File getGameDirectory() {
      return this.getGameDir().toFile();
   }

   @Override
   public Path getConfigDir() {
      if (!Files.exists(this.configDir)) {
         try {
            Files.createDirectories(this.configDir);
         } catch (IOException e) {
            throw new RuntimeException("Creating config directory", e);
         }
      }

      return this.configDir;
   }

   @Deprecated
   @Override
   public File getConfigDirectory() {
      return this.getConfigDir().toFile();
   }

   public void load() {
      if (this.provider == null) {
         throw new IllegalStateException("game provider not set");
      }

      if (this.frozen) {
         throw new IllegalStateException("Frozen - cannot load additional mods!");
      }

      try {
         this.setup();
      } catch (ModResolutionException exception) {
         if (exception.getCause() == null) {
            throw FormattedException.ofLocalized("exception.incompatible", exception.getMessage());
         } else {
            throw FormattedException.ofLocalized("exception.incompatible", exception);
         }
      }
   }

   private void setup() throws ModResolutionException {
      boolean remapRegularMods = this.isDevelopmentEnvironment();
      VersionOverrides versionOverrides = new VersionOverrides();
      DependencyOverrides depOverrides = new DependencyOverrides(this.configDir);
      ModDiscoverer discoverer = new ModDiscoverer(versionOverrides, depOverrides);
      discoverer.addCandidateFinder(new ClasspathModCandidateFinder());
      discoverer.addCandidateFinder(new DirectoryModCandidateFinder(this.getModsDirectory0(), remapRegularMods));
      discoverer.addCandidateFinder(new ArgumentModCandidateFinder(remapRegularMods));
      Map<String, Set<ModCandidateImpl>> envDisabledMods = new HashMap<>();
      this.modCandidates = discoverer.discoverMods(this, envDisabledMods);
      if (!versionOverrides.getAffectedModIds().isEmpty()) {
         Log.info(LogCategory.GENERAL, "Versions overridden for %s", String.join(", ", versionOverrides.getAffectedModIds()));
      }

      if (!depOverrides.getAffectedModIds().isEmpty()) {
         Log.info(LogCategory.GENERAL, "Dependencies overridden for %s", String.join(", ", depOverrides.getAffectedModIds()));
      }

      this.modCandidates = ModResolver.resolve(this.modCandidates, this.getEnvironmentType(), envDisabledMods);
      this.dumpModList(this.modCandidates);
      this.dumpNonFabricMods(discoverer.getNonFabricMods());
      Path cacheDir = this.gameDir.resolve(".fabric");
      Path outputdir = cacheDir.resolve("processedMods");
      if (remapRegularMods) {
         if (System.getProperty("fabric.remapClasspathFile") == null) {
            MappingConfiguration config = FabricLauncherBase.getLauncher().getMappingConfiguration();
            if (!config.getRuntimeNamespace().equals(config.getDefaultModDistributionNamespace())) {
               Log.warn(
                  LogCategory.MOD_REMAP, "Runtime mod remapping disabled due to no fabric.remapClasspathFile being specified. You may need to update loom."
               );
            }
         } else {
            RuntimeModRemapper.remap(this.modCandidates, cacheDir.resolve("tmp"), outputdir);
         }
      }

      if (this.isDevelopmentEnvironment() && !SystemProperties.isSet("fabric.debug.disableModShuffle")) {
         Collections.shuffle(this.modCandidates);
      }

      String modsToLoadLate = System.getProperty("fabric.debug.loadLate");
      if (modsToLoadLate != null) {
         for (String modId : modsToLoadLate.split(",")) {
            Iterator<ModCandidateImpl> it = this.modCandidates.iterator();

            while (it.hasNext()) {
               ModCandidateImpl mod = it.next();
               if (mod.getId().equals(modId)) {
                  it.remove();
                  this.modCandidates.add(mod);
                  break;
               }
            }
         }
      }

      for (ModCandidateImpl mod : this.modCandidates) {
         if (!mod.hasPath() && !mod.isBuiltin()) {
            try {
               mod.setPaths(Collections.singletonList(mod.copyToDir(outputdir, false)));
            } catch (IOException e) {
               throw new RuntimeException("Error extracting mod " + mod, e);
            }
         }

         this.addMod(mod);
      }

      this.modCandidates = null;
   }

   @VisibleForTesting
   public void dumpNonFabricMods(List<Path> nonFabricMods) {
      if (!nonFabricMods.isEmpty()) {
         StringBuilder outputText = new StringBuilder();

         for (Path nonFabricMod : nonFabricMods) {
            outputText.append("\n\t- ").append(nonFabricMod.getFileName());
         }

         int modsCount = nonFabricMods.size();
         Log.warn(LogCategory.GENERAL, "Found %d non-fabric mod%s:%s", modsCount, modsCount != 1 ? "s" : "", outputText);
      }
   }

   private void dumpModList(List<ModCandidateImpl> mods) {
      StringBuilder modListText = new StringBuilder();
      boolean[] lastItemOfNestLevel = new boolean[mods.size()];
      List<ModCandidateImpl> topLevelMods = mods.stream().filter(mod -> mod.getParentMods().isEmpty()).collect(Collectors.toList());
      int topLevelModsCount = topLevelMods.size();

      for (int i = 0; i < topLevelModsCount; i++) {
         boolean lastItem = i == topLevelModsCount - 1;
         if (lastItem) {
            lastItemOfNestLevel[0] = true;
         }

         this.dumpModList0(topLevelMods.get(i), modListText, 0, lastItemOfNestLevel);
      }

      int modsCount = mods.size();
      Log.info(LogCategory.GENERAL, "Loading %d mod%s:%n%s", modsCount, modsCount != 1 ? "s" : "", modListText);
   }

   private void dumpModList0(ModCandidateImpl mod, StringBuilder log, int nestLevel, boolean[] lastItemOfNestLevel) {
      if (log.length() > 0) {
         log.append('\n');
      }

      for (int depth = 0; depth < nestLevel; depth++) {
         log.append(depth == 0 ? "\t" : (lastItemOfNestLevel[depth] ? "     " : "   | "));
      }

      log.append(nestLevel == 0 ? "\t" : "  ");
      log.append(nestLevel == 0 ? "-" : (lastItemOfNestLevel[nestLevel] ? " \\--" : " |--"));
      log.append(' ');
      log.append(mod.getId());
      log.append(' ');
      log.append(mod.getVersion().getFriendlyString());
      List<ModCandidateImpl> nestedMods = new ArrayList<>(mod.getNestedMods());
      nestedMods.sort(Comparator.comparing(nestedModx -> nestedModx.getMetadata().getId()));
      if (!nestedMods.isEmpty()) {
         Iterator<ModCandidateImpl> iterator = nestedMods.iterator();

         while (iterator.hasNext()) {
            ModCandidateImpl nestedMod = iterator.next();
            boolean lastItem = !iterator.hasNext();
            if (lastItem) {
               lastItemOfNestLevel[nestLevel + 1] = true;
            }

            this.dumpModList0(nestedMod, log, nestLevel + 1, lastItemOfNestLevel);
            if (lastItem) {
               lastItemOfNestLevel[nestLevel + 1] = false;
            }
         }
      }
   }

   private void finishModLoading() {
      for (ModContainerImpl mod : this.mods) {
         if (!mod.getMetadata().getId().equals("fabricloader") && !mod.getMetadata().getType().equals("builtin")) {
            for (Path path : mod.getCodeSourcePaths()) {
               FabricLauncherBase.getLauncher().addToClassPath(path);
            }
         }
      }

      this.setupLanguageAdapters();
      this.setupMods();
   }

   public boolean hasEntrypoints(String key) {
      return this.entrypointStorage.hasEntrypoints(key);
   }

   @Override
   public <T> List<T> getEntrypoints(String key, Class<T> type) {
      return this.entrypointStorage.getEntrypoints(key, type);
   }

   @Override
   public <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
      return this.entrypointStorage.getEntrypointContainers(key, type);
   }

   @Override
   public <T> void invokeEntrypoints(String key, Class<T> type, Consumer<? super T> invoker) {
      if (!this.hasEntrypoints(key)) {
         Log.debug(LogCategory.ENTRYPOINT, "No subscribers for entrypoint '%s'", key);
      } else {
         RuntimeException exception = null;
         Collection<EntrypointContainer<T>> entrypoints = INSTANCE.getEntrypointContainers(key, type);
         Log.debug(LogCategory.ENTRYPOINT, "Iterating over entrypoint '%s'", key);

         for (EntrypointContainer<T> container : entrypoints) {
            try {
               invoker.accept(container.getEntrypoint());
            } catch (Throwable t) {
               exception = ExceptionUtil.gatherExceptions(
                  t,
                  exception,
                  exc -> new RuntimeException(
                     String.format(
                        "Could not execute entrypoint stage '%s' due to errors, provided by '%s' at '%s'!",
                        key,
                        container.getProvider().getMetadata().getId(),
                        container.getDefinition()
                     ),
                     exc
                  )
               );
            }
         }

         if (exception != null) {
            throw exception;
         }
      }
   }

   @Override
   public MappingResolver getMappingResolver() {
      if (this.mappingResolver == null) {
         MappingConfiguration config = FabricLauncherBase.getLauncher().getMappingConfiguration();
         String runtimeNamespace = config.getRuntimeNamespace();
         this.mappingResolver = new LazyMappingResolver(() -> new MappingResolverImpl(config.getMappings(), runtimeNamespace), runtimeNamespace);
      }

      return this.mappingResolver;
   }

   @Override
   public ObjectShare getObjectShare() {
      return this.objectShare;
   }

   public ModCandidateImpl getModCandidate(String id) {
      if (this.modCandidates == null) {
         return null;
      }

      for (ModCandidateImpl mod : this.modCandidates) {
         if (mod.getId().equals(id)) {
            return mod;
         }
      }

      return null;
   }

   @Override
   public Optional<ModContainer> getModContainer(String id) {
      return Optional.ofNullable(this.modMap.get(id));
   }

   @Override
   public Collection<ModContainer> getAllMods() {
      return Collections.unmodifiableList(this.mods);
   }

   public List<ModContainerImpl> getModsInternal() {
      return this.mods;
   }

   @Override
   public boolean isModLoaded(String id) {
      return this.modMap.containsKey(id);
   }

   @Override
   public boolean isDevelopmentEnvironment() {
      return FabricLauncherBase.getLauncher().isDevelopment();
   }

   private void addMod(ModCandidateImpl candidate) throws ModResolutionException {
      ModContainerImpl container = new ModContainerImpl(candidate);
      this.mods.add(container);
      this.modMap.put(candidate.getId(), container);

      for (String provides : candidate.getProvides()) {
         this.modMap.put(provides, container);
      }
   }

   private void setupLanguageAdapters() {
      this.adapterMap.put("default", DefaultLanguageAdapter.INSTANCE);

      for (ModContainerImpl mod : this.mods) {
         for (Entry<String, String> laEntry : mod.getInfo().getLanguageAdapterDefinitions().entrySet()) {
            if (this.adapterMap.containsKey(laEntry.getKey())) {
               throw new RuntimeException(
                  "Duplicate language adapter key: "
                     + laEntry.getKey()
                     + "! ("
                     + laEntry.getValue()
                     + ", "
                     + this.adapterMap.get(laEntry.getKey()).getClass().getName()
                     + ")"
               );
            }

            try {
               this.adapterMap
                  .put(
                     laEntry.getKey(),
                     (LanguageAdapter)Class.forName(laEntry.getValue(), true, FabricLauncherBase.getLauncher().getTargetClassLoader())
                        .getDeclaredConstructor()
                        .newInstance()
                  );
            } catch (Exception e) {
               throw new RuntimeException("Failed to instantiate language adapter: " + laEntry.getKey(), e);
            }
         }
      }
   }

   private void setupMods() {
      for (ModContainerImpl mod : this.mods) {
         try {
            for (String in : mod.getInfo().getOldInitializers()) {
               String adapter = mod.getInfo().getOldStyleLanguageAdapter();
               this.entrypointStorage.addDeprecated(mod, adapter, in);
            }

            for (String key : mod.getInfo().getEntrypointKeys()) {
               for (EntrypointMetadata in : mod.getInfo().getEntrypoints(key)) {
                  this.entrypointStorage.add(mod, key, in, this.adapterMap);
               }
            }
         } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to setup mod %s (%s)", mod.getInfo().getName(), mod.getOrigin()), e);
         }
      }
   }

   public void loadClassTweakers() {
      ClassTweakerReader ctReader = ClassTweakerReader.create(this.classTweaker);

      for (ModContainer modContainer : this.getAllMods()) {
         LoaderModMetadata modMetadata = (LoaderModMetadata)modContainer.getMetadata();
         String location = modMetadata.getClassTweaker();
         if (location != null) {
            Path path = modContainer.findPath(location).orElse(null);
            if (path == null) {
               throw new RuntimeException(String.format("Missing classTweaker file %s from mod %s", location, modContainer.getMetadata().getId()));
            }

            try {
               BufferedReader reader = Files.newBufferedReader(path);

               try {
                  ctReader.read(reader, FabricLauncherBase.getLauncher().getMappingConfiguration().getRuntimeNamespace());
               } catch (Throwable var11) {
                  if (reader != null) {
                     try {
                        reader.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (reader != null) {
                  reader.close();
               }
            } catch (Exception e) {
               throw new RuntimeException("Failed to read classTweaker file from mod " + modMetadata.getId(), e);
            }
         }
      }
   }

   public void prepareModInit(Path newRunDir, Object gameInstance) {
      if (!this.frozen) {
         throw new RuntimeException("Cannot instantiate mods when not frozen!");
      }

      if (gameInstance != null && FabricLauncherBase.getLauncher() instanceof Knot) {
         ClassLoader gameClassLoader = gameInstance.getClass().getClassLoader();
         ClassLoader targetClassLoader = FabricLauncherBase.getLauncher().getTargetClassLoader();
         boolean matchesKnot = gameClassLoader == targetClassLoader;
         boolean containsKnot = false;
         if (matchesKnot) {
            containsKnot = true;
         } else {
            for (gameClassLoader = gameClassLoader.getParent();
               gameClassLoader != null && gameClassLoader.getParent() != gameClassLoader;
               gameClassLoader = gameClassLoader.getParent()
            ) {
               if (gameClassLoader == targetClassLoader) {
                  containsKnot = true;
               }
            }
         }

         if (!matchesKnot) {
            if (containsKnot) {
               Log.info(LogCategory.KNOT, "Environment: Target class loader is parent of game class loader.");
            } else {
               Log.warn(
                  LogCategory.KNOT,
                  "\n\n* CLASS LOADER MISMATCH! THIS IS VERY BAD AND WILL PROBABLY CAUSE WEIRD ISSUES! *\n - Expected game class loader: %s\n - Actual game class loader: %s\nCould not find the expected class loader in game class loader parents!\n",
                  FabricLauncherBase.getLauncher().getTargetClassLoader(),
                  gameClassLoader
               );
            }
         }
      }

      this.gameInstance = gameInstance;
      if (this.gameDir != null) {
         try {
            if (!this.gameDir.toRealPath().equals(newRunDir.toRealPath())) {
               Log.warn(
                  LogCategory.GENERAL,
                  "Inconsistent game execution directories: engine says %s, while initializer says %s...",
                  newRunDir.toRealPath(),
                  this.gameDir.toRealPath()
               );
               this.setGameDir(newRunDir);
            }
         } catch (IOException e) {
            Log.warn(LogCategory.GENERAL, "Exception while checking game execution directory consistency!", e);
         }
      } else {
         this.setGameDir(newRunDir);
      }
   }

   public ClassTweaker getClassTweaker() {
      return this.classTweaker;
   }

   public void setGameInstance(Object gameInstance) {
      if (this.getEnvironmentType() != EnvType.SERVER) {
         throw new UnsupportedOperationException("Cannot set game instance on a client!");
      }

      if (this.gameInstance != null) {
         throw new UnsupportedOperationException("Cannot overwrite current game instance!");
      }

      this.gameInstance = gameInstance;
   }

   @Override
   public String[] getLaunchArguments(boolean sanitize) {
      return this.getGameProvider().getLaunchArguments(sanitize);
   }

   @Override
   protected Path getModsDirectory0() {
      String directory = System.getProperty("fabric.modsFolder");
      return directory != null ? Paths.get(directory) : this.gameDir.resolve("mods");
   }

   static {
      LoaderUtil.verifyNotInTargetCl(FabricLoaderImpl.class);
   }

   public static class InitHelper {
      private static FabricLoaderImpl instance;

      public static FabricLoaderImpl get() {
         if (instance == null) {
            instance = new FabricLoaderImpl();
         }

         return instance;
      }
   }
}
