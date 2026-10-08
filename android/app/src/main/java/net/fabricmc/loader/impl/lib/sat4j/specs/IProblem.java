package net.fabricmc.loader.impl.lib.sat4j.specs;

public interface IProblem extends RandomAccessModel {
   int[] model();

   int[] primeImplicant();

   boolean primeImplicant(int var1);

   boolean isSatisfiable() throws TimeoutException;

   boolean isSatisfiable(IVecInt var1, boolean var2) throws TimeoutException;

   boolean isSatisfiable(IVecInt var1) throws TimeoutException;

   int nVars();
}
