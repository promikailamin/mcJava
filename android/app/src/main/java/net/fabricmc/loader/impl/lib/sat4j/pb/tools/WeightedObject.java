package net.fabricmc.loader.impl.lib.sat4j.pb.tools;

import java.math.BigInteger;

public final class WeightedObject<T> implements Comparable<WeightedObject<T>> {
   public final T thing;
   private BigInteger weight;

   private WeightedObject(T thing, BigInteger weight) {
      this.thing = thing;
      this.weight = weight;
   }

   public BigInteger getWeight() {
      return this.weight;
   }

   public int compareTo(WeightedObject<T> arg0) {
      return this.weight.compareTo(arg0.getWeight());
   }

   public static <E> WeightedObject<E> newWO(E e, BigInteger w) {
      return new WeightedObject<>(e, w);
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.thing == null ? 0 : this.thing.hashCode());
      return 31 * result + (this.weight == null ? 0 : this.weight.hashCode());
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      WeightedObject<?> other = (WeightedObject<?>)obj;
      if (this.thing == null) {
         if (other.thing != null) {
            return false;
         }
      } else if (!this.thing.equals(other.thing)) {
         return false;
      }

      if (this.weight == null) {
         if (other.weight != null) {
            return false;
         }
      } else if (!this.weight.equals(other.weight)) {
         return false;
      }

      return true;
   }
}
