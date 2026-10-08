package net.fabricmc.loader.impl.lib.sat4j.pb.constraints;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public interface IPBConstructor {
   Constr constructPB(UnitPropagationListener var1, ILits var2, int[] var3, BigInteger[] var4, BigInteger var5, BigInteger var6) throws ContradictionException;
}
