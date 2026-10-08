package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public interface ISimplifier extends Serializable {
   void simplify(IVecInt var1);
}
