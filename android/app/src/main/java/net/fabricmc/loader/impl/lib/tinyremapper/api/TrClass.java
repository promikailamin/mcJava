package net.fabricmc.loader.impl.lib.tinyremapper.api;

import java.util.Collection;
import java.util.function.Predicate;

public interface TrClass {
   TrEnvironment getEnvironment();

   String getName();

   int getAccess();

   TrMethod getMethod(String var1, String var2);

   Collection<? extends TrMethod> getMethods();

   Collection<TrField> getFields(String var1, String var2, boolean var3, Predicate<TrField> var4, Collection<TrField> var5);

   Collection<TrMethod> getMethods(String var1, String var2, boolean var3, Predicate<TrMethod> var4, Collection<TrMethod> var5);

   Collection<TrField> resolveFields(String var1, String var2, boolean var3, Predicate<TrField> var4, Collection<TrField> var5);

   Collection<TrMethod> resolveMethods(String var1, String var2, boolean var3, Predicate<TrMethod> var4, Collection<TrMethod> var5);

   boolean isAssignableFrom(TrClass var1);

   default boolean isInterface() {
      return (this.getAccess() & 512) != 0;
   }

   default boolean isRecord() {
      return (this.getAccess() & 65536) != 0;
   }

   boolean isInput();
}
