package net.fabricmc.loader.impl.lib.sat4j.minisat.orders;

import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;

public final class RSATPhaseSelectionStrategy extends AbstractPhaserecordingSelectionStrategy {
   @Override
   public void assignLiteral(int p) {
      this.phase[LiteralsUtils.var(p)] = p;
   }

   @Override
   public String toString() {
      return "lightweight component caching from RSAT";
   }

   @Override
   public void updateVar(int p) {
   }

   @Override
   public void updateVarAtDecisionLevel(int p) {
   }
}
