package net.fabricmc.loader.impl.lib.sat4j.tools.xplain;

import java.io.Serializable;
import java.util.Map;
import net.fabricmc.loader.impl.lib.sat4j.specs.ISolver;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.TimeoutException;

public interface MinimizationStrategy extends Serializable {
   IVecInt explain(ISolver var1, Map<Integer, ?> var2, IVecInt var3) throws TimeoutException;
}
