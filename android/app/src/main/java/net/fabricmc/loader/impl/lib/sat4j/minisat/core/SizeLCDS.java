package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.util.Comparator;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;

final class SizeLCDS implements LearnedConstraintsDeletionStrategy {
   private final ConflictTimer timer;
   private final Solver<? extends DataStructureFactory> solver;
   private static final Comparator<Constr> comparator = new SizeComparator();

   SizeLCDS(Solver<? extends DataStructureFactory> solver, ConflictTimer timer) {
      this.timer = timer;
      this.solver = solver;
   }

   @Override
   public void reduce(IVec<Constr> learnedConstrs) {
      learnedConstrs.sort(comparator);

      int j;
      for (int i = j = learnedConstrs.size() / 2; i < learnedConstrs.size(); i++) {
         Constr c = learnedConstrs.get(i);
         if (!c.locked() && c.size() != 2) {
            c.remove(this.solver);
            this.solver.slistener.delete(c);
         } else {
            learnedConstrs.set(j++, learnedConstrs.get(i));
         }
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
      return "Sized based learned constraints deletion strategy with timer " + this.timer;
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
}
