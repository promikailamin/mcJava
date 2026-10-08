package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.MandatoryLiteralListener;
import net.fabricmc.loader.impl.lib.sat4j.specs.Propagatable;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public abstract class BinaryClause implements Serializable, Constr, Propagatable {
   protected double activity;
   private final ILits voc;
   protected int head;
   protected int tail;

   public BinaryClause(IVecInt ps, ILits voc) {
      assert ps.size() == 2;
      this.head = ps.get(0);
      this.tail = ps.get(1);
      this.voc = voc;
      this.activity = 0.0;
   }

   @Override
   public void calcReason(int p, IVecInt outReason) {
      if (this.voc.isFalsified(this.head)) {
         outReason.push(LiteralsUtils.neg(this.head));
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
      return this.voc.isSatisfied(this.head) || this.voc.isSatisfied(this.tail);
   }

   @Override
   public boolean propagate(UnitPropagationListener s, int p) {
      this.voc.watch(p, this);
      if (this.head == LiteralsUtils.neg(p)) {
         return s.enqueue(this.tail, this);
      }

      assert this.tail == LiteralsUtils.neg(p);
      return s.enqueue(this.head, this);
   }

   @Override
   public boolean propagatePI(MandatoryLiteralListener m, int p) {
      this.voc.watch(p, this);
      if (this.head == LiteralsUtils.neg(p)) {
         m.isMandatory(this.tail);
      } else {
         assert this.tail == LiteralsUtils.neg(p);
         m.isMandatory(this.head);
      }

      return true;
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
      stb.append("@");
      stb.append(this.voc.getLevel(this.head));
      stb.append("]");
      stb.append(" ");
      stb.append(Lits.toString(this.tail));
      stb.append("[");
      stb.append(this.voc.valueToString(this.tail));
      stb.append("@");
      stb.append(this.voc.getLevel(this.tail));
      stb.append("]");
      return stb.toString();
   }

   @Override
   public int get(int i) {
      if (i == 0) {
         return this.head;
      }

      assert i == 1;
      return this.tail;
   }

   @Override
   public void rescaleBy(double d) {
      this.activity *= d;
   }

   @Override
   public int size() {
      return 2;
   }

   @Override
   public void assertConstraint(UnitPropagationListener s) {
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
         BinaryClause wcl = (BinaryClause)obj;
         return wcl.head == this.head && wcl.tail == this.tail;
      } catch (ClassCastException e) {
         return false;
      }
   }

   @Override
   public int hashCode() {
      long sum = (long)this.head + this.tail;
      return (int)sum / 2;
   }

   @Override
   public void register() {
      this.voc.watch(LiteralsUtils.neg(this.head), this);
      this.voc.watch(LiteralsUtils.neg(this.tail), this);
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
      return this.voc.isSatisfied(this.head) ? true : this.voc.isSatisfied(this.tail);
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
