package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public class UnitClauses implements Constr {
   protected final int[] literals;

   public UnitClauses(IVecInt values) {
      this.literals = new int[values.size()];
      values.copyTo(this.literals);
   }

   @Override
   public void assertConstraint(UnitPropagationListener s) {
      for (int p : this.literals) {
         s.enqueue(p, this);
      }
   }

   @Override
   public void calcReason(int p, IVecInt outReason) {
      throw new UnsupportedOperationException();
   }

   @Override
   public double getActivity() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void incActivity(double claInc) {
   }

   @Override
   public void setActivity(double claInc) {
   }

   @Override
   public boolean locked() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void register() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void remove(UnitPropagationListener upl) {
      int oldLevel = upl.getPropagationLevel();

      for (int i = this.literals.length - 1; i >= 0; i--) {
         upl.unset(this.literals[i]);
      }

      if (upl.getPropagationLevel() < oldLevel - this.literals.length) {
         throw new IllegalStateException("removed unit clause which caused propagations");
      }
   }

   @Override
   public void rescaleBy(double d) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setLearnt() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean simplify() {
      return false;
   }

   @Override
   public int get(int i) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean learnt() {
      throw new UnsupportedOperationException();
   }

   @Override
   public int size() {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean canBePropagatedMultipleTimes() {
      return false;
   }

   @Override
   public boolean canBeSatisfiedByCountingLiterals() {
      throw new UnsupportedOperationException("Not implemented yet!");
   }

   @Override
   public int requiredNumberOfSatisfiedLiterals() {
      throw new UnsupportedOperationException("Not implemented yet!");
   }

   @Override
   public boolean isSatisfied() {
      return true;
   }

   @Override
   public int getAssertionLevel(IVecInt trail, int decisionLevel) {
      throw new UnsupportedOperationException("Not implemented yet!");
   }
}
