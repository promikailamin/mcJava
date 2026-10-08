package net.fabricmc.loader.impl.launch.knot;

import java.util.Objects;
import org.spongepowered.asm.service.IPropertyKey;

public class MixinStringPropertyKey implements IPropertyKey {
   public final String key;

   public MixinStringPropertyKey(String key) {
      this.key = key;
   }

   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof MixinStringPropertyKey) ? false : Objects.equals(this.key, ((MixinStringPropertyKey)obj).key);
   }

   @Override
   public int hashCode() {
      return this.key.hashCode();
   }

   @Override
   public String toString() {
      return this.key;
   }
}
