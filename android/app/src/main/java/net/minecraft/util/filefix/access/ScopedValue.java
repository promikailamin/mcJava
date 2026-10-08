package net.minecraft.util.filefix.access;

import org.jspecify.annotations.Nullable;

/**
 * Minimal stand-in for {@code java.lang.ScopedValue} (JDK 21) that is safe on Android.
 * Values are bound per-thread; nesting within a single thread restores the previous value.
 */
public class ScopedValue<T> {

   private final ThreadLocal<T> value = new ThreadLocal<>();

   private ScopedValue() {
   }

   public static <T> ScopedValue<T> newInstance() {
      return new ScopedValue<>();
   }

   public @Nullable T get() {
      return this.value.get();
   }

   public T orElse(T other) {
      T current = this.value.get();
      return current != null ? current : other;
   }

   public boolean isBound() {
      return this.value.get() != null;
   }

   public static <T> Carrier<T> where(ScopedValue<T> scopedValue, T newValue) {
      return new Carrier<>(scopedValue, newValue);
   }

   public static final class Carrier<T> {
      private final ScopedValue<T> value;
      private final T newValue;

      private Carrier(final ScopedValue<T> value, final T newValue) {
         this.value = value;
         this.newValue = newValue;
      }

      public void run(Runnable action) {
         T previous = this.value.value.get();
         this.value.value.set(this.newValue);
         try {
            action.run();
         } finally {
            if (previous != null) {
               this.value.value.set(previous);
            } else {
               this.value.value.remove();
            }
         }
      }
   }
}