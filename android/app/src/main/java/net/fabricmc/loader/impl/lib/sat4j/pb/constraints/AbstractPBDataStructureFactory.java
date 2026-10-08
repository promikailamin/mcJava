package net.fabricmc.loader.impl.lib.sat4j.pb.constraints;

import java.math.BigInteger;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.AbstractDataStructureFactory;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.Clauses;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.LearntBinaryClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.LearntHTClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.Lits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.OriginalBinaryClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.OriginalHTClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf.UnitClause;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb.AtLeastPB;
import net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb.IDataStructurePB;
import net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb.Pseudos;
import net.fabricmc.loader.impl.lib.sat4j.pb.core.PBDataStructureFactory;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public abstract class AbstractPBDataStructureFactory extends AbstractDataStructureFactory implements PBDataStructureFactory {
   public static final AbstractPBDataStructureFactory.INormalizer FOR_COMPETITION = new AbstractPBDataStructureFactory.INormalizer() {
      @Override
      public PBContainer nice(IVecInt literals, IVec<BigInteger> coefs, boolean moreThan, BigInteger degree, ILits voc) throws ContradictionException {
         if (literals.size() != coefs.size()) {
            throw new IllegalArgumentException("Number of coeff and literals are different!!!");
         }

         IVecInt cliterals = new VecInt(literals.size());
         literals.copyTo(cliterals);
         IVec<BigInteger> ccoefs = new Vec<>(literals.size());
         coefs.copyTo(ccoefs);
         int i = 0;

         while (i < cliterals.size()) {
            if (ccoefs.get(i).equals(BigInteger.ZERO)) {
               cliterals.delete(i);
               ccoefs.delete(i);
            } else if (voc.isSatisfied(cliterals.get(i))) {
               degree = degree.subtract(ccoefs.get(i));
               cliterals.delete(i);
               ccoefs.delete(i);
            } else if (voc.isFalsified(cliterals.get(i))) {
               cliterals.delete(i);
               ccoefs.delete(i);
            } else {
               i++;
            }
         }

         int[] theLits = new int[cliterals.size()];
         cliterals.copyTo(theLits);
         BigInteger[] normCoefs = new BigInteger[ccoefs.size()];
         ccoefs.copyTo(normCoefs);
         BigInteger degRes = Pseudos.niceParametersForCompetition(theLits, normCoefs, moreThan, degree);
         return new PBContainer(theLits, normCoefs, degRes);
      }
   };
   public static final AbstractPBDataStructureFactory.INormalizer NO_COMPETITION = new AbstractPBDataStructureFactory.INormalizer() {
      @Override
      public PBContainer nice(IVecInt literals, IVec<BigInteger> coefs, boolean moreThan, BigInteger degree, ILits voc) throws ContradictionException {
         IVecInt cliterals = new VecInt(literals.size());
         literals.copyTo(cliterals);
         IVec<BigInteger> ccoefs = new Vec<>(literals.size());
         coefs.copyTo(ccoefs);
         int i = 0;

         while (i < cliterals.size()) {
            if (voc.isSatisfied(cliterals.get(i))) {
               degree = degree.subtract(ccoefs.get(i));
               cliterals.delete(i);
               ccoefs.delete(i);
            } else if (voc.isFalsified(cliterals.get(i))) {
               cliterals.delete(i);
               ccoefs.delete(i);
            } else {
               i++;
            }
         }

         IDataStructurePB res = Pseudos.niceParameters(cliterals, ccoefs, moreThan, degree, voc);
         int size = res.size();
         int[] theLits = new int[size];
         BigInteger[] theCoefs = new BigInteger[size];
         res.buildConstraintFromMapPb(theLits, theCoefs);
         BigInteger theDegree = res.getDegree();
         return new PBContainer(theLits, theCoefs, theDegree);
      }
   };
   private AbstractPBDataStructureFactory.INormalizer norm = FOR_COMPETITION;

   protected AbstractPBDataStructureFactory.INormalizer getNormalizer() {
      return this.norm;
   }

   @Override
   public Constr createClause(IVecInt literals) throws ContradictionException {
      IVecInt v = Clauses.sanityCheck(literals, this.getVocabulary(), this.solver);
      if (v == null) {
         return null;
      } else if (v.size() == 1) {
         return new UnitClause(v.last());
      } else {
         return v.size() == 2
            ? OriginalBinaryClause.brandNewClause(this.solver, this.getVocabulary(), v)
            : OriginalHTClause.brandNewClause(this.solver, this.getVocabulary(), v);
      }
   }

   @Override
   public Constr createUnregisteredClause(IVecInt literals) {
      if (literals.size() == 1) {
         return new UnitClause(literals.last(), true);
      } else {
         return literals.size() == 2 ? new LearntBinaryClause(literals, this.getVocabulary()) : new LearntHTClause(literals, this.getVocabulary());
      }
   }

   @Override
   public Constr createCardinalityConstraint(IVecInt literals, int degree) throws ContradictionException {
      return AtLeastPB.atLeastNew(this.solver, this.getVocabulary(), literals, degree);
   }

   @Override
   public Constr createPseudoBooleanConstraint(IVecInt literals, IVec<BigInteger> coefs, boolean moreThan, BigInteger degree) throws ContradictionException {
      PBContainer res = this.getNormalizer().nice(literals, coefs, moreThan, degree, this.getVocabulary());
      return this.constraintFactory(res.lits, res.coefs, res.degree);
   }

   protected abstract Constr constraintFactory(int[] var1, BigInteger[] var2, BigInteger var3) throws ContradictionException;

   @Override
   protected ILits createLits() {
      return new Lits();
   }

   interface INormalizer {
      PBContainer nice(IVecInt var1, IVec<BigInteger> var2, boolean var3, BigInteger var4, ILits var5) throws ContradictionException;
   }
}
