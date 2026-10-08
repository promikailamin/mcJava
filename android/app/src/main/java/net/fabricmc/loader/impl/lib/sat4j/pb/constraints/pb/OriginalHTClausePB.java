package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.OriginalHTClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public final class OriginalHTClausePB extends OriginalHTClause implements PBConstr {
   public OriginalHTClausePB(IVecInt ps, ILits voc) {
      super(ps, voc);
   }

   public static OriginalHTClausePB brandNewClause(UnitPropagationListener s, ILits voc, IVecInt literals) {
      OriginalHTClausePB c = new OriginalHTClausePB(literals, voc);
      c.register();
      return c;
   }
}
