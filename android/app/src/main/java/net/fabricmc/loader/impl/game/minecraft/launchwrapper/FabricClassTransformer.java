package net.fabricmc.loader.impl.game.minecraft.launchwrapper;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import net.minecraft.launchwrapper.IClassTransformer;

public class FabricClassTransformer implements IClassTransformer {
   public byte[] transform(String name, String transformedName, byte[] bytes) {
      boolean isDevelopment = FabricLauncherBase.getLauncher().isDevelopment();
      EnvType envType = FabricLauncherBase.getLauncher().getEnvironmentType();
      byte[] input = FabricLoaderImpl.INSTANCE.getGameProvider().getEntrypointTransformer().transform(name);
      if (input != null) {
         return FabricTransformer.transform(isDevelopment, envType, name, input);
      } else {
         return bytes != null ? FabricTransformer.transform(isDevelopment, envType, name, bytes) : null;
      }
   }
}
