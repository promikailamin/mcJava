package net.fabricmc.loader.impl.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;

public final class SimpleClassPath implements Closeable {
   private final List<Path> paths;
   private final boolean[] jarMarkers;
   private final ZipFile[] openJars;

   public SimpleClassPath(List<Path> paths) {
      this.paths = paths;
      this.jarMarkers = new boolean[paths.size()];
      this.openJars = new ZipFile[paths.size()];

      for (int i = 0; i < this.jarMarkers.length; i++) {
         if (!Files.isDirectory(paths.get(i))) {
            this.jarMarkers[i] = true;
         }
      }
   }

   @Override
   public void close() throws IOException {
      IOException exc = null;

      for (int i = 0; i < this.openJars.length; i++) {
         Closeable file = this.openJars[i];

         try {
            if (file != null) {
               file.close();
            }
         } catch (IOException e) {
            if (exc == null) {
               exc = e;
            } else {
               exc.addSuppressed(e);
            }
         }

         this.openJars[i] = null;
      }

      if (exc != null) {
         throw exc;
      }
   }

   public List<Path> getPaths() {
      return this.paths;
   }

   public SimpleClassPath.CpEntry getEntry(String subPath) throws IOException {
      for (int i = 0; i < this.jarMarkers.length; i++) {
         if (this.jarMarkers[i]) {
            ZipFile zf = this.openJars[i];
            if (zf == null) {
               Path path = this.paths.get(i);

               try {
                  this.openJars[i] = zf = new ZipFile(path.toFile());
               } catch (IOException | ZipError e) {
                  throw new IOException(String.format("error opening %s: %s", LoaderUtil.normalizePath(path), e), e);
               }
            }

            ZipEntry entry = zf.getEntry(subPath);
            if (entry != null) {
               return new SimpleClassPath.CpEntry(i, subPath, entry);
            }
         } else {
            Path file = this.paths.get(i).resolve(subPath);
            if (Files.isRegularFile(file)) {
               return new SimpleClassPath.CpEntry(i, subPath, file);
            }
         }
      }

      return null;
   }

   public InputStream getInputStream(String subPath) throws IOException {
      SimpleClassPath.CpEntry entry = this.getEntry(subPath);
      return entry != null ? entry.getInputStream() : null;
   }

   public final class CpEntry {
      private final int idx;
      private final String subPath;
      private final Object instance;

      private CpEntry(int idx, String subPath, Object instance) {
         this.idx = idx;
         this.subPath = subPath;
         this.instance = instance;
      }

      public Path getOrigin() {
         return SimpleClassPath.this.paths.get(this.idx);
      }

      public String getSubPath() {
         return this.subPath;
      }

      public InputStream getInputStream() throws IOException {
         return this.instance instanceof ZipEntry
            ? SimpleClassPath.this.openJars[this.idx].getInputStream((ZipEntry)this.instance)
            : Files.newInputStream((Path)this.instance);
      }

      @Override
      public String toString() {
         return String.format("%s:%s", this.getOrigin(), this.subPath);
      }
   }
}
