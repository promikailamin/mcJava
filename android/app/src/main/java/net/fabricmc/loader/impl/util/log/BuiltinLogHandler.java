package net.fabricmc.loader.impl.util.log;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.impl.util.LoaderUtil;

final class BuiltinLogHandler extends ConsoleLogHandler {
   private static final String DEFAULT_LOG_FILE = "fabricloader.log";
   private boolean configured;
   private boolean enableOutput;
   private List<BuiltinLogHandler.ReplayEntry> buffer = new ArrayList<>();
   private final Thread shutdownHook = new BuiltinLogHandler.ShutdownHook();

   BuiltinLogHandler() {
      Runtime.getRuntime().addShutdownHook(this.shutdownHook);
   }

   @Override
   public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
      boolean output;
      synchronized (this) {
         if (this.enableOutput) {
            output = true;
         } else if (level.isLessThan(LogLevel.ERROR)) {
            output = false;
         } else {
            this.startOutput();
            output = true;
         }

         if (this.buffer != null) {
            this.buffer.add(new BuiltinLogHandler.ReplayEntry(time, level, category, msg, exc));
         }
      }

      if (output) {
         super.log(time, level, category, msg, exc, fromReplay, wasSuppressed);
      }
   }

   private void startOutput() {
      if (!this.enableOutput) {
         if (this.buffer != null) {
            for (int i = 0; i < this.buffer.size(); i++) {
               BuiltinLogHandler.ReplayEntry entry = this.buffer.get(i);
               super.log(entry.time, entry.level, entry.category, entry.msg, entry.exc, true, true);
            }
         }

         this.enableOutput = true;
      }
   }

   @Override
   public void close() {
      Thread shutdownHook = this.shutdownHook;
      if (shutdownHook != null) {
         try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
         } catch (IllegalStateException var3) {
         }
      }
   }

   synchronized void configure(boolean buffer, boolean output) {
      if (!buffer && !output) {
         throw new IllegalArgumentException("can't both disable buffering and the output");
      }

      if (output) {
         this.startOutput();
      } else {
         this.enableOutput = false;
      }

      if (buffer) {
         if (this.buffer == null) {
            this.buffer = new ArrayList<>();
         }
      } else {
         this.buffer = null;
      }

      this.configured = true;
   }

   synchronized void finishConfig() {
      if (!this.configured) {
         this.configure(false, true);
      }
   }

   synchronized boolean replay(LogHandler target) {
      if (this.buffer != null && !this.buffer.isEmpty()) {
         for (int i = 0; i < this.buffer.size(); i++) {
            BuiltinLogHandler.ReplayEntry entry = this.buffer.get(i);
            target.log(entry.time, entry.level, entry.category, entry.msg, entry.exc, true, !this.enableOutput);
         }

         return true;
      } else {
         return false;
      }
   }

   private static final class ReplayEntry {
      final long time;
      final LogLevel level;
      final LogCategory category;
      final String msg;
      final Throwable exc;

      ReplayEntry(long time, LogLevel level, LogCategory category, String msg, Throwable exc) {
         this.time = time;
         this.level = level;
         this.category = category;
         this.msg = msg;
         this.exc = exc;
      }
   }

   private final class ShutdownHook extends Thread {
      ShutdownHook() {
         super("BuiltinLogHandler shutdown hook");
      }

      @Override
      public void run() {
         synchronized (BuiltinLogHandler.this) {
            if (BuiltinLogHandler.this.buffer != null && !BuiltinLogHandler.this.buffer.isEmpty()) {
               if (!BuiltinLogHandler.this.enableOutput) {
                  BuiltinLogHandler.this.enableOutput = true;

                  for (int i = 0; i < BuiltinLogHandler.this.buffer.size(); i++) {
                     BuiltinLogHandler.ReplayEntry entry = BuiltinLogHandler.this.buffer.get(i);
                     BuiltinLogHandler.super.log(entry.time, entry.level, entry.category, entry.msg, entry.exc, true, true);
                  }
               }

               String fileName = System.getProperty("fabric.log.file", "fabricloader.log");
               if (!fileName.isEmpty()) {
                  try {
                     Path file = LoaderUtil.normalizePath(Paths.get(fileName));
                     Files.createDirectories(file.getParent());
                     Writer writer = Files.newBufferedWriter(file, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

                     try {
                        for (int i = 0; i < BuiltinLogHandler.this.buffer.size(); i++) {
                           BuiltinLogHandler.ReplayEntry entry = BuiltinLogHandler.this.buffer.get(i);
                           writer.write(ConsoleLogHandler.formatLog(entry.time, entry.level, entry.category, entry.msg, entry.exc));
                        }
                     } catch (Throwable var9) {
                        if (writer != null) {
                           try {
                              writer.close();
                           } catch (Throwable var8) {
                              var9.addSuppressed(var8);
                           }
                        }

                        throw var9;
                     }

                     if (writer != null) {
                        writer.close();
                     }
                  } catch (IOException e) {
                     System.err.printf("Error saving log: %s", e);
                  }
               }
            }
         }
      }
   }
}
