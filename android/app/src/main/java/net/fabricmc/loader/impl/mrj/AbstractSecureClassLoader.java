package net.fabricmc.loader.impl.mrj;

import java.security.SecureClassLoader;

public abstract class AbstractSecureClassLoader extends SecureClassLoader {
   public AbstractSecureClassLoader(String name, ClassLoader parent) {
      super(parent);
   }

   static {
      ClassLoader.registerAsParallelCapable();
   }
}
