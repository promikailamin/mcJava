package net.fabricmc.loader.impl.launch.knot;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.jar.Manifest;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.GameProvider;

interface KnotClassLoaderInterface {
   static KnotClassLoaderInterface create(boolean useCompatibility, boolean isDevelopment, EnvType envType, GameProvider provider) {
      return useCompatibility
         ? new KnotCompatibilityClassLoader(isDevelopment, envType, provider).getDelegate()
         : new KnotClassLoader(isDevelopment, envType, provider).getDelegate();
   }

   void initializeTransformers();

   ClassLoader getClassLoader();

   void addCodeSource(Path var1);

   void setAllowedPrefixes(Path var1, String... var2);

   void setValidParentClassPath(Collection<Path> var1);

   Manifest getManifest(Path var1);

   boolean isClassLoaded(String var1);

   Class<?> loadIntoTarget(String var1) throws ClassNotFoundException;

   byte[] getRawClassBytes(String var1) throws IOException;

   byte[] getPreMixinClassBytes(String var1);
}
