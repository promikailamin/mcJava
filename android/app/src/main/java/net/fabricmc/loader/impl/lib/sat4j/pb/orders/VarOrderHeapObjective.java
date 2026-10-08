package net.fabricmc.loader.impl.lib.sat4j.pb.orders;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.IPhaseSelectionStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.orders.PhaseInLastLearnedClauseSelectionStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.orders.VarOrderHeap;
import net.fabricmc.loader.impl.lib.sat4j.pb.ObjectiveFunction;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public class VarOrderHeapObjective extends VarOrderHeap implements IOrderObjective {
   private ObjectiveFunction obj;

   public VarOrderHeapObjective() {
      this(new PhaseInLastLearnedClauseSelectionStrategy());
   }

   public VarOrderHeapObjective(IPhaseSelectionStrategy strategy) {
      super(strategy);
   }

   @Override
   public void setObjectiveFunction(ObjectiveFunction obj) {
      this.obj = obj;
   }

   @Override
   public void init() {
      super.init();
      if (this.obj != null) {
         IVecInt vars = this.obj.getVars();
         IVec<BigInteger> coefs = this.obj.getCoeffs();

         for (int i = 0; i < vars.size(); i++) {
            int dimacsLiteral = vars.get(i);
            if (this.lits.belongsToPool(Math.abs(dimacsLiteral))) {
               int p = this.lits.getFromPool(dimacsLiteral);
               BigInteger c = coefs.get(i);
               if (c.signum() < 0) {
                  p = LiteralsUtils.neg(p);
               }

               int var = LiteralsUtils.var(p);
               this.activity[var] = c.bitLength() < 64 ? c.abs().longValue() : 9.223372E18F;
               if (this.heap.inHeap(var)) {
                  this.heap.increase(var);
               } else {
                  this.heap.insert(var);
               }

               this.phaseStrategy.init(var, LiteralsUtils.neg(p));
            }
         }
      }
   }

   @Override
   public String toString() {
      return super.toString() + " taking into account the objective function";
   }
}
