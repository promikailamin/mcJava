package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.minisat.orders.VariableComparator;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public final class Heap implements Serializable {
   private final IVecInt heap = new VecInt();
   private final IVecInt indices = new VecInt();
   private final VariableComparator comparator;

   private static int left(int i) {
      return i << 1;
   }

   private static int right(int i) {
      return i << 1 ^ 1;
   }

   private static int parent(int i) {
      return i >> 1;
   }

   void percolateUp(int i) {
      int x = this.heap.get(i);

      for (int p = parent(i); i != 1 && this.comparator.preferredTo(x, this.heap.get(p)); p = parent(p)) {
         this.heap.set(i, this.heap.get(p));
         this.indices.set(this.heap.get(p), i);
         i = p;
      }

      this.heap.set(i, x);
      this.indices.set(x, i);
   }

   void percolateDown(int i) {
      int x = this.heap.get(i);

      while (left(i) < this.heap.size()) {
         int child = right(i) < this.heap.size() && this.comparator.preferredTo(this.heap.get(right(i)), this.heap.get(left(i))) ? right(i) : left(i);
         if (!this.comparator.preferredTo(this.heap.get(child), x)) {
            break;
         }

         this.heap.set(i, this.heap.get(child));
         this.indices.set(this.heap.get(i), i);
         i = child;
      }

      this.heap.set(i, x);
      this.indices.set(x, i);
   }

   boolean ok(int n) {
      return n >= 0 && n < this.indices.size();
   }

   public Heap(VariableComparator comparator) {
      this.comparator = comparator;
      this.heap.push(-1);
   }

   public void setBounds(int size) {
      assert size >= 0;
      this.indices.growTo(size, 0);
   }

   public boolean inHeap(int n) {
      assert this.ok(n);
      return this.indices.get(n) != 0;
   }

   public void increase(int n) {
      assert this.ok(n);
      assert this.inHeap(n);
      this.percolateUp(this.indices.get(n));
   }

   public boolean empty() {
      return this.heap.size() == 1;
   }

   public int get(int i) {
      int r = this.heap.get(i);
      this.heap.set(i, this.heap.last());
      this.indices.set(this.heap.get(i), i);
      this.indices.set(r, 0);
      this.heap.pop();
      if (this.heap.size() > 1) {
         this.percolateDown(1);
      }

      return r;
   }

   public void insert(int n) {
      assert this.ok(n);
      this.indices.set(n, this.heap.size());
      this.heap.push(n);
      this.percolateUp(this.indices.get(n));
   }

   public int getmin() {
      return this.get(1);
   }
}
