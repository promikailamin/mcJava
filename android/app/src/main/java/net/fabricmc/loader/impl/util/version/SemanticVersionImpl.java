package net.fabricmc.loader.impl.util.version;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

public class SemanticVersionImpl extends net.fabricmc.loader.util.version.SemanticVersionImpl implements SemanticVersion {
   private static final Pattern DOT_SEPARATED_ID = Pattern.compile("|[-0-9A-Za-z]+(\\.[-0-9A-Za-z]+)*");
   private static final Pattern UNSIGNED_INTEGER = Pattern.compile("0|[1-9][0-9]*");
   private final int[] components;
   private final String prerelease;
   private final String build;
   private String friendlyName;

   public SemanticVersionImpl(String version, boolean storeX) throws VersionParsingException {
      int buildDelimPos = version.indexOf(43);
      if (buildDelimPos >= 0) {
         this.build = version.substring(buildDelimPos + 1);
         version = version.substring(0, buildDelimPos);
      } else {
         this.build = null;
      }

      int dashDelimPos = version.indexOf(45);
      if (dashDelimPos >= 0) {
         this.prerelease = version.substring(dashDelimPos + 1);
         version = version.substring(0, dashDelimPos);
      } else {
         this.prerelease = null;
      }

      if (this.prerelease != null && !DOT_SEPARATED_ID.matcher(this.prerelease).matches()) {
         throw new VersionParsingException("Invalid prerelease string '" + this.prerelease + "'!");
      }

      if (version.endsWith(".")) {
         throw new VersionParsingException("Negative version number component found!");
      }

      if (version.startsWith(".")) {
         throw new VersionParsingException("Missing version component!");
      }

      String[] componentStrings = version.split("\\.");
      if (componentStrings.length < 1) {
         throw new VersionParsingException("Did not provide version numbers!");
      }

      int[] components = new int[componentStrings.length];
      int firstWildcardIdx = -1;

      for (int i = 0; i < componentStrings.length; i++) {
         String compStr = componentStrings[i];
         if (storeX) {
            if (compStr.equals("x") || compStr.equals("X") || compStr.equals("*")) {
               if (this.prerelease != null) {
                  throw new VersionParsingException("Pre-release versions are not allowed to use X-ranges!");
               }

               components[i] = Integer.MIN_VALUE;
               if (firstWildcardIdx < 0) {
                  firstWildcardIdx = i;
               }
               continue;
            }

            if (i > 0 && components[i - 1] == Integer.MIN_VALUE) {
               throw new VersionParsingException("Interjacent wildcard (1.x.2) are disallowed!");
            }
         }

         if (compStr.trim().isEmpty()) {
            throw new VersionParsingException("Missing version number component!");
         }

         try {
            components[i] = Integer.parseInt(compStr);
            if (components[i] < 0) {
               throw new VersionParsingException("Negative version number component '" + compStr + "'!");
            }
         } catch (NumberFormatException e) {
            throw new VersionParsingException("Could not parse version number component '" + compStr + "'!", e);
         }
      }

      if (storeX && components.length == 1 && components[0] == Integer.MIN_VALUE) {
         throw new VersionParsingException("Versions of form 'x' or 'X' not allowed!");
      }

      if (firstWildcardIdx > 0 && components.length > firstWildcardIdx + 1) {
         components = Arrays.copyOf(components, firstWildcardIdx + 1);
      }

      this.components = components;
      this.buildFriendlyName();
   }

   public SemanticVersionImpl(int[] components, String prerelease, String build) {
      if (components.length != 0 && components[0] != Integer.MIN_VALUE) {
         this.components = components;
         this.prerelease = prerelease;
         this.build = build;
         this.buildFriendlyName();
      } else {
         throw new IllegalArgumentException("Invalid components: " + Arrays.toString(components));
      }
   }

   private void buildFriendlyName() {
      StringBuilder fnBuilder = new StringBuilder();
      boolean first = true;

      for (int i : this.components) {
         if (first) {
            first = false;
         } else {
            fnBuilder.append('.');
         }

         if (i == Integer.MIN_VALUE) {
            fnBuilder.append('x');
         } else {
            fnBuilder.append(i);
         }
      }

      if (this.prerelease != null) {
         fnBuilder.append('-').append(this.prerelease);
      }

      if (this.build != null) {
         fnBuilder.append('+').append(this.build);
      }

      this.friendlyName = fnBuilder.toString();
   }

   @Override
   public int getVersionComponentCount() {
      return this.components.length;
   }

   @Override
   public int getVersionComponent(int pos) {
      if (pos < 0) {
         throw new RuntimeException("Tried to access negative version number component!");
      } else if (pos >= this.components.length) {
         return this.components[this.components.length - 1] == Integer.MIN_VALUE ? Integer.MIN_VALUE : 0;
      } else {
         return this.components[pos];
      }
   }

   public int[] getVersionComponents() {
      return (int[])this.components.clone();
   }

   @Override
   public Optional<String> getPrereleaseKey() {
      return Optional.ofNullable(this.prerelease);
   }

   @Override
   public Optional<String> getBuildKey() {
      return Optional.ofNullable(this.build);
   }

   @Override
   public String getFriendlyString() {
      return this.friendlyName;
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof SemanticVersionImpl)) {
         return false;
      }

      SemanticVersionImpl other = (SemanticVersionImpl)o;
      return !this.equalsComponentsExactly(other) ? false : Objects.equals(this.prerelease, other.prerelease) && Objects.equals(this.build, other.build);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.components) * 73
         + (this.prerelease != null ? this.prerelease.hashCode() * 11 : 0)
         + (this.build != null ? this.build.hashCode() : 0);
   }

   @Override
   public String toString() {
      return this.getFriendlyString();
   }

   @Override
   public boolean hasWildcard() {
      for (int i : this.components) {
         if (i < 0) {
            return true;
         }
      }

      return false;
   }

   public boolean equalsComponentsExactly(SemanticVersionImpl other) {
      for (int i = 0; i < Math.max(this.getVersionComponentCount(), other.getVersionComponentCount()); i++) {
         if (this.getVersionComponent(i) != other.getVersionComponent(i)) {
            return false;
         }
      }

      return true;
   }

   @Override
   public int compareTo(Version other) {
      if (!(other instanceof SemanticVersion)) {
         return this.getFriendlyString().compareTo(other.getFriendlyString());
      }

      SemanticVersion o = (SemanticVersion)other;

      for (int i = 0; i < Math.max(this.getVersionComponentCount(), o.getVersionComponentCount()); i++) {
         int first = this.getVersionComponent(i);
         int second = o.getVersionComponent(i);
         if (first != Integer.MIN_VALUE && second != Integer.MIN_VALUE) {
            int compare = Integer.compare(first, second);
            if (compare != 0) {
               return compare;
            }
         }
      }

      Optional<String> prereleaseA = this.getPrereleaseKey();
      Optional<String> prereleaseB = o.getPrereleaseKey();
      if (!prereleaseA.isPresent() && !prereleaseB.isPresent()) {
         return 0;
      }

      if (prereleaseA.isPresent() && prereleaseB.isPresent()) {
         StringTokenizer prereleaseATokenizer = new StringTokenizer(prereleaseA.get(), ".");
         StringTokenizer prereleaseBTokenizer = new StringTokenizer(prereleaseB.get(), ".");

         while (prereleaseATokenizer.hasMoreElements()) {
            if (!prereleaseBTokenizer.hasMoreElements()) {
               return 1;
            }

            String partA = prereleaseATokenizer.nextToken();
            String partB = prereleaseBTokenizer.nextToken();
            if (UNSIGNED_INTEGER.matcher(partA).matches()) {
               if (!UNSIGNED_INTEGER.matcher(partB).matches()) {
                  return -1;
               }

               int compare = Integer.compare(partA.length(), partB.length());
               if (compare != 0) {
                  return compare;
               }
            } else if (UNSIGNED_INTEGER.matcher(partB).matches()) {
               return 1;
            }

            int compare = partA.compareTo(partB);
            if (compare != 0) {
               return compare;
            }
         }

         return prereleaseBTokenizer.hasMoreElements() ? -1 : 0;
      } else if (prereleaseA.isPresent()) {
         return o.hasWildcard() ? 0 : -1;
      } else {
         return this.hasWildcard() ? 0 : 1;
      }
   }
}
