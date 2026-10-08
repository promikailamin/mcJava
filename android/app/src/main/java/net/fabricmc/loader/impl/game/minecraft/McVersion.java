package net.fabricmc.loader.impl.game.minecraft;

import java.util.OptionalInt;

public final class McVersion {
   private final String id;
   private final String name;
   private final String raw;
   private final String normalized;
   private final OptionalInt classVersion;

   private McVersion(String id, String name, String raw, String release, OptionalInt classVersion) {
      this.id = id;
      this.name = name;
      this.raw = raw;
      this.normalized = McVersionLookup.normalizeVersion(raw, release);
      this.classVersion = classVersion;
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public String getRaw() {
      return this.raw;
   }

   public String getNormalized() {
      return this.normalized;
   }

   public OptionalInt getClassVersion() {
      return this.classVersion;
   }

   @Override
   public String toString() {
      return String.format(
         "McVersion{id=%s, name=%s, raw=%s, normalized=%s, classVersion=%s}", this.id, this.name, this.raw, this.normalized, this.classVersion
      );
   }

   public static final class Builder {
      private String id;
      private String name;
      private String version;
      private String release;
      private OptionalInt classVersion = OptionalInt.empty();

      public McVersion.Builder setId(String id) {
         this.id = id;
         return this;
      }

      public McVersion.Builder setName(String name) {
         this.name = name;
         return this;
      }

      public McVersion.Builder setVersion(String name) {
         this.version = name;
         return this;
      }

      public McVersion.Builder setRelease(String release) {
         this.release = release;
         return this;
      }

      public McVersion.Builder setClassVersion(int classVersion) {
         this.classVersion = OptionalInt.of(classVersion);
         return this;
      }

      public McVersion.Builder setNameAndRelease(String name) {
         return this.setVersion(name).setRelease(McVersionLookup.getRelease(name));
      }

      public McVersion.Builder setFromFileName(String name) {
         int pos = name.lastIndexOf(46);
         if (pos > 0) {
            name = name.substring(0, pos);
         }

         return this.setNameAndRelease(name);
      }

      public McVersion build() {
         return new McVersion(this.id, this.name, this.version, this.release, this.classVersion);
      }
   }
}
