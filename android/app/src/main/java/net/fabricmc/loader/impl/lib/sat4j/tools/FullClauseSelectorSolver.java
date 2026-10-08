package net.fabricmc.loader.impl.lib.sat4j.tools;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IConstr;
import net.fabricmc.loader.impl.lib.sat4j.specs.ISolver;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;

public class FullClauseSelectorSolver<T extends ISolver> extends AbstractClauseSelectorSolver<T> {
   private final Map<Integer, IConstr> constrs = new HashMap<>();
   private final IVecInt lastClause = new VecInt();
   private IConstr lastConstr;
   private final boolean skipDuplicatedEntries;

   public FullClauseSelectorSolver(T solver, boolean skipDuplicatedEntries) {
      super(solver);
      this.skipDuplicatedEntries = skipDuplicatedEntries;
   }

   public IConstr addControlableClause(IVecInt literals) throws ContradictionException {
      if (this.skipDuplicatedEntries) {
         if (literals.equals(this.lastClause)) {
            return null;
         }

         this.lastClause.clear();
         literals.copyTo(this.lastClause);
      }

      int newvar = this.createNewVar(literals);
      literals.push(newvar);
      this.lastConstr = super.addClause(literals);
      if (this.lastConstr == null) {
         this.discardLastestVar();
      } else {
         this.constrs.put(newvar, this.lastConstr);
      }

      return this.lastConstr;
   }

   @Override
   public IConstr addClause(IVecInt literals) throws ContradictionException {
      return this.addControlableClause(literals);
   }

   @Override
   public int[] model() {
      int[] fullmodel = super.modelWithInternalVariables();
      if (fullmodel == null) {
         return null;
      }

      int[] model = new int[fullmodel.length - this.constrs.size()];
      int j = 0;

      for (int element : fullmodel) {
         if (this.constrs.get(Math.abs(element)) == null) {
            model[j++] = element;
         }
      }

      return model;
   }

   @Override
   public Collection<Integer> getAddedVars() {
      return this.constrs.keySet();
   }

   public IConstr getLastConstr() {
      return this.lastConstr;
   }

   public void setLastConstr(IConstr lastConstr) {
      this.lastConstr = lastConstr;
   }

   public Map<Integer, IConstr> getConstrs() {
      return this.constrs;
   }

   public IVecInt getLastClause() {
      return this.lastClause;
   }
}
