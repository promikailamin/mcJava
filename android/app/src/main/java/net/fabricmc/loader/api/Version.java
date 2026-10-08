package net.fabricmc.loader.api;

import net.fabricmc.loader.impl.util.version.VersionParser;

public interface Version extends Comparable<Version> {
   String getFriendlyString();

   static Version parse(String string) throws VersionParsingException {
      return VersionParser.parse(string, false);
   }
}
