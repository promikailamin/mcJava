package net.fabricmc.loader.impl.lib.sat4j.core;

public final class LiteralsUtils {
   private LiteralsUtils() {
   }

   public static int var(int p) {
      assert p > 1;
      return p >> 1;
   }

   public static int neg(int p) {
      return p ^ 1;
   }

   public static int negLit(int var) {
      return var << 1 ^ 1;
   }

   public static int toDimacs(int p) {
      return ((p & 1) == 0 ? 1 : -1) * (p >> 1);
   }

   public static int toInternal(int x) {
      return x < 0 ? -x << 1 ^ 1 : x << 1;
   }
}
