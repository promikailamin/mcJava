package net.fabricmc.loader.impl.lib.sat4j.pb;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IConstr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.tools.SolverDecorator;

public class PBSolverDecorator extends SolverDecorator<IPBSolver> implements IPBSolver {
   public PBSolverDecorator(IPBSolver solver) {
      super(solver);
   }

   @Override
   public IConstr addPseudoBoolean(IVecInt lits, IVec<BigInteger> coeffs, boolean moreThan, BigInteger d) throws ContradictionException {
      return this.decorated().addPseudoBoolean(lits, coeffs, moreThan, d);
   }

   @Override
   public void setObjectiveFunction(ObjectiveFunction obj) {
      this.decorated().setObjectiveFunction(obj);
   }

   @Override
   public ObjectiveFunction getObjectiveFunction() {
      return this.decorated().getObjectiveFunction();
   }

   @Override
   public IConstr addAtMost(IVecInt literals, IVecInt coeffs, int degree) throws ContradictionException {
      return this.decorated().addAtMost(literals, coeffs, degree);
   }

   @Override
   public IConstr addAtLeast(IVecInt literals, IVecInt coeffs, int degree) throws ContradictionException {
      return this.decorated().addAtLeast(literals, coeffs, degree);
   }
}
