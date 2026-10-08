package net.fabricmc.loader.impl.game.minecraft.patch;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

public class EntrypointPatchFML125 extends GamePatch {
   private static final String FROM = ModClassLoader_125_FML.class.getName();
   private static final String TO = "cpw.mods.fml.common.ModClassLoader";
   private static final String FROM_INTERNAL = FROM.replace('.', '/');
   private static final String TO_INTERNAL = "cpw/mods/fml/common/ModClassLoader";

   @Override
   public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
      if (classSource.apply("cpw.mods.fml.common.ModClassLoader") != null && classSource.apply("cpw.mods.fml.relauncher.FMLRelauncher") == null) {
         if (!(launcher instanceof Knot)) {
            throw new RuntimeException("1.2.5 FML patch only supported on Knot!");
         }

         Log.debug(LogCategory.GAME_PATCH, "Detected 1.2.5 FML - Knotifying ModClassLoader...");
         ClassNode patchedClassLoader = new ClassNode();

         try {
            InputStream stream = launcher.getResourceAsStream(LoaderUtil.getClassFileName(FROM));

            try {
               if (stream == null) {
                  throw new IOException("Could not find class " + FROM + " in the launcher classpath while transforming ModClassLoader");
               }

               ClassReader patchedClassLoaderReader = new ClassReader(stream);
               patchedClassLoaderReader.accept(patchedClassLoader, 0);
            } catch (Throwable var9) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (IOException e) {
            throw new RuntimeException("An error occurred while reading class " + FROM + " while transforming ModClassLoader", e);
         }

         ClassNode remappedClassLoader = new ClassNode();
         patchedClassLoader.accept(new ClassRemapper(remappedClassLoader, new Remapper() {
            public String map(String internalName) {
               return EntrypointPatchFML125.FROM_INTERNAL.equals(internalName) ? "cpw/mods/fml/common/ModClassLoader" : internalName;
            }
         }));
         classEmitter.accept(remappedClassLoader);
      }
   }
}
