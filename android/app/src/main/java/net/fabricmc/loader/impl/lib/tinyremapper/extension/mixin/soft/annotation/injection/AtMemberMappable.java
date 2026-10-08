package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.Objects;
import java.util.Optional;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrRemapper;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.ResolveUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.data.MemberInfo;

class AtMemberMappable {
   private final CommonData data;
   private final MemberInfo info;

   AtMemberMappable(CommonData data, MemberInfo info) {
      this.data = Objects.requireNonNull(data);
      this.info = Objects.requireNonNull(info);
   }

   public MemberInfo result() {
      if (!this.info.isFullyQualified()) {
         this.data.getLogger().warn("%s is not fully qualified.", this.info);
         return this.info;
      } else {
         TrRemapper trRemapper = this.data.mapper.asTrRemapper();
         Optional<TrMember> resolved = this.data
            .resolver
            .resolveMember(this.info.getOwner(), this.info.getName(), this.info.getDesc(), ResolveUtility.FLAG_UNIQUE | ResolveUtility.FLAG_RECURSIVE);
         if (resolved.isPresent()) {
            String newOwner = trRemapper.map(this.info.getOwner());
            String newName = this.data.mapper.mapName(resolved.get());
            String newDesc = this.data.mapper.mapDesc(resolved.get());
            return new MemberInfo(newOwner, newName, this.info.getQuantifier(), newDesc);
         } else {
            String newOwner = trRemapper.map(this.info.getOwner());
            String newDesc = this.info.getType() == TrMember.MemberType.FIELD
               ? trRemapper.mapDesc(this.info.getDesc())
               : trRemapper.mapMethodDesc(this.info.getDesc());
            return new MemberInfo(newOwner, this.info.getName(), this.info.getQuantifier(), newDesc);
         }
      }
   }
}
