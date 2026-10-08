package net.fabricmc.loader.impl.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;

public class FabricMixinVersions {
   private static final List<FabricMixinVersions.LoaderMixinVersionEntry> versions = new ArrayList<>();
   private static final Map<Integer, String> minLoaderVersions = new HashMap<>();

   static List<FabricMixinVersions.LoaderMixinVersionEntry> getVersions() {
      return versions;
   }

   public static String getMinLoaderVersion(int mixinCompat) {
      return minLoaderVersions.get(mixinCompat);
   }

   private static void addVersion(String minLoaderVersion, int mixinCompat) {
      try {
         versions.add(new FabricMixinVersions.LoaderMixinVersionEntry(SemanticVersion.parse(minLoaderVersion), mixinCompat));
      } catch (VersionParsingException e) {
         throw new RuntimeException(e);
      }

      minLoaderVersions.put(mixinCompat, minLoaderVersion);
   }

   static {
      addVersion("0.19.4", 17004);
      addVersion("0.19.0", 17001);
      addVersion("0.18.4", 17000);
      addVersion("0.17.3", 16005);
      addVersion("0.16.0", 14000);
      addVersion("0.12.0-", 10000);
   }

   static final class LoaderMixinVersionEntry {
      public final SemanticVersion loaderVersion;
      public final int mixinVersion;

      private LoaderMixinVersionEntry(SemanticVersion loaderVersion, int mixinVersion) {
         this.loaderVersion = loaderVersion;
         this.mixinVersion = mixinVersion;
      }
   }
}
