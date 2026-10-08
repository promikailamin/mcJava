package net.fabricmc.loader.impl.launch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.jar.Manifest;
import net.fabricmc.api.EnvType;

public interface FabricLauncher {
   MappingConfiguration getMappingConfiguration();

   void addToClassPath(Path var1, String... var2);

   void setAllowedPrefixes(Path var1, String... var2);

   void setValidParentClassPath(Collection<Path> var1);

   EnvType getEnvironmentType();

   boolean isClassLoaded(String var1);

   Class<?> loadIntoTarget(String var1) throws ClassNotFoundException;

   InputStream getResourceAsStream(String var1);

   ClassLoader getTargetClassLoader();

   byte[] getClassByteArray(String var1, boolean var2) throws IOException;

   Manifest getManifest(Path var1);

   boolean isDevelopment();

   String getEntrypoint();

   List<Path> getClassPath();
}
