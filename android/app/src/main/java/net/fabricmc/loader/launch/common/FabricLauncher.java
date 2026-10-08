package net.fabricmc.loader.launch.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import net.fabricmc.api.EnvType;

@Deprecated
public interface FabricLauncher {
   void propose(URL var1);

   EnvType getEnvironmentType();

   boolean isClassLoaded(String var1);

   InputStream getResourceAsStream(String var1);

   ClassLoader getTargetClassLoader();

   byte[] getClassByteArray(String var1, boolean var2) throws IOException;

   boolean isDevelopment();

   Collection<URL> getLoadTimeDependencies();
}
