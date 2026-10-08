package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;

public interface RestartStrategy extends Serializable, ConflictTimer {
   void init(SearchParams var1, SolverStats var2);

   boolean shouldRestart();

   void onRestart();

   void onBackjumpToRootLevel();

   void newLearnedClause(Constr var1, int var2);
}
