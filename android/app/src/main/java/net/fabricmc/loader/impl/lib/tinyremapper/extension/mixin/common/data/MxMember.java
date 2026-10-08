package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data;

import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.ResolveUtility;

public class MxMember {
   private final String owner;
   private final String name;
   private final String desc;

   MxMember(String owner, String name, String desc) {
      this.owner = Objects.requireNonNull(owner);
      this.name = Objects.requireNonNull(name);
      this.desc = Objects.requireNonNull(desc);
   }

   public String getName() {
      return this.name;
   }

   public String getDesc() {
      return this.desc;
   }

   public TrMember asTrMember(ResolveUtility resolver) {
      return resolver.resolveMember(this.owner, this.name, this.desc, ResolveUtility.FLAG_UNIQUE)
         .orElseThrow(() -> new RuntimeException(String.format("Cannot convert %s %s %s to TrMember", this.owner, this.name, this.desc)));
   }
}
