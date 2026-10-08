package net.fabricmc.loader.impl.game.minecraft;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.impl.util.ManifestUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.LogHandler;
import net.fabricmc.loader.impl.util.log.LogLevel;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;

public final class Log4jLogHandler implements LogHandler {
   @Override
   public boolean shouldLog(LogLevel level, LogCategory category) {
      return getLogger(category).isEnabled(translateLogLevel(level));
   }

   @Override
   public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
      getLogger(category).log(translateLogLevel(level), msg, exc);
   }

   private static Logger getLogger(LogCategory category) {
      Logger ret = (Logger)category.data;
      if (ret == null) {
         category.data = ret = LogManager.getLogger(category.toString());
      }

      return ret;
   }

   private static Level translateLogLevel(LogLevel level) {
      if (level == LogLevel.ERROR) {
         return Level.ERROR;
      } else if (level == LogLevel.WARN) {
         return Level.WARN;
      } else if (level == LogLevel.INFO) {
         return Level.INFO;
      } else if (level == LogLevel.DEBUG) {
         return Level.DEBUG;
      } else if (level == LogLevel.TRACE) {
         return Level.TRACE;
      } else {
         throw new IllegalArgumentException("unknown log level: " + level);
      }
   }

   @Override
   public void close() {
   }

   private static boolean needsLookupRemoval() {
      Manifest manifest;
      try {
         manifest = ManifestUtil.readManifest(LogManager.class);
      } catch (IOException | URISyntaxException e) {
         Log.warn(LogCategory.GAME_PROVIDER, "Can't read Log4J2 Manifest", e);
         return true;
      }

      if (manifest == null) {
         return true;
      }

      String title = ManifestUtil.getManifestValue(manifest, Name.IMPLEMENTATION_TITLE);
      if (title != null && title.toLowerCase(Locale.ENGLISH).contains("log4j")) {
         String version = ManifestUtil.getManifestValue(manifest, Name.IMPLEMENTATION_VERSION);
         if (version == null) {
            return true;
         }

         try {
            return Version.parse(version).compareTo(Version.parse("2.16")) < 0;
         } catch (VersionParsingException e) {
            Log.warn(LogCategory.GAME_PROVIDER, "Can't parse Log4J2 Manifest version %s", version, e);
            return true;
         }
      } else {
         return true;
      }
   }

   private static void patchJndi() {
      LoggerContext context = LogManager.getContext(false);

      try {
         context.getClass().getMethod("addPropertyChangeListener", PropertyChangeListener.class).invoke(context, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
               if (evt.getPropertyName().equals("config")) {
                  Log4jLogHandler.removeSubstitutionLookups(true);
               }
            }
         });
      } catch (Exception e) {
         Log.warn(LogCategory.GAME_PROVIDER, "Can't register Log4J2 PropertyChangeListener: %s", e.toString());
      }

      removeSubstitutionLookups(false);
   }

   private static void removeSubstitutionLookups(boolean ignoreMissing) {
      try {
         LoggerContext context = LogManager.getContext(false);
         if (context.getClass().getName().equals("org.apache.logging.log4j.simple.SimpleLoggerContext")) {
            return;
         }

         Object config = context.getClass().getMethod("getConfiguration").invoke(context);
         Object substitutor = config.getClass().getMethod("getStrSubstitutor").invoke(config);
         Object varResolver = substitutor.getClass().getMethod("getVariableResolver").invoke(substitutor);
         if (varResolver == null) {
            return;
         }

         boolean removed = false;

         for (Field field : varResolver.getClass().getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
               field.setAccessible(true);
               Map<String, ?> map = (Map<String, ?>)field.get(varResolver);
               if (map.remove("jndi") != null) {
                  map.clear();
                  removed = true;
                  break;
               }
            }
         }

         if (!removed) {
            if (ignoreMissing) {
               return;
            }

            throw new RuntimeException("couldn't find JNDI lookup entry");
         }

         Log.debug(LogCategory.GAME_PROVIDER, "Removed Log4J2 substitution lookups");
      } catch (Exception e) {
         Log.warn(LogCategory.GAME_PROVIDER, "Can't remove Log4J2 JNDI substitution Lookup: %s", e.toString());
      }
   }

   static {
      if (needsLookupRemoval()) {
         patchJndi();
      } else {
         Log.debug(LogCategory.GAME_PROVIDER, "Log4J2 JNDI removal is unnecessary");
      }
   }
}
