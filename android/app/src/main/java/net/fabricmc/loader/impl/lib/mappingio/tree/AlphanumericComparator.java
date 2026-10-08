package net.fabricmc.loader.impl.lib.mappingio.tree;

import java.nio.Buffer;
import java.nio.CharBuffer;
import java.util.Comparator;

class AlphanumericComparator implements Comparator<CharSequence> {
   public int compare(CharSequence s1, CharSequence s2) {
      CharBuffer b1 = CharBuffer.wrap(s1);
      CharBuffer b2 = CharBuffer.wrap(s2);

      while (b1.hasRemaining() && b2.hasRemaining()) {
         this.moveWindow(b1);
         this.moveWindow(b2);
         int result = this.compare(b1, b2);
         if (result != 0) {
            return result;
         }

         this.prepareForNextIteration(b1);
         this.prepareForNextIteration(b2);
      }

      return s1.length() - s2.length();
   }

   private void moveWindow(CharBuffer buffer) {
      int start = buffer.position();
      int end = buffer.position();
      boolean isNumerical = this.isDigit(buffer.get(start));

      while (end < buffer.limit() && isNumerical == this.isDigit(buffer.get(end))) {
         end++;
         if (isNumerical && start + 1 < buffer.limit() && this.isZero(buffer.get(start)) && this.isDigit(buffer.get(end))) {
            start++;
         }
      }

      ((Buffer)buffer).position(start).limit(end);
   }

   private int compare(CharBuffer b1, CharBuffer b2) {
      return this.isNumerical(b1) && this.isNumerical(b2) ? this.compareNumerically(b1, b2) : this.compareAsStrings(b1, b2);
   }

   private boolean isNumerical(CharBuffer buffer) {
      return this.isDigit(buffer.charAt(0));
   }

   private boolean isDigit(char c) {
      return Character.isDigit(c);
   }

   private int compareNumerically(CharBuffer b1, CharBuffer b2) {
      int diff = b1.length() - b2.length();
      if (diff != 0) {
         return diff;
      }

      for (int i = 0; i < b1.remaining() && i < b2.remaining(); i++) {
         int result = Character.compare(b1.charAt(i), b2.charAt(i));
         if (result != 0) {
            return result;
         }
      }

      return 0;
   }

   private void prepareForNextIteration(CharBuffer buffer) {
      ((Buffer)buffer).position(buffer.limit()).limit(buffer.capacity());
   }

   private int compareAsStrings(CharBuffer b1, CharBuffer b2) {
      return b1.toString().compareTo(b2.toString());
   }

   private boolean isZero(char c) {
      return c == '0';
   }
}
