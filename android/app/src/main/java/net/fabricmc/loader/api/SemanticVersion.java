package net.fabricmc.loader.api;

import java.util.Optional;
import net.fabricmc.loader.impl.util.version.VersionParser;

public interface SemanticVersion extends Version {
   int COMPONENT_WILDCARD = Integer.MIN_VALUE;

   int getVersionComponentCount();

   int getVersionComponent(int var1);

   Optional<String> getPrereleaseKey();

   Optional<String> getBuildKey();

   boolean hasWildcard();

   @Deprecated
   default int compareTo(SemanticVersion o) {
      return this.compareTo(o);
   }

   static SemanticVersion parse(String s) throws VersionParsingException {
      return VersionParser.parseSemantic(s);
   }
}
