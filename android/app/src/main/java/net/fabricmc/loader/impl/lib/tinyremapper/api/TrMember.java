package net.fabricmc.loader.impl.lib.tinyremapper.api;

public interface TrMember {
   default boolean isField() {
      return this.getType() == TrMember.MemberType.FIELD;
   }

   default boolean isMethod() {
      return this.getType() == TrMember.MemberType.METHOD;
   }

   TrMember.MemberType getType();

   TrClass getOwner();

   String getName();

   String getDesc();

   int getAccess();

   int getIndex();

   default boolean isPublic() {
      return (this.getAccess() & 1) != 0;
   }

   default boolean isProtected() {
      return (this.getAccess() & 4) != 0;
   }

   default boolean isPrivate() {
      return (this.getAccess() & 2) != 0;
   }

   default boolean isStatic() {
      return (this.getAccess() & 8) != 0;
   }

   default boolean isSynthetic() {
      return (this.getAccess() & 4096) != 0;
   }

   enum MemberType {
      METHOD,
      FIELD;
   }
}
