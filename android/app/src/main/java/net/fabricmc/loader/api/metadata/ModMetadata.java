package net.fabricmc.loader.api.metadata;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.Version;

public interface ModMetadata {
   String getType();

   String getId();

   Collection<String> getProvides();

   Version getVersion();

   ModEnvironment getEnvironment();

   Collection<ModDependency> getDependencies();

   @Deprecated
   default Collection<ModDependency> getDepends() {
      return this.getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.DEPENDS).collect(Collectors.toList());
   }

   @Deprecated
   default Collection<ModDependency> getRecommends() {
      return this.getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.RECOMMENDS).collect(Collectors.toList());
   }

   @Deprecated
   default Collection<ModDependency> getSuggests() {
      return this.getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.SUGGESTS).collect(Collectors.toList());
   }

   @Deprecated
   default Collection<ModDependency> getConflicts() {
      return this.getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.CONFLICTS).collect(Collectors.toList());
   }

   @Deprecated
   default Collection<ModDependency> getBreaks() {
      return this.getDependencies().stream().filter(d -> d.getKind() == ModDependency.Kind.BREAKS).collect(Collectors.toList());
   }

   String getName();

   String getDescription();

   Collection<Person> getAuthors();

   Collection<Person> getContributors();

   ContactInformation getContact();

   Collection<String> getLicense();

   Optional<String> getIconPath(int var1);

   boolean containsCustomValue(String var1);

   CustomValue getCustomValue(String var1);

   Map<String, CustomValue> getCustomValues();

   @Deprecated
   boolean containsCustomElement(String var1);
}
