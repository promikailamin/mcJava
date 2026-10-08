package net.fabricmc.loader.impl.lib.sat4j.specs;

import java.io.Serializable;
import java.util.Comparator;

public interface IVecInt extends Serializable, Cloneable {
   int size();

   void shrink(int var1);

   IVecInt pop();

   void growTo(int var1, int var2);

   void ensure(int var1);

   IVecInt push(int var1);

   void unsafePush(int var1);

   int unsafeGet(int var1);

   void clear();

   int last();

   int get(int var1);

   void set(int var1, int var2);

   boolean contains(int var1);

   int indexOf(int var1);

   void copyTo(IVecInt var1);

   void copyTo(int[] var1);

   void moveTo(int[] var1);

   void moveTo(int var1, int var2);

   void remove(int var1);

   int delete(int var1);

   void sort();

   void sort(Comparator<Integer> var1);

   void sortUnique();

   boolean isEmpty();

   IteratorInt iterator();

   int[] toArray();
}
