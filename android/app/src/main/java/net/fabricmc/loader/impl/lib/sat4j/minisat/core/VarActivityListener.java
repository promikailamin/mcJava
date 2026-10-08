package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.Serializable;

public interface VarActivityListener extends Serializable {
   void varBumpActivity(int var1);
}
