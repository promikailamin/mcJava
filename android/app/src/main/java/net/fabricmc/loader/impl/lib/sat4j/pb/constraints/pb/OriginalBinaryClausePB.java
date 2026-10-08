package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.OriginalBinaryClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public final class OriginalBinaryClausePB extends OriginalBinaryClause implements PBConstr {
   public OriginalBinaryClausePB(IVecInt ps, ILits voc) {
      super(ps, voc);
   }

   public static OriginalBinaryClausePB brandNewClause(UnitPropagationListener s, ILits voc, IVecInt literals) {
      OriginalBinaryClausePB c = new OriginalBinaryClausePB(literals, voc);
      c.register();
      return c;
   }
}
