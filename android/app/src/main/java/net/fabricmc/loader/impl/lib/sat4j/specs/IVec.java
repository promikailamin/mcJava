package net.fabricmc.loader.impl.lib.sat4j.specs;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;

public interface IVec<T> extends Serializable, Cloneable {
   int size();

   void shrinkTo(int var1);

   void pop();

   void ensure(int var1);

   IVec<T> push(T var1);

   void clear();

   T last();

   T get(int var1);

   void set(int var1, T var2);

   void remove(T var1);

   void removeFromLast(T var1);

   T delete(int var1);

   void copyTo(IVec<T> var1);

   <E> void copyTo(E[] var1);

   void moveTo(IVec<T> var1);

   void moveTo(int var1, int var2);

   void sort(Comparator<T> var1);

   boolean isEmpty();

   Iterator<T> iterator();
}
