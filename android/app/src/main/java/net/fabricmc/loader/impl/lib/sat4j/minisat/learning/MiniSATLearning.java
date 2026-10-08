package net.fabricmc.loader.impl.lib.sat4j.minisat.learning;

import net.fabricmc.loader.impl.lib.sat4j.minisat.core.DataStructureFactory;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Solver;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;

public final class MiniSATLearning<D extends DataStructureFactory> extends AbstractLearning<D> {
   private DataStructureFactory dsf;

   public void setDataStructureFactory(DataStructureFactory dsf) {
      this.dsf = dsf;
   }

   @Override
   public void setSolver(Solver<D> s) {
      super.setSolver(s);
      if (s != null) {
         this.dsf = s.getDSFactory();
      }
   }

   @Override
   public void learns(Constr constr) {
      this.claBumpActivity(constr);
      this.dsf.learnConstraint(constr);
   }

   @Override
   public String toString() {
      return "Learn all clauses as in MiniSAT";
   }
}
