package net.fabricmc.loader.impl.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

public final class Localization {
   public static final ResourceBundle BUNDLE = createBundle("net.fabricmc.loader.Messages", Locale.getDefault());
   public static final ResourceBundle ROOT_LOCALE_BUNDLE = createBundle("net.fabricmc.loader.Messages", Locale.ROOT);

   public static String format(String key, Object... args) {
      String pattern = BUNDLE.getString(key);
      return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
   }

   public static String formatRoot(String key, Object... args) {
      String pattern = ROOT_LOCALE_BUNDLE.getString(key);
      return args.length == 0 ? pattern : MessageFormat.format(pattern, args);
   }

   private static ResourceBundle createBundle(String name, Locale locale) {
      return System.getProperty("java.version", "").startsWith("1.")
         ? ResourceBundle.getBundle(
            name,
            locale,
            new Control() {
               @Override
               public ResourceBundle newBundle(String baseName, Locale localex, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
                  if (format.equals("java.properties")) {
                     InputStream is = loader.getResourceAsStream(this.toResourceName(this.toBundleName(baseName, localex), "properties"));
                     if (is != null) {
                        InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

                        PropertyResourceBundle var8;
                        try {
                           var8 = new PropertyResourceBundle(reader);
                        } catch (Throwable var11) {
                           try {
                              reader.close();
                           } catch (Throwable var10) {
                              var11.addSuppressed(var10);
                           }

                           throw var11;
                        }

                        reader.close();
                        return var8;
                     }
                  }

                  return super.newBundle(baseName, localex, format, loader, reload);
               }
            }
         )
         : ResourceBundle.getBundle(name, locale);
   }
}
