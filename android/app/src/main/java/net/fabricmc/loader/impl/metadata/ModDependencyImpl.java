package net.fabricmc.loader.impl.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public final class ModDependencyImpl implements ModDependency {
   private ModDependency.Kind kind;
   private final String modId;
   private final List<String> matcherStringList;
   private final Collection<VersionPredicate> ranges;

   public ModDependencyImpl(ModDependency.Kind kind, String modId, List<String> matcherStringList) throws VersionParsingException {
      this.kind = kind;
      this.modId = modId;
      this.matcherStringList = matcherStringList;
      this.ranges = VersionPredicate.parse(this.matcherStringList);
   }

   @Override
   public ModDependency.Kind getKind() {
      return this.kind;
   }

   public void setKind(ModDependency.Kind kind) {
      this.kind = kind;
   }

   @Override
   public String getModId() {
      return this.modId;
   }

   @Override
   public boolean matches(Version version) {
      for (VersionPredicate predicate : this.ranges) {
         if (predicate.test(version)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof ModDependency)) {
         return false;
      }

      ModDependency o = (ModDependency)obj;
      return this.kind == o.getKind() && this.modId.equals(o.getModId()) && this.ranges.equals(o.getVersionRequirements());
   }

   @Override
   public int hashCode() {
      return (this.kind.ordinal() * 31 + this.modId.hashCode()) * 257 + this.ranges.hashCode();
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder("{");
      builder.append(this.kind.getKey());
      builder.append(' ');
      builder.append(this.modId);
      builder.append(" @ [");

      for (int i = 0; i < this.matcherStringList.size(); i++) {
         if (i > 0) {
            builder.append(" || ");
         }

         builder.append(this.matcherStringList.get(i));
      }

      builder.append("]}");
      return builder.toString();
   }

   @Override
   public Collection<VersionPredicate> getVersionRequirements() {
      return this.ranges;
   }

   @Override
   public List<VersionInterval> getVersionIntervals() {
      List<VersionInterval> ret = Collections.emptyList();

      for (VersionPredicate predicate : this.ranges) {
         ret = VersionInterval.or(ret, predicate.getInterval());
      }

      return ret;
   }
}
