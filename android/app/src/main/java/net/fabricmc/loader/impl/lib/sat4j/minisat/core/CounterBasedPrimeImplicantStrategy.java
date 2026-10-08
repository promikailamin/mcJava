package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.IteratorInt;

public class CounterBasedPrimeImplicantStrategy implements PrimeImplicantStrategy {
   private int[] prime;

   @Override
   public int[] compute(Solver<? extends DataStructureFactory> solver) {
      long begin = System.currentTimeMillis();
      IVecInt[] watched = new IVecInt[solver.voc.nVars() * 2 + 2];

      for (int d : solver.fullmodel) {
         watched[LiteralsUtils.toInternal(d)] = new VecInt();
      }

      int[] count = new int[solver.constrs.size()];

      for (int i = 0; i < solver.constrs.size(); i++) {
         Constr constr = solver.constrs.get(i);
         if (!constr.canBeSatisfiedByCountingLiterals()) {
            throw new IllegalStateException("Algo2 does not work with constraints other than clauses and cardinalities" + constr.getClass());
         }

         count[i] = 0;

         for (int j = 0; j < constr.size(); j++) {
            IVecInt watch = watched[constr.get(j)];
            if (watch != null) {
               watch.push(i);
            }
         }
      }

      for (int d : solver.fullmodel) {
         IteratorInt it = watched[LiteralsUtils.toInternal(d)].iterator();

         while (it.hasNext()) {
            count[it.next()]++;
         }
      }

      this.prime = new int[solver.voc.nVars() + 1];

      for (int i = 0; i < this.prime.length; i++) {
         this.prime[i] = 0;
      }

      IteratorInt it = solver.implied.iterator();

      while (it.hasNext()) {
         int d = it.next();
         this.prime[Math.abs(d)] = d;
      }

      int removed = 0;
      int posremoved = 0;
      int propagated = 0;

      label78:
      for (int i = 0; i < solver.decisions.size(); i++) {
         int d = solver.decisions.get(i);
         IteratorInt itx = watched[LiteralsUtils.toInternal(d)].iterator();

         while (itx.hasNext()) {
            int constrNumber = itx.next();
            if (count[constrNumber] == solver.constrs.get(constrNumber).requiredNumberOfSatisfiedLiterals()) {
               this.prime[Math.abs(d)] = d;
               propagated++;
               continue label78;
            }
         }

         removed++;
         if (d > 0 && d > solver.nVars()) {
            posremoved++;
         }

         itx = watched[LiteralsUtils.toInternal(d)].iterator();

         while (itx.hasNext()) {
            count[itx.next()]--;
         }
      }

      int[] implicant = new int[this.prime.length - removed - 1];
      int index = 0;

      for (int i : this.prime) {
         if (i != 0) {
            implicant[index++] = i;
         }
      }

      long end = System.currentTimeMillis();
      if (solver.isVerbose()) {
         System.out.printf("%s prime implicant computation statistics ALGO2%n", solver.getLogPrefix());
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

   @Override
   public int[] getPrimeImplicantAsArrayWithHoles() {
      if (this.prime == null) {
         throw new UnsupportedOperationException("Call the compute method first!");
      } else {
         return this.prime;
      }
   }
}
