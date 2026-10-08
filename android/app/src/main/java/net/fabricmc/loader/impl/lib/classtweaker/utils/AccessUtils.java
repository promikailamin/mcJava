package net.fabricmc.loader.impl.lib.classtweaker.utils;

public class AccessUtils {
   public static int makePublic(int i) {
      return i & -7 | 1;
   }

   public static int makeProtected(int i) {
      return (i & 1) != 0 ? i : i & -3 | 4;
   }

   public static int makeFinalIfPrivate(int access, String name, int ownerAccess) {
      if (name.equals("<init>")) {
         return access;
      } else if ((ownerAccess & 512) != 0 || (access & 8) != 0) {
         return access;
      } else {
         return (access & 2) != 0 ? access | 16 : access;
      }
   }

   public static int removeFinal(int i) {
      return i & -17;
   }
}
