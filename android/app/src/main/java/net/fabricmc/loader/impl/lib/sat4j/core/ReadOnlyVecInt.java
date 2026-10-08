package net.fabricmc.loader.impl.lib.sat4j.core;

import java.util.Comparator;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.IteratorInt;

public final class ReadOnlyVecInt implements IVecInt {
   private final IVecInt vec;

   public ReadOnlyVecInt(IVecInt vec) {
      this.vec = vec;
   }

   @Override
   public void clear() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean contains(int e) {
      return this.vec.contains(e);
   }

   @Override
   public void copyTo(IVecInt copy) {
      this.vec.copyTo(copy);
   }

   @Override
   public void copyTo(int[] is) {
      this.vec.copyTo(is);
   }

   @Override
   public int delete(int i) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void ensure(int nsize) {
      throw new UnsupportedOperationException();
   }

   @Override
   public int get(int i) {
      return this.vec.get(i);
   }

   @Override
   public void growTo(int newsize, int pad) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isEmpty() {
      return this.vec.isEmpty();
   }

   @Override
   public IteratorInt iterator() {
      return this.vec.iterator();
   }

   @Override
   public int last() {
      return this.vec.last();
   }

   @Override
   public void moveTo(int[] dest) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void moveTo(int dest, int source) {
      throw new UnsupportedOperationException();
   }

   @Override
   public IVecInt pop() {
      throw new UnsupportedOperationException();
   }

   @Override
   public IVecInt push(int elem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void remove(int elem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void set(int i, int o) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void shrink(int nofelems) {
      throw new UnsupportedOperationException();
   }

   @Override
   public int size() {
      return this.vec.size();
   }

   @Override
   public void sort() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void sortUnique() {
      throw new UnsupportedOperationException();
   }

   @Override
   public int unsafeGet(int eleem) {
      return this.vec.unsafeGet(eleem);
   }

   @Override
   public void unsafePush(int elem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public int[] toArray() {
      int[] copy = new int[this.vec.size()];
      this.vec.copyTo(copy);
      return copy;
   }

   @Override
   public int indexOf(int e) {
      return this.vec.indexOf(e);
   }

   @Override
   public String toString() {
      return this.vec.toString();
   }

   @Override
   public int hashCode() {
      return this.vec.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      return this.vec.equals(obj);
   }

   @Override
   public void sort(Comparator<Integer> comparator) {
      throw new UnsupportedOperationException();
   }

   public IVecInt clone() {
      IVecInt cloned = new VecInt(this.size());
      this.copyTo(cloned);
      return cloned;
   }
}
