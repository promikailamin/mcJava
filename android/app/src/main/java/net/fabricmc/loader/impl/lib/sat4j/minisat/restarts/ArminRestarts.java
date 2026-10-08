package net.fabricmc.loader.impl.lib.sat4j.minisat.restarts;

import net.fabricmc.loader.impl.lib.sat4j.minisat.core.RestartStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.SearchParams;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.SolverStats;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;

public final class ArminRestarts implements RestartStrategy {
   private double inner;
   private double outer;
   private long conflicts;
   private SearchParams params;
   private long conflictcount = 0L;

   @Override
   public void init(SearchParams theParams, SolverStats stats) {
      this.params = theParams;
      this.inner = theParams.getInitConflictBound();
      this.outer = theParams.getInitConflictBound();
      this.conflicts = Math.round(this.inner);
   }

   @Override
   public void onRestart() {
      if (this.inner >= this.outer) {
         this.outer = this.outer * this.params.getConflictBoundIncFactor();
         this.inner = this.params.getInitConflictBound();
      } else {
         this.inner = this.inner * this.params.getConflictBoundIncFactor();
      }

      this.conflicts = Math.round(this.inner);
      this.conflictcount = 0L;
   }

   @Override
   public String toString() {
      return "Armin Biere (Picosat) restarts strategy";
   }

   @Override
   public boolean shouldRestart() {
      return this.conflictcount >= this.conflicts;
   }

   @Override
   public void onBackjumpToRootLevel() {
      this.conflictcount = 0L;
   }

   @Override
   public void reset() {
      this.conflictcount = 0L;
   }

   @Override
   public void newConflict() {
      this.conflictcount++;
   }

   @Override
   public void newLearnedClause(Constr learned, int trailLevel) {
   }
}
