package net.fabricmc.loader.util;

import java.net.URL;
import java.nio.file.Path;
import net.fabricmc.loader.impl.util.ExceptionUtil;

@Deprecated
public final class UrlUtil {
   private UrlUtil() {
   }

   public static Path asPath(URL url) throws UrlConversionException {
      try {
         return net.fabricmc.loader.impl.util.UrlUtil.asPath(url);
      } catch (ExceptionUtil.WrappedException e) {
         throw new UrlConversionException(e.getCause());
      }
   }
}
