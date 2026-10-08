package net.fabricmc.loader.impl.lib.sat4j.pb.constraints;

public class CompetResolutionPBLongMixedWLClauseCardConstrDataStructure extends AbstractPBClauseCardConstrDataStructure {
   public CompetResolutionPBLongMixedWLClauseCardConstrDataStructure() {
      super(new UnitBinaryWLClauseConstructor(), new MinCardConstructor(), new MaxLongWatchPBConstructor());
   }
}
