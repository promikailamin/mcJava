package net.fabricmc.loader.impl.lib.tinyremapper.api;

import org.objectweb.asm.commons.Remapper;

public abstract class TrRemapper extends Remapper {
   public abstract String mapMethodArg(String var1, String var2, String var3, int var4, String var5);

   @Deprecated
   public String mapAnnotationAttributeName(String descriptor, String name) {
      return super.mapAnnotationAttributeName(descriptor, name);
   }
}
