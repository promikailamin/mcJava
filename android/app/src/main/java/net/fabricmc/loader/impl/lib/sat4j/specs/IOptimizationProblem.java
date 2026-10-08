package net.fabricmc.loader.impl.lib.sat4j.specs;

public interface IOptimizationProblem extends IProblem {
   boolean admitABetterSolution(IVecInt var1) throws TimeoutException;

   boolean hasNoObjectiveFunction();

   Number getObjectiveValue();

   void discardCurrentSolution() throws ContradictionException;
}
