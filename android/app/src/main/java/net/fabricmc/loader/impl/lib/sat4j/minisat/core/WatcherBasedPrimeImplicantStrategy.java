package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.util.Comparator;
import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.MandatoryLiteralListener;
import net.fabricmc.loader.impl.lib.sat4j.specs.Propagatable;

public class WatcherBasedPrimeImplicantStrategy implements PrimeImplicantStrategy, MandatoryLiteralListener {
   private int[] prime;
   private final Comparator<Integer> comparator;

   public WatcherBasedPrimeImplicantStrategy(Comparator<Integer> comparator) {
      this.comparator = comparator;
   }

   public WatcherBasedPrimeImplicantStrategy() {
      this(null);
   }

   @Override
   public void isMandatory(int p) {
      this.prime[LiteralsUtils.var(p)] = LiteralsUtils.toDimacs(p);
   }

   @Override
   public int[] compute(Solver<? extends DataStructureFactory> solver) {
      assert solver.qhead == solver.trail.size() + solver.learnedLiterals.size();
      long begin = System.currentTimeMillis();
      if (solver.learnedLiterals.size() > 0) {
         solver.qhead = solver.trail.size();
      }

      this.prime = new int[solver.voc.nVars() + 1];

      for (int i = 0; i < this.prime.length; i++) {
         this.prime[i] = 0;
      }

      for (int i = 0; i < solver.trail.size(); i++) {
         this.isMandatory(solver.trail.get(i));
      }

      for (int d : solver.fullmodel) {
         int p = LiteralsUtils.toInternal(d);
         if (solver.voc.isUnassigned(p)) {
            solver.assume(p);
         }
      }

      for (int d : solver.fullmodel) {
         this.reduceClausesContainingTheNegationOfPI(solver, LiteralsUtils.toInternal(d));
      }

      int removed = 0;
      int posremoved = 0;
      int propagated = 0;

      for (int d : this.fullModel(solver)) {
         if (this.prime[Math.abs(d)] != 0) {
            propagated++;
         } else {
            solver.forget(Math.abs(d));
            this.reduceClausesContainingTheNegationOfPI(solver, LiteralsUtils.toInternal(-d));
            removed++;
            if (d > 0 && d > solver.nVars()) {
               posremoved++;
            }
         }
      }

      solver.cancelUntil(0);
      int[] implicant = new int[propagated];
      int index = 0;

      for (int i : this.prime) {
         if (i != 0) {
            implicant[index++] = i;
         }
      }

      long end = System.currentTimeMillis();
      if (solver.isVerbose()) {
         System.out.printf("%s prime implicant computation statistics BRESIL (reverse = %b)%n", solver.getLogPrefix(), this.comparator != null);
         System.out
            .printf(
               "%s implied: %d, decision: %d, removed %d (+%d), propagated %d, time(ms):%d %n",
               solver.getLogPrefix(),
               solver.implied.size(),
               solver.decisions.size(),
               removed,
               posremoved,
               propagated,
               end - begin
            );
      }

      return implicant;
   }

   Constr reduceClausesContainingTheNegationOfPI(Solver<? extends DataStructureFactory> solver, int p) {
      assert p > 1;
      IVec<Propagatable> lwatched = solver.watched;
      lwatched.clear();
      solver.voc.watches(p).moveTo(lwatched);
      int size = lwatched.size();

      for (int i = 0; i < size; i++) {
         solver.stats.incInspects();
         lwatched.get(i).propagatePI(this, p);
      }

      return null;
   }

   @Override
   public int[] getPrimeImplicantAsArrayWithHoles() {
      if (this.prime == null) {
         throw new UnsupportedOperationException("Call the compute method first!");
      } else {
         return this.prime;
      }
   }

   private int[] fullModel(Solver<? extends DataStructureFactory> solver) {
      if (this.comparator == null) {
         return solver.fullmodel;
      }

      int n = solver.fullmodel.length;
      IVecInt reversed = new VecInt(n);

      for (int i : solver.fullmodel) {
         reversed.push(i);
      }

      reversed.sort(this.comparator);
      return reversed.toArray();
   }
}
