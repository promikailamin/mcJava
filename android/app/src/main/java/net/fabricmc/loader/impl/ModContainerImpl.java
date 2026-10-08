package net.fabricmc.loader.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.loader.impl.discovery.ModCandidateImpl;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.ModOriginImpl;
import net.fabricmc.loader.impl.util.FileSystemUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public class ModContainerImpl extends net.fabricmc.loader.ModContainer {
   private final LoaderModMetadata info;
   private final ModOrigin origin;
   private final List<Path> codeSourcePaths;
   private final String parentModId;
   private final Collection<String> childModIds;
   private volatile List<Path> roots;
   private static boolean warnedMultiPath = false;
   private boolean warnedClose = false;

   public ModContainerImpl(ModCandidateImpl candidate) {
      this.info = candidate.getMetadata();
      this.codeSourcePaths = candidate.getPaths();
      this.parentModId = candidate.getParentMods().isEmpty() ? null : candidate.getParentMods().iterator().next().getId();
      this.childModIds = candidate.getNestedMods().isEmpty() ? Collections.emptyList() : new ArrayList<>(candidate.getNestedMods().size());

      for (ModCandidateImpl c : candidate.getNestedMods()) {
         if (c.getParentMods().size() <= 1 || c.getParentMods().iterator().next() == candidate) {
            this.childModIds.add(c.getId());
         }
      }

      List<Path> paths = candidate.getOriginPaths();
      this.origin = paths != null ? new ModOriginImpl(paths) : new ModOriginImpl(this.parentModId, candidate.getLocalPath());
   }

   public LoaderModMetadata getMetadata() {
      return this.info;
   }

   @Override
   public ModOrigin getOrigin() {
      return this.origin;
   }

   @Override
   public List<Path> getCodeSourcePaths() {
      return this.codeSourcePaths;
   }

   @Override
   public Path getRootPath() {
      List<Path> paths = this.getRootPaths();
      if (paths.size() != 1 && !warnedMultiPath) {
         if (!FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment()) {
            warnedMultiPath = true;
         }

         Log.warn(LogCategory.GENERAL, "getRootPath access for %s with multiple paths, returning only one which may incur unexpected behavior!", this);
      }

      return paths.get(0);
   }

   @Override
   public List<Path> getRootPaths() {
      List<Path> ret = this.roots;
      if (ret == null || !this.checkFsOpen(ret)) {
         this.roots = ret = this.obtainRootPaths();
      }

      return ret;
   }

   private boolean checkFsOpen(List<Path> paths) {
      for (Path path : paths) {
         if (!path.getFileSystem().isOpen()) {
            if (!this.warnedClose) {
               if (!FabricLoaderImpl.INSTANCE.isDevelopmentEnvironment()) {
                  this.warnedClose = true;
               }

               Log.warn(LogCategory.GENERAL, "FileSystem for %s has been closed unexpectedly, existing root path references may break!", this);
            }

            return false;
         }
      }

      return true;
   }

   private List<Path> obtainRootPaths() {
      boolean allDirs = true;

      for (Path path : this.codeSourcePaths) {
         if (!Files.isDirectory(path)) {
            allDirs = false;
            break;
         }
      }

      if (allDirs) {
         return this.codeSourcePaths;
      }

      try {
         if (this.codeSourcePaths.size() == 1) {
            return Collections.singletonList(obtainRootPath(this.codeSourcePaths.get(0)));
         }

         List<Path> ret = new ArrayList<>(this.codeSourcePaths.size());

         for (Path path : this.codeSourcePaths) {
            ret.add(obtainRootPath(path));
         }

         return Collections.unmodifiableList(ret);
      } catch (IOException e) {
         throw new RuntimeException("Failed to obtain root directory for mod '" + this.info.getId() + "'!", e);
      }
   }

   private static Path obtainRootPath(Path path) throws IOException {
      if (Files.isDirectory(path)) {
         return path;
      } else {
         FileSystemUtil.FileSystemDelegate delegate = FileSystemUtil.getJarFileSystem(path, false);
         FileSystem fs = delegate.get();
         if (fs == null) {
            throw new RuntimeException("Could not open JAR file " + path + " for NIO reading!");
         } else {
            return fs.getRootDirectories().iterator().next();
         }
      }
   }

   @Override
   public Path getPath(String file) {
      Optional<Path> res = this.findPath(file);
      if (res.isPresent()) {
         return res.get();
      } else {
         List<Path> roots = this.roots;
         if (!roots.isEmpty()) {
            Path root = roots.get(0);
            return root.resolve(file.replace("/", root.getFileSystem().getSeparator()));
         } else {
            return Paths.get(".").resolve("missing_ae236f4970ce").resolve(file.replace('/', File.separatorChar));
         }
      }
   }

   @Override
   public Optional<ModContainer> getContainingMod() {
      return this.parentModId != null ? FabricLoaderImpl.INSTANCE.getModContainer(this.parentModId) : Optional.empty();
   }

   @Override
   public Collection<ModContainer> getContainedMods() {
      if (this.childModIds.isEmpty()) {
         return Collections.emptyList();
      }

      List<ModContainer> ret = new ArrayList<>(this.childModIds.size());

      for (String id : this.childModIds) {
         ModContainer mod = FabricLoaderImpl.INSTANCE.getModContainer(id).orElse(null);
         if (mod != null) {
            ret.add(mod);
         }
      }

      return ret;
   }

   @Deprecated
   public LoaderModMetadata getInfo() {
      return this.info;
   }

   @Override
   public String toString() {
      return String.format("%s %s", this.info.getId(), this.info.getVersion());
   }
}
