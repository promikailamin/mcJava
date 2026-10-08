package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public class LearntBinaryClause extends BinaryClause {
   public LearntBinaryClause(IVecInt ps, ILits voc) {
      super(ps, voc);
   }

   @Override
   public void setLearnt() {
   }

   @Override
   public boolean learnt() {
      return true;
   }

   @Override
   public void incActivity(double claInc) {
      this.activity += claInc;
   }

   @Override
   public void setActivity(double d) {
      this.activity = d;
   }
}
