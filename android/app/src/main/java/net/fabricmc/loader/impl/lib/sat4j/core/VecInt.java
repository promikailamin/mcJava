package net.fabricmc.loader.impl.lib.sat4j.core;

import java.util.Comparator;
import java.util.NoSuchElementException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.IteratorInt;

public final class VecInt implements IVecInt {
   public static final IVecInt EMPTY = new EmptyVecInt();
   private int nbelem;
   private int[] myarray;

   public VecInt() {
      this(5);
   }

   public VecInt(int size) {
      this.myarray = new int[size];
   }

   public VecInt(int[] lits) {
      this.myarray = lits;
      this.nbelem = lits.length;
   }

   public VecInt(int size, int pad) {
      this.myarray = new int[size];

      for (int i = 0; i < size; i++) {
         this.myarray[i] = pad;
      }

      this.nbelem = size;
   }

   @Override
   public int size() {
      return this.nbelem;
   }

   @Override
   public void shrink(int nofelems) {
      this.nbelem -= nofelems;
   }

   @Override
   public IVecInt pop() {
      this.nbelem--;
      return this;
   }

   @Override
   public void growTo(int newsize, int pad) {
      this.ensure(newsize);

      while (--newsize >= 0) {
         this.myarray[this.nbelem++] = pad;
      }
   }

   @Override
   public void ensure(int nsize) {
      if (nsize >= this.myarray.length) {
         int[] narray = new int[Math.max(nsize, this.nbelem * 2)];
         System.arraycopy(this.myarray, 0, narray, 0, this.nbelem);
         this.myarray = narray;
      }
   }

   @Override
   public IVecInt push(int elem) {
      this.ensure(this.nbelem + 1);
      this.myarray[this.nbelem++] = elem;
      return this;
   }

   @Override
   public void unsafePush(int elem) {
      this.myarray[this.nbelem++] = elem;
   }

   @Override
   public void clear() {
      this.nbelem = 0;
   }

   @Override
   public int last() {
      return this.myarray[this.nbelem - 1];
   }

   @Override
   public int get(int i) {
      return this.myarray[i];
   }

   @Override
   public int unsafeGet(int i) {
      return this.myarray[i];
   }

   @Override
   public void set(int i, int o) {
      assert i >= 0 && i < this.nbelem;
      this.myarray[i] = o;
   }

   @Override
   public boolean contains(int e) {
      int[] workArray = this.myarray;

      for (int i = 0; i < this.nbelem; i++) {
         if (workArray[i] == e) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int indexOf(int e) {
      int[] workArray = this.myarray;

      for (int i = 0; i < this.nbelem; i++) {
         if (workArray[i] == e) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public void copyTo(IVecInt copy) {
      VecInt ncopy = (VecInt)copy;
      int nsize = this.nbelem + ncopy.nbelem;
      ncopy.ensure(nsize);
      System.arraycopy(this.myarray, 0, ncopy.myarray, ncopy.nbelem, this.nbelem);
      ncopy.nbelem = nsize;
   }

   @Override
   public void copyTo(int[] is) {
      System.arraycopy(this.myarray, 0, is, 0, this.nbelem);
   }

   @Override
   public void moveTo(int dest, int source) {
      this.myarray[dest] = this.myarray[source];
   }

   @Override
   public void moveTo(int[] dest) {
      System.arraycopy(this.myarray, 0, dest, 0, this.nbelem);
      this.nbelem = 0;
   }

   @Override
   public void remove(int elem) {
      int j;
      for (j = 0; this.myarray[j] != elem; j++) {
         if (j == this.size()) {
            throw new NoSuchElementException();
         }
      }

      System.arraycopy(this.myarray, j + 1, this.myarray, j, this.size() - j - 1);
      this.pop();
   }

   @Override
   public int delete(int i) {
      int ith = this.myarray[i];
      this.myarray[i] = this.myarray[--this.nbelem];
      return ith;
   }

   @Override
   public String toString() {
      StringBuilder stb = new StringBuilder();

      for (int i = 0; i < this.nbelem - 1; i++) {
         stb.append(this.myarray[i]);
         stb.append(",");
      }

      if (this.nbelem > 0) {
         stb.append(this.myarray[this.nbelem - 1]);
      }

      return stb.toString();
   }

   void selectionSort(int from, int to) {
      for (int i = from; i < to - 1; i++) {
         int besti = i;

         for (int j = i + 1; j < to; j++) {
            if (this.myarray[j] < this.myarray[besti]) {
               besti = j;
            }
         }

         int tmp = this.myarray[i];
         this.myarray[i] = this.myarray[besti];
         this.myarray[besti] = tmp;
      }
   }

   void sort(int from, int to) {
      int width = to - from;
      if (width <= 15) {
         this.selectionSort(from, to);
      } else {
         int[] locarray = this.myarray;
         int pivot = locarray[width / 2 + from];
         int i = from - 1;
         int j = to;

         while (true) {
            while (locarray[++i] < pivot) {
            }

            while (pivot < locarray[--j]) {
            }

            if (i >= j) {
               this.sort(from, i);
               this.sort(i, to);
               break;
            }

            int tmp = locarray[i];
            locarray[i] = locarray[j];
            locarray[j] = tmp;
         }
      }
   }

   @Override
   public void sort() {
      this.sort(0, this.nbelem);
   }

   @Override
   public void sortUnique() {
      if (this.nbelem != 0) {
         this.sort(0, this.nbelem);
         int i = 1;
         int[] locarray = this.myarray;
         int last = locarray[0];

         for (int j = 1; j < this.nbelem; j++) {
            if (last < locarray[j]) {
               last = locarray[i] = locarray[j];
               i++;
            }
         }

         this.nbelem = i;
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof IVecInt) {
         IVecInt v = (IVecInt)obj;
         if (v.size() != this.nbelem) {
            return false;
         }

         for (int i = 0; i < this.nbelem; i++) {
            if (v.get(i) != this.myarray[i]) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      long sum = 0L;

      for (int i = 0; i < this.nbelem; i++) {
         sum += this.myarray[i];
      }

      return (int)sum / this.nbelem;
   }

   @Override
   public IteratorInt iterator() {
      return new IteratorInt() {
         private int i = 0;

         @Override
         public boolean hasNext() {
            return this.i < VecInt.this.nbelem;
         }

         @Override
         public int next() {
            if (this.i == VecInt.this.nbelem) {
               throw new NoSuchElementException();
            } else {
               return VecInt.this.myarray[this.i++];
            }
         }
      };
   }

   @Override
   public boolean isEmpty() {
      return this.nbelem == 0;
   }

   @Override
   public int[] toArray() {
      return this.myarray;
   }

   void selectionSort(int from, int to, Comparator<Integer> cmp) {
      for (int i = from; i < to - 1; i++) {
         int besti = i;

         for (int j = i + 1; j < to; j++) {
            if (cmp.compare(this.myarray[j], this.myarray[besti]) < 0) {
               besti = j;
            }
         }

         int tmp = this.myarray[i];
         this.myarray[i] = this.myarray[besti];
         this.myarray[besti] = tmp;
      }
   }

   void sort(int from, int to, Comparator<Integer> cmp) {
      int width = to - from;
      if (width <= 15) {
         this.selectionSort(from, to, cmp);
      } else {
         int pivot = this.myarray[width / 2 + from];
         int i = from - 1;
         int j = to;

         while (true) {
            while (cmp.compare(this.myarray[++i], pivot) < 0) {
            }

            while (cmp.compare(pivot, this.myarray[--j]) < 0) {
            }

            if (i >= j) {
               this.sort(from, i, cmp);
               this.sort(i, to, cmp);
               break;
            }

            int tmp = this.myarray[i];
            this.myarray[i] = this.myarray[j];
            this.myarray[j] = tmp;
         }
      }
   }

   @Override
   public void sort(Comparator<Integer> comparator) {
      this.sort(0, this.nbelem, comparator);
   }

   public IVecInt clone() {
      IVecInt cloned = new VecInt(this.size());
      this.copyTo(cloned);
      return cloned;
   }
}
