package net.fabricmc.loader.impl.lib.sat4j.core;

import java.util.Comparator;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.IteratorInt;

final class EmptyVecInt implements IVecInt {
   @Override
   public int size() {
      return 0;
   }

   @Override
   public void shrink(int nofelems) {
   }

   @Override
   public IVecInt pop() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void growTo(int newsize, int pad) {
   }

   @Override
   public void ensure(int nsize) {
   }

   @Override
   public IVecInt push(int elem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void unsafePush(int elem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void clear() {
   }

   @Override
   public int last() {
      throw new UnsupportedOperationException();
   }

   @Override
   public int get(int i) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void set(int i, int o) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean contains(int e) {
      return false;
   }

   @Override
   public void copyTo(IVecInt copy) {
   }

   @Override
   public void copyTo(int[] is) {
   }

   @Override
   public void moveTo(int[] dest) {
   }

   @Override
   public void remove(int elem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public int delete(int i) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sort() {
   }

   @Override
   public void sortUnique() {
   }

   @Override
   public int unsafeGet(int eleem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void moveTo(int dest, int source) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isEmpty() {
      return true;
   }

   @Override
   public IteratorInt iterator() {
      return new IteratorInt() {
         @Override
         public boolean hasNext() {
            return false;
         }

         @Override
         public int next() {
            throw new UnsupportedOperationException();
         }
      };
   }

   @Override
   public int[] toArray() {
      throw new UnsupportedOperationException();
   }

   @Override
   public int indexOf(int e) {
      return -1;
   }

   @Override
   public String toString() {
      return "[]";
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof IVecInt ? ((IVecInt)o).isEmpty() : false;
   }

   @Override
   public int hashCode() {
      return 0;
   }

   @Override
   public void sort(Comparator<Integer> comparator) {
   }

   public IVecInt clone() {
      return new EmptyVecInt();
   }
}
