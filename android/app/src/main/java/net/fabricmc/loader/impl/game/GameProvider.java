package net.fabricmc.loader.impl.game;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.LoaderUtil;

public interface GameProvider {
   String getGameId();

   String getGameName();

   String getRawGameVersion();

   String getNormalizedGameVersion();

   Collection<GameProvider.BuiltinMod> getBuiltinMods();

   String getEntrypoint();

   Path getLaunchDirectory();

   boolean requiresUrlClassLoader();

   Set<GameProvider.BuiltinTransform> getBuiltinTransforms(String var1);

   boolean isEnabled();

   boolean locateGame(FabricLauncher var1, String[] var2);

   void initialize(FabricLauncher var1);

   GameTransformer getEntrypointTransformer();

   void unlockClassPath(FabricLauncher var1);

   void launch(ClassLoader var1);

   default boolean displayCrash(Throwable exception, String context) {
      return false;
   }

   Arguments getArguments();

   String[] getLaunchArguments(boolean var1);

   default String getRuntimeNamespace(String defaultNs) {
      return defaultNs;
   }

   default String getDefaultModDistributionNamespace(String defaultNs) {
      return defaultNs;
   }

   default boolean canOpenErrorGui() {
      return true;
   }

   default boolean hasAwtSupport() {
      return LoaderUtil.hasAwtSupport();
   }

   class BuiltinMod {
      public final List<Path> paths;
      public final ModMetadata metadata;

      public BuiltinMod(List<Path> paths, ModMetadata metadata) {
         Objects.requireNonNull(paths, "null paths");
         Objects.requireNonNull(metadata, "null metadata");
         this.paths = paths;
         this.metadata = metadata;
      }
   }

   enum BuiltinTransform {
      STRIP_ENVIRONMENT,
      WIDEN_ALL_PACKAGE_ACCESS,
      CLASS_TWEAKS;
   }
}
