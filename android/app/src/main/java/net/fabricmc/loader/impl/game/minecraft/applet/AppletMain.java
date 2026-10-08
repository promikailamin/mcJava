package net.fabricmc.loader.impl.game.minecraft.applet;

import java.awt.EventQueue;
import java.io.File;

public final class AppletMain implements Runnable {
   final String[] args;

   private AppletMain(String[] args) {
      this.args = args;
   }

   public static File hookGameDir(File file) {
      File proposed = AppletLauncher.gameDir;
      return proposed != null ? proposed : file;
   }

   public static void main(String[] args) {
      EventQueue.invokeLater(new AppletMain(args));
   }

   @Override
   public void run() {
      AppletFrame me = new AppletFrame("Minecraft", null);
      me.launch(this.args);
   }
}
