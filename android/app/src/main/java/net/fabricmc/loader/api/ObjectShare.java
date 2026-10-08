package net.fabricmc.loader.api;

import java.util.function.BiConsumer;

public interface ObjectShare {
   Object get(String var1);

   void whenAvailable(String var1, BiConsumer<String, Object> var2);

   Object put(String var1, Object var2);

   Object putIfAbsent(String var1, Object var2);

   Object remove(String var1);
}
