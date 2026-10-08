package net.fabricmc.loader.api.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public interface ModDependency {
   ModDependency.Kind getKind();

   String getModId();

   boolean matches(Version var1);

   Collection<VersionPredicate> getVersionRequirements();

   List<VersionInterval> getVersionIntervals();

   enum Kind {
      DEPENDS("depends", true, false),
      RECOMMENDS("recommends", true, true),
      SUGGESTS("suggests", true, true),
      CONFLICTS("conflicts", false, true),
      BREAKS("breaks", false, false);

      private static final Map<String, ModDependency.Kind> map = createMap();
      private final String key;
      private final boolean positive;
      private final boolean soft;

      Kind(String key, boolean positive, boolean soft) {
         this.key = key;
         this.positive = positive;
         this.soft = soft;
      }

      public String getKey() {
         return this.key;
      }

      public boolean isPositive() {
         return this.positive;
      }

      public boolean isSoft() {
         return this.soft;
      }

      public static ModDependency.Kind parse(String key) {
         return map.get(key);
      }

      private static Map<String, ModDependency.Kind> createMap() {
         ModDependency.Kind[] values = values();
         Map<String, ModDependency.Kind> ret = new HashMap<>(values.length);

         for (ModDependency.Kind kind : values) {
            ret.put(kind.key, kind);
         }

         return ret;
      }
   }
}
