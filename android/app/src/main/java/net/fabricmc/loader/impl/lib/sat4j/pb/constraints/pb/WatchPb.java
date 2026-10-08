package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

import java.io.Serializable;
import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.Lits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Undoable;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.Propagatable;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public abstract class WatchPb implements Serializable, Undoable, IWatchPb, Propagatable {
   protected double activity;
   protected BigInteger[] coefs;
   protected BigInteger sumcoefs;
   protected BigInteger degree;
   protected int[] lits;
   protected boolean learnt = false;
   protected ILits voc;

   WatchPb() {
   }

   WatchPb(int[] lits, BigInteger[] coefs, BigInteger degree, BigInteger sumCoefs) {
      this.lits = lits;
      this.coefs = coefs;
      this.degree = degree;
      this.sumcoefs = sumCoefs;
      this.sort();
   }

   @Override
   public void calcReason(int p, IVecInt outReason) {
      BigInteger sumfalsified = BigInteger.ZERO;
      int[] mlits = this.lits;

      for (int i = 0; i < mlits.length; i++) {
         int q = mlits[i];
         if (this.voc.isFalsified(q)) {
            outReason.push(q ^ 1);
            sumfalsified = sumfalsified.add(this.coefs[i]);
            if (this.sumcoefs.subtract(sumfalsified).compareTo(this.degree) < 0) {
               return;
            }
         }
      }
   }

   protected abstract void computeWatches() throws ContradictionException;

   protected abstract void computePropagation(UnitPropagationListener var1) throws ContradictionException;

   @Override
   public int get(int i) {
      return this.lits[i];
   }

   @Override
   public double getActivity() {
      return this.activity;
   }

   @Override
   public void incActivity(double claInc) {
      if (this.learnt) {
         this.activity += claInc;
      }
   }

   @Override
   public void setActivity(double d) {
      if (this.learnt) {
         this.activity = d;
      }
   }

   public BigInteger slackConstraint() {
      return this.computeLeftSide().subtract(this.degree);
   }

   public BigInteger computeLeftSide(BigInteger[] theCoefs) {
      BigInteger poss = BigInteger.ZERO;

      for (int i = 0; i < this.lits.length; i++) {
         if (!this.voc.isFalsified(this.lits[i])) {
            assert theCoefs[i].signum() >= 0;
            poss = poss.add(theCoefs[i]);
         }
      }

      return poss;
   }

   public BigInteger computeLeftSide() {
      return this.computeLeftSide(this.coefs);
   }

   protected boolean isSatisfiable() {
      return this.computeLeftSide().compareTo(this.degree) >= 0;
   }

   @Override
   public boolean learnt() {
      return this.learnt;
   }

   @Override
   public boolean locked() {
      for (int p : this.lits) {
         if (this.voc.getReason(p) == this) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void rescaleBy(double d) {
      this.activity *= d;
   }

   void selectionSort(int from, int to) {
      for (int i = from; i < to - 1; i++) {
         int bestIndex = i;

         for (int j = i + 1; j < to; j++) {
            if (this.coefs[j].compareTo(this.coefs[bestIndex]) > 0 || this.coefs[j].equals(this.coefs[bestIndex]) && this.lits[j] < this.lits[bestIndex]) {
               bestIndex = j;
            }
         }

         BigInteger tmp = this.coefs[i];
         this.coefs[i] = this.coefs[bestIndex];
         this.coefs[bestIndex] = tmp;
         int tmp2 = this.lits[i];
         this.lits[i] = this.lits[bestIndex];
         this.lits[bestIndex] = tmp2;
      }
   }

   @Override
   public void setLearnt() {
      this.learnt = true;
   }

   @Override
   public boolean simplify() {
      BigInteger cumul = BigInteger.ZERO;

      for (int i = 0; i < this.lits.length && cumul.compareTo(this.degree) < 0; i++) {
         if (this.voc.isSatisfied(this.lits[i])) {
            cumul = cumul.add(this.coefs[i]);
         }
      }

      return cumul.compareTo(this.degree) >= 0;
   }

   @Override
   public final int size() {
      return this.lits.length;
   }

   protected final void sort() {
      assert this.lits != null;
      if (this.coefs.length > 0) {
         this.sort(0, this.size());
         BigInteger buffInt = this.coefs[0];

         for (int i = 1; i < this.coefs.length; i++) {
            assert buffInt.compareTo(this.coefs[i]) >= 0;
            buffInt = this.coefs[i];
         }
      }
   }

   protected final void sort(int from, int to) {
      int width = to - from;
      if (width <= 15) {
         this.selectionSort(from, to);
      } else {
         int indPivot = width / 2 + from;
         BigInteger pivot = this.coefs[indPivot];
         int litPivot = this.lits[indPivot];
         int i = from - 1;
         int j = to;

         while (true) {
            while (this.coefs[++i].compareTo(pivot) > 0) {
            }

            if (!this.coefs[i].equals(pivot) || this.lits[i] >= litPivot) {
               do {
                  while (pivot.compareTo(this.coefs[--j]) > 0) {
                  }
               } while (this.coefs[j].equals(pivot) && this.lits[j] > litPivot);

               if (i >= j) {
                  this.sort(from, i);
                  this.sort(i, to);
                  break;
               }

               BigInteger tmp = this.coefs[i];
               this.coefs[i] = this.coefs[j];
               this.coefs[j] = tmp;
               int tmp2 = this.lits[i];
               this.lits[i] = this.lits[j];
               this.lits[j] = tmp2;
            }
         }
      }
   }

   @Override
   public String toString() {
      StringBuilder stb = new StringBuilder();
      if (this.lits.length > 0) {
         for (int i = 0; i < this.lits.length; i++) {
            stb.append(" + ");
            stb.append(this.coefs[i]);
            stb.append(".");
            stb.append(Lits.toString(this.lits[i]));
            stb.append("[");
            stb.append(this.voc.valueToString(this.lits[i]));
            stb.append("@");
            stb.append(this.voc.getLevel(this.lits[i]));
            stb.append("]");
            stb.append(" ");
         }

         stb.append(">= ");
         stb.append(this.degree);
      }

      return stb.toString();
   }

   @Override
   public void assertConstraint(UnitPropagationListener s) {
      BigInteger tmp = this.slackConstraint();

      for (int i = 0; i < this.lits.length; i++) {
         if (this.voc.isUnassigned(this.lits[i]) && tmp.compareTo(this.coefs[i]) < 0) {
            boolean ret = s.enqueue(this.lits[i], this);
            assert ret;
         }
      }
   }

   @Override
   public void register() {
      assert this.learnt;

      try {
         this.computeWatches();
      } catch (ContradictionException e) {
         System.out.println(this);
         assert false;
      }
   }

   @Override
   public boolean equals(Object pb) {
      if (pb == null) {
         return false;
      }

      if (this.getClass() != pb.getClass()) {
         return false;
      }

      try {
         WatchPb wpb = (WatchPb)pb;
         if (this.degree.equals(wpb.degree) && this.coefs.length == wpb.coefs.length && this.lits.length == wpb.lits.length) {
            for (int ilit = 0; ilit < this.coefs.length; ilit++) {
               int lit = this.lits[ilit];
               boolean ok = false;

               for (int ilit2 = 0; ilit2 < this.coefs.length; ilit2++) {
                  if (wpb.lits[ilit2] == lit) {
                     if (!wpb.coefs[ilit2].equals(this.coefs[ilit])) {
                        return false;
                     }

                     ok = true;
                     break;
                  }
               }

               if (!ok) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      } catch (ClassCastException e) {
         return false;
      }
   }

   @Override
   public int hashCode() {
      long sum = 0L;

      for (int p : this.lits) {
         sum += p;
      }

      return (int)sum / this.lits.length;
   }

   @Override
   public boolean canBePropagatedMultipleTimes() {
      return true;
   }

   @Override
   public Constr toConstraint() {
      return this;
   }

   @Override
   public boolean canBeSatisfiedByCountingLiterals() {
      return false;
   }

   @Override
   public int requiredNumberOfSatisfiedLiterals() {
      throw new UnsupportedOperationException("Not applicable for PB constraints");
   }

   @Override
   public boolean isSatisfied() {
      BigInteger satisfied = BigInteger.ZERO;

      for (int i = 0; i < this.lits.length; i++) {
         if (this.voc.isSatisfied(this.lits[i])) {
            assert this.coefs[i].signum() >= 0;
            satisfied = satisfied.add(this.coefs[i]);
         }
      }

      return satisfied.compareTo(this.degree) >= 0;
   }
}
