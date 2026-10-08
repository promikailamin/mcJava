package net.fabricmc.loader.impl.lib.tinyremapper;

import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLocal;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;

public class LocalInstance implements TrLocal {
   final TrMethod owner;
   final String name;
   final String desc;
   final int index;

   public LocalInstance(TrMethod owner, String name, String desc, int index) {
      this.owner = owner;
      this.name = name;
      this.desc = desc;
      this.index = index;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public int getIndex() {
      return this.index;
   }
}
