package net.minecraft.util;

/**
 * Lightweight replacement for {@code java.lang.management.ThreadInfo} that is safe on
 * Android. Wraps a live {@link Thread}; matches the accessor surface the game uses.
 */
public class ThreadInfo {

   private final Thread thread;

   public ThreadInfo(final Thread thread) {
      this.thread = thread;
   }

   public String getThreadName() {
      return this.thread.getName();
   }

   public boolean isDaemon() {
      return this.thread.isDaemon();
   }

   public Thread.State getThreadState() {
      return this.thread.getState();
   }

   public long getThreadId() {
      return this.thread.getId();
   }

   public StackTraceElement[] getStackTrace() {
      return this.thread.getStackTrace();
   }

   @Override
   public String toString() {
      return "\"" + this.getThreadName() + "\""
         + (this.isDaemon() ? " daemon" : "")
         + " prio=" + this.thread.getPriority()
         + " id=" + this.getThreadId()
         + " state=" + this.getThreadState();
   }
}