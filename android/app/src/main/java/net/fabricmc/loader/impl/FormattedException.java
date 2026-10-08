package net.fabricmc.loader.impl;

import net.fabricmc.loader.impl.util.Localization;

public final class FormattedException extends RuntimeException {
   private final String mainText;
   private String translatedText;

   public FormattedException(String mainText, String message) {
      super(message);
      this.mainText = mainText;
   }

   public FormattedException(String mainText, String format, Object... args) {
      super(String.format(format, args));
      this.mainText = mainText;
   }

   public FormattedException(String mainText, String message, Throwable cause) {
      super(message, cause);
      this.mainText = mainText;
   }

   public FormattedException(String mainText, Throwable cause) {
      super(cause);
      this.mainText = mainText;
   }

   public static FormattedException ofLocalized(String key, String message) {
      return new FormattedException(Localization.formatRoot(key), message).addTranslation(key);
   }

   public static FormattedException ofLocalized(String key, String format, Object... args) {
      return new FormattedException(Localization.formatRoot(key), format, args).addTranslation(key);
   }

   public static FormattedException ofLocalized(String key, String message, Throwable cause) {
      return new FormattedException(Localization.formatRoot(key), message, cause).addTranslation(key);
   }

   public static FormattedException ofLocalized(String key, Throwable cause) {
      return new FormattedException(Localization.formatRoot(key), cause).addTranslation(key);
   }

   public String getMainText() {
      return this.mainText;
   }

   public String getDisplayedText() {
      return this.translatedText != null && !this.translatedText.equals(this.mainText) ? this.translatedText + " (" + this.mainText + ")" : this.mainText;
   }

   private FormattedException addTranslation(String key) {
      this.translatedText = Localization.format(key);
      return this;
   }
}
