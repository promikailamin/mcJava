package net.fabricmc.loader.impl.lib.mappingio.format.tiny;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class Tiny2Util {
   public static String unescape(String str) {
      int pos = str.indexOf(92);
      if (pos < 0) {
         return str;
      }

      StringBuilder ret = new StringBuilder(str.length() - 1);
      int start = 0;

      do {
         ret.append(str, start, pos);
         if (++pos >= str.length()) {
            throw new RuntimeException("incomplete escape sequence at the end");
         }

         int type;
         if ((type = "\\nr0t".indexOf(str.charAt(pos))) < 0) {
            throw new RuntimeException("invalid escape character: \\" + str.charAt(pos));
         }

         ret.append("\\\n\r\u0000\t".charAt(type));
         start = pos + 1;
      } while ((pos = str.indexOf(92, start)) >= 0);

      ret.append(str, start, str.length());
      return ret.toString();
   }
}
