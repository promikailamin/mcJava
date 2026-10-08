package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public class UnitClause implements Constr {
   protected final int literal;
   protected double activity;
   private boolean learnt;

   public UnitClause(int value) {
      this(value, false);
   }

   public UnitClause(int value, boolean learnt) {
      this.literal = value;
      this.learnt = learnt;
   }

   @Override
   public void assertConstraint(UnitPropagationListener s) {
      s.enqueue(this.literal, this);
   }

   @Override
   public void calcReason(int p, IVecInt outReason) {
      if (p == -1) {
         outReason.push(LiteralsUtils.neg(this.literal));
      }
   }

   @Override
   public double getActivity() {
      return this.activity;
   }

   @Override
   public void incActivity(double claInc) {
   }

   @Override
   public void setActivity(double claInc) {
      this.activity = claInc;
   }

   @Override
   public boolean locked() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void register() {
   }

   @Override
   public void remove(UnitPropagationListener upl) {
      int oldLevel = upl.getPropagationLevel();
      upl.unset(this.literal);
      if (upl.getPropagationLevel() < oldLevel - 1) {
         throw new IllegalStateException("removed unit clause which caused propagations");
      }
   }

   @Override
   public void rescaleBy(double d) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setLearnt() {
      this.learnt = true;
   }

   @Override
   public boolean simplify() {
      return false;
   }

   @Override
   public int get(int i) {
      if (i > 0) {
         throw new IllegalArgumentException();
      } else {
         return this.literal;
      }
   }

   @Override
   public boolean learnt() {
      return this.learnt;
   }

   @Override
   public int size() {
      return 1;
   }

   @Override
   public String toString() {
      return Lits.toString(this.literal);
   }

   @Override
   public boolean canBePropagatedMultipleTimes() {
      return false;
   }

   @Override
   public boolean canBeSatisfiedByCountingLiterals() {
      return true;
   }

   @Override
   public int requiredNumberOfSatisfiedLiterals() {
      return 1;
   }

   @Override
   public boolean isSatisfied() {
      return true;
   }

   @Override
   public int getAssertionLevel(IVecInt trail, int decisionLevel) {
      return 0;
   }
}
