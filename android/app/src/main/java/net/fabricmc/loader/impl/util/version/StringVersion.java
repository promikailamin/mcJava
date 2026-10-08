package net.fabricmc.loader.impl.util.version;

import net.fabricmc.loader.api.Version;

public class StringVersion implements Version {
   private final String version;

   public StringVersion(String version) {
      this.version = version;
   }

   @Override
   public String getFriendlyString() {
      return this.version;
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof StringVersion ? this.version.equals(((StringVersion)obj).version) : false;
   }

   public int compareTo(Version o) {
      return this.getFriendlyString().compareTo(o.getFriendlyString());
   }

   @Override
   public String toString() {
      return this.version;
   }
}
