package net.fabricmc.loader.impl.lib.sat4j.pb.core;

import java.io.PrintWriter;
import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.SolverStats;

public class PBSolverStats extends SolverStats {
   private long numberOfReductions;
   private long numberOfReductionsByPower2;
   private long numberOfRightShiftsForCoeffs;
   private long numberOfReductionsByGCD;
   private long numberOfLearnedConstraintsReduced;
   private long numberOfResolution;
   private long numberOfCP;
   private long numberOfRoundingOperations;
   private long numberOfEasyRoundingOperations;
   private long numberOfEndingSkipping;
   private long numberOfInternalSkipping;
   private long numberOfDerivationSteps;
   private long numberOfRemainingUnassigned;
   private long numberOfRemainingAssigned;
   private long falsifiedLiteralsRemovedFromConflict;
   private long falsifiedLiteralsRemovedFromReason;
   private long timeForArtithmeticOperations;
   private BigInteger minRemoved;
   private BigInteger maxRemoved;
   private int nbRemoved;

   @Override
   public void reset() {
      super.reset();
      this.numberOfReductions = 0L;
      this.numberOfLearnedConstraintsReduced = 0L;
      this.numberOfResolution = 0L;
      this.numberOfCP = 0L;
      this.numberOfRoundingOperations = 0L;
      this.numberOfReductionsByPower2 = 0L;
      this.numberOfRightShiftsForCoeffs = 0L;
      this.numberOfReductionsByGCD = 0L;
      this.numberOfEndingSkipping = 0L;
      this.numberOfInternalSkipping = 0L;
      this.numberOfDerivationSteps = 0L;
      this.numberOfRemainingUnassigned = 0L;
      this.numberOfRemainingAssigned = 0L;
   }

   @Override
   public void printStat(PrintWriter out, String prefix) {
      super.printStat(out, prefix);
      out.println(prefix + "number of reductions to clauses (during analyze)\t: " + this.getNumberOfReductions());
      out.println(prefix + "number of learned constraints concerned by reduction\t: " + this.numberOfLearnedConstraintsReduced);
      out.println(prefix + "number of learning phase by resolution\t: " + this.numberOfResolution);
      out.println(prefix + "number of learning phase by cutting planes\t: " + this.numberOfCP);
      out.println(prefix + "number of rounding to 1 operations\t: " + this.numberOfRoundingOperations);
      out.println(prefix + "number of easy rounding to 1 operations (no literal elimination needed)\t: " + this.numberOfEasyRoundingOperations);
      out.println(prefix + "number of reductions of the coefficients by power 2 \t: " + this.getNumberOfReductionsByPower2());
      out.println(prefix + "number of right shift for reduction by power 2 \t: " + this.numberOfRightShiftsForCoeffs);
      out.println(prefix + "number of reductions of the coefficients by GCD over coefficients \t: " + this.numberOfReductionsByGCD);
      out.println(prefix + "number of ending skipping \t: " + this.numberOfEndingSkipping);
      out.println(prefix + "number of internal skipping \t: " + this.numberOfInternalSkipping);
      out.println(prefix + "number of derivation steps \t: " + this.getNumberOfDerivationSteps());
      out.println(prefix + "number of skipped derivation steps \t: " + (this.numberOfInternalSkipping + this.numberOfEndingSkipping));
      out.println(prefix + "number of remaining unassigned \t: " + this.numberOfRemainingUnassigned);
      out.println(prefix + "number of remaining assigned \t: " + this.numberOfRemainingAssigned);
      out.println(prefix + "number of falsified literals weakened from reason\t: " + this.falsifiedLiteralsRemovedFromReason);
      out.println(prefix + "number of falsified literals weakened from conflict\t: " + this.falsifiedLiteralsRemovedFromConflict);
      out.println(prefix + "time for arithmetic operations\t: " + this.timeForArtithmeticOperations);
      out.println(prefix + "minimum degree of deleted constraints\t: " + this.minRemoved);
      out.println(prefix + "maximum degree of deleted constraints\t: " + this.maxRemoved);
      out.println(prefix + "number of deleted constraints\t: " + this.nbRemoved);
   }

   public long getNumberOfReductions() {
      return this.numberOfReductions;
   }

   public long getNumberOfReductionsByPower2() {
      return this.numberOfReductionsByPower2;
   }

   public long getNumberOfDerivationSteps() {
      return this.numberOfDerivationSteps;
   }
}
