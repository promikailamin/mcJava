package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;

class GlucoseLCDS<D extends DataStructureFactory> implements LearnedConstraintsDeletionStrategy {
   protected final Solver<D> solver;
   private int[] flags = new int[0];
   private int flag = 0;
   private final ConflictTimer timer;

   protected GlucoseLCDS(Solver<D> solver, ConflictTimer timer) {
      this.solver = solver;
      this.timer = timer;
   }

   @Override
   public void reduce(IVec<Constr> learnedConstrs) {
      learnedConstrs.sort(this.solver.getActivityComparator());

      int j;
      for (int i = j = learnedConstrs.size() / 2; i < learnedConstrs.size(); i++) {
         Constr c = learnedConstrs.get(i);
         if (!c.locked() && !(c.getActivity() <= 2.0)) {
            c.remove(this.solver);
            this.solver.slistener.delete(c);
            this.onRemove(c);
         } else {
            learnedConstrs.set(j++, learnedConstrs.get(i));
         }
      }

      if (this.solver.isVerbose()) {
         this.solver
            .out
            .log(
               this.solver.getLogPrefix()
                  + "cleaning "
                  + (learnedConstrs.size() - j)
                  + " clauses out of "
                  + learnedConstrs.size()
                  + " with flag "
                  + this.flag
                  + "/"
                  + this.solver.stats.getConflicts()
            );
      }

      learnedConstrs.shrinkTo(j);
   }

   protected void onRemove(Constr c) {
   }

   @Override
   public ConflictTimer getTimer() {
      return this.timer;
   }

   @Override
   public String toString() {
      return "Glucose learned constraints deletion strategy with timer " + this.timer;
   }

   @Override
   public void init() {
      int howmany = this.solver.voc.nVars();
      if (this.flags.length <= howmany) {
         this.flags = new int[howmany + 1];
      }

      this.flag = 0;
      this.timer.reset();
   }

   @Override
   public void onClauseLearning(Constr constr) {
      int nblevel = this.computeLBD(constr, -1);
      constr.setActivity(nblevel);
   }

   protected int computeLBD(Constr constr, int propagated) {
      int nblevel = 1;
      this.flag++;

      for (int i = 1; i < constr.size(); i++) {
         int currentLevel = this.solver.voc.getLevel(constr.get(i));
         if (currentLevel >= 0 && this.flags[currentLevel] != this.flag) {
            this.flags[currentLevel] = this.flag;
            nblevel++;
         }
      }

      return nblevel;
   }

   @Override
   public void onConflictAnalysis(Constr reason) {
   }

   @Override
   public void onPropagation(Constr from, int propagated) {
   }

   protected Solver<D> getSolver() {
      return this.solver;
   }
}
