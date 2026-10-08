package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.data;

public final class SoftInterface {
   private String target;
   private String prefix;
   private SoftInterface.Remap remap;

   public String getTarget() {
      return this.target;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public SoftInterface.Remap getRemap() {
      return this.remap;
   }

   public void setTarget(String target) {
      this.target = target;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }

   public void setRemap(SoftInterface.Remap remap) {
      this.remap = remap;
   }

   @Override
   public String toString() {
      return "Interface{target='" + this.target + '\'' + ", prefix='" + this.prefix + '\'' + ", remap=" + this.remap + '}';
   }

   public enum Remap {
      NONE,
      ONLY_PREFIX,
      ALL,
      FORCE;
   }
}
