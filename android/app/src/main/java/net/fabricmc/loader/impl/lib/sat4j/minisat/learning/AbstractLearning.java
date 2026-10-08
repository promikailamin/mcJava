package net.fabricmc.loader.impl.lib.sat4j.minisat.learning;

import net.fabricmc.loader.impl.lib.sat4j.minisat.core.DataStructureFactory;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.LearningStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Solver;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.VarActivityListener;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;

abstract class AbstractLearning<D extends DataStructureFactory> implements LearningStrategy<D> {
   private VarActivityListener val;

   public void setVarActivityListener(VarActivityListener s) {
      this.val = s;
   }

   @Override
   public void setSolver(Solver<D> s) {
      this.val = s;
   }

   public final void claBumpActivity(Constr reason) {
      for (int i = 0; i < reason.size(); i++) {
         int q = reason.get(i);
         assert q > 1;
         this.val.varBumpActivity(q);
      }
   }

   @Override
   public void init() {
   }
}
