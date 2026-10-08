package net.fabricmc.loader.impl.util;

import java.io.UncheckedIOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public final class ExceptionUtil {
   private static final boolean THROW_DIRECTLY = SystemProperties.isSet("fabric.debug.throwDirectly");

   public static <T extends Throwable> T gatherExceptions(Throwable exc, T prev, Function<Throwable, T> mainExcFactory) throws T {
      exc = unwrap(exc);
      if (THROW_DIRECTLY) {
         throw mainExcFactory.apply(exc);
      }

      if (prev == null) {
         return mainExcFactory.apply(exc);
      }

      if (exc != prev) {
         for (Throwable t : prev.getSuppressed()) {
            if (exc.equals(t)) {
               return prev;
            }
         }

         prev.addSuppressed(exc);
      }

      return prev;
   }

   public static RuntimeException wrap(Throwable exc) {
      if (exc instanceof RuntimeException) {
         return (RuntimeException)exc;
      }

      exc = unwrap(exc);
      return exc instanceof RuntimeException ? (RuntimeException)exc : new ExceptionUtil.WrappedException(exc);
   }

   private static Throwable unwrap(Throwable exc) {
      if (exc instanceof ExceptionUtil.WrappedException
         || exc instanceof UncheckedIOException
         || exc instanceof ExecutionException
         || exc instanceof CompletionException) {
         Throwable ret = exc.getCause();
         if (ret != null) {
            return unwrap(ret);
         }
      }

      return exc;
   }

   public static final class WrappedException extends RuntimeException {
      public WrappedException(Throwable cause) {
         super(cause);
      }
   }
}
