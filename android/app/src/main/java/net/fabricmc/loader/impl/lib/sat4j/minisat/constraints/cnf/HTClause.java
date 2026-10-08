package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.Propagatable;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public abstract class HTClause implements Serializable, Constr, Propagatable {
   protected double activity;
   protected final int[] middleLits;
   protected final ILits voc;
   protected int head;
   protected int tail;

   public HTClause(IVecInt ps, ILits voc) {
      assert ps.size() > 1;
      this.head = ps.get(0);
      this.tail = ps.last();
      int size = ps.size() - 2;
      assert size > 0;
      this.middleLits = new int[size];
      System.arraycopy(ps.toArray(), 1, this.middleLits, 0, size);
      ps.clear();
      assert ps.size() == 0;
      this.voc = voc;
      this.activity = 0.0;
   }

   @Override
   public void calcReason(int p, IVecInt outReason) {
      if (this.voc.isFalsified(this.head)) {
         outReason.push(LiteralsUtils.neg(this.head));
      }

      int[] mylits = this.middleLits;

      for (int mylit : mylits) {
         if (this.voc.isFalsified(mylit)) {
            outReason.push(LiteralsUtils.neg(mylit));
         }
      }

      if (this.voc.isFalsified(this.tail)) {
         outReason.push(LiteralsUtils.neg(this.tail));
      }
   }

   @Override
   public void remove(UnitPropagationListener upl) {
      this.voc.watches(LiteralsUtils.neg(this.head)).remove(this);
      this.voc.watches(LiteralsUtils.neg(this.tail)).remove(this);
   }

   @Override
   public boolean simplify() {
      if (!this.voc.isSatisfied(this.head) && !this.voc.isSatisfied(this.tail)) {
         for (int middleLit : this.middleLits) {
            if (this.voc.isSatisfied(middleLit)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   @Override
   public boolean propagate(UnitPropagationListener s, int p) {
      if (this.head == LiteralsUtils.neg(p)) {
         int[] mylits = this.middleLits;
         int temphead = 0;

         while (temphead < mylits.length && this.voc.isFalsified(mylits[temphead])) {
            temphead++;
         }

         assert temphead <= mylits.length;
         if (temphead == mylits.length) {
            this.voc.watch(p, this);
            return s.enqueue(this.tail, this);
         } else {
            this.head = mylits[temphead];
            mylits[temphead] = LiteralsUtils.neg(p);
            this.voc.watch(LiteralsUtils.neg(this.head), this);
            return true;
         }
      } else {
         assert this.tail == LiteralsUtils.neg(p);
         int[] mylits = this.middleLits;
         int temptail = mylits.length - 1;

         while (temptail >= 0 && this.voc.isFalsified(mylits[temptail])) {
            temptail--;
         }

         assert -1 <= temptail;
         if (-1 == temptail) {
            this.voc.watch(p, this);
            return s.enqueue(this.head, this);
         } else {
            this.tail = mylits[temptail];
            mylits[temptail] = LiteralsUtils.neg(p);
            this.voc.watch(LiteralsUtils.neg(this.tail), this);
            return true;
         }
      }
   }

   @Override
   public boolean locked() {
      return this.voc.getReason(this.head) == this || this.voc.getReason(this.tail) == this;
   }

   @Override
   public double getActivity() {
      return this.activity;
   }

   @Override
   public String toString() {
      StringBuilder stb = new StringBuilder();
      stb.append(Lits.toString(this.head));
      stb.append("[");
      stb.append(this.voc.valueToString(this.head));
      stb.append("]");
      stb.append(" ");

      for (int middleLit : this.middleLits) {
         stb.append(Lits.toString(middleLit));
         stb.append("[");
         stb.append(this.voc.valueToString(middleLit));
         stb.append("]");
         stb.append(" ");
      }

      stb.append(Lits.toString(this.tail));
      stb.append("[");
      stb.append(this.voc.valueToString(this.tail));
      stb.append("]");
      return stb.toString();
   }

   @Override
   public int get(int i) {
      if (i == 0) {
         return this.head;
      } else {
         return i == this.middleLits.length + 1 ? this.tail : this.middleLits[i - 1];
      }
   }

   @Override
   public void rescaleBy(double d) {
      this.activity *= d;
   }

   @Override
   public int size() {
      return this.middleLits.length + 2;
   }

   @Override
   public void assertConstraint(UnitPropagationListener s) {
      assert this.voc.isUnassigned(this.head);
      boolean ret = s.enqueue(this.head, this);
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
         HTClause wcl = (HTClause)obj;
         if (wcl.head == this.head && wcl.tail == this.tail) {
            if (this.middleLits.length != wcl.middleLits.length) {
               return false;
            }

            for (int lit : this.middleLits) {
               boolean ok = false;

               for (int lit2 : wcl.middleLits) {
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
         } else {
            return false;
         }
      } catch (ClassCastException e) {
         return false;
      }
   }

   @Override
   public int hashCode() {
      long sum = (long)this.head + this.tail;

      for (int p : this.middleLits) {
         sum += p;
      }

      return (int)sum / this.middleLits.length;
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
      if (this.voc.isSatisfied(this.head)) {
         return true;
      }

      if (this.voc.isSatisfied(this.tail)) {
         return true;
      }

      for (int p : this.middleLits) {
         if (this.voc.isSatisfied(p)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int getAssertionLevel(IVecInt trail, int decisionLevel) {
      for (int i = trail.size() - 1; i >= 0; i--) {
         if (LiteralsUtils.var(trail.get(i)) == LiteralsUtils.var(this.head)) {
            return i;
         }
      }

      return -1;
   }
}
