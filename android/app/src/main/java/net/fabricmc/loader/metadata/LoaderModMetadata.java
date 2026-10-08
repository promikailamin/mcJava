package net.fabricmc.loader.metadata;

import java.util.Collection;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.metadata.ModMetadata;

@Deprecated
public interface LoaderModMetadata extends ModMetadata {
   boolean loadsInEnvironment(EnvType var1);

   List<? extends EntrypointMetadata> getEntrypoints(String var1);

   Collection<String> getEntrypointKeys();
}
