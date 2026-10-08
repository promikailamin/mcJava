package net.fabricmc.loader.impl.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

public final class ManifestUtil {
   public static Manifest readManifest(Class<?> cls) throws IOException, URISyntaxException {
      CodeSource cs = cls.getProtectionDomain().getCodeSource();
      if (cs == null) {
         return null;
      }

      URL url = cs.getLocation();
      return url == null ? null : readManifest(url);
   }

   private static Manifest readManifest(URL codeSourceUrl) throws IOException, URISyntaxException {
      Path path = UrlUtil.asPath(codeSourceUrl);
      if (Files.isDirectory(path)) {
         return readManifestFromBasePath(path);
      }

      URLConnection connection = new URL("jar:" + codeSourceUrl.toString() + "!/").openConnection();
      if (connection instanceof JarURLConnection) {
         return ((JarURLConnection)connection).getManifest();
      }

      FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(path, false);

      Manifest var4;
      try {
         var4 = readManifestFromBasePath(jarFs.get().getRootDirectories().iterator().next());
      } catch (Throwable var7) {
         if (jarFs != null) {
            try {
               jarFs.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (jarFs != null) {
         jarFs.close();
      }

      return var4;
   }

   public static Manifest readManifest(Path codeSource) throws IOException {
      if (Files.isDirectory(codeSource)) {
         return readManifestFromBasePath(codeSource);
      }

      FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(codeSource, false);

      Manifest var2;
      try {
         var2 = readManifestFromBasePath(jarFs.get().getRootDirectories().iterator().next());
      } catch (Throwable var5) {
         if (jarFs != null) {
            try {
               jarFs.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (jarFs != null) {
         jarFs.close();
      }

      return var2;
   }

   public static Manifest readManifestFromBasePath(Path basePath) throws IOException {
      Path path = basePath.resolve("META-INF").resolve("MANIFEST.MF");
      if (!Files.exists(path)) {
         return null;
      }

      InputStream stream = Files.newInputStream(path);

      Manifest var3;
      try {
         var3 = new Manifest(stream);
      } catch (Throwable var6) {
         if (stream != null) {
            try {
               stream.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stream != null) {
         stream.close();
      }

      return var3;
   }

   public static String getManifestValue(Manifest manifest, Name name) {
      return manifest.getMainAttributes().getValue(name);
   }

   public static List<URL> getClassPath(Manifest manifest, Path baseDir) throws MalformedURLException {
      String cp = getManifestValue(manifest, Name.CLASS_PATH);
      if (cp == null) {
         return null;
      }

      StringTokenizer tokenizer = new StringTokenizer(cp);
      List<URL> ret = new ArrayList<>();
      URL context = UrlUtil.asUrl(baseDir);

      while (tokenizer.hasMoreElements()) {
         ret.add(new URL(context, tokenizer.nextToken()));
      }

      return ret;
   }
}
