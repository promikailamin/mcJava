package net.fabricmc.loader.impl.lib.tinyremapper.api;

public interface TrMethod extends TrMember {
   default boolean isBridge() {
      return this.getType().equals(TrMember.MemberType.METHOD) && (this.getAccess() & 64) != 0;
   }

   default boolean isAbstract() {
      return this.getType().equals(TrMember.MemberType.METHOD) && (this.getAccess() & 1024) != 0;
   }

   default boolean isVirtual() {
      return this.getType().equals(TrMember.MemberType.METHOD) && (this.getAccess() & 10) == 0;
   }

   TrLocal[] getLocals();
}
