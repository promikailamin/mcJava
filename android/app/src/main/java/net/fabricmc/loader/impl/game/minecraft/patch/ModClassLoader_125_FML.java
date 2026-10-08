package net.fabricmc.loader.impl.game.minecraft.patch;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.util.UrlUtil;

public class ModClassLoader_125_FML extends URLClassLoader {
   private URL[] localUrls = new URL[0];

   public ModClassLoader_125_FML() {
      super(new URL[0], FabricLauncherBase.getLauncher().getTargetClassLoader());
   }

   @Override
   protected void addURL(URL url) {
      FabricLauncherBase.getLauncher().addToClassPath(UrlUtil.asPath(url));
      URL[] newLocalUrls = new URL[this.localUrls.length + 1];
      System.arraycopy(this.localUrls, 0, newLocalUrls, 0, this.localUrls.length);
      newLocalUrls[this.localUrls.length] = url;
      this.localUrls = newLocalUrls;
   }

   @Override
   public URL[] getURLs() {
      return this.localUrls;
   }

   @Override
   public URL findResource(String name) {
      return this.getParent().getResource(name);
   }

   @Override
   public Enumeration<URL> findResources(String name) throws IOException {
      return this.getParent().getResources(name);
   }

   public void addFile(File file) throws MalformedURLException {
      try {
         this.addURL(UrlUtil.asUrl(file));
      } catch (MalformedURLException e) {
         throw new MalformedURLException(e.getMessage());
      }
   }

   public File getParentSource() {
      return ((MinecraftGameProvider)FabricLoaderImpl.INSTANCE.getGameProvider()).getGameJar().toFile();
   }

   public File[] getParentSources() {
      return new File[]{this.getParentSource()};
   }

   static {
      registerAsParallelCapable();
   }
}
