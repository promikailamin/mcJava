package net.fabricmc.loader.impl.game.patch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipError;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.SimpleClassPath;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class GameTransformer {
   private final List<GamePatch> patches;
   private Map<String, byte[]> patchedClasses;
   private boolean entrypointsLocated = false;

   public GameTransformer(GamePatch... patches) {
      this.patches = Arrays.asList(patches);
   }

   private void addPatchedClass(ClassNode node) {
      String key = node.name.replace('/', '.');
      if (this.patchedClasses.containsKey(key)) {
         throw new RuntimeException("Duplicate addPatchedClasses call: " + key);
      }

      ClassWriter writer = new ClassWriter(0);
      node.accept(writer);
      this.patchedClasses.put(key, writer.toByteArray());
   }

   public void locateEntrypoints(FabricLauncher launcher, List<Path> gameJars) {
      if (!this.entrypointsLocated) {
         this.patchedClasses = new HashMap<>();

         try {
            SimpleClassPath cp = new SimpleClassPath(gameJars);

            try {
               Map<String, ClassNode> patchedClassNodes = new HashMap<>();
               Function<String, ClassNode> classSource = name -> patchedClassNodes.containsKey(name)
                  ? patchedClassNodes.get(name)
                  : this.readClassNode(cp, name);

               for (GamePatch patch : this.patches) {
                  patch.process(launcher, classSource, classNode -> patchedClassNodes.put(classNode.name, classNode));
               }

               for (ClassNode patchedClassNode : patchedClassNodes.values()) {
                  this.addPatchedClass(patchedClassNode);
               }
            } catch (Throwable var9) {
               try {
                  cp.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            cp.close();
         } catch (IOException e) {
            throw ExceptionUtil.wrap(e);
         }

         Log.debug(LogCategory.GAME_PATCH, "Patched %d class%s", this.patchedClasses.size(), this.patchedClasses.size() != 1 ? "s" : "");
         this.entrypointsLocated = true;
      }
   }

   private ClassNode readClassNode(SimpleClassPath classpath, String name) {
      byte[] data = this.patchedClasses.get(name);
      if (data != null) {
         return readClass(new ClassReader(data));
      }

      try {
         SimpleClassPath.CpEntry entry = classpath.getEntry(LoaderUtil.getClassFileName(name));
         if (entry == null) {
            return null;
         }

         try {
            InputStream is = entry.getInputStream();

            ClassNode var6;
            try {
               var6 = readClass(new ClassReader(is));
            } catch (Throwable var9) {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (is != null) {
               is.close();
            }

            return var6;
         } catch (IOException | ZipError e) {
            throw new RuntimeException(String.format("error reading %s in %s: %s", name, LoaderUtil.normalizePath(entry.getOrigin()), e), e);
         }
      } catch (IOException e) {
         throw ExceptionUtil.wrap(e);
      }
   }

   public byte[] transform(String className) {
      return this.patchedClasses.get(className);
   }

   private static ClassNode readClass(ClassReader reader) {
      if (reader == null) {
         return null;
      }

      ClassNode node = new ClassNode();
      reader.accept(node, 0);
      return node;
   }
}
