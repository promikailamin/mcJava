package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.PrintWriter;

public interface IOrder {
   void setLits(ILits var1);

   int select();

   void undo(int var1);

   void updateVar(int var1);

   void init();

   void printStat(PrintWriter var1, String var2);

   void setVarDecay(double var1);

   void varDecayActivity();

   void assignLiteral(int var1);

   void updateVarAtDecisionLevel(int var1);
}
