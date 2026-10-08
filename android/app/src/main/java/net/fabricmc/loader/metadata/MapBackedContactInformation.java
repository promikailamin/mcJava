package net.fabricmc.loader.metadata;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.loader.api.metadata.ContactInformation;

@Deprecated
public class MapBackedContactInformation implements ContactInformation {
   private final Map<String, String> map;

   public MapBackedContactInformation(Map<String, String> map) {
      this.map = Collections.unmodifiableMap(map);
   }

   @Override
   public Optional<String> get(String key) {
      return Optional.ofNullable(this.map.get(key));
   }

   @Override
   public Map<String, String> asMap() {
      return this.map;
   }
}
