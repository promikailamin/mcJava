package net.fabricmc.loader.impl.lib.sat4j.tools.xplain;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.ISolver;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.IteratorInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.TimeoutException;

public class DeletionStrategy implements MinimizationStrategy {
   private boolean computationCanceled;

   @Override
   public IVecInt explain(ISolver solver, Map<Integer, ?> constrs, IVecInt assumps) throws TimeoutException {
      this.computationCanceled = false;
      IVecInt encodingAssumptions = new VecInt(constrs.size() + assumps.size());
      assumps.copyTo(encodingAssumptions);
      IVecInt firstExplanation = solver.unsatExplanation();
      IVecInt results = new VecInt(firstExplanation.size());
      if (firstExplanation.size() == 1) {
         results.push(-firstExplanation.get(0));
         return results;
      }

      if (solver.isVerbose()) {
         System.out.print(solver.getLogPrefix() + "initial unsat core ");
         firstExplanation.sort();
         IteratorInt it = firstExplanation.iterator();

         while (it.hasNext()) {
            System.out.print(constrs.get(-it.next()));
            System.out.print(" ");
         }

         System.out.println();
         solver.printStat(new PrintWriter(System.out, true), "c ");
      }

      int i = 0;

      while (i < firstExplanation.size()) {
         if (assumps.contains(firstExplanation.get(i))) {
            firstExplanation.delete(i);
         } else {
            i++;
         }
      }

      Set<Integer> constraintsVariables = constrs.keySet();
      IVecInt remainingVariables = new VecInt(constraintsVariables.size());

      for (Integer v : constraintsVariables) {
         remainingVariables.push(v);
      }

      IteratorInt it = firstExplanation.iterator();

      while (it.hasNext()) {
         int p = it.next();
         if (p < 0) {
            p = -p;
         }

         remainingVariables.remove(p);
      }

      remainingVariables.copyTo(encodingAssumptions);
      int unsatcorebegin = encodingAssumptions.size();
      firstExplanation.copyTo(encodingAssumptions);
      assert !solver.isSatisfiable(encodingAssumptions);
      int unsatcorelimit = encodingAssumptions.size() - 1;

      for (int ix = unsatcorebegin; ix < unsatcorelimit; ix++) {
         if (this.computationCanceled) {
            throw new TimeoutException();
         }

         encodingAssumptions.set(ix, -encodingAssumptions.get(ix));
         if (solver.isVerbose()) {
            System.out.println(solver.getLogPrefix() + "checking " + constrs.get(encodingAssumptions.get(ix)) + " ...");
         }

         if (solver.isSatisfiable(encodingAssumptions)) {
            encodingAssumptions.set(ix, -encodingAssumptions.get(ix));
            results.push(-encodingAssumptions.get(ix));
            if (solver.isVerbose()) {
               System.out.println(solver.getLogPrefix() + "mandatory.");
            }
         } else if (solver.isVerbose()) {
            System.out.println(solver.getLogPrefix() + "not needed.");
         }
      }

      if (results.size() == 0) {
         results.push(-encodingAssumptions.get(unsatcorelimit));
         if (solver.isVerbose()) {
            System.out
               .println(
                  solver.getLogPrefix()
                     + "skipping last test,the remaining element "
                     + constrs.get(encodingAssumptions.get(unsatcorelimit))
                     + " is causing the inconsistency!"
               );
         }
      } else {
         encodingAssumptions.set(unsatcorelimit, -encodingAssumptions.get(unsatcorelimit));
         if (solver.isVerbose()) {
            System.out.println(solver.getLogPrefix() + "checking " + constrs.get(encodingAssumptions.get(unsatcorelimit)) + " ...");
         }

         if (solver.isSatisfiable(encodingAssumptions)) {
            encodingAssumptions.set(unsatcorelimit, -encodingAssumptions.get(unsatcorelimit));
            results.push(-encodingAssumptions.get(unsatcorelimit));
            if (solver.isVerbose()) {
               System.out.println(solver.getLogPrefix() + "mandatory.");
            }
         } else if (solver.isVerbose()) {
            System.out.println(solver.getLogPrefix() + "not needed.");
         }
      }

      return results;
   }

   @Override
   public String toString() {
      return "Deletion based minimization strategy";
   }
}
