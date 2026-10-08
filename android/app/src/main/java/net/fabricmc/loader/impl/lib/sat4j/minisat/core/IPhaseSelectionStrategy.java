package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;

public interface IPhaseSelectionStrategy extends Serializable {
   void updateVar(int var1);

   void init(int var1);

   void init(int var1, int var2);

   void assignLiteral(int var1);

   int select(int var1);

   void updateVarAtDecisionLevel(int var1);
}
