package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;

public interface LearnedConstraintsDeletionStrategy extends Serializable {
   void init();

   ConflictTimer getTimer();

   void reduce(IVec<Constr> var1);

   void onClauseLearning(Constr var1);

   void onConflictAnalysis(Constr var1);

   void onPropagation(Constr var1, int var2);
}
