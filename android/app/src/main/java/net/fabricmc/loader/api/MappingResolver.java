package net.fabricmc.loader.api;

import java.util.Collection;

public interface MappingResolver {
   Collection<String> getNamespaces();

   String getCurrentRuntimeNamespace();

   String mapClassName(String var1, String var2);

   String unmapClassName(String var1, String var2);

   String mapFieldName(String var1, String var2, String var3, String var4);

   String mapMethodName(String var1, String var2, String var3, String var4);
}
