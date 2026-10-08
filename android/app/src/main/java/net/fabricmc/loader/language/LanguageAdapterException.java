package net.fabricmc.loader.language;

@Deprecated
public class LanguageAdapterException extends Exception {
   public LanguageAdapterException(String s) {
      super(s);
   }

   public LanguageAdapterException(String s, Throwable t) {
      super(s, t);
   }
}
