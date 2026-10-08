package net.fabricmc.loader.impl.entrypoint;

import net.fabricmc.loader.api.EntrypointException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

public final class EntrypointContainerImpl<T> implements EntrypointContainer<T> {
   private final String key;
   private final Class<T> type;
   private final EntrypointStorage.Entry entry;
   private T instance;

   public EntrypointContainerImpl(String key, Class<T> type, EntrypointStorage.Entry entry) {
      this.key = key;
      this.type = type;
      this.entry = entry;
   }

   public EntrypointContainerImpl(EntrypointStorage.Entry entry, T instance) {
      this.key = null;
      this.type = null;
      this.entry = entry;
      this.instance = instance;
   }

   @Override
   public synchronized T getEntrypoint() {
      if (this.instance == null) {
         try {
            this.instance = this.entry.getOrCreate(this.type);
            assert this.instance != null;
         } catch (Exception ex) {
            throw new EntrypointException(this.key, this.getProvider().getMetadata().getId(), ex);
         }
      }

      return this.instance;
   }

   @Override
   public ModContainer getProvider() {
      return this.entry.getModContainer();
   }

   @Override
   public String getDefinition() {
      return this.entry.getDefinition();
   }
}
