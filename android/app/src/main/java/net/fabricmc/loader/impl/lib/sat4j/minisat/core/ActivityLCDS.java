package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;

public class ActivityLCDS implements LearnedConstraintsDeletionStrategy {
   private final ConflictTimer timer;
   protected final Solver<? extends DataStructureFactory> solver;

   public ActivityLCDS(Solver<? extends DataStructureFactory> solver, ConflictTimer timer) {
      this.timer = timer;
      this.solver = solver;
   }

   @Override
   public void reduce(IVec<Constr> learnedConstrs) {
      learnedConstrs.sort(this.solver.getActivityComparator());
      int j = 0;

      int i;
      for (i = 0; i < learnedConstrs.size() / 2; i++) {
         Constr c = learnedConstrs.get(i);
         if (!c.locked() && c.size() != 2) {
            this.onRemove(c);
            c.remove(this.solver);
            this.solver.slistener.delete(c);
         } else {
            learnedConstrs.set(j++, learnedConstrs.get(i));
         }
      }

      while (i < learnedConstrs.size()) {
         learnedConstrs.set(j++, learnedConstrs.get(i));
         i++;
      }

      if (this.solver.isVerbose()) {
         this.solver.out.log(this.solver.getLogPrefix() + "cleaning " + (learnedConstrs.size() - j) + " clauses out of " + learnedConstrs.size());
      }

      learnedConstrs.shrinkTo(j);
   }

   @Override
   public ConflictTimer getTimer() {
      return this.timer;
   }

   @Override
   public String toString() {
      return "Activity based learned constraints deletion strategy with timer " + this.timer;
   }

   @Override
   public void init() {
   }

   @Override
   public void onClauseLearning(Constr constr) {
   }

   @Override
   public void onConflictAnalysis(Constr reason) {
      if (reason.learnt()) {
         this.solver.claBumpActivity(reason);
      }
   }

   @Override
   public void onPropagation(Constr from, int propagated) {
   }

   protected void onRemove(Constr c) {
   }
}
