package net.fabricmc.loader.impl.lib.sat4j.specs;

public interface IConstr {
   boolean learnt();

   int size();

   int get(int var1);

   double getActivity();

   boolean canBePropagatedMultipleTimes();
}
