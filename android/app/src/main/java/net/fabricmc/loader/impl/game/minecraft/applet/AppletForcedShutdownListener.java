package net.fabricmc.loader.impl.game.minecraft.applet;

class AppletForcedShutdownListener implements Runnable {
   private final long duration;

   AppletForcedShutdownListener(long duration) {
      this.duration = duration;
   }

   @Override
   public void run() {
      try {
         Thread.sleep(this.duration);
      } catch (InterruptedException ie) {
         ie.printStackTrace();
      }

      System.out.println("~~~ Forcing exit! ~~~");
      System.exit(0);
   }
}
