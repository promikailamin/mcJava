package net.fabricmc.loader.impl.lib.sat4j.pb.tools;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.fabricmc.loader.impl.lib.sat4j.core.VecInt;
import net.fabricmc.loader.impl.lib.sat4j.pb.IPBSolver;
import net.fabricmc.loader.impl.lib.sat4j.pb.ObjectiveFunction;
import net.fabricmc.loader.impl.lib.sat4j.specs.ContradictionException;
import net.fabricmc.loader.impl.lib.sat4j.specs.IConstr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.TimeoutException;
import net.fabricmc.loader.impl.lib.sat4j.tools.GateTranslator;

public class DependencyHelper<T, C> {
   public static final INegator NO_NEGATION = new INegator() {
      @Override
      public boolean isNegated(Object thing) {
         return false;
      }

      @Override
      public Object unNegate(Object thing) {
         return thing;
      }
   };
   public static final INegator BASIC_NEGATION = new INegator() {
      @Override
      public boolean isNegated(Object thing) {
         return thing instanceof DependencyHelper.Negation;
      }

      @Override
      public Object unNegate(Object thing) {
         return ((DependencyHelper.Negation)thing).getThing();
      }
   };
   private final Map<T, Integer> mapToDimacs = new HashMap<>();
   private final Map<Integer, T> mapToDomain = new HashMap<>();
   final Map<IConstr, C> descs = new HashMap<>();
   private final XplainPB xplain;
   private final GateTranslator gator;
   final IPBSolver solver;
   private INegator negator = BASIC_NEGATION;
   private ObjectiveFunction objFunction;
   private IVecInt objLiterals;
   private IVec<BigInteger> objCoefs;
   private final boolean explanationEnabled;
   private final boolean canonicalOptFunction;

   public DependencyHelper(IPBSolver solver) {
      this(solver, true);
   }

   public DependencyHelper(IPBSolver solver, boolean explanationEnabled) {
      this(solver, explanationEnabled, true);
   }

   public DependencyHelper(IPBSolver solver, boolean explanationEnabled, boolean canonicalOptFunctionEnabled) {
      if (explanationEnabled) {
         this.xplain = new XplainPB(solver);
         this.solver = this.xplain;
      } else {
         this.xplain = null;
         this.solver = solver;
      }

      this.gator = new GateTranslator(this.solver);
      this.explanationEnabled = explanationEnabled;
      this.canonicalOptFunction = canonicalOptFunctionEnabled;
   }

   public void setNegator(INegator negator) {
      this.negator = negator;
   }

   protected int getIntValue(T thing) {
      return this.getIntValue(thing, true);
   }

   protected int getIntValue(T thing, boolean create) {
      boolean negated = this.negator.isNegated(thing);
      T myThing;
      if (negated) {
         myThing = (T)this.negator.unNegate(thing);
      } else {
         myThing = thing;
      }

      Integer intValue = this.mapToDimacs.get(myThing);
      if (intValue == null) {
         if (!create) {
            throw new IllegalArgumentException("" + myThing + " is unknown in the solver!");
         }

         intValue = this.solver.nextFreeVarId(true);
         this.mapToDomain.put(intValue, myThing);
         this.mapToDimacs.put(myThing, intValue);
      }

      return negated ? -intValue : intValue;
   }

   public Collection<T> getASolution() {
      int[] model = this.solver.model();
      Collection<T> toInstall = new ArrayList<>();
      if (model != null) {
         for (int i : model) {
            if (i > 0) {
               toInstall.add(this.mapToDomain.get(i));
            }
         }
      }

      return toInstall;
   }

   public boolean hasASolution() throws TimeoutException {
      return this.solver.isSatisfiable();
   }

   public Set<C> why() throws TimeoutException {
      if (!this.explanationEnabled) {
         throw new UnsupportedOperationException("Explanation not enabled!");
      }

      Collection<IConstr> explanation = this.xplain.explain();
      Set<C> ezexplain = new TreeSet<>();

      for (IConstr constr : explanation) {
         C desc = this.descs.get(constr);
         if (desc != null) {
            ezexplain.add(desc);
         }
      }

      return ezexplain;
   }

   public void setTrue(T thing, C name) throws ContradictionException {
      IConstr constr = this.gator.gateTrue(this.getIntValue(thing));
      if (constr != null) {
         this.descs.put(constr, name);
      }
   }

   public void setFalse(T thing, C name) throws ContradictionException {
      IConstr constr = this.gator.gateFalse(this.getIntValue(thing));
      if (constr != null) {
         this.descs.put(constr, name);
      }
   }

   public ImplicationRHS<T, C> implication(T... lhs) {
      IVecInt clause = new VecInt();

      for (T t : lhs) {
         clause.push(-this.getIntValue(t));
      }

      return new ImplicationRHS<>(this, clause);
   }

   public ImplicationNamer<T, C> atMost(int i, T... things) throws ContradictionException {
      IVec<IConstr> toName = new Vec<>();
      IVecInt literals = new VecInt();

      for (T t : things) {
         literals.push(this.getIntValue(t));
      }

      toName.push(this.solver.addAtMost(literals, i));
      return new ImplicationNamer<>(this, toName);
   }

   public void clause(C name, T... things) throws ContradictionException {
      IVecInt literals = new VecInt(things.length);

      for (T t : things) {
         literals.push(this.getIntValue(t));
      }

      IConstr constr = this.gator.addClause(literals);
      if (constr != null) {
         this.descs.put(constr, name);
      }
   }

   public void setObjectiveFunction(WeightedObject<T>... wobj) {
      this.createObjectivetiveFunctionIfNeeded(wobj.length);

      for (WeightedObject<T> wo : wobj) {
         this.addProperly(wo.thing, wo.getWeight());
      }
   }

   private void addProperly(T thing, BigInteger weight) {
      int lit = this.getIntValue(thing);
      int index;
      if (this.canonicalOptFunction && (index = this.objLiterals.indexOf(lit)) != -1) {
         this.objCoefs.set(index, this.objCoefs.get(index).add(weight));
         if (this.objCoefs.get(index).equals(BigInteger.ZERO)) {
            this.objLiterals.delete(index);
            this.objCoefs.delete(index);
         }
      } else {
         this.objLiterals.push(lit);
         this.objCoefs.push(weight);
      }
   }

   private void createObjectivetiveFunctionIfNeeded(int n) {
      if (this.objFunction == null) {
         this.objLiterals = new VecInt(n);
         this.objCoefs = new Vec<>(n);
         this.objFunction = new ObjectiveFunction(this.objLiterals, this.objCoefs);
         this.solver.setObjectiveFunction(this.objFunction);
      }
   }

   public IPBSolver getSolver() {
      return this.explanationEnabled ? this.xplain.decorated() : this.solver;
   }

   public void reset() {
      this.mapToDimacs.clear();
      this.mapToDomain.clear();
      this.descs.clear();
      this.solver.reset();
      if (this.objLiterals != null) {
         this.objLiterals.clear();
         this.objCoefs.clear();
      }
   }

   private static final class Negation {
      private final Object thing;

      Object getThing() {
         return this.thing;
      }

      @Override
      public String toString() {
         return "-" + this.thing;
      }
   }
}
