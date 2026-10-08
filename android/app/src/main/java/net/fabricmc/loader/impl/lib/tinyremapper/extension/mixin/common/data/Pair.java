package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data;

import java.util.Objects;

public final class Pair<L, R> {
   private final L first;
   private final R second;

   private Pair(L first, R second) {
      this.first = first;
      this.second = second;
   }

   public static <L, R> Pair<L, R> of(L first, R second) {
      return new Pair<>(first, second);
   }

   public L first() {
      return this.first;
   }

   public R second() {
      return this.second;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Pair<?, ?> pair = (Pair<?, ?>)o;
         return !Objects.equals(this.first, pair.first) ? false : Objects.equals(this.second, pair.second);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.first != null ? this.first.hashCode() : 0;
      return 31 * result + (this.second != null ? this.second.hashCode() : 0);
   }
}
