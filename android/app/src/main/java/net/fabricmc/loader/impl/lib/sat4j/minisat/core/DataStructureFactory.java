package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public interface DataStructureFactory {
   Constr createClause(IVecInt var1) throws ContradictionException;

   Constr createUnregisteredClause(IVecInt var1);

   void learnConstraint(Constr var1);

   Constr createCardinalityConstraint(IVecInt var1, int var2) throws ContradictionException;

   void setUnitPropagationListener(UnitPropagationListener var1);

   void setLearner(Learner var1);

   void reset();

   ILits getVocabulary();
}
