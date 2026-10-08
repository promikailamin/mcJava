package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

public interface ConflictTimer {
   void reset();

   void newConflict();
}
