package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;

public class ConflictTimerContainer implements Serializable, ConflictTimer {
   private final IVec<ConflictTimer> timers = new Vec<>();

   ConflictTimerContainer add(ConflictTimer timer) {
      this.timers.push(timer);
      return this;
   }

   ConflictTimerContainer remove(ConflictTimer timer) {
      this.timers.remove(timer);
      return this;
   }

   @Override
   public void reset() {
      for (int i = 0; i < this.timers.size(); i++) {
         this.timers.get(i).reset();
      }
   }

   @Override
   public void newConflict() {
      for (int i = 0; i < this.timers.size(); i++) {
         this.timers.get(i).newConflict();
      }
   }
}
