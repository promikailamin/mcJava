package net.fabricmc.loader.impl.launch.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import net.fabricmc.loader.impl.launch.knot.KnotServer;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.SystemProperties;

public class FabricServerLauncher {
   private static final ClassLoader parentLoader = FabricServerLauncher.class.getClassLoader();
   private static String mainClass = KnotServer.class.getName();

   public static void main(String[] args) {
      URL propUrl = parentLoader.getResource("fabric-server-launch.properties");
      if (propUrl != null) {
         Properties properties = new Properties();

         try {
            InputStreamReader reader = new InputStreamReader(propUrl.openStream(), StandardCharsets.UTF_8);

            try {
               properties.load(reader);
            } catch (Throwable var9) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            reader.close();
         } catch (IOException e) {
            e.printStackTrace();
         }

         if (properties.containsKey("launch.mainClass")) {
            mainClass = properties.getProperty("launch.mainClass");
         }
      }

      boolean dev = SystemProperties.isSet("fabric.development");
      if (!dev) {
         try {
            setup(args);
         } catch (Exception e) {
            throw new RuntimeException("Failed to setup Fabric server environment!", e);
         }
      }

      try {
         Class<?> c = Class.forName(mainClass);
         MethodHandles.lookup().findStatic(c, "main", MethodType.methodType(void.class, String[].class)).invokeExact(args);
      } catch (Throwable e) {
         throw new RuntimeException("An exception occurred when launching the server!", e);
      }
   }

   private static void setup(String... runArguments) throws IOException {
      String path = System.getProperty("fabric.gameJarPath");
      if (path == null) {
         path = getServerJarPath();
         System.setProperty("fabric.gameJarPath", path);
      }

      Path serverJar = LoaderUtil.normalizePath(Paths.get(path));
      if (!Files.exists(serverJar)) {
         System.err.println("The Minecraft server .JAR is missing (" + serverJar + ")!");
         System.err.println();
         System.err.println("Fabric's server-side launcher expects the server .JAR to be provided.");
         System.err.println("You can edit its location in fabric-server-launcher.properties.");
         System.err.println();
         System.err.println("Without the official Minecraft server .JAR, Fabric Loader cannot launch.");
         throw new RuntimeException("Missing game jar at " + serverJar);
      }
   }

   private static String getServerJarPath() throws IOException {
      Path propertiesFile = Paths.get("fabric-server-launcher.properties");
      Properties properties = new Properties();
      if (Files.exists(propertiesFile)) {
         Reader reader = Files.newBufferedReader(propertiesFile);

         try {
            properties.load(reader);
         } catch (Throwable var8) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var8.addSuppressed(var6);
               }
            }

            throw var8;
         }

         if (reader != null) {
            reader.close();
         }
      }

      if (!properties.containsKey("serverJar")) {
         properties.put("serverJar", "server.jar");
         Writer writer = Files.newBufferedWriter(propertiesFile);

         try {
            properties.store(writer, null);
         } catch (Throwable var7) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var5) {
                  var7.addSuppressed(var5);
               }
            }

            throw var7;
         }

         if (writer != null) {
            writer.close();
         }
      }

      return (String)properties.get("serverJar");
   }
}
