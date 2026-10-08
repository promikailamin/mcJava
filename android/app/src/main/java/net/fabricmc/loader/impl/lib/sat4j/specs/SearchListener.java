package net.fabricmc.loader.impl.lib.sat4j.specs;

import java.io.Serializable;

public interface SearchListener<S extends ISolverService> extends Serializable, UnitClauseConsumer {
   void init(S var1);

   void assuming(int var1);

   void propagating(int var1);

   void enqueueing(int var1, IConstr var2);

   void backtracking(int var1);

   void adding(int var1);

   void learn(IConstr var1);

   void delete(IConstr var1);

   void conflictFound(IConstr var1, int var2, int var3);

   void conflictFound(int var1);

   void solutionFound(int[] var1, RandomAccessModel var2);

   void beginLoop();

   void start();

   void end(Lbool var1);

   void restarting();

   void backjump(int var1);

   void cleaning();
}
