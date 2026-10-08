package net.fabricmc.loader.impl.lib.tinyremapper;

import java.util.Locale;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

public final class PackageAccessChecker {
   public static void checkClass(String accessingClass, String targetClass, String source, AsmRemapper remapper) {
      if (!accessingClass.equals(targetClass)) {
         ClassInstance targetCls = remapper.getClass(targetClass);
         if (targetCls != null) {
            targetCls = targetCls.getMrjOrigin();
            if (!targetCls.isPublicOrPrivate()) {
               String mappedAccessor = remapper.map(accessingClass);
               int pkgEnd = mappedAccessor.lastIndexOf(47);
               String mappedTarget = remapper.map(targetClass);
               if (!isSamePackage(mappedAccessor, pkgEnd, mappedTarget)) {
                  remapper.tr
                     .getLogger()
                     .warn("Invalid access from %s in %s to package-private class %s after remapping.%n", source, mappedAccessor, mappedTarget);
                  remapper.tr.classesToMakePublic.add(targetCls);
               }
            }
         }
      }
   }

   public static void checkDesc(String accessingClass, String targetDesc, String source, AsmRemapper remapper) {
      int startPos = 0;

      int pos;
      while ((pos = targetDesc.indexOf(76, startPos)) >= 0) {
         int end = targetDesc.indexOf(59, ++pos);
         if (end < 0) {
            throw new IllegalArgumentException("invalid descriptor: ".concat(targetDesc));
         }

         checkClass(accessingClass, targetDesc.substring(pos, end), source, remapper);
         startPos = end + 1;
      }
   }

   public static void checkValue(String accessingClass, Object value, String source, AsmRemapper remapper) {
      if (value instanceof Type) {
         checkDesc(accessingClass, ((Type)value).getDescriptor(), source, remapper);
      } else if (value instanceof Handle) {
         Handle handle = (Handle)value;
         checkMember(accessingClass, handle.getOwner(), handle.getName(), handle.getDesc(), TrMember.MemberType.METHOD, source, remapper);
      } else if (value instanceof ConstantDynamic) {
         ConstantDynamic constantDynamic = (ConstantDynamic)value;
         int i = 0;

         for (int max = constantDynamic.getBootstrapMethodArgumentCount(); i < max; i++) {
            checkValue(accessingClass, constantDynamic.getBootstrapMethodArgument(i), source, remapper);
         }

         checkValue(accessingClass, constantDynamic.getBootstrapMethod(), source, remapper);
         checkDesc(accessingClass, constantDynamic.getDescriptor(), source, remapper);
      }
   }

   public static void checkMember(String accessingOwner, String owner, String name, String desc, TrMember.MemberType type, String source, AsmRemapper remapper) {
      checkDesc(accessingOwner, desc, source, remapper);
      ClassInstance cls = remapper.getClass(owner);
      if (cls != null) {
         cls = cls.getMrjOrigin();
         String id = MemberInstance.getId(type, name, desc, remapper.tr.ignoreFieldDesc);
         MemberInstance member = cls.resolve(type, id);
         if (member != null) {
            boolean clsAccessible = cls.isPublicOrPrivate() || accessingOwner.equals(owner);
            boolean memberAccessible = member.isPublic() || member.isPrivate() && cls.getClassVersion() < 55 || accessingOwner.equals(member.cls.getName());
            if (!clsAccessible || !memberAccessible) {
               String mappedAccessor = remapper.map(accessingOwner);
               int pkgEnd = mappedAccessor.lastIndexOf(47);
               if (!clsAccessible && isSamePackage(mappedAccessor, pkgEnd, remapper.map(owner))) {
                  if (memberAccessible || owner.equals(member.cls.getName())) {
                     return;
                  }

                  clsAccessible = true;
               }

               if (!memberAccessible && isSamePackage(mappedAccessor, pkgEnd, remapper.map(member.cls.getName()))) {
                  if (clsAccessible) {
                     return;
                  }

                  memberAccessible = true;
               }

               if (!member.isProtected()
                  || !hasSuperCls(accessingOwner, member.cls.getName(), remapper)
                  || !member.isStatic()
                     && !owner.equals(accessingOwner)
                     && !owner.equals(member.cls.getName())
                     && !hasSuperCls(owner, accessingOwner, remapper)
                     && !hasSuperCls(accessingOwner, owner, remapper)) {
                  assert !clsAccessible || !memberAccessible;
                  String mappedName;
                  String mappedDesc;
                  if (type == TrMember.MemberType.FIELD) {
                     mappedName = remapper.mapFieldName(owner, name, desc);
                     mappedDesc = remapper.mapDesc(desc);
                  } else {
                     mappedName = remapper.mapMethodName(owner, name, desc);
                     mappedDesc = remapper.mapMethodDesc(desc);
                  }

                  String inaccessible = null;
                  if (!clsAccessible) {
                     inaccessible = String.format("package-private class %s", remapper.map(owner));
                  }

                  if (!memberAccessible) {
                     String memberMsg = String.format(
                        "%s %s %s/%s",
                        member.isProtected() ? "protected" : (member.isPrivate() ? "private" : "package-private"),
                        type.name().toLowerCase(Locale.ENGLISH),
                        remapper.map(member.cls.getName()),
                        MemberInstance.getId(type, mappedName, mappedDesc, remapper.tr.ignoreFieldDesc)
                     );
                     if (inaccessible == null) {
                        inaccessible = memberMsg;
                     } else {
                        inaccessible = String.format("%s, %s", inaccessible, memberMsg);
                     }
                  }

                  remapper.tr.getLogger().warn("Invalid access from %s in %s to %s after remapping.%n", source, mappedAccessor, inaccessible);
                  if (!clsAccessible) {
                     remapper.tr.classesToMakePublic.add(cls);
                  }

                  if (!memberAccessible) {
                     remapper.tr.membersToMakePublic.add(member);
                  }
               }
            }
         }
      }
   }

   private static boolean isSamePackage(String clsA, int pkgEnd, String clsB) {
      return pkgEnd < 0 && clsB.indexOf(47) < 0
         || pkgEnd >= 0 && pkgEnd < clsB.length() && clsB.charAt(pkgEnd) == '/' && clsB.indexOf(47, pkgEnd + 1) < 0 && clsA.regionMatches(0, clsB, 0, pkgEnd);
   }

   private static boolean hasSuperCls(String cls, String reqSuperCls, AsmRemapper remapper) {
      assert !cls.equals(reqSuperCls);

      ClassInstance c;
      while ((c = remapper.getClass(cls)) != null && (cls = c.getSuperName()) != null) {
         if (cls.equals(reqSuperCls)) {
            return true;
         }
      }

      return false;
   }
}
