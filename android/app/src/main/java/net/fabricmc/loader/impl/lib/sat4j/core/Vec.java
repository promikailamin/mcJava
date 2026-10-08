package net.fabricmc.loader.impl.lib.sat4j.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;

public final class Vec<T> implements IVec<T> {
   private int nbelem;
   private T[] myarray;

   public Vec() {
      this(5);
   }

   public Vec(int size) {
      this.myarray = (T[])(new Object[size]);
   }

   @Override
   public int size() {
      return this.nbelem;
   }

   @Override
   public void shrinkTo(int newsize) {
      for (int i = this.nbelem; i > newsize; i--) {
         this.myarray[i - 1] = null;
      }

      this.nbelem = newsize;
   }

   @Override
   public void pop() {
      this.myarray[--this.nbelem] = null;
   }

   @Override
   public void ensure(int nsize) {
      if (nsize >= this.myarray.length) {
         T[] narray = (T[])(new Object[Math.max(nsize, this.nbelem * 2)]);
         System.arraycopy(this.myarray, 0, narray, 0, this.nbelem);
         this.myarray = narray;
      }
   }

   @Override
   public IVec<T> push(T elem) {
      this.ensure(this.nbelem + 1);
      this.myarray[this.nbelem++] = elem;
      return this;
   }

   @Override
   public void clear() {
      Arrays.fill(this.myarray, 0, this.nbelem, null);
      this.nbelem = 0;
   }

   @Override
   public T last() {
      return this.myarray[this.nbelem - 1];
   }

   @Override
   public T get(int index) {
      return this.myarray[index];
   }

   @Override
   public void set(int index, T elem) {
      this.myarray[index] = elem;
   }

   @Override
   public void remove(T elem) {
      int j;
      for (j = 0; this.myarray[j] != elem; j++) {
         if (j == this.size()) {
            throw new NoSuchElementException();
         }
      }

      System.arraycopy(this.myarray, j + 1, this.myarray, j, this.size() - j - 1);
      this.myarray[--this.nbelem] = null;
   }

   @Override
   public void removeFromLast(T elem) {
      int j;
      for (j = this.nbelem - 1; this.myarray[j] != elem; j--) {
         if (j == -1) {
            throw new NoSuchElementException();
         }
      }

      System.arraycopy(this.myarray, j + 1, this.myarray, j, this.size() - j - 1);
      this.myarray[--this.nbelem] = null;
   }

   @Override
   public T delete(int index) {
      T ith = this.myarray[index];
      this.myarray[index] = this.myarray[--this.nbelem];
      this.myarray[this.nbelem] = null;
      return ith;
   }

   @Override
   public void copyTo(IVec<T> copy) {
      Vec<T> ncopy = (Vec<T>)copy;
      int nsize = this.nbelem + ncopy.nbelem;
      copy.ensure(nsize);
      System.arraycopy(this.myarray, 0, ncopy.myarray, ncopy.nbelem, this.nbelem);
      ncopy.nbelem = nsize;
   }

   @Override
   public <E> void copyTo(E[] dest) {
      System.arraycopy(this.myarray, 0, dest, 0, this.nbelem);
   }

   @Override
   public void moveTo(IVec<T> dest) {
      this.copyTo(dest);
      this.clear();
   }

   @Override
   public void moveTo(int dest, int source) {
      if (dest != source) {
         this.myarray[dest] = this.myarray[source];
         this.myarray[source] = null;
      }
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

   void selectionSort(int from, int to, Comparator<T> cmp) {
      for (int i = from; i < to - 1; i++) {
         int besti = i;

         for (int j = i + 1; j < to; j++) {
            if (cmp.compare(this.myarray[j], this.myarray[besti]) < 0) {
               besti = j;
            }
         }

         T tmp = this.myarray[i];
         this.myarray[i] = this.myarray[besti];
         this.myarray[besti] = tmp;
      }
   }

   void sort(int from, int to, Comparator<T> cmp) {
      int width = to - from;
      if (width <= 15) {
         this.selectionSort(from, to, cmp);
      } else {
         T pivot = this.myarray[width / 2 + from];
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

            T tmp = this.myarray[i];
            this.myarray[i] = this.myarray[j];
            this.myarray[j] = tmp;
         }
      }
   }

   @Override
   public void sort(Comparator<T> comparator) {
      this.sort(0, this.nbelem, comparator);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof IVec) {
         IVec<?> v = (IVec<?>)obj;
         if (v.size() != this.size()) {
            return false;
         }

         for (int i = 0; i < this.size(); i++) {
            if (!v.get(i).equals(this.get(i))) {
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
      int sum = 0;

      for (int i = 0; i < this.nbelem; i++) {
         sum += this.myarray[i].hashCode() / this.nbelem;
      }

      return sum;
   }

   @Override
   public Iterator<T> iterator() {
      return new Iterator<T>() {
         private int i = 0;

         @Override
         public boolean hasNext() {
            return this.i < Vec.this.nbelem;
         }

         @Override
         public T next() {
            if (this.i == Vec.this.nbelem) {
               throw new NoSuchElementException();
            } else {
               return Vec.this.myarray[this.i++];
            }
         }

         @Override
         public void remove() {
            throw new UnsupportedOperationException();
         }
      };
   }

   @Override
   public boolean isEmpty() {
      return this.nbelem == 0;
   }

   public IVec<T> clone() {
      IVec<T> cloned = new Vec<>(this.size());
      this.copyTo(cloned);
      return cloned;
   }
}
