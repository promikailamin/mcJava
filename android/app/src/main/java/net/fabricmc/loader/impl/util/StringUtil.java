package net.fabricmc.loader.impl.util;

public final class StringUtil {
   public static String capitalize(String s) {
      if (s.isEmpty()) {
         return s;
      }

      int pos = 0;

      while (pos < s.length() && !Character.isLetterOrDigit(s.codePointAt(pos))) {
         pos++;
      }

      if (pos == s.length()) {
         return s;
      }

      int cp = s.codePointAt(pos);
      int cpUpper = Character.toUpperCase(cp);
      if (cpUpper == cp) {
         return s;
      }

      StringBuilder ret = new StringBuilder(s.length());
      ret.append(s, 0, pos);
      ret.appendCodePoint(cpUpper);
      ret.append(s, pos + Character.charCount(cp), s.length());
      return ret.toString();
   }

   public static String[] splitNamespaced(String s, String defaultNamespace) {
      int i = s.indexOf(58);
      return i >= 0 ? new String[]{s.substring(0, i), s.substring(i + 1)} : new String[]{defaultNamespace, s};
   }

   public static String wrapLines(String str, int limit) {
      if (str.length() < limit) {
         return str;
      }

      StringBuilder sb = new StringBuilder(str.length() + 20);
      int lastSpace = -1;
      int len = 0;
      int i = 0;

      for (int max = str.length(); i <= max; i++) {
         char c = i < max ? str.charAt(i) : ' ';
         if (c != '\r') {
            if (c == '\n') {
               lastSpace = sb.length();
               sb.append(c);
               len = 0;
            } else if (Character.isWhitespace(c)) {
               if (len > limit && lastSpace >= 0) {
                  sb.setCharAt(lastSpace, '\n');
                  len = sb.length() - lastSpace - 1;
               }

               if (i == max) {
                  break;
               }

               if (len >= limit) {
                  lastSpace = -1;
                  sb.append('\n');
                  len = 0;
               } else {
                  lastSpace = sb.length();
                  sb.append(c);
                  len++;
               }
            } else if (c != '"' && c != '\'') {
               sb.append(c);
               len++;
            } else {
               int next = str.indexOf(c, i + 1) + 1;
               if (next <= 0) {
                  next = str.length();
               }

               sb.append(str, i, next);
               len += next - i;
               i = next - 1;
            }
         }
      }

      return sb.toString();
   }
}
