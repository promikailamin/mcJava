package net.fabricmc.loader.impl.lib.sat4j.specs;

public interface Constr extends IConstr {
   Constr TAUTOLOGY = new Constr() {
      @Override
      public boolean learnt() {
         return false;
      }

      @Override
      public int size() {
         return 0;
      }

      @Override
      public int get(int i) {
         throw new UnsupportedOperationException("No elements in a tautology");
      }

      @Override
      public double getActivity() {
         return 0.0;
      }

      @Override
      public boolean canBePropagatedMultipleTimes() {
         return false;
      }

      @Override
      public void remove(UnitPropagationListener upl) {
      }

      @Override
      public boolean simplify() {
         return false;
      }

      @Override
      public void calcReason(int p, IVecInt outReason) {
         throw new UnsupportedOperationException("A tautology cannot be a reason");
      }

      @Override
      public void incActivity(double claInc) {
      }

      @Override
      public boolean locked() {
         return false;
      }

      @Override
      public void setLearnt() {
      }

      @Override
      public void register() {
      }

      @Override
      public void rescaleBy(double d) {
      }

      @Override
      public void setActivity(double d) {
      }

      @Override
      public void assertConstraint(UnitPropagationListener s) {
      }

      @Override
      public boolean canBeSatisfiedByCountingLiterals() {
         return false;
      }

      @Override
      public int requiredNumberOfSatisfiedLiterals() {
         return 0;
      }

      @Override
      public boolean isSatisfied() {
         return true;
      }

      @Override
      public int getAssertionLevel(IVecInt trail, int decisionLevel) {
         return 0;
      }
   };

   void remove(UnitPropagationListener var1);

   boolean simplify();

   void calcReason(int var1, IVecInt var2);

   void incActivity(double var1);

   boolean locked();

   void setLearnt();

   void register();

   void rescaleBy(double var1);

   void setActivity(double var1);

   void assertConstraint(UnitPropagationListener var1);

   boolean canBeSatisfiedByCountingLiterals();

   int requiredNumberOfSatisfiedLiterals();

   boolean isSatisfied();

   int getAssertionLevel(IVecInt var1, int var2);
}
