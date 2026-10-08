package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public class InternalMapPBStructure {
   private final IVecInt lits;
   private final IVec<BigInteger> coefs;
   private IVecInt allLits;

   InternalMapPBStructure(int size) {
      assert size > 0;
      this.allLits = new VecInt(size, -1);
      this.coefs = new Vec<>();
      this.lits = new VecInt();
   }

   BigInteger get(int lit) {
      assert this.allLits.get(lit) != -1;
      return this.coefs.get(this.allLits.get(lit));
   }

   int getLit(int indLit) {
      assert indLit < this.lits.size();
      return this.lits.get(indLit);
   }

   BigInteger getCoef(int indLit) {
      assert indLit < this.coefs.size();
      return this.coefs.get(indLit);
   }

   boolean containsKey(int lit) {
      return this.allLits.get(lit) != -1;
   }

   int size() {
      return this.lits.size();
   }

   void put(int lit, BigInteger newValue) {
      int indLit = this.allLits.get(lit);
      if (indLit != -1) {
         this.coefs.set(indLit, newValue);
      } else {
         this.lits.push(lit);
         this.coefs.push(newValue);
         this.allLits.set(lit, this.lits.size() - 1);
      }
   }

   void changeCoef(int indLit, BigInteger newValue) {
      assert indLit <= this.coefs.size();
      this.coefs.set(indLit, newValue);
   }

   void remove(int lit) {
      int indLit = this.allLits.get(lit);
      if (indLit != -1) {
         int tmp = this.lits.last();
         this.coefs.delete(indLit);
         this.lits.delete(indLit);
         this.allLits.set(tmp, indLit);
         this.allLits.set(lit, -1);
      }
   }

   void copyCoefs(BigInteger[] dest) {
      this.coefs.copyTo(dest);
   }

   void copyLits(int[] dest) {
      this.lits.copyTo(dest);
   }
}
