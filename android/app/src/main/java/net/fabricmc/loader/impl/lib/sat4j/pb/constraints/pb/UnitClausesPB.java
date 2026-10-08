package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.UnitClauses;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public class UnitClausesPB extends UnitClauses implements PBConstr {
   public UnitClausesPB(IVecInt values) {
      super(values);
   }
}
