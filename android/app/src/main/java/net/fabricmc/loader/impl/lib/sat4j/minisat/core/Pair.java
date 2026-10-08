package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;

public final class Pair implements Serializable {
   private int backtrackLevel;
   private Constr reason;

   public int getBacktrackLevel() {
      return this.backtrackLevel;
   }

   public void setBacktrackLevel(int backtrackLevel) {
      this.backtrackLevel = backtrackLevel;
   }

   public Constr getReason() {
      return this.reason;
   }

   public void setReason(Constr reason) {
      this.reason = reason;
   }
}
