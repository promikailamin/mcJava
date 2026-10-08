package net.fabricmc.loader.util.version;

import java.util.Optional;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;

@Deprecated
public class SemanticVersionImpl implements SemanticVersion {
   private final SemanticVersion parent;

   protected SemanticVersionImpl() {
      this.parent = null;
   }

   public SemanticVersionImpl(String version, boolean storeX) throws net.fabricmc.loader.api.VersionParsingException {
      this.parent = SemanticVersion.parse(version);
   }

   @Override
   public int getVersionComponentCount() {
      return this.parent.getVersionComponentCount();
   }

   @Override
   public int getVersionComponent(int pos) {
      return this.parent.getVersionComponent(pos);
   }

   @Override
   public Optional<String> getPrereleaseKey() {
      return this.parent.getPrereleaseKey();
   }

   @Override
   public Optional<String> getBuildKey() {
      return this.parent.getBuildKey();
   }

   @Override
   public String getFriendlyString() {
      return this.parent.getFriendlyString();
   }

   @Override
   public boolean equals(Object o) {
      return this.parent.equals(o);
   }

   @Override
   public int hashCode() {
      return this.parent.hashCode();
   }

   @Override
   public String toString() {
      return this.parent.toString();
   }

   @Override
   public boolean hasWildcard() {
      return this.parent.hasWildcard();
   }

   public boolean equalsComponentsExactly(SemanticVersionImpl other) {
      for (int i = 0; i < Math.max(this.getVersionComponentCount(), other.getVersionComponentCount()); i++) {
         if (this.getVersionComponent(i) != other.getVersionComponent(i)) {
            return false;
         }
      }

      return true;
   }

   public int compareTo(Version o) {
      return this.parent.compareTo(o);
   }
}
