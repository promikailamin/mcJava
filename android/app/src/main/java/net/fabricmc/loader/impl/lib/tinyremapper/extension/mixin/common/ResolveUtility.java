package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrEnvironment;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrField;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLogger;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;

public final class ResolveUtility {
   public static int FLAG_UNIQUE = 1;
   public static int FLAG_FIRST = 2;
   public static int FLAG_RECURSIVE = 4;
   public static int FLAG_NON_SYN = 8;
   private final TrEnvironment environment;
   private final TrLogger logger;

   public ResolveUtility(TrEnvironment environment) {
      this.environment = Objects.requireNonNull(environment);
      this.logger = environment.getLogger();
   }

   public Optional<TrClass> resolveClass(String name) {
      TrClass _class = this.environment.getClass(name);
      return Optional.ofNullable(_class);
   }

   private <T extends TrMember> Optional<T> resolveMember0(
      TrClass owner, String name, String desc, int flag, Supplier<Collection<T>> get, Supplier<Collection<T>> resolve
   ) {
      if ((flag & (FLAG_UNIQUE | FLAG_FIRST)) == 0) {
         throw new RuntimeException("Unspecified resolution strategy, please use FLAG_UNIQUE or FLAG_FIRST.");
      }

      if (owner == null) {
         return Optional.empty();
      }

      Collection<T> collection;
      if ((flag & FLAG_RECURSIVE) != 0) {
         collection = resolve.get();
      } else {
         collection = get.get();
      }

      if ((flag & FLAG_UNIQUE) != 0) {
         if (collection.size() > 1) {
            throw new RuntimeException(
               String.format("The member %s:%s is ambiguous in class %s for FLAG_UNIQUE. Please use FLAG_FIRST.", name, desc, owner.getName())
            );
         } else {
            return collection.stream().findFirst();
         }
      } else {
         Comparator<T> comparator;
         if ((flag & FLAG_NON_SYN) != 0) {
            comparator = (x, y) -> Boolean.compare(x.isSynthetic(), y.isSynthetic()) != 0
               ? Boolean.compare(x.isSynthetic(), y.isSynthetic())
               : Integer.compare(x.getIndex(), y.getIndex());
         } else {
            comparator = Comparator.comparingInt(TrMember::getIndex);
         }

         return collection.stream().min(comparator);
      }
   }

   public Optional<TrField> resolveField(TrClass owner, String name, String desc, int flag) {
      return this.resolveMember0(
         owner, name, desc, flag, () -> owner.getFields(name, desc, false, null, null), () -> owner.resolveFields(name, desc, false, null, null)
      );
   }

   public Optional<TrField> resolveField(String owner, String name, String desc, int flag) {
      return this.resolveClass(owner).flatMap(cls -> this.resolveField(cls, name, desc, flag));
   }

   public Optional<TrMethod> resolveMethod(TrClass owner, String name, String desc, int flag) {
      return this.resolveMember0(
         owner, name, desc, flag, () -> owner.getMethods(name, desc, false, null, null), () -> owner.resolveMethods(name, desc, false, null, null)
      );
   }

   public Optional<TrMethod> resolveMethod(String owner, String name, String desc, int flag) {
      return this.resolveClass(owner).flatMap(cls -> this.resolveMethod(cls, name, desc, flag));
   }

   public Optional<TrMember> resolveMember(TrClass owner, String name, String desc, int flag) {
      if (desc == null) {
         throw new RuntimeException("desc cannot be null for resolveMember. Please use resolveMethod or resolveField.");
      } else {
         TrMember.MemberType type = StringUtility.getTypeByDesc(desc);
         if (type.equals(TrMember.MemberType.FIELD)) {
            return this.resolveField(owner, name, desc, flag).map(m -> (TrMember)m);
         } else if (type.equals(TrMember.MemberType.METHOD)) {
            return this.resolveMethod(owner, name, desc, flag).map(m -> (TrMember)m);
         } else {
            throw new RuntimeException(String.format("Unknown member type %s", type.name()));
         }
      }
   }

   public Optional<TrMember> resolveMember(String owner, String name, String desc, int flag) {
      return this.resolveClass(owner).flatMap(cls -> this.resolveMember(cls, name, desc, flag));
   }
}
