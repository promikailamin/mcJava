package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;

public class Glucose2LCDS<D extends DataStructureFactory> extends GlucoseLCDS<D> {
   protected Glucose2LCDS(Solver<D> solver, ConflictTimer timer) {
      super(solver, timer);
   }

   @Override
   public String toString() {
      return "Glucose 2 learned constraints deletion strategy (LBD updated on propagation) with timer " + this.getTimer();
   }

   @Override
   public void onPropagation(Constr from, int propagated) {
      if (from.getActivity() > 2.0) {
         int nblevel = this.computeLBD(from, propagated);
         if (nblevel < from.getActivity()) {
            this.getSolver().stats.incUpdateLBD();
            from.setActivity(nblevel);
         }
      }
   }
}
