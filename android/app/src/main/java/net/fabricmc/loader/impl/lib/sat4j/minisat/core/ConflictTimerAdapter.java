package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;

public abstract class ConflictTimerAdapter implements Serializable, ConflictTimer {
   private int counter;
   private final int bound;
   private final Solver<? extends DataStructureFactory> solver;

   public ConflictTimerAdapter(Solver<? extends DataStructureFactory> solver, int bound) {
      this.bound = bound;
      this.counter = 0;
      this.solver = solver;
   }

   @Override
   public void reset() {
      this.counter = 0;
   }

   @Override
   public void newConflict() {
      this.counter++;
      if (this.counter == this.bound) {
         this.run();
         this.counter = 0;
      }
   }

   public abstract void run();

   public Solver<? extends DataStructureFactory> getSolver() {
      return this.solver;
   }

   public int bound() {
      return this.bound;
   }
}
