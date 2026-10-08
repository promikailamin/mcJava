package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;

public class CircularBuffer implements Serializable {
   private final int[] values;
   private int index = 0;
   private long sum = 0L;
   private boolean full = false;

   public CircularBuffer(int capacity) {
      this.values = new int[capacity];
   }

   public void push(int value) {
      if (!this.full) {
         this.values[this.index++] = value;
         this.sum += value;
         if (this.index == this.values.length) {
            this.full = true;
            this.index = -1;
         }
      } else {
         this.index++;
         if (this.index == this.values.length) {
            this.index = 0;
         }

         this.sum = this.sum - this.values[this.index];
         this.values[this.index] = value;
         this.sum += value;
      }
   }

   public long average() {
      if (this.full) {
         return this.sum / this.values.length;
      } else {
         return this.index == 0 ? 0L : this.sum / this.index;
      }
   }

   public void clear() {
      this.index = 0;
      this.full = false;
      this.sum = 0L;
   }

   public boolean isFull() {
      return this.full;
   }
}
