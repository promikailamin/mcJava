package net.fabricmc.loader.impl.util.log;

import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLogger;

public final class TinyRemapperLoggerAdapter implements TrLogger {
   private final LogCategory category;

   public TinyRemapperLoggerAdapter(LogCategory category) {
      this.category = category;
   }

   @Override
   public void log(TrLogger.Level level, String message) {
      switch (level) {
         case ERROR:
            Log.error(this.category, message);
            break;
         case WARN:
            Log.warn(this.category, message);
            break;
         case INFO:
            Log.info(this.category, message);
            break;
         case DEBUG:
            Log.debug(this.category, message);
      }
   }
}
