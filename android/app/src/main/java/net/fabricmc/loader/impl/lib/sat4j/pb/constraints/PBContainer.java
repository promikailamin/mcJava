package net.fabricmc.loader.impl.lib.sat4j.pb.constraints;

import java.math.BigInteger;

final class PBContainer {
   final int[] lits;
   final BigInteger[] coefs;
   final BigInteger degree;

   PBContainer(int[] lits, BigInteger[] coefs, BigInteger degree) {
      this.lits = lits;
      this.coefs = coefs;
      this.degree = degree;
   }
}
