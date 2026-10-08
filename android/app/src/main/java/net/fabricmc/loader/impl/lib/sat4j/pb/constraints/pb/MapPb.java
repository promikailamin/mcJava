package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.Lits;

public class MapPb implements IDataStructurePB {
   protected InternalMapPBStructure weightedLits;
   protected BigInteger degree;
   protected int assertiveLiteral = -1;
   private int cpCardsReduction = 0;
   protected final AutoDivisionStrategy autoDivisionStrategy;

   MapPb(int size) {
      this.weightedLits = new InternalMapPBStructure(size);
      this.degree = BigInteger.ZERO;
      this.autoDivisionStrategy = AutoDivisionStrategy.ENABLED;
   }

   @Override
   public BigInteger saturation() {
      assert this.degree.signum() > 0;
      BigInteger minimum = this.degree;

      for (int ind = 0; ind < this.size(); ind++) {
         assert this.weightedLits.getCoef(ind).signum() >= 0;
         if (this.degree.compareTo(this.weightedLits.getCoef(ind)) < 0) {
            this.changeCoef(ind, this.degree);
         }

         assert this.weightedLits.getCoef(ind).signum() >= 0;
         if (this.weightedLits.getCoef(ind).signum() > 0) {
            minimum = minimum.min(this.weightedLits.getCoef(ind));
         }
      }

      if (minimum.equals(this.degree) && minimum.compareTo(BigInteger.ONE) > 0) {
         this.degree = BigInteger.ONE;

         for (int ind = 0; ind < this.size(); ind++) {
            this.changeCoef(ind, BigInteger.ONE);
         }
      }

      return this.degree;
   }

   @Override
   public BigInteger cuttingPlane(int[] lits, BigInteger[] reducedCoefs, BigInteger deg) {
      return this.cuttingPlane(lits, reducedCoefs, deg, BigInteger.ONE);
   }

   public BigInteger cuttingPlane(int[] lits, BigInteger[] reducedCoefs, BigInteger degreeCons, BigInteger coefMult) {
      this.degree = this.degree.add(degreeCons);
      assert this.degree.signum() > 0;

      for (int i = 0; i < lits.length; i++) {
         this.cuttingPlaneStep(lits[i], reducedCoefs[i].multiply(coefMult));
      }

      return this.degree;
   }

   private void cuttingPlaneStep(int lit, BigInteger coef) {
      assert coef.signum() >= 0;
      int nlit = lit ^ 1;
      if (coef.signum() > 0) {
         if (this.weightedLits.containsKey(nlit)) {
            assert !this.weightedLits.containsKey(lit);
            assert this.weightedLits.get(nlit) != null;
            if (this.weightedLits.get(nlit).compareTo(coef) < 0) {
               BigInteger tmp = this.weightedLits.get(nlit);
               this.setCoef(lit, coef.subtract(tmp));
               assert this.weightedLits.get(lit).signum() > 0;
               this.degree = this.degree.subtract(tmp);
               this.removeCoef(nlit);
            } else if (this.weightedLits.get(nlit).equals(coef)) {
               this.degree = this.degree.subtract(coef);
               this.removeCoef(nlit);
            } else {
               this.decreaseCoef(nlit, coef);
               assert this.weightedLits.get(nlit).signum() > 0;
               this.degree = this.degree.subtract(coef);
            }
         } else {
            assert !this.weightedLits.containsKey(lit) || this.weightedLits.get(lit).signum() > 0;
            if (this.weightedLits.containsKey(lit)) {
               this.increaseCoef(lit, coef);
            } else {
               this.setCoef(lit, coef);
            }

            assert this.weightedLits.get(lit).signum() > 0;
         }
      }

      assert !this.weightedLits.containsKey(nlit) || !this.weightedLits.containsKey(lit);
   }

   @Override
   public void buildConstraintFromMapPb(int[] resLits, BigInteger[] resCoefs) {
      assert resLits.length == resCoefs.length;
      assert resLits.length == this.size();
      this.weightedLits.copyCoefs(resCoefs);
      this.weightedLits.copyLits(resLits);
   }

   @Override
   public BigInteger getDegree() {
      return this.degree;
   }

   @Override
   public int size() {
      return this.weightedLits.size();
   }

   @Override
   public String toString() {
      StringBuilder stb = new StringBuilder();

      for (int ind = 0; ind < this.size(); ind++) {
         stb.append(this.weightedLits.getCoef(ind));
         stb.append(".");
         stb.append(Lits.toString(this.weightedLits.getLit(ind)));
         stb.append(" ");
      }

      return stb.toString() + " >= " + this.degree;
   }

   void increaseCoef(int lit, BigInteger incCoef) {
      this.weightedLits.put(lit, this.weightedLits.get(lit).add(incCoef));
   }

   void decreaseCoef(int lit, BigInteger decCoef) {
      this.weightedLits.put(lit, this.weightedLits.get(lit).subtract(decCoef));
   }

   void setCoef(int lit, BigInteger newValue) {
      this.weightedLits.put(lit, newValue);
   }

   void changeCoef(int indLit, BigInteger newValue) {
      this.weightedLits.changeCoef(indLit, newValue);
   }

   void removeCoef(int lit) {
      this.weightedLits.remove(lit);
   }
}
