package net.fabricmc.loader.util;

@Deprecated
public class UrlConversionException extends Exception {
   public UrlConversionException() {
   }

   public UrlConversionException(String s) {
      super(s);
   }

   public UrlConversionException(Throwable t) {
      super(t);
   }

   public UrlConversionException(String s, Throwable t) {
      super(s, t);
   }
}
