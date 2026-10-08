package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Comparator;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.Lits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Undoable;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.Propagatable;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public abstract class WatchPbLong implements Serializable, Undoable, Constr, Propagatable {
   protected double activity;
   protected long[] coefs;
   protected long sumcoefs;
   protected long degree;
   protected int[] lits;
   protected boolean learnt = false;
   protected ILits voc;
   private final Comparator<Integer> levelBased = new Comparator<Integer>() {
      public int compare(Integer o1, Integer o2) {
         return WatchPbLong.this.voc.getLevel(o1) - WatchPbLong.this.voc.getLevel(o2);
      }
   };

   WatchPbLong() {
   }

   WatchPbLong(int[] lits, BigInteger[] coefs, BigInteger degree, BigInteger sumCoefs) {
      this.lits = lits;
      this.coefs = toLong(coefs);
      this.degree = degree.longValue();
      this.sumcoefs = sumCoefs.longValue();
      this.sort();
   }

   public static long[] toLong(BigInteger[] bigValues) {
      long[] res = new long[bigValues.length];

      for (int i = 0; i < res.length; i++) {
         res[i] = bigValues[i].longValue();
      }

      return res;
   }

   @Override
   public void calcReason(int p, IVecInt outReason) {
      long sumfalsified = 0L;
      long tous = 0L;
      int[] mlits = this.lits;
      boolean ok = true;

      for (int i = 0; i < mlits.length; i++) {
         int q = mlits[i];
         if (this.voc.isFalsified(q)) {
            if (ok) {
               outReason.push(q ^ 1);
               sumfalsified += this.coefs[i];
               if (this.sumcoefs - sumfalsified < this.degree) {
                  ok = false;
                  tous = sumfalsified;
               }
            } else {
               tous += this.coefs[i];
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

   public long slackConstraint() {
      return this.computeLeftSide() - this.degree;
   }

   public long computeLeftSide(long[] theCoefs) {
      long poss = 0L;

      for (int i = 0; i < this.lits.length; i++) {
         if (!this.voc.isFalsified(this.lits[i])) {
            assert theCoefs[i] >= 0L;
            poss += theCoefs[i];
         }
      }

      return poss;
   }

   public long computeLeftSide() {
      return this.computeLeftSide(this.coefs);
   }

   protected boolean isSatisfiable() {
      return this.computeLeftSide() >= this.degree;
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
            if (this.coefs[j] > this.coefs[bestIndex] || this.coefs[j] == this.coefs[bestIndex] && this.lits[j] < this.lits[bestIndex]) {
               bestIndex = j;
            }
         }

         long tmp = this.coefs[i];
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
      long cumul = 0L;

      for (int i = 0; i < this.lits.length && cumul < this.degree; i++) {
         if (this.voc.isSatisfied(this.lits[i])) {
            cumul += this.coefs[i];
         }
      }

      return cumul >= this.degree;
   }

   @Override
   public final int size() {
      return this.lits.length;
   }

   protected final void sort() {
      assert this.lits != null;
      if (this.coefs.length > 0) {
         this.sort(0, this.size());
         long buffInt = this.coefs[0];

         for (int i = 1; i < this.coefs.length; i++) {
            assert buffInt >= this.coefs[i];
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
         long pivot = this.coefs[indPivot];
         int litPivot = this.lits[indPivot];
         int i = from - 1;
         int j = to;

         while (true) {
            while (this.coefs[++i] > pivot) {
            }

            if (this.coefs[i] != pivot || this.lits[i] >= litPivot) {
               do {
                  while (pivot > this.coefs[--j]) {
                  }
               } while (this.coefs[j] == pivot && this.lits[j] > litPivot);

               if (i >= j) {
                  this.sort(from, i);
                  this.sort(i, to);
                  break;
               }

               long tmp = this.coefs[i];
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
      long tmp = this.slackConstraint();

      for (int i = 0; i < this.lits.length; i++) {
         if (this.voc.isUnassigned(this.lits[i]) && tmp < this.coefs[i]) {
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
         WatchPbLong wpb = (WatchPbLong)pb;
         if (this.degree == wpb.degree && this.coefs.length == wpb.coefs.length && this.lits.length == wpb.lits.length) {
            for (int ilit = 0; ilit < this.coefs.length; ilit++) {
               int lit = this.lits[ilit];
               boolean ok = false;

               for (int ilit2 = 0; ilit2 < this.coefs.length; ilit2++) {
                  if (wpb.lits[ilit2] == lit) {
                     if (wpb.coefs[ilit2] != this.coefs[ilit]) {
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
   public void remove(UnitPropagationListener upl) {
   }

   @Override
   public boolean propagate(UnitPropagationListener s, int p) {
      return false;
   }

   @Override
   public void undo(int p) {
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
      throw new UnsupportedOperationException("Not implemented yet");
   }
}
