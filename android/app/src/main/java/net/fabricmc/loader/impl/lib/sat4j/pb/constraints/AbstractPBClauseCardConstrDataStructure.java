package net.fabricmc.loader.impl.lib.sat4j.pb.constraints;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.Clauses;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public abstract class AbstractPBClauseCardConstrDataStructure extends AbstractPBDataStructureFactory {
   static final BigInteger MAX_INT_VALUE = BigInteger.valueOf(2147483647L);
   private final IPBConstructor ipbc;
   private final ICardConstructor icardc;
   private final IClauseConstructor iclausec;

   AbstractPBClauseCardConstrDataStructure(IClauseConstructor iclausec, ICardConstructor icardc, IPBConstructor ipbc) {
      this.iclausec = iclausec;
      this.icardc = icardc;
      this.ipbc = ipbc;
   }

   @Override
   public Constr createClause(IVecInt literals) throws ContradictionException {
      IVecInt v = Clauses.sanityCheck(literals, this.getVocabulary(), this.solver);
      return this.constructClause(v);
   }

   @Override
   public Constr createUnregisteredClause(IVecInt literals) {
      return this.constructLearntClause(literals);
   }

   @Override
   public Constr createCardinalityConstraint(IVecInt literals, int degree) throws ContradictionException {
      return this.constructCard(literals, degree);
   }

   @Override
   protected Constr constraintFactory(int[] literals, BigInteger[] coefs, BigInteger degree) throws ContradictionException {
      if (literals.length == 0 && degree.signum() <= 0) {
         return Constr.TAUTOLOGY;
      } else if (degree.equals(BigInteger.ONE)) {
         IVecInt v = Clauses.sanityCheck(new VecInt(literals), this.getVocabulary(), this.solver);
         return v == Constr.TAUTOLOGY ? null : this.constructClause(v);
      } else if (coefficientsEqualTo(BigInteger.ONE, coefs)) {
         assert degree.compareTo(MAX_INT_VALUE) < 0;
         return this.constructCard(new VecInt(literals), degree.intValue());
      } else {
         return this.constructPB(literals, coefs, degree);
      }
   }

   static boolean coefficientsEqualTo(BigInteger value, BigInteger[] coefs) {
      for (int i = 0; i < coefs.length; i++) {
         if (!coefs[i].equals(value)) {
            return false;
         }
      }

      return true;
   }

   protected Constr constructClause(IVecInt v) {
      return this.iclausec.constructClause(this.solver, this.getVocabulary(), v);
   }

   protected Constr constructCard(IVecInt theLits, int degree) throws ContradictionException {
      return this.icardc.constructCard(this.solver, this.getVocabulary(), theLits, degree);
   }

   protected Constr constructPB(int[] theLits, BigInteger[] coefs, BigInteger degree) throws ContradictionException {
      return this.ipbc.constructPB(this.solver, this.getVocabulary(), theLits, coefs, degree, sumOfCoefficients(coefs));
   }

   protected Constr constructLearntClause(IVecInt literals) {
      return this.iclausec.constructLearntClause(this.getVocabulary(), literals);
   }

   public static final BigInteger sumOfCoefficients(BigInteger[] coefs) {
      BigInteger sumCoefs = BigInteger.ZERO;

      for (BigInteger c : coefs) {
         sumCoefs = sumCoefs.add(c);
      }

      return sumCoefs;
   }
}
