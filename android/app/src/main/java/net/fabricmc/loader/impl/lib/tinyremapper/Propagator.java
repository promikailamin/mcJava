package net.fabricmc.loader.impl.lib.tinyremapper;

import java.util.Set;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;

final class Propagator {
   static void propagate(MemberInstance member, String memberId, String nameDst, Set<ClassInstance> visitedUp, Set<ClassInstance> visitedDown) {
      ClassInstance cls = member.cls;
      boolean isVirtual = member.isVirtual();
      visitedUp.add(cls);
      visitedDown.add(cls);
      cls.propagate(
         member.type,
         cls.getName(),
         memberId,
         nameDst,
         isVirtual ? TinyRemapper.Direction.ANY : TinyRemapper.Direction.DOWN,
         isVirtual,
         false,
         true,
         visitedUp,
         visitedDown
      );
      visitedUp.clear();
      visitedDown.clear();
      if (cls.tr.propagateRecordComponents != TinyRemapper.LinkedMethodPropagation.DISABLED && cls.isRecord() && member.isField() && (member.access & 26) == 18
         )
       {
         String getterIdSrc = MemberInstance.getMethodId(member.name, "()".concat(member.desc));
         MemberInstance getter = cls.getMember(TrMember.MemberType.METHOD, getterIdSrc);
         if (getter != null && getter.isVirtual()) {
            visitedUp.add(cls);
            visitedDown.add(cls);
            cls.propagate(TrMember.MemberType.METHOD, cls.getName(), getterIdSrc, nameDst, TinyRemapper.Direction.ANY, true, true, true, visitedUp, visitedDown);
            visitedUp.clear();
            visitedDown.clear();
         }
      }
   }
}
