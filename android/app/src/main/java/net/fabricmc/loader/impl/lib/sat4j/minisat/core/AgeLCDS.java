package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;

final class AgeLCDS implements LearnedConstraintsDeletionStrategy {
   private final ConflictTimer timer;
   private final Solver<? extends DataStructureFactory> solver;

   AgeLCDS(Solver<? extends DataStructureFactory> solver, ConflictTimer timer) {
      this.timer = timer;
      this.solver = solver;
   }

   @Override
   public void reduce(IVec<Constr> learnedConstrs) {
      int j = 0;

      int i;
      for (i = 0; i < this.solver.learnts.size() / 2; i++) {
         Constr c = this.solver.learnts.get(i);
         if (!c.locked() && c.size() != 2) {
            c.remove(this.solver);
            this.solver.slistener.delete(c);
         } else {
            this.solver.learnts.set(j++, this.solver.learnts.get(i));
         }
      }

      while (i < this.solver.learnts.size()) {
         this.solver.learnts.set(j++, this.solver.learnts.get(i));
         i++;
      }

      if (this.solver.isVerbose()) {
         this.solver.out.log(this.solver.getLogPrefix() + "cleaning " + (this.solver.learnts.size() - j) + " clauses out of " + this.solver.learnts.size());
      }

      this.solver.learnts.shrinkTo(j);
   }

   @Override
   public ConflictTimer getTimer() {
      return this.timer;
   }

   @Override
   public String toString() {
      return "Age based learned constraints deletion strategy with timer " + this.timer;
   }

   @Override
   public void init() {
   }

   @Override
   public void onClauseLearning(Constr constr) {
   }

   @Override
   public void onConflictAnalysis(Constr reason) {
   }

   @Override
   public void onPropagation(Constr from, int propagated) {
   }
}
