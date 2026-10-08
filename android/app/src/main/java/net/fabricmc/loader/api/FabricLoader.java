package net.fabricmc.loader.api;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;

public interface FabricLoader {
   static FabricLoader getInstance() {
      FabricLoader ret = FabricLoaderImpl.INSTANCE;
      if (ret == null) {
         throw new RuntimeException("Accessed FabricLoader too early!");
      } else {
         return ret;
      }
   }

   <T> List<T> getEntrypoints(String var1, Class<T> var2);

   <T> List<EntrypointContainer<T>> getEntrypointContainers(String var1, Class<T> var2);

   <T> void invokeEntrypoints(String var1, Class<T> var2, Consumer<? super T> var3);

   ObjectShare getObjectShare();

   MappingResolver getMappingResolver();

   Optional<ModContainer> getModContainer(String var1);

   Collection<ModContainer> getAllMods();

   boolean isModLoaded(String var1);

   boolean isDevelopmentEnvironment();

   EnvType getEnvironmentType();

   String getRawGameVersion();

   @Deprecated
   Object getGameInstance();

   Path getGameDir();

   @Deprecated
   File getGameDirectory();

   Path getConfigDir();

   @Deprecated
   File getConfigDirectory();

   String[] getLaunchArguments(boolean var1);
}
