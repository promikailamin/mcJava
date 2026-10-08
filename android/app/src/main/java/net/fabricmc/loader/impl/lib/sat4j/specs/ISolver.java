package net.fabricmc.loader.impl.lib.sat4j.specs;

import java.io.PrintWriter;
import java.io.Serializable;

public interface ISolver extends Serializable, IProblem {
   int nextFreeVarId(boolean var1);

   IConstr addClause(IVecInt var1) throws ContradictionException;

   IVecInt createBlockingClauseForCurrentModel();

   boolean removeConstr(IConstr var1);

   boolean removeSubsumedConstr(IConstr var1);

   IConstr addAtMost(IVecInt var1, int var2) throws ContradictionException;

   void setTimeout(int var1);

   void expireTimeout();

   void reset();

   @Deprecated
   void printStat(PrintWriter var1, String var2);

   boolean isVerbose();

   String getLogPrefix();

   IVecInt unsatExplanation();

   int[] modelWithInternalVariables();
}
