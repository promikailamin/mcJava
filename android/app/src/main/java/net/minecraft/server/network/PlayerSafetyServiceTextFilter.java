package net.minecraft.server.network;

import com.google.gson.JsonObject;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import org.jspecify.annotations.Nullable;

/**
 * Realms-hosted Player Safety text filtering (see {@code PlayerSafetyConfig}) talks to
 * Microsoft Entra via {@code com.microsoft.aad.msal4j}, a desktop OAuth stack that is not
 * available on Android. This port stubs the factory: the safety filter is simply not
 * installed, which is the same behaviour as an unset/empty filtering config.
 */
public class PlayerSafetyServiceTextFilter extends ServerTextFilter {

   protected PlayerSafetyServiceTextFilter(
      final URL chatEndpoint,
      final ServerTextFilter.MessageEncoder chatEncoder,
      final ServerTextFilter.IgnoreStrategy chatIgnoreStrategy,
      final ExecutorService workerPool
   ) {
      super(chatEndpoint, chatEncoder, chatIgnoreStrategy, workerPool);
   }

   public static @Nullable ServerTextFilter createTextFilterFromConfig(final String textFilteringConfig) {
      JsonObject parsedConfig = com.google.gson.GsonHelper.parse(textFilteringConfig);
      String apiServer = com.google.gson.GsonHelper.getAsString(parsedConfig, "apiServer", "");
      if (apiServer.isEmpty()) {
         LOGGER.warn("Player Safety service not enabled");
         return null;
      }
      LOGGER.warn("Player Safety service is unavailable in the Android port (MSAL is desktop-only), skipping filter");
      return null;
   }
}