package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.specs.IConstr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IteratorInt;

public class QuadraticPrimeImplicantStrategy implements PrimeImplicantStrategy {
   private int[] prime;

   boolean setAndPropagate(Solver<? extends DataStructureFactory> solver, int p) {
      if (!solver.voc.isUnassigned(p)) {
         return solver.voc.isSatisfied(p);
      }

      assert !solver.trail.contains(p);
      assert !solver.trail.contains(LiteralsUtils.neg(p));
      return solver.assume(p) && solver.propagate() == null;
   }

   @Override
   public int[] compute(Solver<? extends DataStructureFactory> solver) {
      assert solver.qhead == solver.trail.size() + solver.learnedLiterals.size();
      long begin = System.currentTimeMillis();
      if (solver.learnedLiterals.size() > 0) {
         solver.qhead = solver.trail.size();
      }

      if (solver.isVerbose()) {
         System.out.printf("%s implied: %d, decision: %d %n", solver.getLogPrefix(), solver.implied.size(), solver.decisions.size());
      }

      this.prime = new int[solver.realNumberOfVariables() + 1];

      for (int i = 0; i < this.prime.length; i++) {
         this.prime[i] = 0;
      }

      IteratorInt it = solver.implied.iterator();

      while (it.hasNext()) {
         int d = it.next();
         int p = LiteralsUtils.toInternal(d);
         this.prime[Math.abs(d)] = d;
         boolean noproblem = this.setAndPropagate(solver, p);
         assert noproblem;
      }

      int removed = 0;
      int posremoved = 0;
      int propagated = 0;
      int tested = 0;
      int l2propagation = 0;

      for (int i = 0; i < solver.decisions.size(); i++) {
         int d = solver.decisions.get(i);
         assert !solver.voc.isFalsified(LiteralsUtils.toInternal(d));
         if (solver.voc.isSatisfied(LiteralsUtils.toInternal(d))) {
            this.prime[Math.abs(d)] = d;
            propagated++;
         } else if (!this.setAndPropagate(solver, LiteralsUtils.toInternal(-d))) {
            this.prime[Math.abs(d)] = d;
            solver.cancel();
            boolean noproblem = this.setAndPropagate(solver, LiteralsUtils.toInternal(d));
            assert noproblem;
         } else {
            boolean canBeRemoved = true;
            tested++;
            int rightlevel = solver.currentDecisionLevel();

            for (int j = i + 1; j < solver.decisions.size(); j++) {
               l2propagation++;
               if (!this.setAndPropagate(solver, LiteralsUtils.toInternal(solver.decisions.get(j)))) {
                  canBeRemoved = false;
                  break;
               }
            }

            solver.cancelUntil(rightlevel);
            if (canBeRemoved) {
               solver.forget(Math.abs(d));
               IConstr confl = solver.propagate();
               assert confl == null;
               removed++;
               if (d > 0 && d > solver.nVars()) {
                  posremoved++;
               }
            } else {
               this.prime[Math.abs(d)] = d;
               solver.cancel();
               assert solver.voc.isUnassigned(LiteralsUtils.toInternal(d));
               boolean noproblem = this.setAndPropagate(solver, LiteralsUtils.toInternal(d));
               assert noproblem;
            }
         }
      }

      solver.cancelUntil(0);
      int[] implicant = new int[this.prime.length - removed - 1];
      int index = 0;

      for (int i : this.prime) {
         if (i != 0) {
            implicant[index++] = i;
         }
      }

      long end = System.currentTimeMillis();
      if (solver.isVerbose()) {
         System.out.printf("%s prime implicant computation statistics ORIG%n", solver.getLogPrefix());
         System.out
            .printf(
               "%s implied: %d, decision: %d, removed %d (+%d), tested %d, propagated %d), l2 propagation:%d, time(ms):%d %n",
               solver.getLogPrefix(),
               solver.implied.size(),
               solver.decisions.size(),
               removed,
               posremoved,
               tested,
               propagated,
               l2propagation,
               end - begin
            );
      }

      return implicant;
   }

   @Override
   public int[] getPrimeImplicantAsArrayWithHoles() {
      if (this.prime == null) {
         throw new UnsupportedOperationException("Call the compute method first!");
      } else {
         return this.prime;
      }
   }
}
