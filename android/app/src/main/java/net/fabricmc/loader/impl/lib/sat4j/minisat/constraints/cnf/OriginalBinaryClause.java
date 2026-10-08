package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public class OriginalBinaryClause extends BinaryClause {
   public OriginalBinaryClause(IVecInt ps, ILits voc) {
      super(ps, voc);
   }

   @Override
   public void setLearnt() {
   }

   @Override
   public boolean learnt() {
      return false;
   }

   public static OriginalBinaryClause brandNewClause(UnitPropagationListener s, ILits voc, IVecInt literals) {
      OriginalBinaryClause c = new OriginalBinaryClause(literals, voc);
      c.register();
      return c;
   }

   @Override
   public void incActivity(double claInc) {
   }

   @Override
   public void setActivity(double claInc) {
   }
}
