package net.fabricmc.loader.impl.lib.classtweaker.api;

import net.fabricmc.loader.impl.lib.classtweaker.utils.EntryTriple;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface AccessWidener {
   AccessWidener.Access getClassAccess();

   AccessWidener.Access getMethodAccess(EntryTriple var1);

   AccessWidener.Access getFieldAccess(EntryTriple var1);

   AccessWidener.Access getCanonicalConstructorAccess();

   @NonExtendable
   interface Access {
      boolean isAccessible();

      boolean isExtendable();

      boolean isMutable();

      default boolean isChanged() {
         return this.isAccessible() || this.isExtendable() || this.isMutable();
      }

      int apply(int var1, String var2, int var3);
   }
}
