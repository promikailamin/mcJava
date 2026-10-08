package net.fabricmc.loader.impl.metadata;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.metadata.ModOrigin;

public final class ModOriginImpl implements ModOrigin {
   private final ModOrigin.Kind kind;
   private List<Path> paths;
   private String parentModId;
   private String parentSubLocation;

   public ModOriginImpl() {
      this.kind = ModOrigin.Kind.UNKNOWN;
   }

   public ModOriginImpl(List<Path> paths) {
      this.kind = ModOrigin.Kind.PATH;
      this.paths = paths;
   }

   public ModOriginImpl(String parentModId, String parentSubLocation) {
      this.kind = ModOrigin.Kind.NESTED;
      this.parentModId = parentModId;
      this.parentSubLocation = parentSubLocation;
   }

   @Override
   public ModOrigin.Kind getKind() {
      return this.kind;
   }

   @Override
   public List<Path> getPaths() {
      if (this.kind != ModOrigin.Kind.PATH) {
         throw new UnsupportedOperationException("kind " + this.kind.name() + " doesn't have paths");
      } else {
         return this.paths;
      }
   }

   @Override
   public String getParentModId() {
      if (this.kind != ModOrigin.Kind.NESTED) {
         throw new UnsupportedOperationException("kind " + this.kind.name() + " doesn't have a parent mod");
      } else {
         return this.parentModId;
      }
   }

   @Override
   public String getParentSubLocation() {
      if (this.kind != ModOrigin.Kind.NESTED) {
         throw new UnsupportedOperationException("kind " + this.kind.name() + " doesn't have a parent sub-location");
      } else {
         return this.parentSubLocation;
      }
   }

   @Override
   public String toString() {
      switch (this.getKind()) {
         case PATH:
            return this.paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
         case NESTED:
            return String.format("%s:%s", this.parentModId, this.parentSubLocation);
         default:
            return "unknown";
      }
   }
}
