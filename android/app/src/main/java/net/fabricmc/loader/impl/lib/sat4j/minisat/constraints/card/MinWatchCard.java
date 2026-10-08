package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.card;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.Lits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.UnitClauses;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Undoable;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.MandatoryLiteralListener;
import net.fabricmc.loader.impl.lib.sat4j.specs.Propagatable;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public class MinWatchCard implements Serializable, Undoable, Constr, Propagatable {
   protected int degree;
   protected final int[] lits;
   private boolean moreThan;
   protected int watchCumul;
   private final ILits voc;
   protected final int maxUnsatisfied;
   private int savedindex;

   public MinWatchCard(ILits voc, IVecInt ps, boolean moreThan, int degree) {
      this.savedindex = this.degree + 1;
      this.voc = voc;
      this.degree = degree;
      this.moreThan = moreThan;
      int[] index = new int[voc.nVars() * 2 + 2];

      for (int i = 0; i < ps.size(); i++) {
         int p = ps.get(i);
         if (index[p ^ 1] == 0) {
            index[p]++;
         } else {
            index[p ^ 1]--;
         }
      }

      int ind = 0;

      while (ind < ps.size()) {
         if (index[ps.get(ind)] > 0) {
            index[ps.get(ind)]--;
            ind++;
         } else {
            if ((ps.get(ind) & 1) != 0) {
               this.degree--;
            }

            ps.delete(ind);
         }
      }

      this.lits = new int[ps.size()];
      ps.moveTo(this.lits);
      this.normalize();
      this.maxUnsatisfied = this.lits.length - this.degree;
   }

   @Override
   public void calcReason(int p, IVecInt outReason) {
      int c = p == -1 ? -1 : 0;

      for (int q : this.lits) {
         if (this.voc.isFalsified(q)) {
            outReason.push(q ^ 1);
            if (++c >= this.maxUnsatisfied) {
               return;
            }
         }
      }
   }

   @Override
   public double getActivity() {
      return 0.0;
   }

   @Override
   public void incActivity(double claInc) {
   }

   @Override
   public void setActivity(double d) {
   }

   @Override
   public boolean learnt() {
      return false;
   }

   protected static int linearisation(ILits voc, IVecInt ps) {
      int modif = 0;
      int i = 0;

      while (i < ps.size()) {
         if (voc.isUnassigned(ps.get(i))) {
            i++;
         } else {
            if (voc.isSatisfied(ps.get(i))) {
               modif--;
            }

            ps.set(i, ps.last());
            ps.pop();
         }
      }

      assert modif <= 0;
      return modif;
   }

   @Override
   public boolean locked() {
      return true;
   }

   public static Constr minWatchCardNew(UnitPropagationListener s, ILits voc, IVecInt ps, boolean moreThan, int degree) throws ContradictionException {
      int mydegree = degree + linearisation(voc, ps);
      if (ps.size() < mydegree) {
         throw new ContradictionException();
      }

      if (ps.size() == mydegree) {
         for (int i = 0; i < ps.size(); i++) {
            if (!s.enqueue(ps.get(i))) {
               throw new ContradictionException();
            }
         }

         return new UnitClauses(ps);
      } else {
         MinWatchCard retour = new MinWatchCard(voc, ps, moreThan, mydegree);
         if (retour.degree <= 0) {
            return null;
         }

         retour.computeWatches();
         retour.computePropagation(s);
         return retour;
      }
   }

   public final void normalize() {
      if (!this.moreThan) {
         this.degree = 0 - this.degree;

         for (int indLit = 0; indLit < this.lits.length; indLit++) {
            this.lits[indLit] = this.lits[indLit] ^ 1;
            this.degree++;
         }

         this.moreThan = true;
      }
   }

   @Override
   public boolean propagate(UnitPropagationListener s, int p) {
      this.savedindex = this.degree + 1;
      if (this.watchCumul == this.degree) {
         this.voc.watch(p, this);
         return false;
      }

      int indFalsified = 0;

      while ((this.lits[indFalsified] ^ 1) != p) {
         indFalsified++;
      }

      assert this.watchCumul > this.degree;
      int indSwap = this.degree + 1;

      while (indSwap < this.lits.length && this.voc.isFalsified(this.lits[indSwap])) {
         indSwap++;
      }

      if (indSwap == this.lits.length) {
         this.voc.watch(p, this);
         this.watchCumul--;
         assert this.watchCumul == this.degree;
         this.voc.undos(p).push(this);

         for (int i = 0; i <= this.degree; i++) {
            if (p != (this.lits[i] ^ 1) && !s.enqueue(this.lits[i], this)) {
               return false;
            }
         }

         return true;
      } else {
         int tmpInt = this.lits[indSwap];
         this.lits[indSwap] = this.lits[indFalsified];
         this.lits[indFalsified] = tmpInt;
         this.voc.watch(tmpInt ^ 1, this);
         return true;
      }
   }

   @Override
   public void remove(UnitPropagationListener upl) {
      for (int i = 0; i < Math.min(this.degree + 1, this.lits.length); i++) {
         this.voc.watches(this.lits[i] ^ 1).remove(this);
      }
   }

   @Override
   public void rescaleBy(double d) {
   }

   @Override
   public boolean simplify() {
      int i = 0;
      int count = 0;

      while (i < this.lits.length) {
         if (this.voc.isSatisfied(this.lits[i])) {
            if (++count == this.degree) {
               return true;
            }
         }

         i++;
      }

      return false;
   }

   @Override
   public String toString() {
      StringBuilder stb = new StringBuilder();
      if (this.lits.length > 0) {
         stb.append(Lits.toStringX(this.lits[0]));
         stb.append("[");
         stb.append(this.voc.valueToString(this.lits[0]));
         stb.append("]");
         stb.append(" ");

         for (int i = 1; i < this.lits.length; i++) {
            stb.append(Lits.toStringX(this.lits[i]));
            stb.append("[");
            stb.append(this.voc.valueToString(this.lits[i]));
            stb.append("]");
            stb.append(" ");
         }

         stb.append(">= ");
         stb.append(this.degree);
      }

      return stb.toString();
   }

   @Override
   public void undo(int p) {
      this.watchCumul++;
   }

   @Override
   public void setLearnt() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void register() {
      this.computeWatches();
   }

   @Override
   public int size() {
      return this.lits.length;
   }

   @Override
   public int get(int i) {
      return this.lits[i];
   }

   @Override
   public void assertConstraint(UnitPropagationListener s) {
      boolean ret = true;
      int[] var3 = this.lits;
      int var4 = var3.length;

      for (int var5 = 0; var5 < var4; var5++) {
         Integer lit = var3[var5];
         if (this.voc.isUnassigned(lit)) {
            ret &= s.enqueue(lit, this);
         }
      }

      assert ret;
   }

   protected void computeWatches() {
      int indSwap = this.lits.length;

      for (int i = 0; i <= this.degree && i < indSwap; i++) {
         while (true) {
            if (this.voc.isFalsified(this.lits[i])) {
               if (--indSwap > i) {
                  int tmpInt = this.lits[i];
                  this.lits[i] = this.lits[indSwap];
                  this.lits[indSwap] = tmpInt;
                  continue;
               }
            }

            if (!this.voc.isFalsified(this.lits[i])) {
               this.watchCumul++;
               this.voc.watch(this.lits[i] ^ 1, this);
            }
            break;
         }
      }

      if (this.watchCumul <= this.degree) {
         int free = 1;

         while (this.watchCumul <= this.degree && free > 0) {
            free = 0;
            int maxlevel = -1;
            int maxi = -1;

            for (int i = this.watchCumul; i < this.lits.length; i++) {
               if (this.voc.isFalsified(this.lits[i])) {
                  free++;
                  int level = this.voc.getLevel(this.lits[i]);
                  if (level > maxlevel) {
                     maxi = i;
                     maxlevel = level;
                  }
               }
            }

            if (free > 0) {
               assert maxi >= 0;
               this.voc.watch(this.lits[maxi] ^ 1, this);
               int tmpInt = this.lits[maxi];
               this.lits[maxi] = this.lits[this.watchCumul];
               this.lits[this.watchCumul] = tmpInt;
               this.watchCumul++;
               assert --free >= 0;
            }
         }

         assert this.lits.length == 1 || this.watchCumul > 1;
      }
   }

   protected MinWatchCard computePropagation(UnitPropagationListener s) throws ContradictionException {
      if (this.watchCumul == this.degree) {
         for (int i = 0; i < this.lits.length; i++) {
            if (!s.enqueue(this.lits[i])) {
               throw new ContradictionException();
            }
         }

         return null;
      } else if (this.watchCumul < this.degree) {
         throw new ContradictionException();
      } else {
         return this;
      }
   }

   @Override
   public boolean equals(Object card) {
      if (card == null) {
         return false;
      }

      if (this.getClass() != card.getClass()) {
         return false;
      }

      try {
         MinWatchCard mcard = (MinWatchCard)card;
         if (mcard.degree != this.degree) {
            return false;
         }

         if (this.lits.length != mcard.lits.length) {
            return false;
         }

         for (int lit : this.lits) {
            boolean ok = false;

            for (int lit2 : mcard.lits) {
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

      sum += this.degree;
      return (int)sum / (this.lits.length + 1);
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
   public boolean propagatePI(MandatoryLiteralListener l, int p) {
      int indFalsified = 0;

      while ((this.lits[indFalsified] ^ 1) != p) {
         indFalsified++;
      }

      assert this.watchCumul >= this.degree;
      int indSwap = this.savedindex;

      while (indSwap < this.lits.length && this.voc.isFalsified(this.lits[indSwap])) {
         indSwap++;
      }

      if (indSwap == this.lits.length) {
         this.voc.watch(p, this);

         for (int i = 0; i <= this.degree; i++) {
            if (p != (this.lits[i] ^ 1)) {
               l.isMandatory(this.lits[i]);
            }
         }

         return true;
      } else {
         this.savedindex = indSwap + 1;
         int tmpInt = this.lits[indSwap];
         this.lits[indSwap] = this.lits[indFalsified];
         this.lits[indFalsified] = tmpInt;
         this.voc.watch(tmpInt ^ 1, this);
         return true;
      }
   }

   @Override
   public boolean canBeSatisfiedByCountingLiterals() {
      return true;
   }

   @Override
   public int requiredNumberOfSatisfiedLiterals() {
      return this.degree;
   }

   @Override
   public boolean isSatisfied() {
      throw new UnsupportedOperationException("Not implemented yet!");
   }

   @Override
   public int getAssertionLevel(IVecInt trail, int decisionLevel) {
      int nUnsat = 0;
      Set<Integer> litsSet = new HashSet<>();
      int[] i = this.lits;
      int var6 = i.length;

      for (int var7 = 0; var7 < var6; var7++) {
         Integer ix = i[var7];
         litsSet.add(ix);
      }

      for (int ix = 0; ix < trail.size(); ix++) {
         if (litsSet.contains(trail.get(ix) ^ 1)) {
            if (++nUnsat == this.maxUnsatisfied) {
               return ix;
            }
         }
      }

      return -1;
   }
}
