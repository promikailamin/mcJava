package net.fabricmc.loader.impl.launch.knot;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.mrj.AbstractSecureClassLoader;

final class KnotClassLoader extends AbstractSecureClassLoader implements KnotClassDelegate.ClassLoaderAccess {
   private final KnotClassLoader.DynamicURLClassLoader urlLoader;
   private final ClassLoader originalLoader = this.getClass().getClassLoader();
   private final KnotClassDelegate<KnotClassLoader> delegate;

   KnotClassLoader(boolean isDevelopment, EnvType envType, GameProvider provider) {
      super("knot", new KnotClassLoader.DynamicURLClassLoader(new URL[0]));
      this.urlLoader = (KnotClassLoader.DynamicURLClassLoader)this.getParent();
      this.delegate = new KnotClassDelegate<>(isDevelopment, envType, this, this.originalLoader, provider);
   }

   KnotClassDelegate<?> getDelegate() {
      return this.delegate;
   }

   @Override
   public URL getResource(String name) {
      Objects.requireNonNull(name);
      URL url = this.urlLoader.getResource(name);
      if (url == null) {
         url = this.originalLoader.getResource(name);
      }

      return url;
   }

   @Override
   public URL findResource(String name) {
      Objects.requireNonNull(name);
      return this.urlLoader.findResource(name);
   }

   @Override
   public Enumeration<URL> findResources(String name) throws IOException {
      Objects.requireNonNull(name);
      return this.urlLoader.findResources(name);
   }

   @Override
   public InputStream getResourceAsStream(String name) {
      Objects.requireNonNull(name);
      InputStream inputStream = this.urlLoader.getResourceAsStream(name);
      if (inputStream == null) {
         inputStream = this.originalLoader.getResourceAsStream(name);
      }

      return inputStream;
   }

   @Override
   public Enumeration<URL> getResources(String name) throws IOException {
      Objects.requireNonNull(name);
      Enumeration<URL> resources = this.urlLoader.getResources(name);
      return !resources.hasMoreElements() ? this.originalLoader.getResources(name) : resources;
   }

   @Override
   protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      return this.delegate.loadClass(name, resolve);
   }

   @Override
   protected Class<?> findClass(String name) throws ClassNotFoundException {
      return this.delegate.tryLoadClass(name, false);
   }

   @Override
   protected String findLibrary(String libname) {
      return this.delegate.findLibrary(libname);
   }

   @Override
   public void addUrlFwd(URL url) {
      this.urlLoader.addURL(url);
   }

   @Override
   public URL findResourceFwd(String name) {
      return this.urlLoader.findResource(name);
   }

   @Override
   public Package getPackageFwd(String name) {
      return super.getPackage(name);
   }

   @Override
   public Package definePackageFwd(
      String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase
   ) throws IllegalArgumentException {
      return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
   }

   @Override
   public Object getClassLoadingLockFwd(String name) {
      return super.getClassLoadingLock(name);
   }

   @Override
   public Class<?> findLoadedClassFwd(String name) {
      return super.findLoadedClass(name);
   }

   @Override
   public Class<?> defineClassFwd(String name, byte[] b, int off, int len, CodeSource cs) {
      return super.defineClass(name, b, off, len, cs);
   }

   @Override
   public void resolveClassFwd(Class<?> cls) {
      super.resolveClass(cls);
   }

   static {
      registerAsParallelCapable();
   }

   private static final class DynamicURLClassLoader extends URLClassLoader {
      private DynamicURLClassLoader(URL[] urls) {
         super(urls, new DummyClassLoader());
      }

      @Override
      public void addURL(URL url) {
         super.addURL(url);
      }

      static {
         registerAsParallelCapable();
      }
   }
}
