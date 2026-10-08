package net.fabricmc.loader.impl.lib.sat4j.pb.core;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ConflictTimer;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ConflictTimerAdapter;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.DataStructureFactory;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.IOrder;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.LearnedConstraintsDeletionStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.LearningStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.PrimeImplicantStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.RestartStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Solver;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.WatcherBasedPrimeImplicantStrategy;
import net.fabricmc.loader.impl.lib.sat4j.pb.IPBSolverService;
import net.fabricmc.loader.impl.lib.sat4j.pb.ObjectiveFunction;
import net.fabricmc.loader.impl.lib.sat4j.pb.ObjectiveFunctionComparator;
import net.fabricmc.loader.impl.lib.sat4j.pb.orders.IOrderObjective;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IConstr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.IteratorInt;

public abstract class PBSolver extends Solver<PBDataStructureFactory> implements IPBSolverService, IPBCDCLSolver<PBDataStructureFactory> {
   private ObjectiveFunction objf;
   protected PBSolverStats pbStats;
   public final LearnedConstraintsDeletionStrategy objectiveFunctionBased = new LearnedConstraintsDeletionStrategy() {
      private boolean[] inObjectiveFunction;
      private final ConflictTimer clauseManagement = new ConflictTimerAdapter(PBSolver.this, 1000) {
         private int nbconflict = 0;
         private int nextbound = 5000;

         @Override
         public void run() {
            this.nbconflict = this.nbconflict + this.bound();
            if (this.nbconflict >= this.nextbound) {
               this.nextbound += 1000;
               this.nbconflict = 0;
               PBSolver.this.setNeedToReduceDB(true);
            }
         }

         @Override
         public void reset() {
            super.reset();
            this.nextbound = 5000;
            if (this.nbconflict >= this.nextbound) {
               this.nbconflict = 0;
               PBSolver.this.setNeedToReduceDB(true);
            }
         }
      };

      @Override
      public void reduce(IVec<Constr> learnedConstrs) {
         int j = 0;

         for (int i = 0; i < learnedConstrs.size(); i++) {
            Constr c = learnedConstrs.get(i);
            if (!c.locked() && !(c.getActivity() <= 2.0)) {
               c.remove(PBSolver.this);
            } else {
               learnedConstrs.set(j++, learnedConstrs.get(i));
            }
         }

         if (PBSolver.this.isVerbose()) {
            System.out
               .println(
                  PBSolver.this.getLogPrefix()
                     + "cleaning "
                     + (learnedConstrs.size() - j)
                     + " clauses out of "
                     + learnedConstrs.size()
                     + "/"
                     + PBSolver.this.pbStats.getConflicts()
               );
            System.out.flush();
         }

         learnedConstrs.shrinkTo(j);
      }

      @Override
      public ConflictTimer getTimer() {
         return this.clauseManagement;
      }

      @Override
      public String toString() {
         return "Objective function driven learned constraints deletion strategy";
      }

      @Override
      public void init() {
         this.inObjectiveFunction = new boolean[PBSolver.this.nVars() + 1];
         if (PBSolver.this.objf == null) {
            throw new IllegalStateException("The strategy does not make sense if there is no objective function");
         }

         IteratorInt it = PBSolver.this.objf.getVars().iterator();

         while (it.hasNext()) {
            this.inObjectiveFunction[Math.abs(it.next())] = true;
         }

         this.clauseManagement.reset();
      }

      @Override
      public void onClauseLearning(Constr constr) {
         boolean fullObj = true;

         for (int i = 0; i < constr.size(); i++) {
            fullObj = fullObj && this.inObjectiveFunction[LiteralsUtils.var(constr.get(i))];
         }

         if (fullObj) {
            constr.incActivity(1.0);
         } else {
            constr.incActivity(constr.size());
         }
      }

      @Override
      public void onConflictAnalysis(Constr reason) {
      }

      @Override
      public void onPropagation(Constr from, int propagated) {
      }
   };

   public PBSolver(LearningStrategy<PBDataStructureFactory> learner, PBDataStructureFactory dsf, IOrder order, RestartStrategy restarter) {
      super(learner, dsf, order, restarter);
      this.pbStats = new PBSolverStats();
      this.initStats(this.pbStats);
   }

   @Override
   public IConstr addPseudoBoolean(IVecInt literals, IVec<BigInteger> coeffs, boolean moreThan, BigInteger degree) throws ContradictionException {
      IVecInt vlits = this.dimacs2internal(literals);
      assert vlits.size() == literals.size();
      assert literals.size() == coeffs.size();
      return this.addConstr(this.dsfactory.createPseudoBooleanConstraint(vlits, coeffs, moreThan, degree));
   }

   @Override
   public void setObjectiveFunction(ObjectiveFunction obj) {
      this.objf = obj;
      IOrder order = this.getOrder();
      if (order instanceof IOrderObjective) {
         ((IOrderObjective)order).setObjectiveFunction(obj);
      }

      if (obj != null) {
         this.dimacs2internal(obj.getVars());
      }
   }

   @Override
   public ObjectiveFunction getObjectiveFunction() {
      return this.objf;
   }

   @Override
   public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree) throws ContradictionException {
      IVec<BigInteger> bcoeffs = new Vec<>(coeffs.size());

      for (int i = 0; i < coeffs.size(); i++) {
         bcoeffs.push(BigInteger.valueOf(coeffs.get(i)));
      }

      return this.addAtMost(literals, bcoeffs, BigInteger.valueOf(degree));
   }

   public IConstr addAtMost(IVecInt literals, IVec<BigInteger> coeffs, BigInteger degree) throws ContradictionException {
      IVecInt vlits = this.dimacs2internal(literals);
      assert vlits.size() == literals.size();
      assert literals.size() == coeffs.size();
      return this.addConstr(this.dsfactory.createPseudoBooleanConstraint(vlits, coeffs, false, degree));
   }

   @Override
   public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree) throws ContradictionException {
      IVec<BigInteger> bcoeffs = new Vec<>(coeffs.size());

      for (int i = 0; i < coeffs.size(); i++) {
         bcoeffs.push(BigInteger.valueOf(coeffs.get(i)));
      }

      return this.addAtLeast(literals, bcoeffs, BigInteger.valueOf(degree));
   }

   public IConstr addAtLeast(IVecInt literals, IVec<BigInteger> coeffs, BigInteger degree) throws ContradictionException {
      IVecInt vlits = this.dimacs2internal(literals);
      assert vlits.size() == literals.size();
      assert literals.size() == coeffs.size();
      return this.addConstr(this.dsfactory.createPseudoBooleanConstraint(vlits, coeffs, true, degree));
   }

   @Override
   public int[] primeImplicant() {
      String primeApproach = System.getProperty("prime");
      PrimeImplicantStrategy strategy;
      if ("OBJECTIVE".equals(primeApproach)) {
         strategy = new WatcherBasedPrimeImplicantStrategy(new ObjectiveFunctionComparator(this.objf));
      } else {
         strategy = new WatcherBasedPrimeImplicantStrategy();
      }

      int[] implicant = strategy.compute(this);
      this.prime = strategy.getPrimeImplicantAsArrayWithHoles();
      return implicant;
   }
}
