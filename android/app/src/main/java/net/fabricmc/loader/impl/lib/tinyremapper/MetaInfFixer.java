package net.fabricmc.loader.impl.lib.tinyremapper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

public class MetaInfFixer implements OutputConsumerPath.ResourceRemapper {
   public static final MetaInfFixer INSTANCE = new MetaInfFixer();

   protected MetaInfFixer() {
   }

   @Override
   public boolean canTransform(TinyRemapper remapper, Path relativePath) {
      return relativePath.startsWith("META-INF")
         && (
            shouldStripForFixMeta(relativePath)
               || relativePath.getFileName().toString().equals("MANIFEST.MF")
               || remapper != null && relativePath.getNameCount() == 3 && relativePath.getName(1).toString().equals("services")
         );
   }

   @Override
   public void transform(Path destinationDirectory, Path relativePath, InputStream input, TinyRemapper remapper) throws IOException {
      String fileName = relativePath.getFileName().toString();
      if (relativePath.getNameCount() == 2 && fileName.equals("MANIFEST.MF")) {
         Manifest manifest = new Manifest(input);
         fixManifest(manifest, remapper);
         Path outputFile = destinationDirectory.resolve(relativePath.toString());
         Path outputDir = outputFile.getParent();
         if (outputDir != null) {
            Files.createDirectories(outputDir);
         }

         OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputFile));

         try {
            manifest.write(os);
         } catch (Throwable var15) {
            try {
               os.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }

            throw var15;
         }

         os.close();
      } else if (remapper != null && relativePath.getNameCount() == 3 && relativePath.getName(1).toString().equals("services")) {
         Path outputDir = destinationDirectory.resolve(relativePath.toString()).getParent();
         Files.createDirectories(outputDir);
         Path outputFile = outputDir.resolve(mapFullyQualifiedClassName(fileName, remapper));
         BufferedReader reader = new BufferedReader(new InputStreamReader(input));

         try {
            BufferedWriter writer = Files.newBufferedWriter(
               outputFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
            );

            try {
               fixServiceDecl(reader, writer, remapper);
            } catch (Throwable var16) {
               if (writer != null) {
                  try {
                     writer.close();
                  } catch (Throwable var13) {
                     var16.addSuppressed(var13);
                  }
               }

               throw var16;
            }

            if (writer != null) {
               writer.close();
            }
         } catch (Throwable var17) {
            try {
               reader.close();
            } catch (Throwable var12) {
               var17.addSuppressed(var12);
            }

            throw var17;
         }

         reader.close();
      }
   }

   private static boolean shouldStripForFixMeta(Path file) {
      if (file.getNameCount() != 2) {
         return false;
      }

      assert file.getName(0).toString().equals("META-INF");
      String fileName = file.getFileName().toString();
      return fileName.endsWith(".SF") || fileName.endsWith(".DSA") || fileName.endsWith(".RSA") || fileName.endsWith(".EC") || fileName.startsWith("SIG-");
   }

   private static String mapFullyQualifiedClassName(String name, TinyRemapper tr) {
      assert name.indexOf(47) < 0;
      return tr.defaultState.remapper.map(name.replace('.', '/')).replace('/', '.');
   }

   private static void fixManifest(Manifest manifest, TinyRemapper remapper) {
      Attributes mainAttrs = manifest.getMainAttributes();
      if (remapper != null) {
         String val = mainAttrs.getValue(Name.MAIN_CLASS);
         if (val != null) {
            mainAttrs.put(Name.MAIN_CLASS, mapFullyQualifiedClassName(val, remapper));
         }

         val = mainAttrs.getValue("Launcher-Agent-Class");
         if (val != null) {
            mainAttrs.putValue("Launcher-Agent-Class", mapFullyQualifiedClassName(val, remapper));
         }
      }

      mainAttrs.remove(Name.SIGNATURE_VERSION);
      Iterator<Attributes> it = manifest.getEntries().values().iterator();

      while (it.hasNext()) {
         Attributes attrs = it.next();
         Iterator<Object> it2 = attrs.keySet().iterator();

         while (it2.hasNext()) {
            Name attrName = (Name)it2.next();
            String name = attrName.toString();
            if (name.endsWith("-Digest") || name.contains("-Digest-") || name.equals("Magic")) {
               it2.remove();
            }
         }

         if (attrs.isEmpty()) {
            it.remove();
         }
      }
   }

   private static void fixServiceDecl(BufferedReader reader, BufferedWriter writer, TinyRemapper remapper) throws IOException {
      String line;
      while ((line = reader.readLine()) != null) {
         int end = line.indexOf(35);
         if (end < 0) {
            end = line.length();
         }

         int start = 0;

         char c;
         while (start < end && ((c = line.charAt(start)) == ' ' || c == '\t')) {
            start++;
         }

         while (end > start && ((c = line.charAt(end - 1)) == ' ' || c == '\t')) {
            end--;
         }

         if (start == end) {
            writer.write(line);
         } else {
            writer.write(line, 0, start);
            writer.write(mapFullyQualifiedClassName(line.substring(start, end), remapper));
            writer.write(line, end, line.length() - end);
         }

         writer.newLine();
      }
   }
}
