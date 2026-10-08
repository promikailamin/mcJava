package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import java.io.PrintWriter;
import java.io.Serializable;

public class SolverStats implements Serializable {
   private int starts;
   private long decisions;
   private long propagations;
   private long inspects;
   private long conflicts;
   private long learnedliterals;
   private long learnedbinaryclauses;
   private long learnedternaryclauses;
   private long learnedclauses;
   private long ignoredclauses;
   private long rootSimplifications;
   private long reducedliterals;
   private long changedreason;
   private int reduceddb;
   private int shortcuts;
   private long updateLBD;
   private int importedUnits;

   public void reset() {
      this.starts = 0;
      this.decisions = 0L;
      this.propagations = 0L;
      this.inspects = 0L;
      this.shortcuts = 0;
      this.conflicts = 0L;
      this.learnedliterals = 0L;
      this.learnedclauses = 0L;
      this.ignoredclauses = 0L;
      this.learnedbinaryclauses = 0L;
      this.learnedternaryclauses = 0L;
      this.rootSimplifications = 0L;
      this.reducedliterals = 0L;
      this.changedreason = 0L;
      this.reduceddb = 0;
      this.updateLBD = 0L;
      this.importedUnits = 0;
   }

   public void printStat(PrintWriter out, String prefix) {
      out.println(prefix + "starts\t\t: " + this.getStarts());
      out.println(prefix + "conflicts\t\t: " + this.conflicts);
      out.println(prefix + "decisions\t\t: " + this.decisions);
      out.println(prefix + "propagations\t\t: " + this.propagations);
      out.println(prefix + "inspects\t\t: " + this.inspects);
      out.println(prefix + "shortcuts\t\t: " + this.shortcuts);
      out.println(prefix + "learnt literals\t: " + this.learnedliterals);
      out.println(prefix + "learnt binary clauses\t: " + this.learnedbinaryclauses);
      out.println(prefix + "learnt ternary clauses\t: " + this.learnedternaryclauses);
      out.println(prefix + "learnt constraints\t: " + this.learnedclauses);
      out.println(prefix + "ignored constraints\t: " + this.ignoredclauses);
      out.println(prefix + "root simplifications\t: " + this.rootSimplifications);
      out.println(prefix + "removed literals (reason simplification)\t: " + this.reducedliterals);
      out.println(prefix + "reason swapping (by a shorter reason)\t: " + this.changedreason);
      out.println(prefix + "Calls to reduceDB\t: " + this.reduceddb);
      out.println(prefix + "Number of update (reduction) of LBD\t: " + this.updateLBD);
      out.println(prefix + "Imported unit clauses\t: " + this.importedUnits);
   }

   public int getStarts() {
      return this.starts;
   }

   public void incStarts() {
      this.starts++;
   }

   public void incDecisions() {
      this.decisions++;
   }

   public long getPropagations() {
      return this.propagations;
   }

   public void incPropagations() {
      this.propagations++;
   }

   public void incInspects() {
      this.inspects++;
   }

   public long getConflicts() {
      return this.conflicts;
   }

   public void incConflicts() {
      this.conflicts++;
   }

   public void incLearnedliterals() {
      this.learnedliterals++;
   }

   public void incLearnedbinaryclauses() {
      this.learnedbinaryclauses++;
   }

   public void incLearnedternaryclauses() {
      this.learnedternaryclauses++;
   }

   public void incLearnedclauses() {
      this.learnedclauses++;
   }

   public void incRootSimplifications() {
      this.rootSimplifications++;
   }

   public void incReducedliterals(int increment) {
      this.reducedliterals += increment;
   }

   public void incReduceddb() {
      this.reduceddb++;
   }

   public void incUpdateLBD() {
      this.updateLBD++;
   }

   public void incImportedUnits(int increment) {
      this.importedUnits += increment;
   }
}
