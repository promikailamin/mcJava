package net.fabricmc.loader.impl.lib.sat4j.pb.constraints;

import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.LearntBinaryClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.LearntWLClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.OriginalBinaryClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.OriginalWLClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.UnitClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public class UnitBinaryWLClauseConstructor implements IClauseConstructor {
   @Override
   public Constr constructClause(UnitPropagationListener solver, ILits voc, IVecInt v) {
      if (v == null) {
         return Constr.TAUTOLOGY;
      } else if (v.size() == 1) {
         return new UnitClause(v.last());
      } else {
         return v.size() == 2 ? OriginalBinaryClause.brandNewClause(solver, voc, v) : OriginalWLClause.brandNewClause(solver, voc, v);
      }
   }

   @Override
   public Constr constructLearntClause(ILits voc, IVecInt literals) {
      if (literals.size() == 1) {
         return new UnitClause(literals.last(), true);
      } else {
         return literals.size() == 2 ? new LearntBinaryClause(literals, voc) : new LearntWLClause(literals, voc);
      }
   }
}
