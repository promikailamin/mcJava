package net.fabricmc.loader.impl.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.Person;

final class V0ModMetadata extends AbstractModMetadata implements LoaderModMetadata {
   private static final V0ModMetadata.Mixins EMPTY_MIXINS = new V0ModMetadata.Mixins(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
   private final String id;
   private Version version;
   private Collection<ModDependency> dependencies;
   private final String languageAdapter = "net.fabricmc.loader.language.JavaLanguageAdapter";
   private final V0ModMetadata.Mixins mixins;
   private final ModEnvironment environment;
   private final String initializer;
   private final Collection<String> initializers;
   private final String name;
   private final String description;
   private final Collection<Person> authors;
   private final Collection<Person> contributors;
   private final ContactInformation links;
   private final String license;

   V0ModMetadata(
      String id,
      Version version,
      Collection<ModDependency> dependencies,
      V0ModMetadata.Mixins mixins,
      ModEnvironment environment,
      String initializer,
      Collection<String> initializers,
      String name,
      String description,
      Collection<Person> authors,
      Collection<Person> contributors,
      ContactInformation links,
      String license
   ) {
      this.id = id;
      this.version = version;
      this.dependencies = Collections.unmodifiableCollection(dependencies);
      if (mixins == null) {
         this.mixins = EMPTY_MIXINS;
      } else {
         this.mixins = mixins;
      }

      this.environment = environment;
      this.initializer = initializer;
      this.initializers = Collections.unmodifiableCollection(initializers);
      this.name = name;
      if (description == null) {
         this.description = "";
      } else {
         this.description = description;
      }

      this.authors = Collections.unmodifiableCollection(authors);
      this.contributors = Collections.unmodifiableCollection(contributors);
      this.links = links;
      this.license = license;
   }

   @Override
   public int getSchemaVersion() {
      return 0;
   }

   @Override
   public String getType() {
      return "fabric";
   }

   @Override
   public String getId() {
      return this.id;
   }

   @Override
   public Collection<String> getProvides() {
      return Collections.emptyList();
   }

   @Override
   public Version getVersion() {
      return this.version;
   }

   @Override
   public void setVersion(Version version) {
      this.version = version;
   }

   @Override
   public ModEnvironment getEnvironment() {
      return this.environment;
   }

   @Override
   public boolean loadsInEnvironment(EnvType type) {
      return this.environment.matches(type);
   }

   @Override
   public Collection<ModDependency> getDependencies() {
      return this.dependencies;
   }

   @Override
   public void setDependencies(Collection<ModDependency> dependencies) {
      this.dependencies = Collections.unmodifiableCollection(dependencies);
   }

   @Override
   public String getName() {
      return this.name != null && this.name.isEmpty() ? this.id : this.name;
   }

   @Override
   public String getDescription() {
      return this.description;
   }

   @Override
   public Collection<Person> getAuthors() {
      return this.authors;
   }

   @Override
   public Collection<Person> getContributors() {
      return this.contributors;
   }

   @Override
   public ContactInformation getContact() {
      return this.links;
   }

   @Override
   public Collection<String> getLicense() {
      return Collections.singleton(this.license);
   }

   @Override
   public Optional<String> getIconPath(int size) {
      return Optional.of("assets/" + this.getId() + "/icon.png");
   }

   @Override
   public String getOldStyleLanguageAdapter() {
      return "net.fabricmc.loader.language.JavaLanguageAdapter";
   }

   @Override
   public Map<String, CustomValue> getCustomValues() {
      return Collections.emptyMap();
   }

   @Override
   public boolean containsCustomValue(String key) {
      return false;
   }

   @Override
   public CustomValue getCustomValue(String key) {
      return null;
   }

   @Override
   public Map<String, String> getLanguageAdapterDefinitions() {
      return Collections.emptyMap();
   }

   @Override
   public Collection<NestedJarEntry> getJars() {
      return Collections.emptyList();
   }

   @Override
   public Collection<String> getOldInitializers() {
      if (this.initializer != null) {
         return Collections.singletonList(this.initializer);
      } else {
         return !this.initializers.isEmpty() ? this.initializers : Collections.emptyList();
      }
   }

   @Override
   public List<EntrypointMetadata> getEntrypoints(String type) {
      return Collections.emptyList();
   }

   @Override
   public Collection<String> getEntrypointKeys() {
      return Collections.emptyList();
   }

   @Override
   public void emitFormatWarnings() {
   }

   @Override
   public Collection<String> getMixinConfigs(EnvType type) {
      List<String> mixinConfigs = new ArrayList<>(this.mixins.common);
      switch (type) {
         case CLIENT:
            mixinConfigs.addAll(this.mixins.client);
            break;
         case SERVER:
            mixinConfigs.addAll(this.mixins.server);
      }

      return mixinConfigs;
   }

   @Override
   public String getClassTweaker() {
      return null;
   }

   static final class Mixins {
      final Collection<String> client;
      final Collection<String> common;
      final Collection<String> server;

      Mixins(Collection<String> client, Collection<String> common, Collection<String> server) {
         this.client = Collections.unmodifiableCollection(client);
         this.common = Collections.unmodifiableCollection(common);
         this.server = Collections.unmodifiableCollection(server);
      }
   }
}
