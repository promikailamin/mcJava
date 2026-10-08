package net.fabricmc.loader.impl.discovery;

import net.fabricmc.loader.api.metadata.ModDependency;

class Explanation implements Comparable<Explanation> {
   private static int nextCmpId;
   final Explanation.ErrorKind error;
   final ModCandidateImpl mod;
   final ModDependency dep;
   final String data;
   private final int cmpId;

   Explanation(Explanation.ErrorKind error, ModCandidateImpl mod) {
      this(error, mod, null, null);
   }

   Explanation(Explanation.ErrorKind error, ModCandidateImpl mod, ModDependency dep) {
      this(error, mod, dep, null);
   }

   Explanation(Explanation.ErrorKind error, String data) {
      this(error, null, data);
   }

   Explanation(Explanation.ErrorKind error, ModCandidateImpl mod, String data) {
      this(error, mod, null, data);
   }

   private Explanation(Explanation.ErrorKind error, ModCandidateImpl mod, ModDependency dep, String data) {
      this.error = error;
      this.mod = mod;
      this.dep = dep;
      this.data = data;
      this.cmpId = nextCmpId++;
   }

   public int compareTo(Explanation o) {
      return Integer.compare(this.cmpId, o.cmpId);
   }

   @Override
   public String toString() {
      if (this.mod == null) {
         return String.format("%s %s", this.error, this.data);
      } else {
         return this.dep == null ? String.format("%s %s", this.error, this.mod) : String.format("%s %s %s", this.error, this.mod, this.dep);
      }
   }

   enum ErrorKind {
      PRESELECT_HARD_DEP(true),
      PRESELECT_SOFT_DEP(true),
      PRESELECT_NEG_HARD_DEP(true),
      PRESELECT_FORCELOAD(false),
      HARD_DEP_INCOMPATIBLE_PRESELECTED(true),
      HARD_DEP_NO_CANDIDATE(true),
      HARD_DEP(true),
      SOFT_DEP(true),
      NEG_HARD_DEP(true),
      NESTED_FORCELOAD(false),
      NESTED_REQ_PARENT(false),
      ROOT_FORCELOAD_SINGLE(false),
      ROOT_FORCELOAD(false),
      UNIQUE_ID(false);

      final boolean isDependencyError;

      ErrorKind(boolean isDependencyError) {
         this.isDependencyError = isDependencyError;
      }
   }
}
