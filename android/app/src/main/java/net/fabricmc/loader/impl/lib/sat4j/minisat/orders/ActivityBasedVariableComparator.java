package net.fabricmc.loader.impl.lib.sat4j.minisat.orders;

public class ActivityBasedVariableComparator implements VariableComparator {
   private final double[] activity;

   public ActivityBasedVariableComparator(double[] activity) {
      this.activity = activity;
   }

   @Override
   public boolean preferredTo(int a, int b) {
      return this.activity[a] > this.activity[b];
   }

   @Override
   public String toString() {
      return "Activity-based variable heuristic";
   }
}
