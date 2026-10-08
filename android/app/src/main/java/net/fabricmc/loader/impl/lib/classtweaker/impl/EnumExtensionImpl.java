package net.fabricmc.loader.impl.lib.classtweaker.impl;

import net.fabricmc.loader.impl.lib.classtweaker.api.EnumExtension;

public class EnumExtensionImpl implements EnumExtension {
   private final String addedConstant;

   public EnumExtensionImpl(String addedConstant) {
      this.addedConstant = addedConstant;
   }

   @Override
   public String getAddedConstant() {
      return this.addedConstant;
   }
}
