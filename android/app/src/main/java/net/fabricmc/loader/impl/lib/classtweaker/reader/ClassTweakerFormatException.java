package net.fabricmc.loader.impl.lib.classtweaker.reader;

public class ClassTweakerFormatException extends RuntimeException {
   private final int lineNumber;

   public ClassTweakerFormatException(int lineNumber, String message) {
      super(message);
      this.lineNumber = lineNumber;
   }

   @Override
   public String toString() {
      return this.getClass().getName() + ": line " + this.lineNumber + ": " + this.getMessage();
   }
}
