package net.fabricmc.loader.impl.lib.classtweaker.utils;

public final class EntryTriple {
   private final String owner;
   private final String name;
   private final String desc;

   public EntryTriple(String owner, String name, String desc) {
      this.owner = owner;
      this.name = name;
      this.desc = desc;
   }

   @Override
   public String toString() {
      return "EntryTriple{owner=" + this.owner + ",name=" + this.name + ",desc=" + this.desc + "}";
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof EntryTriple)) {
         return false;
      }

      if (o == this) {
         return true;
      }

      EntryTriple other = (EntryTriple)o;
      return other.owner.equals(this.owner) && other.name.equals(this.name) && other.desc.equals(this.desc);
   }

   @Override
   public int hashCode() {
      return this.owner.hashCode() * 37 + this.name.hashCode() * 19 + this.desc.hashCode();
   }
}
