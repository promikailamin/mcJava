package net.fabricmc.loader.impl.util.log;

public interface LogHandler {
   void log(long var1, LogLevel var3, LogCategory var4, String var5, Throwable var6, boolean var7, boolean var8);

   boolean shouldLog(LogLevel var1, LogCategory var2);

   void close();
}
