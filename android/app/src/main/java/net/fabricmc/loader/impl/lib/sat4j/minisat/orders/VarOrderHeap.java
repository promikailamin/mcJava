package net.fabricmc.loader.impl.lib.sat4j.minisat.orders;

import java.io.PrintWriter;
import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Heap;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.IOrder;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.IPhaseSelectionStrategy;

public class VarOrderHeap implements Serializable, IOrder {
   protected double[] activity = new double[1];
   private double varDecay = 1.0;
   private double varInc = 1.0;
   protected ILits lits;
   private long nullchoice = 0L;
   protected Heap heap;
   protected IPhaseSelectionStrategy phaseStrategy;

   public VarOrderHeap() {
      this(new PhaseInLastLearnedClauseSelectionStrategy());
   }

   public VarOrderHeap(IPhaseSelectionStrategy strategy) {
      this.phaseStrategy = strategy;
   }

   @Override
   public void setLits(ILits lits) {
      this.lits = lits;
   }

   @Override
   public int select() {
      while (!this.heap.empty()) {
         int var = this.heap.getmin();
         int next = this.phaseStrategy.select(var);
         if (this.lits.isUnassigned(next)) {
            if (this.activity[var] < 1.0E-4) {
               this.nullchoice++;
            }

            return next;
         }
      }

      return -1;
   }

   @Override
   public void setVarDecay(double d) {
      this.varDecay = d;
   }

   @Override
   public void undo(int x) {
      if (!this.heap.inHeap(x)) {
         this.heap.insert(x);
      }
   }

   @Override
   public void updateVar(int p) {
      this.updateVar(p, 1.0);
   }

   public void updateVar(int p, double value) {
      int var = LiteralsUtils.var(p);
      this.updateActivity(var, value);
      this.phaseStrategy.updateVar(p);
      if (this.heap.inHeap(var)) {
         this.heap.increase(var);
      }
   }

   protected void updateActivity(int var, double inc) {
      if ((this.activity[var] = this.activity[var] + inc * this.varInc) > 1.0E100) {
         this.varRescaleActivity();
      }
   }

   @Override
   public void varDecayActivity() {
      this.varInc = this.varInc * this.varDecay;
   }

   private void varRescaleActivity() {
      for (int i = 1; i < this.activity.length; i++) {
         this.activity[i] = this.activity[i] * 1.0E-100;
      }

      this.varInc *= 1.0E-100;
   }

   protected Heap createHeap(double[] activity) {
      return new Heap(new ActivityBasedVariableComparator(activity));
   }

   @Override
   public void init() {
      int nlength = this.lits.nVars() + 1;
      if (this.activity == null || this.activity.length < nlength) {
         this.activity = new double[nlength];
      }

      this.phaseStrategy.init(nlength);
      this.activity[0] = -1.0;
      this.heap = this.createHeap(this.activity);
      this.heap.setBounds(nlength);

      for (int i = 1; i < nlength; i++) {
         assert i > 0;
         assert i <= this.lits.nVars() : "" + this.lits.nVars() + "/" + i;
         this.activity[i] = 0.0;
         if (this.lits.belongsToPool(i)) {
            this.heap.insert(i);
         }
      }
   }

   @Override
   public String toString() {
      return "VSIDS like heuristics from MiniSAT using a heap " + this.phaseStrategy;
   }

   @Override
   public void printStat(PrintWriter out, String prefix) {
      out.println(prefix + "non guided choices\t: " + this.nullchoice);
   }

   @Override
   public void assignLiteral(int p) {
      this.phaseStrategy.assignLiteral(p);
   }

   @Override
   public void updateVarAtDecisionLevel(int q) {
      this.phaseStrategy.updateVarAtDecisionLevel(q);
   }
}
