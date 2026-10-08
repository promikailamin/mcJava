package net.fabricmc.loader.impl.game.minecraft;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

public final class BundlerClassPathCapture {
   static final CompletableFuture<URL[]> FUTURE = new CompletableFuture<>();

   public static void main(String[] args) {
      try {
         URLClassLoader cl = (URLClassLoader)Thread.currentThread().getContextClassLoader();
         URL[] urls = cl.getURLs();
         URL asmUrl = cl.findResource("org/objectweb/asm/ClassReader.class");
         if (asmUrl != null && (asmUrl = getJarUrl(asmUrl)) != null) {
            for (int i = 0; i < urls.length; i++) {
               if (urls[i].equals(asmUrl)) {
                  URL[] newUrls = new URL[urls.length - 1];
                  System.arraycopy(urls, 0, newUrls, 0, i);
                  System.arraycopy(urls, i + 1, newUrls, i, urls.length - i - 1);
                  urls = newUrls;
                  break;
               }
            }
         }

         FUTURE.complete(urls);
      } catch (Throwable t) {
         FUTURE.completeExceptionally(t);
      }
   }

   private static URL getJarUrl(URL url) throws IOException {
      URLConnection connection = url.openConnection();
      return connection instanceof JarURLConnection ? ((JarURLConnection)connection).getJarFileURL() : null;
   }
}
