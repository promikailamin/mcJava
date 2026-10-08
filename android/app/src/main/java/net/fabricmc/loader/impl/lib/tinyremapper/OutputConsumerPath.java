package net.fabricmc.loader.impl.lib.tinyremapper;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class OutputConsumerPath implements Closeable, BiConsumer<String, byte[]> {
   private final Path dstDir;
   private final FileSystemReference fsToClose;
   private final boolean isJarFs;
   private final Lock lock;
   private final Predicate<String> classNameFilter;
   private boolean closed;

   private OutputConsumerPath(Path destination, boolean isJar, boolean threadSyncWrites, Predicate<String> classNameFilter) throws IOException {
      if (!isJar) {
         Files.createDirectories(destination);
         this.fsToClose = null;
      } else {
         createParentDirs(destination);
         this.fsToClose = FileSystemReference.openJar(destination, true);
         if (this.fsToClose.isReadOnly()) {
            throw new IOException("the jar file " + destination + " can't be written");
         }

         destination = this.fsToClose.getPath("/");
      }

      this.dstDir = destination;
      this.isJarFs = isJar;
      this.lock = threadSyncWrites ? new ReentrantLock() : null;
      this.classNameFilter = classNameFilter;
   }

   public void addNonClassFiles(Path srcFile, NonClassCopyMode copyMode, TinyRemapper remapper) throws IOException {
      this.addNonClassFiles(srcFile, remapper, copyMode.remappers);
   }

   public void addNonClassFiles(Path srcFile, TinyRemapper remapper, List<OutputConsumerPath.ResourceRemapper> remappers) throws IOException {
      if (Files.isDirectory(srcFile)) {
         this.addNonClassFiles(srcFile, remapper, false, remappers);
      } else {
         if (!Files.exists(srcFile)) {
            throw new FileNotFoundException("file " + srcFile + " doesn't exist");
         }

         if (!srcFile.getFileName().toString().endsWith(".class")) {
            this.addNonClassFiles(FileSystems.newFileSystem(srcFile, (ClassLoader)null).getPath("/"), remapper, true, remappers);
         }
      }
   }

   public void addNonClassFiles(
      final Path srcDir, final TinyRemapper remapper, boolean closeFs, final List<OutputConsumerPath.ResourceRemapper> resourceRemappers
   ) throws IOException {
      try {
         if (this.lock != null) {
            this.lock.lock();
         }

         if (this.closed) {
            throw new IllegalStateException("consumer already closed");
         }

         Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
               String fileName = file.getFileName().toString();
               if (!fileName.endsWith(".class")) {
                  Path relativePath = srcDir.relativize(file);
                  Path dstFile = OutputConsumerPath.this.dstDir.resolve(relativePath.toString());

                  for (OutputConsumerPath.ResourceRemapper resourceRemapper : resourceRemappers) {
                     if (resourceRemapper.canTransform(remapper, relativePath)) {
                        InputStream input = new BufferedInputStream(Files.newInputStream(file));

                        FileVisitResult var9;
                        try {
                           resourceRemapper.transform(OutputConsumerPath.this.dstDir, relativePath, input, remapper);
                           var9 = FileVisitResult.CONTINUE;
                        } catch (Throwable var12) {
                           try {
                              input.close();
                           } catch (Throwable var11) {
                              var12.addSuppressed(var11);
                           }

                           throw var12;
                        }

                        input.close();
                        return var9;
                     }
                  }

                  OutputConsumerPath.createParentDirs(dstFile);
                  Files.copy(file, dstFile, StandardCopyOption.REPLACE_EXISTING);
               }

               return FileVisitResult.CONTINUE;
            }
         });
      } finally {
         if (this.lock != null) {
            this.lock.unlock();
         }

         if (closeFs) {
            srcDir.getFileSystem().close();
         }
      }
   }

   public void accept(String clsName, byte[] data) {
      if (this.classNameFilter == null || this.classNameFilter.test(clsName)) {
         Path dstFile = null;

         try {
            if (this.lock != null) {
               this.lock.lock();
            }

            if (this.closed) {
               throw new IllegalStateException("consumer already closed");
            }

            dstFile = this.dstDir.resolve(clsName + ".class");
            if (this.isJarFs && Files.exists(dstFile)) {
               if (Files.isDirectory(dstFile)) {
                  throw new FileAlreadyExistsException("dst file " + dstFile + " is a directory");
               }

               Files.delete(dstFile);
            }

            createParentDirs(dstFile);
            Files.write(dstFile, data);
         } catch (IOException e) {
            throw new UncheckedIOException("error writing to " + dstFile, e);
         } finally {
            if (this.lock != null) {
               this.lock.unlock();
            }
         }
      }
   }

   @Override
   public void close() throws IOException {
      if (!this.closed) {
         try {
            if (this.lock != null) {
               this.lock.lock();
            }

            if (this.fsToClose != null) {
               this.fsToClose.close();
            }

            this.closed = true;
         } finally {
            if (this.lock != null) {
               this.lock.unlock();
            }
         }
      }
   }

   private static boolean isJar(Path path) {
      if (Files.exists(path)) {
         return !Files.isDirectory(path);
      }

      String name = path.getFileName().toString().toLowerCase(Locale.ENGLISH);
      return name.endsWith(".jar") || name.endsWith(".zip");
   }

   private static void createParentDirs(Path path) throws IOException {
      Path parent = path.getParent();
      if (parent != null) {
         Files.createDirectories(parent);
      }
   }

   public static class Builder {
      private final Path destination;
      private Boolean assumeArchive;
      private boolean threadSyncWrites = false;
      private Predicate<String> classNameFilter;

      public Builder(Path destination) {
         this.destination = destination;
      }

      public OutputConsumerPath.Builder assumeArchive(boolean value) {
         this.assumeArchive = value;
         return this;
      }

      public OutputConsumerPath build() throws IOException {
         boolean isJar = this.assumeArchive != null && !Files.exists(this.destination) ? this.assumeArchive : OutputConsumerPath.isJar(this.destination);
         return new OutputConsumerPath(this.destination, isJar, this.threadSyncWrites, this.classNameFilter);
      }
   }

   public interface ResourceRemapper {
      boolean canTransform(TinyRemapper var1, Path var2);

      void transform(Path var1, Path var2, InputStream var3, TinyRemapper var4) throws IOException;
   }
}
