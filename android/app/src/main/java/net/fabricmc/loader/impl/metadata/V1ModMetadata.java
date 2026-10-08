package net.fabricmc.loader.impl.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModEnvironment;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

final class V1ModMetadata extends AbstractModMetadata implements LoaderModMetadata {
   static final V1ModMetadata.IconEntry NO_ICON = size -> Optional.empty();
   private final String id;
   private Version version;
   private final Collection<String> provides;
   private final ModEnvironment environment;
   private final Map<String, List<EntrypointMetadata>> entrypoints;
   private final Collection<NestedJarEntry> jars;
   private final Collection<V1ModMetadata.MixinEntry> mixins;
   private final String classTweaker;
   private Collection<ModDependency> dependencies;
   private final boolean hasRequires;
   private final String name;
   private final String description;
   private final Collection<Person> authors;
   private final Collection<Person> contributors;
   private final ContactInformation contact;
   private final Collection<String> license;
   private final V1ModMetadata.IconEntry icon;
   private final Map<String, String> languageAdapters;
   private final Map<String, CustomValue> customValues;

   V1ModMetadata(
      String id,
      Version version,
      Collection<String> provides,
      ModEnvironment environment,
      Map<String, List<EntrypointMetadata>> entrypoints,
      Collection<NestedJarEntry> jars,
      Collection<V1ModMetadata.MixinEntry> mixins,
      String classTweaker,
      Collection<ModDependency> dependencies,
      boolean hasRequires,
      String name,
      String description,
      Collection<Person> authors,
      Collection<Person> contributors,
      ContactInformation contact,
      Collection<String> license,
      V1ModMetadata.IconEntry icon,
      Map<String, String> languageAdapters,
      Map<String, CustomValue> customValues
   ) {
      this.id = id;
      this.version = version;
      this.provides = Collections.unmodifiableCollection(provides);
      this.environment = environment;
      this.entrypoints = Collections.unmodifiableMap(entrypoints);
      this.jars = Collections.unmodifiableCollection(jars);
      this.mixins = Collections.unmodifiableCollection(mixins);
      this.classTweaker = classTweaker;
      this.dependencies = Collections.unmodifiableCollection(dependencies);
      this.hasRequires = hasRequires;
      this.name = name;
      if (description != null) {
         this.description = description;
      } else {
         this.description = "";
      }

      this.authors = Collections.unmodifiableCollection(authors);
      this.contributors = Collections.unmodifiableCollection(contributors);
      if (contact != null) {
         this.contact = contact;
      } else {
         this.contact = ContactInformation.EMPTY;
      }

      this.license = Collections.unmodifiableCollection(license);
      if (icon != null) {
         this.icon = icon;
      } else {
         this.icon = NO_ICON;
      }

      this.languageAdapters = Collections.unmodifiableMap(languageAdapters);
      this.customValues = Collections.unmodifiableMap(customValues);
   }

   @Override
   public int getSchemaVersion() {
      return 1;
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
      return this.provides;
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
      return this.name != null && !this.name.isEmpty() ? this.name : this.id;
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
      return this.contact;
   }

   @Override
   public Collection<String> getLicense() {
      return this.license;
   }

   @Override
   public Optional<String> getIconPath(int size) {
      return this.icon.getIconPath(size);
   }

   @Override
   public Map<String, CustomValue> getCustomValues() {
      return this.customValues;
   }

   @Override
   public Map<String, String> getLanguageAdapterDefinitions() {
      return this.languageAdapters;
   }

   @Override
   public Collection<NestedJarEntry> getJars() {
      return this.jars;
   }

   @Override
   public Collection<String> getMixinConfigs(EnvType type) {
      List<String> mixinConfigs = new ArrayList<>();

      for (V1ModMetadata.MixinEntry mixin : this.mixins) {
         if (mixin.environment.matches(type)) {
            mixinConfigs.add(mixin.config);
         }
      }

      return mixinConfigs;
   }

   @Override
   public String getClassTweaker() {
      return this.classTweaker;
   }

   @Override
   public Collection<String> getOldInitializers() {
      return Collections.emptyList();
   }

   @Override
   public List<EntrypointMetadata> getEntrypoints(String type) {
      if (type == null) {
         return Collections.emptyList();
      }

      List<EntrypointMetadata> entrypoints = this.entrypoints.get(type);
      return entrypoints != null ? entrypoints : Collections.emptyList();
   }

   @Override
   public Collection<String> getEntrypointKeys() {
      return this.entrypoints.keySet();
   }

   @Override
   public void emitFormatWarnings() {
      if (this.hasRequires) {
         Log.warn(LogCategory.METADATA, "Mod `%s` (%s) uses 'requires' key in fabric.mod.json, which is not supported - use 'depends'", this.id, this.version);
      }
   }

   static final class EntrypointMetadataImpl implements EntrypointMetadata {
      private final String adapter;
      private final String value;

      EntrypointMetadataImpl(String adapter, String value) {
         this.adapter = adapter;
         this.value = value;
      }

      @Override
      public String getAdapter() {
         return this.adapter;
      }

      @Override
      public String getValue() {
         return this.value;
      }
   }

   interface IconEntry {
      Optional<String> getIconPath(int var1);
   }

   static final class JarEntry implements NestedJarEntry {
      private final String file;

      JarEntry(String file) {
         this.file = file;
      }

      @Override
      public String getFile() {
         return this.file;
      }
   }

   static final class MapEntry implements V1ModMetadata.IconEntry {
      private final SortedMap<Integer, String> icons;

      MapEntry(SortedMap<Integer, String> icons) {
         this.icons = icons;
      }

      @Override
      public Optional<String> getIconPath(int size) {
         int iconValue = -1;

         for (int i : this.icons.keySet()) {
            iconValue = i;
            if (iconValue >= size) {
               break;
            }
         }

         return Optional.of(this.icons.get(iconValue));
      }
   }

   static final class MixinEntry {
      private final String config;
      private final ModEnvironment environment;

      MixinEntry(String config, ModEnvironment environment) {
         this.config = config;
         this.environment = environment;
      }
   }

   static final class Single implements V1ModMetadata.IconEntry {
      private final String icon;

      Single(String icon) {
         this.icon = icon;
      }

      @Override
      public Optional<String> getIconPath(int size) {
         return Optional.of(this.icon);
      }
   }
}
