package net.fabricmc.loader.impl.lib.sat4j.tools;

import java.io.PrintWriter;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IConstr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ISolver;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.TimeoutException;

public abstract class SolverDecorator<T extends ISolver> implements ISolver {
   private T solver;

   @Override
   public boolean isSatisfiable(IVecInt assumps, boolean global) throws TimeoutException {
      return this.solver.isSatisfiable(assumps, global);
   }

   @Override
   public boolean model(int var) {
      return this.solver.model(var);
   }

   @Override
   public String toString() {
      return this.solver.toString();
   }

   @Override
   public void printStat(PrintWriter out, String prefix) {
      this.solver.printStat(out, prefix);
   }

   public SolverDecorator(T solver) {
      this.solver = solver;
   }

   @Override
   public IConstr addClause(IVecInt literals) throws ContradictionException {
      return this.solver.addClause(literals);
   }

   @Override
   public IVecInt createBlockingClauseForCurrentModel() {
      return this.solver.createBlockingClauseForCurrentModel();
   }

   @Override
   public IConstr addAtMost(IVecInt literals, int degree) throws ContradictionException {
      return this.solver.addAtMost(literals, degree);
   }

   @Override
   public int[] model() {
      return this.solver.model();
   }

   @Override
   public boolean isSatisfiable() throws TimeoutException {
      return this.solver.isSatisfiable();
   }

   @Override
   public boolean isSatisfiable(IVecInt assumps) throws TimeoutException {
      return this.solver.isSatisfiable(assumps);
   }

   @Override
   public void setTimeout(int t) {
      this.solver.setTimeout(t);
   }

   @Override
   public void expireTimeout() {
      this.solver.expireTimeout();
   }

   @Override
   public int nVars() {
      return this.solver.nVars();
   }

   @Override
   public void reset() {
      this.solver.reset();
   }

   public T decorated() {
      return this.solver;
   }

   @Override
   public boolean removeConstr(IConstr c) {
      return this.solver.removeConstr(c);
   }

   @Override
   public int nextFreeVarId(boolean reserve) {
      return this.solver.nextFreeVarId(reserve);
   }

   @Override
   public boolean removeSubsumedConstr(IConstr c) {
      return this.solver.removeSubsumedConstr(c);
   }

   @Override
   public boolean isVerbose() {
      return this.solver.isVerbose();
   }

   @Override
   public String getLogPrefix() {
      return this.solver.getLogPrefix();
   }

   @Override
   public IVecInt unsatExplanation() {
      return this.solver.unsatExplanation();
   }

   @Override
   public int[] primeImplicant() {
      return this.solver.primeImplicant();
   }

   @Override
   public int[] modelWithInternalVariables() {
      return this.solver.modelWithInternalVariables();
   }

   @Override
   public boolean primeImplicant(int p) {
      return this.solver.primeImplicant(p);
   }
}
