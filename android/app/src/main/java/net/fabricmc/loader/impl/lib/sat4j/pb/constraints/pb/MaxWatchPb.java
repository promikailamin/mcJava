package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.MandatoryLiteralListener;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public final class MaxWatchPb extends WatchPb {
   private BigInteger watchCumul = BigInteger.ZERO;
   private final Map<Integer, BigInteger> litToCoeffs;

   private MaxWatchPb(ILits voc, int[] lits, BigInteger[] coefs, BigInteger degree, BigInteger sumCoefs) {
      super(lits, coefs, degree, sumCoefs);
      this.voc = voc;
      this.activity = 0.0;
      this.watchCumul = BigInteger.ZERO;
      if (coefs.length > 100) {
         this.litToCoeffs = new HashMap<>(this.coefs.length);

         for (int i = 0; i < this.coefs.length; i++) {
            this.litToCoeffs.put(this.lits[i], this.coefs[i]);
         }
      } else {
         this.litToCoeffs = null;
      }
   }

   @Override
   protected void computeWatches() throws ContradictionException {
      assert this.watchCumul.equals(BigInteger.ZERO);

      for (int i = 0; i < this.lits.length; i++) {
         if (this.voc.isFalsified(this.lits[i])) {
            if (this.learnt) {
               this.voc.undos(this.lits[i] ^ 1).push(this);
               this.voc.watch(this.lits[i] ^ 1, this);
            }
         } else {
            this.voc.watch(this.lits[i] ^ 1, this);
            this.watchCumul = this.watchCumul.add(this.coefs[i]);
         }
      }

      assert this.watchCumul.compareTo(this.computeLeftSide()) >= 0;
      if (!this.learnt && this.watchCumul.compareTo(this.degree) < 0) {
         throw new ContradictionException("non satisfiable constraint");
      }
   }

   @Override
   protected void computePropagation(UnitPropagationListener s) throws ContradictionException {
      for (int ind = 0; ind < this.coefs.length && this.watchCumul.subtract(this.coefs[ind]).compareTo(this.degree) < 0; ind++) {
         if (this.voc.isUnassigned(this.lits[ind]) && !s.enqueue(this.lits[ind], this)) {
            throw new ContradictionException("non satisfiable constraint");
         }
      }

      assert this.watchCumul.compareTo(this.computeLeftSide()) >= 0;
   }

   @Override
   public boolean propagate(UnitPropagationListener s, int p) {
      this.voc.watch(p, this);
      assert this.watchCumul.compareTo(this.computeLeftSide()) >= 0 : "" + this.watchCumul + "/" + this.computeLeftSide() + ":" + this.learnt;
      BigInteger coefP;
      if (this.litToCoeffs == null) {
         int indiceP = 0;

         while ((this.lits[indiceP] ^ 1) != p) {
            indiceP++;
         }

         coefP = this.coefs[indiceP];
      } else {
         coefP = this.litToCoeffs.get(p ^ 1);
      }

      BigInteger newcumul = this.watchCumul.subtract(coefP);
      if (newcumul.compareTo(this.degree) < 0) {
         assert !this.isSatisfiable();
         return false;
      }

      this.voc.undos(p).push(this);
      this.watchCumul = newcumul;
      int ind = 0;
      int trailPosition = this.voc.getTrailPosition(p);

      for (BigInteger limit = this.watchCumul.subtract(this.degree); ind < this.coefs.length && limit.compareTo(this.coefs[ind]) < 0; ind++) {
         int lit = this.lits[ind];
         if (this.voc.isFalsified(lit) && this.voc.getTrailPosition(lit) > trailPosition) {
            assert !this.isSatisfiable();
            return false;
         }

         if (this.voc.isUnassigned(lit)) {
            boolean enqueued = s.enqueue(lit, this);
            assert enqueued;
         }
      }

      assert this.learnt || this.watchCumul.compareTo(this.computeLeftSide()) >= 0;
      assert this.watchCumul.compareTo(this.computeLeftSide()) >= 0;
      return true;
   }

   @Override
   public void remove(UnitPropagationListener upl) {
      for (int i = 0; i < this.lits.length; i++) {
         if (!this.voc.isFalsified(this.lits[i])) {
            this.voc.watches(this.lits[i] ^ 1).remove(this);
         }
      }

      for (int ind = 0; ind < this.coefs.length && this.watchCumul.subtract(this.coefs[ind]).compareTo(this.degree) < 0; ind++) {
         if (!this.voc.isUnassigned(this.lits[ind]) && this.voc.getReason(this.lits[ind]) == this) {
            upl.unset(this.lits[ind]);
         }
      }
   }

   @Override
   public void undo(int p) {
      BigInteger coefP;
      if (this.litToCoeffs == null) {
         int indiceP = 0;

         while (indiceP < this.lits.length && (this.lits[indiceP] ^ 1) != p) {
            indiceP++;
         }

         coefP = indiceP == this.lits.length ? BigInteger.ZERO : this.coefs[indiceP];
      } else {
         coefP = this.litToCoeffs.get(p ^ 1);
      }

      this.watchCumul = this.watchCumul.add(coefP);
   }

   public static MaxWatchPb normalizedMaxWatchPbNew(
      UnitPropagationListener s, ILits voc, int[] lits, BigInteger[] coefs, BigInteger degree, BigInteger sumCoefs
   ) throws ContradictionException {
      MaxWatchPb outclause = new MaxWatchPb(voc, lits, coefs, degree, sumCoefs);
      if (outclause.degree.signum() <= 0) {
         return null;
      }

      outclause.computeWatches();
      outclause.computePropagation(s);
      return outclause;
   }

   @Override
   public boolean propagatePI(MandatoryLiteralListener l, int p) {
      this.voc.watch(p, this);
      BigInteger coefP;
      if (this.litToCoeffs == null) {
         int indiceP = 0;

         while ((this.lits[indiceP] ^ 1) != p) {
            indiceP++;
         }

         coefP = this.coefs[indiceP];
      } else {
         coefP = this.litToCoeffs.get(p ^ 1);
      }

      BigInteger newcumul = this.watchCumul.subtract(coefP);
      this.voc.undos(p).push(this);
      this.watchCumul = newcumul;
      int ind = 0;

      for (BigInteger limit = this.watchCumul.subtract(this.degree); ind < this.coefs.length && limit.compareTo(this.coefs[ind]) < 0; ind++) {
         if (this.voc.isSatisfied(this.lits[ind])) {
            l.isMandatory(this.lits[ind]);
         }
      }

      return true;
   }

   @Override
   public int getAssertionLevel(IVecInt trail, int decisionLevel) {
      throw new UnsupportedOperationException("To be done");
   }
}
