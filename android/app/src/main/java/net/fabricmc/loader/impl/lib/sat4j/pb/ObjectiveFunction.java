package net.fabricmc.loader.impl.lib.sat4j.pb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.impl.lib.sat4j.core.ReadOnlyVec;
import net.fabricmc.loader.impl.lib.sat4j.core.ReadOnlyVecInt;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.ISolver;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.RandomAccessModel;

public class ObjectiveFunction implements Serializable {
   private IVec<BigInteger> coeffs;
   private IVecInt vars;
   private BigInteger correction = BigInteger.ZERO;
   private BigInteger correctionFactor = BigInteger.ONE;

   public ObjectiveFunction() {
      this.vars = new VecInt();
      this.coeffs = new Vec<>();
   }

   public ObjectiveFunction(IVecInt vars, IVec<BigInteger> coeffs) {
      this.vars = new ReadOnlyVecInt(vars);
      this.coeffs = new ReadOnlyVec<>(coeffs);
   }

   public BigInteger calculateDegree(RandomAccessModel lazyModel) {
      BigInteger tempDegree = BigInteger.ZERO;

      for (int i = 0; i < this.vars.size(); i++) {
         BigInteger coeff = this.coeffs.get(i);
         if (this.varInModel(this.vars.get(i), lazyModel) || coeff.signum() < 0 && !this.varInModel(-this.vars.get(i), lazyModel)) {
            tempDegree = tempDegree.add(coeff);
         }
      }

      return tempDegree;
   }

   public BigInteger calculateDegreeImplicant(ISolver solver) {
      BigInteger tempDegree = BigInteger.ZERO;

      for (int i = 0; i < this.vars.size(); i++) {
         BigInteger coeff = this.coeffs.get(i);
         if (solver.primeImplicant(this.vars.get(i)) || coeff.signum() < 0 && !solver.primeImplicant(-this.vars.get(i))) {
            tempDegree = tempDegree.add(coeff);
         }
      }

      return tempDegree;
   }

   private boolean varInModel(int var, RandomAccessModel lazyModel) {
      return var > 0 ? lazyModel.model(var) : !lazyModel.model(-var);
   }

   public IVec<BigInteger> getCoeffs() {
      return this.coeffs;
   }

   public IVecInt getVars() {
      return this.vars;
   }

   public BigInteger getCorrectionOffset() {
      return this.correction;
   }

   public BigInteger getCorrectionFactor() {
      return this.correctionFactor;
   }

   @Override
   public String toString() {
      StringBuilder stb = new StringBuilder();
      IVecInt lits = this.getVars();
      IVec<BigInteger> coefs = this.getCoeffs();

      for (int i = 0; i < lits.size(); i++) {
         BigInteger coef = coefs.get(i);
         int lit = lits.get(i);
         if (lit < 0) {
            lit = -lit;
            coef = coef.negate();
         }

         stb.append((coef.signum() < 0 ? "" : "+") + coef + " x" + lit + " ");
      }

      return stb.toString();
   }

   @Override
   public int hashCode() {
      return this.coeffs.hashCode() / 3 + this.vars.hashCode() / 3 + this.correction.hashCode() / 3;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof ObjectiveFunction)) {
         return false;
      }

      ObjectiveFunction of = (ObjectiveFunction)obj;
      return of.correction.equals(this.correction) && of.coeffs.equals(this.coeffs) && of.vars.equals(this.vars);
   }

   public Map<Integer, BigInteger> toMap() {
      Map<Integer, BigInteger> map = new HashMap<>();

      for (int i = 0; i < this.vars.size(); i++) {
         map.put(this.vars.get(i), this.coeffs.get(i));
      }

      return map;
   }
}
