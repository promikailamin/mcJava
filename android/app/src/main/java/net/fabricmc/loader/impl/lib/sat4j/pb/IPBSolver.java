package net.fabricmc.loader.impl.lib.sat4j.pb;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IConstr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ISolver;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public interface IPBSolver extends ISolver {
   IConstr addPseudoBoolean(IVecInt var1, IVec<BigInteger> var2, boolean var3, BigInteger var4) throws ContradictionException;

   IConstr addAtMost(IVecInt var1, IVecInt var2, int var3) throws ContradictionException;

   IConstr addAtLeast(IVecInt var1, IVecInt var2, int var3) throws ContradictionException;

   void setObjectiveFunction(ObjectiveFunction var1);

   ObjectiveFunction getObjectiveFunction();
}
