package net.fabricmc.loader.impl.metadata;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModDependency;

public interface LoaderModMetadata extends net.fabricmc.loader.metadata.LoaderModMetadata {
   int getSchemaVersion();

   default String getOldStyleLanguageAdapter() {
      return "net.fabricmc.loader.language.JavaLanguageAdapter";
   }

   Map<String, String> getLanguageAdapterDefinitions();

   Collection<NestedJarEntry> getJars();

   Collection<String> getMixinConfigs(EnvType var1);

   String getClassTweaker();

   @Override
   boolean loadsInEnvironment(EnvType var1);

   Collection<String> getOldInitializers();

   @Override
   List<EntrypointMetadata> getEntrypoints(String var1);

   @Override
   Collection<String> getEntrypointKeys();

   void emitFormatWarnings();

   void setVersion(Version var1);

   void setDependencies(Collection<ModDependency> var1);
}
