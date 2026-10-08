package net.fabricmc.loader.impl.lib.sat4j.pb;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IConstr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IOptimizationProblem;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.TimeoutException;

public class PseudoOptDecorator extends PBSolverDecorator implements IOptimizationProblem {
   protected BigInteger objectiveValue;
   private int[] prevmodel;
   private int[] prevmodelwithadditionalvars;
   private boolean[] prevfullmodel;
   private IVecInt prevModelBlockingClause;
   private IConstr previousPBConstr;
   private boolean isSolutionOptimal;
   private final boolean nonOptimalMeansSatisfiable;
   private final boolean useAnImplicantForEvaluation;
   private int solverTimeout = Integer.MAX_VALUE;
   private int optimizationTimeout = -1;

   public PseudoOptDecorator(IPBSolver solver) {
      this(solver, true);
   }

   public PseudoOptDecorator(IPBSolver solver, boolean nonOptimalMeansSatisfiable) {
      this(solver, nonOptimalMeansSatisfiable, false);
   }

   public PseudoOptDecorator(IPBSolver solver, boolean nonOptimalMeansSatisfiable, boolean useAnImplicantForEvaluation) {
      super(solver);
      this.nonOptimalMeansSatisfiable = nonOptimalMeansSatisfiable;
      this.useAnImplicantForEvaluation = useAnImplicantForEvaluation;
   }

   @Override
   public boolean isSatisfiable() throws TimeoutException {
      return this.isSatisfiable(VecInt.EMPTY);
   }

   @Override
   public boolean isSatisfiable(IVecInt assumps, boolean global) throws TimeoutException {
      boolean result = super.isSatisfiable(assumps, global);
      if (result) {
         this.prevmodel = super.model();
         this.prevModelBlockingClause = super.createBlockingClauseForCurrentModel();
         this.prevmodelwithadditionalvars = super.modelWithInternalVariables();
         this.prevfullmodel = new boolean[this.nVars()];

         for (int i = 0; i < this.nVars(); i++) {
            this.prevfullmodel[i] = this.decorated().model(i + 1);
         }

         if (this.optimizationTimeout > 0) {
            super.expireTimeout();
            super.setTimeout(this.optimizationTimeout);
         }
      } else {
         if (this.previousPBConstr != null) {
            this.decorated().removeConstr(this.previousPBConstr);
            this.previousPBConstr = null;
         }

         super.setTimeout(this.solverTimeout);
      }

      return result;
   }

   @Override
   public boolean isSatisfiable(IVecInt assumps) throws TimeoutException {
      return this.isSatisfiable(assumps, false);
   }

   @Override
   public void setObjectiveFunction(ObjectiveFunction objf) {
      this.decorated().setObjectiveFunction(objf);
   }

   @Override
   public boolean admitABetterSolution(IVecInt assumps) throws TimeoutException {
      try {
         this.isSolutionOptimal = false;
         boolean result = super.isSatisfiable(assumps, true);
         if (result) {
            if (this.useAnImplicantForEvaluation) {
               this.prevmodel = this.modelWithAdaptedNonPrimeLiterals();
            } else {
               this.prevmodel = super.model();
            }

            this.prevModelBlockingClause = super.createBlockingClauseForCurrentModel();
            this.prevmodelwithadditionalvars = super.modelWithInternalVariables();
            this.prevfullmodel = new boolean[this.nVars()];

            for (int i = 0; i < this.nVars(); i++) {
               this.prevfullmodel[i] = this.decorated().model(i + 1);
            }

            if (this.decorated().getObjectiveFunction() != null) {
               this.calculateObjective();
            }

            if (this.optimizationTimeout > 0) {
               super.expireTimeout();
               super.setTimeout(this.optimizationTimeout);
            }
         } else {
            this.isSolutionOptimal = true;
            if (this.previousPBConstr != null) {
               this.decorated().removeConstr(this.previousPBConstr);
               this.previousPBConstr = null;
            }
         }

         return result;
      } catch (TimeoutException te) {
         if (this.previousPBConstr != null) {
            this.decorated().removeConstr(this.previousPBConstr);
            this.previousPBConstr = null;
         }

         throw te;
      }
   }

   private int[] modelWithAdaptedNonPrimeLiterals() {
      int[] completeModel = new int[this.nVars()];

      for (int i = 0; i < this.nVars(); i++) {
         int var = i + 1;
         completeModel[i] = super.model(var) ? var : -var;
      }

      this.primeImplicant();
      ObjectiveFunction obj = this.getObjectiveFunction();

      for (int i = 0; i < obj.getVars().size(); i++) {
         int d = obj.getVars().get(i);
         BigInteger coeff = obj.getCoeffs().get(i);
         if (d <= this.nVars() && !this.primeImplicant(d) && !this.primeImplicant(-d)) {
            assert Math.abs(completeModel[Math.abs(d) - 1]) == d;
            if (coeff.signum() * d < 0) {
               completeModel[Math.abs(d) - 1] = Math.abs(d);
            } else {
               completeModel[Math.abs(d) - 1] = -Math.abs(d);
            }
         }
      }

      return completeModel;
   }

   @Override
   public boolean hasNoObjectiveFunction() {
      return this.decorated().getObjectiveFunction() == null;
   }

   public Number calculateObjective() {
      if (this.decorated().getObjectiveFunction() == null) {
         throw new UnsupportedOperationException("The problem does not contain an objective function");
      }

      if (this.useAnImplicantForEvaluation) {
         this.objectiveValue = this.decorated().getObjectiveFunction().calculateDegreeImplicant(this.decorated());
      } else {
         this.objectiveValue = this.decorated().getObjectiveFunction().calculateDegree(this.decorated());
      }

      return this.getObjectiveValue();
   }

   @Override
   public void discardCurrentSolution() throws ContradictionException {
      if (this.previousPBConstr != null) {
         super.removeSubsumedConstr(this.previousPBConstr);
      }

      if (this.decorated().getObjectiveFunction() != null && this.objectiveValue != null) {
         this.previousPBConstr = super.addPseudoBoolean(
            this.decorated().getObjectiveFunction().getVars(),
            this.decorated().getObjectiveFunction().getCoeffs(),
            false,
            this.objectiveValue.subtract(BigInteger.ONE)
         );
      }
   }

   @Override
   public void reset() {
      this.previousPBConstr = null;
      super.reset();
   }

   @Override
   public int[] model() {
      if (this.prevmodel.length <= this.nVars()) {
         return this.prevmodel;
      } else {
         throw new RuntimeException("New variables have been added since the last model found");
      }
   }

   @Override
   public boolean model(int var) {
      return this.prevfullmodel[var - 1];
   }

   @Override
   public Number getObjectiveValue() {
      BigInteger offset = this.decorated().getObjectiveFunction().getCorrectionOffset();
      BigInteger factor = this.decorated().getObjectiveFunction().getCorrectionFactor();
      return this.objectiveValue.multiply(factor).add(offset);
   }

   @Override
   public int[] modelWithInternalVariables() {
      return this.prevmodelwithadditionalvars;
   }

   @Override
   public void setTimeout(int t) {
      this.solverTimeout = t;
      super.setTimeout(t);
   }
}
