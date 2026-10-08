package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

final class LBDConflictTimer extends ConflictTimerAdapter {
   private int nbconflict = 0;
   private int nextbound = 5000;

   LBDConflictTimer(Solver<? extends DataStructureFactory> solver, int bound) {
      super(solver, bound);
   }

   @Override
   public void run() {
      this.nbconflict = this.nbconflict + this.bound();
      if (this.nbconflict >= this.nextbound) {
         this.nextbound += 1000;
         this.nbconflict = 0;
         this.getSolver().setNeedToReduceDB(true);
      }
   }

   @Override
   public void reset() {
      super.reset();
      this.nextbound = 5000;
      if (this.nbconflict >= this.nextbound) {
         this.nbconflict = 0;
         this.getSolver().setNeedToReduceDB(true);
      }
   }

   @Override
   public String toString() {
      return "check every " + this.bound() + " if the learned constraints reach increasing bounds: " + 5000 + " step " + 1000;
   }
}
