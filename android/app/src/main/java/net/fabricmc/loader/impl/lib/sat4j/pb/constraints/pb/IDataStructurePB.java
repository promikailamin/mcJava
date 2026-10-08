package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

import java.math.BigInteger;

public interface IDataStructurePB {
   IDataStructurePB TAUTOLOGY = new IDataStructurePB() {
      @Override
      public int size() {
         return 0;
      }

      @Override
      public BigInteger saturation() {
         return BigInteger.ZERO;
      }

      @Override
      public BigInteger getDegree() {
         return BigInteger.ZERO;
      }

      @Override
      public BigInteger cuttingPlane(int[] lits, BigInteger[] reducedCoefs, BigInteger deg) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void buildConstraintFromMapPb(int[] resLits, BigInteger[] resCoefs) {
      }
   };

   BigInteger saturation();

   BigInteger cuttingPlane(int[] var1, BigInteger[] var2, BigInteger var3);

   void buildConstraintFromMapPb(int[] var1, BigInteger[] var2);

   BigInteger getDegree();

   int size();
}
