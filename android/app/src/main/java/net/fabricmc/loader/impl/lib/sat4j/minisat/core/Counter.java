package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

public class Counter {
   private int value;

   public Counter() {
      this(1);
   }

   public Counter(int initialValue) {
      this.value = initialValue;
   }

   public void inc() {
      this.value++;
   }

   public void dec() {
      this.value--;
   }

   @Override
   public String toString() {
      return String.valueOf(this.value);
   }
}
