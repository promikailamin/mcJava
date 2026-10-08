package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.DataStructureFactory;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Learner;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.Propagatable;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public abstract class AbstractDataStructureFactory implements Serializable, DataStructureFactory {
   protected ILits lits;
   private final IVec<Propagatable> tmp = new Vec<>();
   protected UnitPropagationListener solver;
   protected Learner learner;

   protected AbstractDataStructureFactory() {
      this.lits = this.createLits();
   }

   protected abstract ILits createLits();

   @Override
   public ILits getVocabulary() {
      return this.lits;
   }

   @Override
   public void setUnitPropagationListener(UnitPropagationListener s) {
      this.solver = s;
   }

   @Override
   public void setLearner(Learner learner) {
      this.learner = learner;
   }

   @Override
   public void reset() {
   }

   @Override
   public void learnConstraint(Constr constr) {
      this.learner.learn(constr);
   }

   @Override
   public Constr createCardinalityConstraint(IVecInt literals, int degree) throws ContradictionException {
      throw new UnsupportedOperationException();
   }
}
