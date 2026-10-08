package net.fabricmc.loader.impl.game.minecraft.launchwrapper;

import net.fabricmc.api.EnvType;

public class FabricClientTweaker extends FabricTweaker {
   @Override
   public EnvType getEnvironmentType() {
      return EnvType.CLIENT;
   }

   public String getLaunchTarget() {
      return "net.minecraft.client.main.Main";
   }
}
