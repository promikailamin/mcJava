package net.fabricmc.loader.impl.launch.knot;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public class MixinServiceKnotBootstrap implements IMixinServiceBootstrap {
   public String getName() {
      return "Knot";
   }

   public String getServiceClassName() {
      return "net.fabricmc.loader.impl.launch.knot.MixinServiceKnot";
   }

   public void bootstrap() {
   }
}
