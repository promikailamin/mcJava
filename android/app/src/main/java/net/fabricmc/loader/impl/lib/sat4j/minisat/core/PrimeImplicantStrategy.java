package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

public interface PrimeImplicantStrategy {
   int[] compute(Solver<? extends DataStructureFactory> var1);

   int[] getPrimeImplicantAsArrayWithHoles();
}
