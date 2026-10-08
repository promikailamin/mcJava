package net.fabricmc.loader.impl.game.minecraft.applet;

import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.impl.game.minecraft.Hooks;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

public class AppletLauncher extends Applet implements AppletStub {
   public static File gameDir;
   private final Map<String, String> params;
   private Applet mcApplet;
   private boolean active;

   public AppletLauncher(File instance, String username, String sessionid, String host, String port, boolean doConnect, boolean fullscreen, boolean demo) {
      gameDir = instance;
      this.params = new HashMap<>();
      this.params.put("username", username);
      this.params.put("sessionid", sessionid);
      this.params.put("stand-alone", "true");
      if (doConnect) {
         this.params.put("server", host);
         this.params.put("port", port);
      }

      this.params.put("fullscreen", Boolean.toString(fullscreen));
      this.params.put("demo", Boolean.toString(demo));

      try {
         this.mcApplet = (Applet)FabricLauncherBase.getLauncher()
            .getTargetClassLoader()
            .loadClass(Hooks.appletMainClass)
            .getDeclaredConstructor()
            .newInstance();
         if (this.mcApplet == null) {
            throw new RuntimeException("Could not instantiate MinecraftApplet - is null?");
         }

         this.add(this.mcApplet, "Center");
      } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
         throw new RuntimeException(e);
      }
   }

   public Map<String, String> getParams() {
      return this.params;
   }

   public void replace(Applet applet) {
      this.mcApplet = applet;
      this.init();
      if (this.active) {
         this.start();
         this.validate();
      }
   }

   @Override
   public void appletResize(int width, int height) {
      this.mcApplet.resize(width, height);
   }

   @Override
   public void resize(int width, int height) {
      this.mcApplet.resize(width, height);
   }

   @Override
   public void resize(Dimension dim) {
      this.mcApplet.resize(dim);
   }

   @Override
   public String getParameter(String name) {
      String value = this.params.get(name);
      if (value != null) {
         return value;
      }

      try {
         return super.getParameter(name);
      } catch (Exception var4) {
         return null;
      }
   }

   @Override
   public boolean isActive() {
      return this.active;
   }

   @Override
   public void init() {
      this.mcApplet.setStub(this);
      this.mcApplet.setSize(this.getWidth(), this.getHeight());
      this.setLayout(new BorderLayout());
      this.add(this.mcApplet, "Center");
      this.mcApplet.init();
   }

   @Override
   public void start() {
      this.mcApplet.start();
      this.active = true;
   }

   @Override
   public void stop() {
      this.mcApplet.stop();
      this.active = false;
   }

   private URL getMinecraftHostingUrl() {
      try {
         return new URL("http://www.minecraft.net/game");
      } catch (MalformedURLException e) {
         e.printStackTrace();
         return null;
      }
   }

   @Override
   public URL getCodeBase() {
      return this.getMinecraftHostingUrl();
   }

   @Override
   public URL getDocumentBase() {
      return this.getMinecraftHostingUrl();
   }

   @Override
   public void setVisible(boolean flag) {
      super.setVisible(flag);
      this.mcApplet.setVisible(flag);
   }
}
