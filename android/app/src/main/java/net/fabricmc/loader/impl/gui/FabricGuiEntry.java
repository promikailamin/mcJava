package net.fabricmc.loader.impl.gui;

import java.awt.GraphicsEnvironment;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.Localization;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.UrlUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public final class FabricGuiEntry {
   public static void open(FabricStatusTree tree) throws Exception {
      GameProvider provider = FabricLoaderImpl.INSTANCE.tryGetGameProvider();
      if ((provider != null || !LoaderUtil.hasAwtSupport()) && (provider == null || !provider.hasAwtSupport())) {
         openForked(tree);
      } else {
         FabricMainWindow.open(tree, true);
      }
   }

   private static void openForked(FabricStatusTree tree) throws IOException, InterruptedException {
      Path javaBinDir = LoaderUtil.normalizePath(Paths.get(System.getProperty("java.home"), "bin"));
      String[] executables = new String[]{"javaw.exe", "java.exe", "java"};
      Path javaPath = null;

      for (String executable : executables) {
         Path path = javaBinDir.resolve(executable);
         if (Files.isRegularFile(path)) {
            javaPath = path;
            break;
         }
      }

      if (javaPath == null) {
         throw new RuntimeException("can't find java executable in " + javaBinDir);
      }

      Process process = new ProcessBuilder(javaPath.toString(), "-Xmx100M", "-cp", UrlUtil.LOADER_CODE_SOURCE.toString(), FabricGuiEntry.class.getName())
         .redirectOutput(Redirect.INHERIT)
         .redirectError(Redirect.INHERIT)
         .start();
      Thread shutdownHook = new Thread(process::destroy);
      Runtime.getRuntime().addShutdownHook(shutdownHook);
      DataOutputStream os = new DataOutputStream(process.getOutputStream());

      try {
         tree.writeTo(os);
      } catch (Throwable var10) {
         try {
            os.close();
         } catch (Throwable var9) {
            var10.addSuppressed(var9);
         }

         throw var10;
      }

      os.close();
      int var14 = process.waitFor();
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
      if (var14 != 0) {
         throw new IOException("subprocess exited with code " + var14);
      }
   }

   public static void main(String[] args) throws Exception {
      FabricStatusTree tree = new FabricStatusTree(new DataInputStream(System.in));
      FabricMainWindow.open(tree, true);
      System.exit(0);
   }

   public static void displayCriticalError(Throwable exception, boolean exitAfter) {
      Log.error(LogCategory.GENERAL, "A critical error occurred", exception);
      displayError(Localization.format("gui.error.header"), exception, exitAfter);
   }

   public static void displayError(String mainText, Throwable exception, boolean exitAfter) {
      displayError(mainText, exception, tree -> {
         StringWriter error = new StringWriter();
         error.append(mainText);
         if (exception != null) {
            error.append(System.lineSeparator());
            exception.printStackTrace(new PrintWriter(error));
         }

         tree.addButton(Localization.format("gui.button.copyError"), FabricStatusTree.FabricBasicButtonType.CLICK_MANY).withClipboard(error.toString());
      }, exitAfter);
   }

   public static void displayError(String mainText, Throwable exception, Consumer<FabricStatusTree> treeCustomiser, boolean exitAfter) {
      boolean isCI = System.getenv("CI") != null;
      boolean isNoGui = SystemProperties.isSet("fabric.noGui");
      GameProvider provider = FabricLoaderImpl.INSTANCE.tryGetGameProvider();
      if (!isCI && !isNoGui && !GraphicsEnvironment.isHeadless() && (provider == null || provider.canOpenErrorGui())) {
         String title = "Fabric Loader 0.19.5";
         FabricStatusTree tree = new FabricStatusTree(title, mainText);
         FabricStatusTree.FabricStatusTab crashTab = tree.addTab(Localization.format("gui.tab.crash"));
         if (exception != null) {
            crashTab.node.addCleanedException(exception);
         } else {
            crashTab.node.addMessage(Localization.format("gui.error.missingException"), FabricStatusTree.FabricTreeWarningLevel.NONE);
         }

         tree.addButton(Localization.format("gui.button.exit"), FabricStatusTree.FabricBasicButtonType.CLICK_ONCE).makeClose();
         treeCustomiser.accept(tree);

         try {
            open(tree);
         } catch (Exception e) {
            if (!exitAfter) {
               throw new RuntimeException("Failed to open the error gui!", e);
            }

            Log.warn(LogCategory.GENERAL, "Failed to open the error gui!", e);
         }
      }

      if (exitAfter) {
         System.exit(1);
      }
   }
}
