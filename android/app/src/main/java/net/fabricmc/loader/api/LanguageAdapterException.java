package net.fabricmc.loader.api;

public class LanguageAdapterException extends Exception {
   public LanguageAdapterException(String s) {
      super(s);
   }

   public LanguageAdapterException(Throwable t) {
      super(t);
   }

   public LanguageAdapterException(String s, Throwable t) {
      super(s, t);
   }
}
