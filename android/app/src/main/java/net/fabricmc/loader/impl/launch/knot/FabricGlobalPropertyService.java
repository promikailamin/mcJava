package net.fabricmc.loader.impl.launch.knot;

import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public class FabricGlobalPropertyService implements IGlobalPropertyService {
   public IPropertyKey resolveKey(String name) {
      return new MixinStringPropertyKey(name);
   }

   private String keyString(IPropertyKey key) {
      return ((MixinStringPropertyKey)key).key;
   }

   public <T> T getProperty(IPropertyKey key) {
      return (T)FabricLauncherBase.getProperties().get(this.keyString(key));
   }

   public void setProperty(IPropertyKey key, Object value) {
      FabricLauncherBase.getProperties().put(this.keyString(key), value);
   }

   public <T> T getProperty(IPropertyKey key, T defaultValue) {
      return (T)FabricLauncherBase.getProperties().getOrDefault(this.keyString(key), defaultValue);
   }

   public String getPropertyString(IPropertyKey key, String defaultValue) {
      Object o = FabricLauncherBase.getProperties().get(this.keyString(key));
      return o != null ? o.toString() : defaultValue;
   }
}
