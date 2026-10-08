package net.fabricmc.loader.impl.entrypoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.EntrypointException;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.metadata.EntrypointMetadata;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public final class EntrypointStorage {
   private final Map<String, List<EntrypointStorage.Entry>> entryMap = new HashMap<>();

   private List<EntrypointStorage.Entry> getOrCreateEntries(String key) {
      return this.entryMap.computeIfAbsent(key, z -> new ArrayList<>());
   }

   public void addDeprecated(ModContainerImpl modContainer, String adapter, String value) throws ClassNotFoundException, LanguageAdapterException {
      Log.debug(LogCategory.ENTRYPOINT, "Registering 0.3.x old-style initializer %s for mod %s", value, modContainer.getMetadata().getId());
      EntrypointStorage.OldEntry oe = new EntrypointStorage.OldEntry(modContainer, adapter, value);
      this.getOrCreateEntries("main").add(oe);
      this.getOrCreateEntries("client").add(oe);
      this.getOrCreateEntries("server").add(oe);
   }

   public void add(ModContainerImpl modContainer, String key, EntrypointMetadata metadata, Map<String, LanguageAdapter> adapterMap) throws Exception {
      if (!adapterMap.containsKey(metadata.getAdapter())) {
         throw new Exception("Could not find adapter '" + metadata.getAdapter() + "' (mod " + modContainer.getMetadata().getId() + "!)");
      }

      Log.debug(
         LogCategory.ENTRYPOINT, "Registering new-style initializer %s for mod %s (key %s)", metadata.getValue(), modContainer.getMetadata().getId(), key
      );
      this.getOrCreateEntries(key).add(new EntrypointStorage.NewEntry(modContainer, adapterMap.get(metadata.getAdapter()), metadata.getValue()));
   }

   public boolean hasEntrypoints(String key) {
      return this.entryMap.containsKey(key);
   }

   public <T> List<T> getEntrypoints(String key, Class<T> type) {
      List<EntrypointStorage.Entry> entries = this.entryMap.get(key);
      if (entries == null) {
         return Collections.emptyList();
      }

      EntrypointException exception = null;
      List<T> results = new ArrayList<>(entries.size());

      for (EntrypointStorage.Entry entry : entries) {
         try {
            T result = entry.getOrCreate(type);
            if (result != null) {
               results.add(result);
            }
         } catch (Throwable t) {
            if (exception == null) {
               exception = new EntrypointException(key, entry.getModContainer().getMetadata().getId(), t);
            } else {
               exception.addSuppressed(t);
            }
         }
      }

      if (exception != null) {
         throw exception;
      } else {
         return results;
      }
   }

   public <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
      List<EntrypointStorage.Entry> entries = this.entryMap.get(key);
      if (entries == null) {
         return Collections.emptyList();
      }

      List<EntrypointContainer<T>> results = new ArrayList<>(entries.size());
      EntrypointException exc = null;
      Iterator var6 = entries.iterator();

      while (true) {
         EntrypointContainerImpl<T> container;
         while (true) {
            if (!var6.hasNext()) {
               if (exc != null) {
                  throw exc;
               }

               return results;
            }

            EntrypointStorage.Entry entry = (EntrypointStorage.Entry)var6.next();
            if (!entry.isOptional()) {
               container = new EntrypointContainerImpl<>(key, type, entry);
               break;
            }

            try {
               T instance = entry.getOrCreate(type);
               if (instance != null) {
                  container = new EntrypointContainerImpl<>(entry, instance);
                  break;
               }
            } catch (Throwable t) {
               if (exc == null) {
                  exc = new EntrypointException(key, entry.getModContainer().getMetadata().getId(), t);
               } else {
                  exc.addSuppressed(t);
               }
            }
         }

         results.add(container);
      }
   }

   static <E extends Throwable> RuntimeException sneakyThrows(Throwable ex) throws E {
      throw ex;
   }

   interface Entry {
      <T> T getOrCreate(Class<T> var1) throws Exception;

      boolean isOptional();

      ModContainerImpl getModContainer();

      String getDefinition();
   }

   private static final class NewEntry implements EntrypointStorage.Entry {
      private final ModContainerImpl mod;
      private final LanguageAdapter adapter;
      private final String value;
      private final Map<Class<?>, Object> instanceMap;

      NewEntry(ModContainerImpl mod, LanguageAdapter adapter, String value) {
         this.mod = mod;
         this.adapter = adapter;
         this.value = value;
         this.instanceMap = new IdentityHashMap<>(1);
      }

      @Override
      public String toString() {
         return this.mod.getMetadata().getId() + "->(0.3.x)" + this.value;
      }

      @Override
      public synchronized <T> T getOrCreate(Class<T> type) throws Exception {
         T ret = (T)this.instanceMap.get(type);
         if (ret == null) {
            ret = this.adapter.create(this.mod, this.value, type);
            assert ret != null;
            T prev = (T)this.instanceMap.putIfAbsent(type, ret);
            if (prev != null) {
               ret = prev;
            }
         }

         return ret;
      }

      @Override
      public boolean isOptional() {
         return false;
      }

      @Override
      public ModContainerImpl getModContainer() {
         return this.mod;
      }

      @Override
      public String getDefinition() {
         return this.value;
      }
   }

   private static class OldEntry implements EntrypointStorage.Entry {
      private static final net.fabricmc.loader.language.LanguageAdapter.Options options = net.fabricmc.loader.language.LanguageAdapter.Options.Builder.create()
         .missingSuperclassBehaviour(net.fabricmc.loader.language.LanguageAdapter.MissingSuperclassBehavior.RETURN_NULL)
         .build();
      private final ModContainerImpl mod;
      private final String languageAdapter;
      private final String value;
      private Object object;

      private OldEntry(ModContainerImpl mod, String languageAdapter, String value) {
         this.mod = mod;
         this.languageAdapter = languageAdapter;
         this.value = value;
      }

      @Override
      public String toString() {
         return this.mod.getInfo().getId() + "->" + this.value;
      }

      @Override
      public synchronized <T> T getOrCreate(Class<T> type) throws Exception {
         if (this.object == null) {
            net.fabricmc.loader.language.LanguageAdapter adapter = (net.fabricmc.loader.language.LanguageAdapter)Class.forName(
                  this.languageAdapter, true, FabricLauncherBase.getLauncher().getTargetClassLoader()
               )
               .getConstructor()
               .newInstance();
            this.object = adapter.createInstance(this.value, options);
         }

         return (T)(this.object != null && type.isAssignableFrom(this.object.getClass()) ? this.object : null);
      }

      @Override
      public boolean isOptional() {
         return true;
      }

      @Override
      public ModContainerImpl getModContainer() {
         return this.mod;
      }

      @Override
      public String getDefinition() {
         return this.value;
      }
   }
}
