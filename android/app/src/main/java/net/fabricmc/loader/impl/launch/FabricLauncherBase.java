package net.fabricmc.loader.impl.launch;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.util.Map;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.gui.FabricGuiEntry;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.jetbrains.annotations.VisibleForTesting;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;

public abstract class FabricLauncherBase implements FabricLauncher {
   protected static final boolean IS_DEVELOPMENT = SystemProperties.isSet("fabric.development");
   private static boolean mixinReady;
   private static Map<String, Object> properties;
   private static FabricLauncher launcher;
   private static MappingConfiguration mappingConfiguration = new MappingConfiguration();

   protected FabricLauncherBase() {
      setLauncher(this);
   }

   public static Class<?> getClass(String className) throws ClassNotFoundException {
      return Class.forName(className, true, getLauncher().getTargetClassLoader());
   }

   @Override
   public final boolean isDevelopment() {
      return IS_DEVELOPMENT;
   }

   @Override
   public MappingConfiguration getMappingConfiguration() {
      return mappingConfiguration;
   }

   protected static void setProperties(Map<String, Object> propertiesA) {
      if (properties != null && properties != propertiesA) {
         throw new RuntimeException("Duplicate setProperties call!");
      }

      properties = propertiesA;
   }

   @VisibleForTesting
   public static void setLauncher(FabricLauncher launcherA) {
      if (launcher != null && launcher != launcherA) {
         throw new RuntimeException("Duplicate setLauncher call!");
      }

      launcher = launcherA;
   }

   public static FabricLauncher getLauncher() {
      return launcher;
   }

   public static Map<String, Object> getProperties() {
      return properties;
   }

   protected static void handleFormattedException(FormattedException exc) {
      Throwable actualExc = exc.getMessage() != null ? exc : exc.getCause();
      Log.error(LogCategory.GENERAL, exc.getMainText(), actualExc);
      GameProvider gameProvider = FabricLoaderImpl.INSTANCE.tryGetGameProvider();
      if (gameProvider != null && gameProvider.displayCrash(actualExc, exc.getDisplayedText())) {
         System.exit(1);
      } else {
         FabricGuiEntry.displayError(exc.getDisplayedText(), actualExc, true);
      }

      throw new AssertionError("exited");
   }

   protected static void setupUncaughtExceptionHandler() {
      final Thread mainThread = Thread.currentThread();
      Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
         @Override
         public void uncaughtException(Thread t, Throwable e) {
            try {
               if (e instanceof FormattedException) {
                  FabricLauncherBase.handleFormattedException((FormattedException)e);
               } else {
                  String mainText = String.format("Uncaught exception in thread \"%s\"", t.getName());
                  Log.error(LogCategory.GENERAL, mainText, e);
                  GameProvider gameProvider = FabricLoaderImpl.INSTANCE.tryGetGameProvider();
                  if (Thread.currentThread() == mainThread && (gameProvider == null || !gameProvider.displayCrash(e, mainText))) {
                     FabricGuiEntry.displayError(mainText, e, false);
                  }
               }
            } catch (Throwable e2) {
               e.addSuppressed(e2);

               try {
                  e.printStackTrace();
               } catch (Throwable e3) {
                  PrintWriter pw = new PrintWriter(new FileOutputStream(FileDescriptor.err));
                  e.printStackTrace(pw);
                  pw.flush();
               }
            }
         }
      });
   }

   protected static void finishMixinBootstrapping() {
      if (mixinReady) {
         throw new RuntimeException("Must not call FabricLauncherBase.finishMixinBootstrapping() twice!");
      }

      try {
         Method m = MixinEnvironment.class.getDeclaredMethod("gotoPhase", Phase.class);
         m.setAccessible(true);
         m.invoke(null, Phase.INIT);
         m.invoke(null, Phase.DEFAULT);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      mixinReady = true;
   }

   public static boolean isMixinReady() {
      return mixinReady;
   }
}
