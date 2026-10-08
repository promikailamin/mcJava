package net.fabricmc.loader.impl.discovery;

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
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.fabricmc.loader.impl.metadata.AbstractModMetadata;
import net.fabricmc.loader.impl.metadata.EntrypointMetadata;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.NestedJarEntry;

class BuiltinMetadataWrapper extends AbstractModMetadata implements LoaderModMetadata {
   private final ModMetadata parent;
   private Version version;
   private Collection<ModDependency> dependencies;

   BuiltinMetadataWrapper(ModMetadata parent) {
      this.parent = parent;
      this.version = parent.getVersion();
      this.dependencies = parent.getDependencies();
   }

   @Override
   public String getType() {
      return this.parent.getType();
   }

   @Override
   public String getId() {
      return this.parent.getId();
   }

   @Override
   public Collection<String> getProvides() {
      return this.parent.getProvides();
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
      return this.parent.getEnvironment();
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
      return this.parent.getName();
   }

   @Override
   public String getDescription() {
      return this.parent.getDescription();
   }

   @Override
   public Collection<Person> getAuthors() {
      return this.parent.getAuthors();
   }

   @Override
   public Collection<Person> getContributors() {
      return this.parent.getContributors();
   }

   @Override
   public ContactInformation getContact() {
      return this.parent.getContact();
   }

   @Override
   public Collection<String> getLicense() {
      return this.parent.getLicense();
   }

   @Override
   public Optional<String> getIconPath(int size) {
      return this.parent.getIconPath(size);
   }

   @Override
   public boolean containsCustomValue(String key) {
      return this.parent.containsCustomValue(key);
   }

   @Override
   public CustomValue getCustomValue(String key) {
      return this.parent.getCustomValue(key);
   }

   @Override
   public Map<String, CustomValue> getCustomValues() {
      return this.parent.getCustomValues();
   }

   @Override
   public int getSchemaVersion() {
      return Integer.MAX_VALUE;
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
   public Collection<String> getMixinConfigs(EnvType type) {
      return Collections.emptyList();
   }

   @Override
   public String getClassTweaker() {
      return null;
   }

   @Override
   public boolean loadsInEnvironment(EnvType type) {
      return true;
   }

   @Override
   public Collection<String> getOldInitializers() {
      return Collections.emptyList();
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
}
