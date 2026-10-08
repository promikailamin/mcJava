package net.fabricmc.loader.impl.lib.sat4j.pb.constraints;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb.MaxWatchPb;
import net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb.MaxWatchPbLong;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public class MaxLongWatchPBConstructor implements IPBConstructor {
   @Override
   public Constr constructPB(UnitPropagationListener solver, ILits voc, int[] theLits, BigInteger[] coefs, BigInteger degree, BigInteger sumCoefs) throws ContradictionException {
      Constr constr;
      if (sumCoefs.bitLength() < 64) {
         constr = MaxWatchPbLong.normalizedMaxWatchPbNew(solver, voc, theLits, coefs, degree, sumCoefs);
      } else {
         constr = MaxWatchPb.normalizedMaxWatchPbNew(solver, voc, theLits, coefs, degree, sumCoefs);
      }

      return constr == null ? Constr.TAUTOLOGY : constr;
   }
}
