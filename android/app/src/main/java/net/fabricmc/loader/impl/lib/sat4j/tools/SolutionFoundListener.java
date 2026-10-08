package net.fabricmc.loader.impl.lib.sat4j.tools;

public interface SolutionFoundListener {
   SolutionFoundListener VOID = new SolutionFoundListener() {
      @Override
      public void onSolutionFound(int[] model) {
      }

      @Override
      public void onUnsatTermination() {
      }
   };

   void onSolutionFound(int[] var1);

   void onUnsatTermination();
}
