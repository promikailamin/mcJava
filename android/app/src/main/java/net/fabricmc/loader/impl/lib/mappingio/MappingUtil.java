package net.fabricmc.loader.impl.lib.mappingio;

import java.util.Map;

public final class MappingUtil {
   public static String mapDesc(String desc, Map<String, String> clsMap) {
      return mapDesc(desc, 0, desc.length(), clsMap);
   }

   public static String mapDesc(String desc, int start, int end, Map<String, String> clsMap) {
      StringBuilder ret = null;
      int searchStart = start;

      int clsStart;
      while ((clsStart = desc.indexOf(76, searchStart)) >= 0) {
         int clsEnd = desc.indexOf(59, clsStart + 1);
         if (clsEnd < 0) {
            throw new IllegalArgumentException();
         }

         String cls = desc.substring(clsStart + 1, clsEnd);
         String mappedCls = clsMap.get(cls);
         if (mappedCls != null) {
            if (ret == null) {
               ret = new StringBuilder(end - start);
            }

            ret.append(desc, start, clsStart + 1);
            ret.append(mappedCls);
            start = clsEnd;
         }

         searchStart = clsEnd + 1;
      }

      if (ret == null) {
         return desc.substring(start, end);
      }

      ret.append(desc, start, end);
      return ret.toString();
   }
}
