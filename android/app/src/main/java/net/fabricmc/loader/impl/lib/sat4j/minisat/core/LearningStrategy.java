package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;

public interface LearningStrategy<D extends DataStructureFactory> extends Serializable {
   void init();

   void learns(Constr var1);

   void setSolver(Solver<D> var1);
}
