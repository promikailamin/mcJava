package net.fabricmc.loader.impl.game.minecraft.launchwrapper;

import net.fabricmc.api.EnvType;

public class FabricServerTweaker extends FabricTweaker {
   @Override
   public EnvType getEnvironmentType() {
      return EnvType.SERVER;
   }

   public String getLaunchTarget() {
      return "net.minecraft.server.MinecraftServer";
   }
}
