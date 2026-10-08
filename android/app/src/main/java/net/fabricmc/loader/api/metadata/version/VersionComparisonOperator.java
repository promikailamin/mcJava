package net.fabricmc.loader.api.metadata.version;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.impl.util.version.SemanticVersionImpl;

public enum VersionComparisonOperator {
   GREATER_EQUAL(">=", true, false) {
      @Override
      public boolean test(SemanticVersion a, SemanticVersion b) {
         return a.compareTo(b) >= 0;
      }

      @Override
      public SemanticVersion minVersion(SemanticVersion version) {
         return version;
      }
   },
   LESS_EQUAL("<=", false, true) {
      @Override
      public boolean test(SemanticVersion a, SemanticVersion b) {
         return a.compareTo(b) <= 0;
      }

      @Override
      public SemanticVersion maxVersion(SemanticVersion version) {
         return version;
      }
   },
   GREATER(">", false, false) {
      @Override
      public boolean test(SemanticVersion a, SemanticVersion b) {
         return a.compareTo(b) > 0;
      }

      @Override
      public SemanticVersion minVersion(SemanticVersion version) {
         return version;
      }
   },
   LESS("<", false, false) {
      @Override
      public boolean test(SemanticVersion a, SemanticVersion b) {
         return a.compareTo(b) < 0;
      }

      @Override
      public SemanticVersion maxVersion(SemanticVersion version) {
         return version;
      }
   },
   EQUAL("=", true, true) {
      @Override
      public boolean test(SemanticVersion a, SemanticVersion b) {
         return a.compareTo(b) == 0;
      }

      @Override
      public SemanticVersion minVersion(SemanticVersion version) {
         return version;
      }

      @Override
      public SemanticVersion maxVersion(SemanticVersion version) {
         return version;
      }
   },
   SAME_TO_NEXT_MINOR("~", true, false) {
      @Override
      public boolean test(SemanticVersion a, SemanticVersion b) {
         return a.compareTo(b) >= 0 && a.getVersionComponent(0) == b.getVersionComponent(0) && a.getVersionComponent(1) == b.getVersionComponent(1);
      }

      @Override
      public SemanticVersion minVersion(SemanticVersion version) {
         return version;
      }

      @Override
      public SemanticVersion maxVersion(SemanticVersion version) {
         return new SemanticVersionImpl(new int[]{version.getVersionComponent(0), version.getVersionComponent(1) + 1}, "", null);
      }
   },
   SAME_TO_NEXT_MAJOR("^", true, false) {
      @Override
      public boolean test(SemanticVersion a, SemanticVersion b) {
         return a.compareTo(b) >= 0 && a.getVersionComponent(0) == b.getVersionComponent(0);
      }

      @Override
      public SemanticVersion minVersion(SemanticVersion version) {
         return version;
      }

      @Override
      public SemanticVersion maxVersion(SemanticVersion version) {
         return new SemanticVersionImpl(new int[]{version.getVersionComponent(0) + 1}, "", null);
      }
   };

   private final String serialized;
   private final boolean minInclusive;
   private final boolean maxInclusive;

   VersionComparisonOperator(String serialized, boolean minInclusive, boolean maxInclusive) {
      this.serialized = serialized;
      this.minInclusive = minInclusive;
      this.maxInclusive = maxInclusive;
   }

   public final String getSerialized() {
      return this.serialized;
   }

   public final boolean isMinInclusive() {
      return this.minInclusive;
   }

   public final boolean isMaxInclusive() {
      return this.maxInclusive;
   }

   public final boolean test(Version a, Version b) {
      if (a instanceof SemanticVersion && b instanceof SemanticVersion) {
         return this.test((SemanticVersion)a, (SemanticVersion)b);
      } else {
         return !this.minInclusive && !this.maxInclusive ? false : a.getFriendlyString().equals(b.getFriendlyString());
      }
   }

   public abstract boolean test(SemanticVersion var1, SemanticVersion var2);

   public SemanticVersion minVersion(SemanticVersion version) {
      return null;
   }

   public SemanticVersion maxVersion(SemanticVersion version) {
      return null;
   }
}
