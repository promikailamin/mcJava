package net.fabricmc.loader.impl.lib.classtweaker.impl;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.impl.lib.classtweaker.api.AccessWidener;
import net.fabricmc.loader.impl.lib.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.loader.impl.lib.classtweaker.utils.AccessUtils;
import net.fabricmc.loader.impl.lib.classtweaker.utils.EntryTriple;
import org.jetbrains.annotations.VisibleForTesting;

public final class AccessWidenerImpl implements AccessWidener, AccessWidenerVisitor {
   private final String owner;
   AccessWidenerImpl.MutableAccess classAccess = AccessWidenerImpl.ClassAccess.DEFAULT;
   final Map<EntryTriple, AccessWidenerImpl.MutableAccess> methodAccess = new HashMap<>();
   final Map<EntryTriple, AccessWidenerImpl.MutableAccess> fieldAccess = new HashMap<>();
   static final AccessWidener DEFAULT = new AccessWidener() {
      @Override
      public AccessWidener.Access getClassAccess() {
         return AccessWidenerImpl.MutableAccess.DEFAULT;
      }

      @Override
      public AccessWidener.Access getMethodAccess(EntryTriple EntryTriple) {
         return AccessWidenerImpl.MutableAccess.DEFAULT;
      }

      @Override
      public AccessWidener.Access getFieldAccess(EntryTriple entryTriple) {
         return AccessWidenerImpl.MutableAccess.DEFAULT;
      }

      @Override
      public AccessWidener.Access getCanonicalConstructorAccess() {
         return AccessWidenerImpl.MutableAccess.DEFAULT;
      }
   };

   public AccessWidenerImpl(String owner) {
      this.owner = owner;
   }

   public AccessWidenerImpl.MutableAccess getClassAccess() {
      return this.classAccess;
   }

   @Override
   public AccessWidener.Access getMethodAccess(EntryTriple entryTriple) {
      AccessWidener.Access access = this.methodAccess.get(entryTriple);
      return access == null ? AccessWidenerImpl.MutableAccess.DEFAULT : access;
   }

   @Override
   public AccessWidener.Access getFieldAccess(EntryTriple entryTriple) {
      AccessWidener.Access access = this.fieldAccess.get(entryTriple);
      return access == null ? AccessWidenerImpl.MutableAccess.DEFAULT : access;
   }

   @Override
   public AccessWidener.Access getCanonicalConstructorAccess() {
      return this.classAccess.isAccessible() ? AccessWidenerImpl.MethodAccess.ACCESSIBLE : AccessWidenerImpl.MethodAccess.DEFAULT;
   }

   @Override
   public void visitClass(AccessWidenerVisitor.AccessType access, boolean transitive) {
      this.classAccess = this.applyAccess(access, this.classAccess, null);
   }

   @Override
   public void visitMethod(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
      this.addOrMerge(this.methodAccess, new EntryTriple(this.owner, name, descriptor), access, AccessWidenerImpl.MethodAccess.DEFAULT);
   }

   @Override
   public void visitField(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
      this.addOrMerge(this.fieldAccess, new EntryTriple(this.owner, name, descriptor), access, AccessWidenerImpl.FieldAccess.DEFAULT);
   }

   AccessWidenerImpl.MutableAccess applyAccess(AccessWidenerVisitor.AccessType input, AccessWidenerImpl.MutableAccess access, EntryTriple entryTriple) {
      switch (input) {
         case ACCESSIBLE:
            this.makeClassAccessible(entryTriple);
            return access.makeAccessible();
         case EXTENDABLE:
            this.makeClassExtendable(entryTriple);
            return access.makeExtendable();
         case MUTABLE:
            return access.makeMutable();
         default:
            throw new UnsupportedOperationException("Unknown access type:" + input);
      }
   }

   private void makeClassAccessible(EntryTriple entryTriple) {
      if (entryTriple != null) {
         this.classAccess = this.applyAccess(AccessWidenerVisitor.AccessType.ACCESSIBLE, this.classAccess, null);
      }
   }

   private void makeClassExtendable(EntryTriple entryTriple) {
      if (entryTriple != null) {
         this.classAccess = this.applyAccess(AccessWidenerVisitor.AccessType.EXTENDABLE, this.classAccess, null);
      }
   }

   void addOrMerge(
      Map<EntryTriple, AccessWidenerImpl.MutableAccess> map,
      EntryTriple entry,
      AccessWidenerVisitor.AccessType access,
      AccessWidenerImpl.MutableAccess defaultAccess
   ) {
      if (entry != null && access != null) {
         map.put(entry, this.applyAccess(access, map.getOrDefault(entry, defaultAccess), entry));
      } else {
         throw new RuntimeException("Input entry or access is null");
      }
   }

   @FunctionalInterface
   public interface AccessOperator {
      int apply(int var1, String var2, int var3);
   }

   @VisibleForTesting
   public enum ClassAccess implements AccessWidenerImpl.MutableAccess {
      DEFAULT((access, name, ownerAccess) -> access),
      ACCESSIBLE((access, name, ownerAccess) -> AccessUtils.makePublic(access)),
      EXTENDABLE((access, name, ownerAccess) -> AccessUtils.makePublic(AccessUtils.removeFinal(access))),
      ACCESSIBLE_EXTENDABLE((access, name, ownerAccess) -> AccessUtils.makePublic(AccessUtils.removeFinal(access)));

      private final AccessWidenerImpl.AccessOperator operator;

      ClassAccess(AccessWidenerImpl.AccessOperator operator) {
         this.operator = operator;
      }

      @Override
      public AccessWidenerImpl.MutableAccess makeAccessible() {
         return this != EXTENDABLE && this != ACCESSIBLE_EXTENDABLE ? ACCESSIBLE : ACCESSIBLE_EXTENDABLE;
      }

      @Override
      public AccessWidenerImpl.MutableAccess makeExtendable() {
         return this.isAccessible() ? ACCESSIBLE_EXTENDABLE : EXTENDABLE;
      }

      @Override
      public AccessWidenerImpl.MutableAccess makeMutable() {
         throw new UnsupportedOperationException("Classes cannot be made mutable");
      }

      @Override
      public boolean isAccessible() {
         return this == ACCESSIBLE || this == ACCESSIBLE_EXTENDABLE;
      }

      @Override
      public boolean isExtendable() {
         return this == EXTENDABLE || this == ACCESSIBLE_EXTENDABLE;
      }

      @Override
      public boolean isMutable() {
         return false;
      }

      @Override
      public int apply(int access, String targetName, int ownerAccess) {
         return this.operator.apply(access, targetName, ownerAccess);
      }
   }

   @VisibleForTesting
   public enum FieldAccess implements AccessWidenerImpl.MutableAccess {
      DEFAULT((access, name, ownerAccess) -> access),
      ACCESSIBLE((access, name, ownerAccess) -> AccessUtils.makePublic(access)),
      MUTABLE((access, name, ownerAccess) -> (ownerAccess & 512) != 0 && (access & 8) != 0 ? access : AccessUtils.removeFinal(access)),
      ACCESSIBLE_MUTABLE(
         (access, name, ownerAccess) -> (ownerAccess & 512) != 0 && (access & 8) != 0
            ? AccessUtils.makePublic(access)
            : AccessUtils.makePublic(AccessUtils.removeFinal(access))
      );

      private final AccessWidenerImpl.AccessOperator operator;

      FieldAccess(AccessWidenerImpl.AccessOperator operator) {
         this.operator = operator;
      }

      @Override
      public AccessWidenerImpl.MutableAccess makeAccessible() {
         return this.isMutable() ? ACCESSIBLE_MUTABLE : ACCESSIBLE;
      }

      @Override
      public AccessWidenerImpl.MutableAccess makeExtendable() {
         throw new UnsupportedOperationException("Fields cannot be made extendable");
      }

      @Override
      public AccessWidenerImpl.MutableAccess makeMutable() {
         return this.isAccessible() ? ACCESSIBLE_MUTABLE : MUTABLE;
      }

      @Override
      public boolean isAccessible() {
         return this == ACCESSIBLE || this == ACCESSIBLE_MUTABLE;
      }

      @Override
      public boolean isExtendable() {
         return false;
      }

      @Override
      public boolean isMutable() {
         return this == MUTABLE || this == ACCESSIBLE_MUTABLE;
      }

      @Override
      public int apply(int access, String targetName, int ownerAccess) {
         return this.operator.apply(access, targetName, ownerAccess);
      }
   }

   @VisibleForTesting
   public enum MethodAccess implements AccessWidenerImpl.MutableAccess {
      DEFAULT((access, name, ownerAccess) -> access),
      ACCESSIBLE((access, name, ownerAccess) -> AccessUtils.makePublic(AccessUtils.makeFinalIfPrivate(access, name, ownerAccess))),
      EXTENDABLE((access, name, ownerAccess) -> AccessUtils.makeProtected(AccessUtils.removeFinal(access))),
      ACCESSIBLE_EXTENDABLE((access, name, owner) -> AccessUtils.makePublic(AccessUtils.removeFinal(access)));

      private final AccessWidenerImpl.AccessOperator operator;

      MethodAccess(AccessWidenerImpl.AccessOperator operator) {
         this.operator = operator;
      }

      @Override
      public AccessWidenerImpl.MutableAccess makeAccessible() {
         return this.isExtendable() ? ACCESSIBLE_EXTENDABLE : ACCESSIBLE;
      }

      @Override
      public AccessWidenerImpl.MutableAccess makeExtendable() {
         return this.isAccessible() ? ACCESSIBLE_EXTENDABLE : EXTENDABLE;
      }

      @Override
      public AccessWidenerImpl.MutableAccess makeMutable() {
         throw new UnsupportedOperationException("Methods cannot be made mutable");
      }

      @Override
      public boolean isAccessible() {
         return this == ACCESSIBLE || this == ACCESSIBLE_EXTENDABLE;
      }

      @Override
      public boolean isExtendable() {
         return this == EXTENDABLE || this == ACCESSIBLE_EXTENDABLE;
      }

      @Override
      public boolean isMutable() {
         return false;
      }

      @Override
      public int apply(int access, String targetName, int ownerAccess) {
         return this.operator.apply(access, targetName, ownerAccess);
      }
   }

   interface MutableAccess extends AccessWidener.Access {
      AccessWidener.Access DEFAULT = new AccessWidener.Access() {
         @Override
         public boolean isAccessible() {
            return false;
         }

         @Override
         public boolean isExtendable() {
            return false;
         }

         @Override
         public boolean isMutable() {
            return false;
         }

         @Override
         public int apply(int access, String targetName, int ownerAccess) {
            return access;
         }
      };

      AccessWidenerImpl.MutableAccess makeAccessible();

      AccessWidenerImpl.MutableAccess makeExtendable();

      AccessWidenerImpl.MutableAccess makeMutable();
   }
}
