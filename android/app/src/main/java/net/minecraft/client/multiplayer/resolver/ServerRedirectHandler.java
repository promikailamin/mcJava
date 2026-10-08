package net.minecraft.client.multiplayer.resolver;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import org.slf4j.Logger;

@FunctionalInterface
public interface ServerRedirectHandler {
   Logger LOGGER = LogUtils.getLogger();
   ServerRedirectHandler EMPTY = originalAddress -> Optional.empty();

   Optional<ServerAddress> lookupRedirect(ServerAddress originalAddress);

   static ServerRedirectHandler createDnsSrvRedirectHandler() {
      // DNS SRV lookups rely on JNDI (com.sun.jndi), which is not available on the
      // Android runtime. The handler is disabled, so servers with SRV records must
      // be joined by their direct address.
      LOGGER.info("SRV redirect resolution disabled on the Android port (JNDI DNS unavailable)");
      return EMPTY;
   }
}