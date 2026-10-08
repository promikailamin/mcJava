package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data;

import java.util.Objects;

public class MxClass {
   private final String name;

   public MxClass(String name) {
      this.name = Objects.requireNonNull(name);
   }

   public MxMember getField(String name, String desc) {
      return new MxMember(this.name, name, desc);
   }

   public MxMember getMethod(String name, String desc) {
      return new MxMember(this.name, name, desc);
   }
}
