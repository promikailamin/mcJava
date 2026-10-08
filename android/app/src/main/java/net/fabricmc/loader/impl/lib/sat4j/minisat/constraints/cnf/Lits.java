package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import java.io.Serializable;
import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.Undoable;
import net.fabricmc.loader.impl.lib.sat4j.specs.Constr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;
import net.fabricmc.loader.impl.lib.sat4j.specs.Propagatable;

public final class Lits implements Serializable, ILits {
   private boolean[] pool = new boolean[1];
   private int realnVars = 0;
   private IVec<Propagatable>[] watches = new IVec[0];
   private int[] level = new int[0];
   private int[] trailPosition = new int[0];
   private Constr[] reason = new Constr[0];
   private int maxvarid = 0;
   private IVec<Undoable>[] undos = new IVec[0];
   private boolean[] falsified = new boolean[0];

   public Lits() {
      this.init(128);
   }

   public void init(int nvar) {
      if (nvar >= this.pool.length) {
         assert nvar >= 0;
         int nvars = nvar + 1;
         boolean[] npool = new boolean[nvars];
         System.arraycopy(this.pool, 0, npool, 0, this.pool.length);
         this.pool = npool;
         int[] nlevel = new int[nvars];
         System.arraycopy(this.level, 0, nlevel, 0, this.level.length);
         this.level = nlevel;
         int[] ntrailPosition = new int[nvars];
         System.arraycopy(this.trailPosition, 0, ntrailPosition, 0, this.trailPosition.length);
         this.trailPosition = ntrailPosition;
         IVec<Propagatable>[] nwatches = new IVec[2 * nvars];
         System.arraycopy(this.watches, 0, nwatches, 0, this.watches.length);
         this.watches = nwatches;
         IVec<Undoable>[] nundos = new IVec[nvars];
         System.arraycopy(this.undos, 0, nundos, 0, this.undos.length);
         this.undos = nundos;
         Constr[] nreason = new Constr[nvars];
         System.arraycopy(this.reason, 0, nreason, 0, this.reason.length);
         this.reason = nreason;
         boolean[] newFalsified = new boolean[2 * nvars];
         System.arraycopy(this.falsified, 0, newFalsified, 0, this.falsified.length);
         this.falsified = newFalsified;
      }
   }

   @Override
   public int getFromPool(int x) {
      int var = Math.abs(x);
      if (var >= this.pool.length) {
         this.init(Math.max(var, this.pool.length << 1));
      }

      assert var < this.pool.length;
      if (var > this.maxvarid) {
         this.maxvarid = var;
      }

      int lit = LiteralsUtils.toInternal(x);
      assert lit > 1;
      if (!this.pool[var]) {
         this.realnVars++;
         this.pool[var] = true;
         this.watches[var << 1] = new Vec<>();
         this.watches[var << 1 | 1] = new Vec<>();
         this.undos[var] = new Vec<>();
         this.level[var] = -1;
         this.trailPosition[var] = -1;
         this.falsified[var << 1] = false;
         this.falsified[var << 1 | 1] = false;
      }

      return lit;
   }

   @Override
   public boolean belongsToPool(int x) {
      assert x > 0;
      return x >= this.pool.length ? false : this.pool[x];
   }

   @Override
   public void resetPool() {
      for (int i = 0; i < this.pool.length; i++) {
         if (this.pool[i]) {
            this.reset(i << 1);
         }
      }

      this.maxvarid = 0;
      this.realnVars = 0;
   }

   public void ensurePool(int howmany) {
      if (howmany >= this.pool.length) {
         this.init(Math.max(howmany, this.pool.length << 1));
      }

      if (this.maxvarid < howmany) {
         this.maxvarid = howmany;
      }
   }

   @Override
   public void unassign(int lit) {
      assert this.falsified[lit] || this.falsified[lit ^ 1];
      this.falsified[lit] = false;
      this.falsified[lit ^ 1] = false;
   }

   @Override
   public void satisfies(int lit) {
      assert !this.falsified[lit] && !this.falsified[lit ^ 1];
      this.falsified[lit] = false;
      this.falsified[lit ^ 1] = true;
   }

   @Override
   public void forgets(int var) {
      this.falsified[var << 1] = true;
      this.falsified[var << 1 ^ 1] = true;
   }

   @Override
   public boolean isSatisfied(int lit) {
      return this.falsified[lit ^ 1];
   }

   @Override
   public boolean isFalsified(int lit) {
      return this.falsified[lit];
   }

   @Override
   public boolean isUnassigned(int lit) {
      return !this.falsified[lit] && !this.falsified[lit ^ 1];
   }

   @Override
   public String valueToString(int lit) {
      if (this.isUnassigned(lit)) {
         return "?";
      } else {
         return this.isSatisfied(lit) ? "T" : "F";
      }
   }

   @Override
   public int nVars() {
      return this.maxvarid;
   }

   public static String toString(int lit) {
      return ((lit & 1) == 0 ? "" : "-") + (lit >> 1);
   }

   public static String toStringX(int lit) {
      return ((lit & 1) == 0 ? "+" : "-") + "x" + (lit >> 1);
   }

   public void reset(int lit) {
      this.watches[lit].clear();
      this.watches[lit ^ 1].clear();
      this.level[lit >> 1] = -1;
      this.trailPosition[lit >> 1] = -1;
      this.reason[lit >> 1] = null;
      this.undos[lit >> 1].clear();
      this.falsified[lit] = false;
      this.falsified[lit ^ 1] = false;
      this.pool[lit >> 1] = false;
   }

   @Override
   public int getLevel(int lit) {
      return this.level[lit >> 1];
   }

   @Override
   public void setLevel(int lit, int l) {
      this.level[lit >> 1] = l;
   }

   @Override
   public Constr getReason(int lit) {
      return this.reason[lit >> 1];
   }

   @Override
   public void setReason(int lit, Constr r) {
      this.reason[lit >> 1] = r;
   }

   @Override
   public IVec<Undoable> undos(int lit) {
      return this.undos[lit >> 1];
   }

   @Override
   public void watch(int lit, Propagatable c) {
      this.watches[lit].push(c);
   }

   @Override
   public IVec<Propagatable> watches(int lit) {
      return this.watches[lit];
   }

   @Override
   public int realnVars() {
      return this.realnVars;
   }

   @Override
   public int nextFreeVarId(boolean reserve) {
      if (reserve) {
         this.ensurePool(this.maxvarid + 1);
         return this.maxvarid;
      } else {
         return this.maxvarid + 1;
      }
   }

   @Override
   public void setTrailPosition(int lit, int position) {
      this.trailPosition[lit >> 1] = position;
   }

   @Override
   public int getTrailPosition(int lit) {
      return this.trailPosition[lit >> 1];
   }
}
