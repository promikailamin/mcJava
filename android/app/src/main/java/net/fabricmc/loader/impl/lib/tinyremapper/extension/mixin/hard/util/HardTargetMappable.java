package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.util;

import java.util.Objects;
import java.util.Optional;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;

public abstract class HardTargetMappable {
   protected CommonData data;
   protected MxMember self;

   public HardTargetMappable(CommonData data, MxMember self) {
      this.data = Objects.requireNonNull(data);
      this.self = Objects.requireNonNull(self);
   }

   protected abstract Optional<String> getMappedName();

   public Void result() {
      this.getMappedName().ifPresent(x -> this.data.propagate(this.self.asTrMember(this.data.resolver), x));
      return null;
   }
}
