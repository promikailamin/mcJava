package net.fabricmc.loader;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import net.fabricmc.loader.impl.FabricLoaderImpl;

@Deprecated
public abstract class FabricLoader implements net.fabricmc.loader.api.FabricLoader {
   @Deprecated
   public static final FabricLoader INSTANCE = FabricLoaderImpl.InitHelper.get();

   public File getModsDirectory() {
      return this.getModsDirectory0().toFile();
   }

   @Override
   public abstract <T> List<T> getEntrypoints(String var1, Class<T> var2);

   public Collection<ModContainer> getModContainers() {
      return this.getAllMods();
   }

   public List<ModContainer> getMods() {
      return (List<ModContainer>)this.getAllMods();
   }

   protected abstract Path getModsDirectory0();
}
