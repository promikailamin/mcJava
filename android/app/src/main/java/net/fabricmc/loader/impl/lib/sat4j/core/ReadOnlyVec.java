package net.fabricmc.loader.impl.lib.sat4j.core;

import java.util.Comparator;
import java.util.Iterator;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;

public final class ReadOnlyVec<T> implements IVec<T> {
   private final IVec<T> vec;

   public ReadOnlyVec(IVec<T> vec) {
      this.vec = vec;
   }

   @Override
   public void clear() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void copyTo(IVec<T> copy) {
      this.vec.copyTo(copy);
   }

   @Override
   public <E> void copyTo(E[] dest) {
      this.vec.copyTo(dest);
   }

   @Override
   public T delete(int i) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void ensure(int nsize) {
      throw new UnsupportedOperationException();
   }

   @Override
   public T get(int i) {
      return this.vec.get(i);
   }

   @Override
   public boolean isEmpty() {
      return this.vec.isEmpty();
   }

   @Override
   public Iterator<T> iterator() {
      return this.vec.iterator();
   }

   @Override
   public T last() {
      return this.vec.last();
   }

   @Override
   public void moveTo(IVec<T> dest) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void moveTo(int dest, int source) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void pop() {
      throw new UnsupportedOperationException();
   }

   @Override
   public IVec<T> push(T elem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void remove(T elem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void removeFromLast(T elem) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void set(int i, T o) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void shrinkTo(int newsize) {
      throw new UnsupportedOperationException();
   }

   @Override
   public int size() {
      return this.vec.size();
   }

   @Override
   public void sort(Comparator<T> comparator) {
      throw new UnsupportedOperationException();
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

   public IVec<T> clone() {
      IVec<T> cloned = new Vec<>(this.size());
      this.copyTo(cloned);
      return new ReadOnlyVec<>(cloned);
   }
}
