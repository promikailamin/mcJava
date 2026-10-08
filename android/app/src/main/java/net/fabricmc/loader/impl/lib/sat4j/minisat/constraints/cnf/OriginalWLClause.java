package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.MandatoryLiteralListener;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public final class OriginalWLClause extends WLClause {
   private int savedindex = 2;

   public OriginalWLClause(IVecInt ps, ILits voc) {
      super(ps, voc);
   }

   @Override
   public void register() {
      assert this.lits.length > 1;
      this.voc.watch(this.lits[0] ^ 1, this);
      this.voc.watch(this.lits[1] ^ 1, this);
   }

   @Override
   public boolean learnt() {
      return false;
   }

   @Override
   public void setLearnt() {
   }

   public static OriginalWLClause brandNewClause(UnitPropagationListener s, ILits voc, IVecInt literals) {
      OriginalWLClause c = new OriginalWLClause(literals, voc);
      c.register();
      return c;
   }

   @Override
   public void incActivity(double claInc) {
   }

   @Override
   public boolean propagatePI(MandatoryLiteralListener s, int p) {
      int[] mylits = this.lits;
      if (mylits[0] == (p ^ 1)) {
         mylits[0] = mylits[1];
         mylits[1] = p ^ 1;
      }

      int previous = p ^ 1;

      for (int i = this.savedindex; i < mylits.length; i++) {
         if (this.voc.isSatisfied(mylits[i])) {
            mylits[1] = mylits[i];
            mylits[i] = previous;
            this.voc.watch(mylits[1] ^ 1, this);
            this.savedindex = i + 1;
            return true;
         }
      }

      this.voc.watch(p, this);
      s.isMandatory(mylits[0]);
      return true;
   }

   @Override
   public boolean propagate(UnitPropagationListener s, int p) {
      this.savedindex = 2;
      return super.propagate(s, p);
   }
}
