package net.fabricmc.loader.impl.lib.sat4j.minisat.orders;

import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.IPhaseSelectionStrategy;

abstract class AbstractPhaserecordingSelectionStrategy implements IPhaseSelectionStrategy {
   protected int[] phase;

   @Override
   public void init(int nlength) {
      if (this.phase == null || this.phase.length < nlength) {
         this.phase = new int[nlength];
      }

      for (int i = 1; i < nlength; i++) {
         this.phase[i] = LiteralsUtils.negLit(i);
      }
   }

   @Override
   public void init(int var, int p) {
      this.phase[var] = p;
   }

   @Override
   public int select(int var) {
      return this.phase[var];
   }
}
