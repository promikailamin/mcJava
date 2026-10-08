package net.fabricmc.loader.impl.game.minecraft.applet;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.ImageIcon;
import net.fabricmc.loader.impl.util.Arguments;

public class AppletFrame extends Frame implements WindowListener {
   private AppletLauncher applet = null;

   public AppletFrame(String title, ImageIcon icon) {
      super(title);
      if (icon != null) {
         Image source = icon.getImage();
         int w = source.getWidth(null);
         int h = source.getHeight(null);
         if (w == -1) {
            w = 32;
            h = 32;
         }

         BufferedImage image = new BufferedImage(w, h, 2);
         Graphics2D g2d = (Graphics2D)image.getGraphics();
         g2d.drawImage(source, 0, 0, null);
         this.setIconImage(image);
         g2d.dispose();
      }

      this.addWindowListener(this);
   }

   public void launch(String[] args) {
      Arguments arguments = new Arguments();
      arguments.parse(args);
      String username = arguments.getOrDefault("username", "Player");
      String sessionid;
      if (arguments.containsKey("session")) {
         sessionid = arguments.get("session");
      } else if (arguments.getExtraArgs().size() == 2) {
         username = arguments.getExtraArgs().get(0);
         sessionid = arguments.getExtraArgs().get(1);
      } else {
         sessionid = "";
      }

      File instance = new File(arguments.getOrDefault("gameDir", "."));
      String targetDir = System.getProperty("minecraft.applet.TargetDirectory");
      if (targetDir == null) {
         System.setProperty("minecraft.applet.TargetDirectory", instance.toString());
      } else {
         instance = new File(targetDir);
      }

      System.setProperty("minecraft.applet.WrapperClass", AppletLauncher.class.getName());
      boolean doConnect = arguments.containsKey("server") && arguments.containsKey("port");
      String host = "";
      String port = "";
      if (doConnect) {
         host = arguments.get("server");
         port = arguments.get("port");
      }

      boolean fullscreen = arguments.getExtraArgs().contains("--fullscreen");
      boolean demo = arguments.getExtraArgs().contains("--demo");
      int width = Integer.parseInt(arguments.getOrDefault("width", "854"));
      int height = Integer.parseInt(arguments.getOrDefault("height", "480"));
      this.applet = new AppletLauncher(instance, username, sessionid, host, port, doConnect, fullscreen, demo);

      for (String key : arguments.keys()) {
         this.applet.getParams().put("fabric.arguments." + key, arguments.get(key));
      }

      this.add(this.applet);
      this.applet.setPreferredSize(new Dimension(width, height));
      this.pack();
      this.setLocationRelativeTo(null);
      this.setResizable(true);
      this.validate();
      this.applet.init();
      this.applet.start();
      this.setVisible(true);
   }

   @Override
   public void windowClosing(WindowEvent e) {
      Thread shutdownListenerThread = new Thread(new AppletForcedShutdownListener(30000L));
      shutdownListenerThread.setDaemon(true);
      shutdownListenerThread.start();
      if (this.applet != null) {
         this.applet.stop();
         this.applet.destroy();
      }
   }

   @Override
   public void windowOpened(WindowEvent e) {
   }

   @Override
   public void windowActivated(WindowEvent e) {
   }

   @Override
   public void windowClosed(WindowEvent e) {
   }

   @Override
   public void windowIconified(WindowEvent e) {
   }

   @Override
   public void windowDeiconified(WindowEvent e) {
   }

   @Override
   public void windowDeactivated(WindowEvent e) {
   }
}
