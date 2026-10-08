package net.fabricmc.loader.impl.lib.sat4j.pb.core;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.DataStructureFactory;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public interface PBDataStructureFactory extends DataStructureFactory {
   Constr createPseudoBooleanConstraint(IVecInt var1, IVec<BigInteger> var2, boolean var3, BigInteger var4) throws ContradictionException;
}
