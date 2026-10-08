package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.Propagatable;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public abstract class WLClause implements Serializable, Constr, Propagatable {
   protected double activity;
   protected final int[] lits;
   protected final ILits voc;

   public WLClause(IVecInt ps, ILits voc) {
      this.lits = new int[ps.size()];
      ps.moveTo(this.lits);
      assert ps.size() == 0;
      this.voc = voc;
      this.activity = 0.0;
   }

   @Override
   public void calcReason(int p, IVecInt outReason) {
      int[] mylits = this.lits;

      for (int i = p == -1 ? 0 : 1; i < mylits.length; i++) {
         assert this.voc.isFalsified(mylits[i]);
         outReason.push(mylits[i] ^ 1);
      }
   }

   @Override
   public void remove(UnitPropagationListener upl) {
      this.voc.watches(this.lits[0] ^ 1).remove(this);
      this.voc.watches(this.lits[1] ^ 1).remove(this);
   }

   @Override
   public boolean simplify() {
      for (int lit : this.lits) {
         if (this.voc.isSatisfied(lit)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean propagate(UnitPropagationListener s, int p) {
      int[] mylits = this.lits;
      if (mylits[0] == (p ^ 1)) {
         mylits[0] = mylits[1];
         mylits[1] = p ^ 1;
      }

      if (this.voc.isSatisfied(mylits[0])) {
         this.voc.watch(p, this);
         return true;
      }

      int previous = p ^ 1;

      for (int i = 2; i < mylits.length; i++) {
         if (!this.voc.isFalsified(mylits[i])) {
            mylits[1] = mylits[i];
            mylits[i] = previous;
            this.voc.watch(mylits[1] ^ 1, this);
            return true;
         }

         int tmp = previous;
         previous = mylits[i];
         mylits[i] = tmp;
      }

      System.arraycopy(mylits, 2, mylits, 1, mylits.length - 2);
      mylits[mylits.length - 1] = previous;
      this.voc.watch(p, this);
      return s.enqueue(mylits[0], this);
   }

   @Override
   public boolean locked() {
      return this.voc.getReason(this.lits[0]) == this;
   }

   @Override
   public double getActivity() {
      return this.activity;
   }

   @Override
   public void setActivity(double d) {
      this.activity = d;
   }

   @Override
   public String toString() {
      StringBuilder stb = new StringBuilder();

      for (int lit : this.lits) {
         stb.append(Lits.toString(lit));
         stb.append("[");
         stb.append(this.voc.valueToString(lit));
         stb.append("@");
         stb.append(this.voc.getLevel(lit));
         stb.append("]");
         stb.append(" ");
      }

      return stb.toString();
   }

   @Override
   public int get(int i) {
      return this.lits[i];
   }

   @Override
   public void rescaleBy(double d) {
      this.activity *= d;
   }

   @Override
   public int size() {
      return this.lits.length;
   }

   @Override
   public void assertConstraint(UnitPropagationListener s) {
      boolean ret = s.enqueue(this.lits[0], this);
      assert ret;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      try {
         WLClause wcl = (WLClause)obj;
         if (this.lits.length != wcl.lits.length) {
            return false;
         }

         for (int lit : this.lits) {
            boolean ok = false;

            for (int lit2 : wcl.lits) {
               if (lit == lit2) {
                  ok = true;
                  break;
               }
            }

            if (!ok) {
               return false;
            }
         }

         return true;
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
      return false;
   }

   @Override
   public Constr toConstraint() {
      return this;
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
      for (int p : this.lits) {
         if (this.voc.isSatisfied(p)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int getAssertionLevel(IVecInt trail, int decisionLevel) {
      for (int i = trail.size() - 1; i >= 0; i--) {
         if (LiteralsUtils.var(trail.get(i)) == LiteralsUtils.var(this.lits[0])) {
            return i;
         }
      }

      return -1;
   }
}
