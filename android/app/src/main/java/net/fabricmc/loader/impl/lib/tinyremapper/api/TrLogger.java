package net.fabricmc.loader.impl.lib.tinyremapper.api;

public interface TrLogger {
   void log(TrLogger.Level var1, String var2);

   default void log(TrLogger.Level level, String message, Object... args) {
      this.log(level, String.format(message, args));
   }

   default void warn(String message) {
      this.log(TrLogger.Level.WARN, message);
   }

   default void warn(String message, Object... args) {
      this.log(TrLogger.Level.WARN, message, args);
   }

   default void error(String message) {
      this.log(TrLogger.Level.ERROR, message);
   }

   default void error(String message, Object... args) {
      this.log(TrLogger.Level.ERROR, message, args);
   }

   enum Level {
      DEBUG,
      INFO,
      WARN,
      ERROR;
   }
}
