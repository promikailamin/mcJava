package net.fabricmc.loader.api;

import net.fabricmc.loader.impl.util.DefaultLanguageAdapter;

public interface LanguageAdapter {
   static LanguageAdapter getDefault() {
      return DefaultLanguageAdapter.INSTANCE;
   }

   <T> T create(ModContainer var1, String var2, Class<T> var3) throws LanguageAdapterException;
}
