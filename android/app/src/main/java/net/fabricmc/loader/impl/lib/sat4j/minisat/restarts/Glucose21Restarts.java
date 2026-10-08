package net.fabricmc.loader.impl.lib.sat4j.minisat.restarts;

import net.fabricmc.loader.impl.lib.sat4j.minisat.core.CircularBuffer;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.RestartStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.SearchParams;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.SolverStats;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;

public class Glucose21Restarts implements RestartStrategy {
   private final CircularBuffer bufferLBD = new CircularBuffer(50);
   private final CircularBuffer bufferTrail = new CircularBuffer(5000);
   private long sumOfAllLBD = 0L;
   private SolverStats stats;

   @Override
   public void reset() {
      this.sumOfAllLBD = 0L;
      this.bufferLBD.clear();
      this.bufferTrail.clear();
   }

   @Override
   public void newConflict() {
   }

   @Override
   public void newLearnedClause(Constr learned, int trailLevel) {
      int lbd = (int)learned.getActivity();
      this.bufferLBD.push(lbd);
      this.sumOfAllLBD += lbd;
      this.bufferTrail.push(trailLevel);
      if (this.stats.getConflicts() > 10000L && this.bufferTrail.isFull() && trailLevel * 5L > 7L * this.bufferTrail.average()) {
         this.bufferLBD.clear();
      }
   }

   @Override
   public void init(SearchParams params, SolverStats stats) {
      this.stats = stats;
      this.reset();
   }

   @Override
   public boolean shouldRestart() {
      return this.bufferLBD.isFull() && this.bufferLBD.average() * this.stats.getConflicts() * 4L > this.sumOfAllLBD * 5L;
   }

   @Override
   public void onRestart() {
      this.bufferLBD.clear();
   }

   @Override
   public void onBackjumpToRootLevel() {
   }

   @Override
   public String toString() {
      return "Glucose 2.1 dynamic restart strategy";
   }
}
