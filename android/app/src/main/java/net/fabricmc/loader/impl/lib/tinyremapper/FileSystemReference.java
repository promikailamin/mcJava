package net.fabricmc.loader.impl.lib.tinyremapper;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class FileSystemReference implements Closeable {
   private static final Map<FileSystem, Set<FileSystemReference>> openFsMap = new IdentityHashMap<>();
   private final FileSystem fileSystem;
   private volatile boolean closed;

   public static FileSystemReference openJar(Path path) throws IOException {
      return openJar(path, false);
   }

   public static FileSystemReference openJar(Path path, boolean create) throws IOException {
      return open(toJarUri(path), create);
   }

   private static URI toJarUri(Path path) {
      URI uri = path.toUri();

      try {
         return new URI("jar:" + uri.getScheme(), uri.getHost(), uri.getPath(), uri.getFragment());
      } catch (URISyntaxException e) {
         throw new RuntimeException("can't convert path " + path + " to uri", e);
      }
   }

   public static FileSystemReference open(URI uri, boolean create) throws IOException {
      synchronized (openFsMap) {
         boolean opened = false;
         FileSystem fs = null;

         try {
            fs = FileSystems.getFileSystem(uri);
         } catch (FileSystemNotFoundException e) {
            try {
               fs = FileSystems.newFileSystem(uri, create ? Collections.singletonMap("create", "true") : Collections.emptyMap());
               opened = true;
            } catch (FileSystemAlreadyExistsException f) {
               fs = FileSystems.getFileSystem(uri);
            }
         }

         FileSystemReference ret = new FileSystemReference(fs);
         Set<FileSystemReference> refs = openFsMap.get(fs);
         if (refs == null) {
            refs = Collections.newSetFromMap(new IdentityHashMap<>());
            openFsMap.put(fs, refs);
            if (!opened) {
               refs.add(null);
            }
         } else if (opened) {
            throw new IllegalStateException("opened but already in refs?");
         }

         refs.add(ret);
         return ret;
      }
   }

   private FileSystemReference(FileSystem fs) {
      this.fileSystem = fs;
   }

   public boolean isReadOnly() {
      if (this.closed) {
         throw new IllegalStateException("fs closed");
      } else {
         return this.fileSystem.isReadOnly();
      }
   }

   public Path getPath(String first, String... more) {
      if (this.closed) {
         throw new IllegalStateException("fs closed");
      } else {
         return this.fileSystem.getPath(first, more);
      }
   }

   @Override
   public void close() throws IOException {
      synchronized (openFsMap) {
         if (!this.closed) {
            this.closed = true;
            Set<FileSystemReference> refs = openFsMap.get(this.fileSystem);
            if (refs != null && refs.remove(this)) {
               if (refs.isEmpty()) {
                  openFsMap.remove(this.fileSystem);
                  this.fileSystem.close();
               } else if (refs.size() == 1 && refs.contains(null)) {
                  openFsMap.remove(this.fileSystem);
               }
            } else {
               throw new IllegalStateException("fs " + this.fileSystem + " was already closed");
            }
         }
      }
   }

   @Override
   public String toString() {
      synchronized (openFsMap) {
         Set<FileSystemReference> refs = openFsMap.getOrDefault(this.fileSystem, Collections.emptySet());
         return String.format("%s=%dx,%s", this.fileSystem, refs.size(), refs.contains(null) ? "existing" : "new");
      }
   }
}
