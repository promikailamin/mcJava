package net.fabricmc.loader.impl.lib.sat4j.pb;

import net.fabricmc.loader.impl.lib.sat4j.core.ASolverFactory;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Solver;
import net.fabricmc.loader.impl.lib.sat4j.minisat.learning.MiniSATLearning;
import net.fabricmc.loader.impl.lib.sat4j.minisat.orders.RSATPhaseSelectionStrategy;
import net.fabricmc.loader.impl.lib.sat4j.minisat.restarts.ArminRestarts;
import net.fabricmc.loader.impl.lib.sat4j.minisat.restarts.Glucose21Restarts;
import net.fabricmc.loader.impl.lib.sat4j.pb.constraints.CompetResolutionPBLongMixedWLClauseCardConstrDataStructure;
import net.fabricmc.loader.impl.lib.sat4j.pb.core.PBDataStructureFactory;
import net.fabricmc.loader.impl.lib.sat4j.pb.core.PBSolver;
import net.fabricmc.loader.impl.lib.sat4j.pb.core.PBSolverResolution;
import net.fabricmc.loader.impl.lib.sat4j.pb.orders.VarOrderHeapObjective;

public final class SolverFactory extends ASolverFactory<IPBSolver> {
   private SolverFactory() {
   }

   public static PBSolverResolution newCompetPBResLongWLMixedConstraintsObjectiveExpSimp() {
      return newCompetPBResMixedConstraintsObjectiveExpSimp(new CompetResolutionPBLongMixedWLClauseCardConstrDataStructure());
   }

   public static PBSolverResolution newCompetPBResMixedConstraintsObjectiveExpSimp(PBDataStructureFactory dsf) {
      MiniSATLearning<PBDataStructureFactory> learning = new MiniSATLearning<>();
      PBSolverResolution solver = new PBSolverResolution(learning, dsf, new VarOrderHeapObjective(new RSATPhaseSelectionStrategy()), new ArminRestarts());
      learning.setDataStructureFactory(solver.getDSFactory());
      learning.setVarActivityListener(solver);
      solver.setSimplifier(solver.EXPENSIVE_SIMPLIFICATION);
      return solver;
   }

   public static PBSolverResolution newResolutionGlucose() {
      PBSolverResolution solver = newCompetPBResLongWLMixedConstraintsObjectiveExpSimp();
      solver.setSimplifier(Solver.NO_SIMPLIFICATION);
      solver.setRestartStrategy(new Glucose21Restarts());
      solver.setLearnedConstraintsDeletionStrategy(solver.lbd_based);
      return solver;
   }

   public static PBSolverResolution newResolutionGlucose21() {
      PBSolverResolution solver = newResolutionGlucose();
      solver.setRestartStrategy(new Glucose21Restarts());
      return solver;
   }

   public static PBSolver newDefault() {
      return newResolutionGlucose21();
   }

   public static IPBSolver newDefaultOptimizer() {
      return new OptToPBSATAdapter(new PseudoOptDecorator(newDefault()));
   }
}
