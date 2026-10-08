package net.fabricmc.loader.impl.lib.sat4j.minisat.constraints.cnf;

import net.fabricmc.loader.impl.lib.sat4j.core.LiteralsUtils;
import net.fabricmc.loader.impl.lib.sat4j.minisat.core.ILits;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVecInt;
import net.fabricmc.loader.impl.lib.sat4j.specs.MandatoryLiteralListener;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public class OriginalHTClause extends HTClause {
   private int savedindexhead;
   private int savedindextail;

   public OriginalHTClause(IVecInt ps, ILits voc) {
      super(ps, voc);
   }

   @Override
   public void register() {
      this.voc.watch(LiteralsUtils.neg(this.head), this);
      this.voc.watch(LiteralsUtils.neg(this.tail), this);
   }

   @Override
   public boolean learnt() {
      return false;
   }

   @Override
   public void setLearnt() {
   }

   public static OriginalHTClause brandNewClause(UnitPropagationListener s, ILits voc, IVecInt literals) {
      OriginalHTClause c = new OriginalHTClause(literals, voc);
      c.register();
      return c;
   }

   @Override
   public void incActivity(double claInc) {
   }

   @Override
   public void setActivity(double claInc) {
   }

   @Override
   public boolean propagatePI(MandatoryLiteralListener l, int p) {
      if (this.head == LiteralsUtils.neg(p)) {
         int[] mylits = this.middleLits;

         while (this.savedindexhead < mylits.length && this.voc.isFalsified(mylits[this.savedindexhead])) {
            this.savedindexhead++;
         }

         assert this.savedindexhead <= mylits.length;
         if (this.savedindexhead == mylits.length) {
            l.isMandatory(this.tail);
         } else {
            this.head = mylits[this.savedindexhead];
            mylits[this.savedindexhead] = LiteralsUtils.neg(p);
            this.voc.watch(LiteralsUtils.neg(this.head), this);
         }
      } else {
         assert this.tail == LiteralsUtils.neg(p);
         int[] mylits = this.middleLits;

         while (this.savedindextail >= 0 && this.voc.isFalsified(mylits[this.savedindextail])) {
            this.savedindextail--;
         }

         assert -1 <= this.savedindextail;
         if (-1 == this.savedindextail) {
            l.isMandatory(this.head);
         } else {
            this.tail = mylits[this.savedindextail];
            mylits[this.savedindextail] = LiteralsUtils.neg(p);
            this.voc.watch(LiteralsUtils.neg(this.tail), this);
         }
      }

      return true;
   }

   @Override
   public boolean propagate(UnitPropagationListener s, int p) {
      this.savedindexhead = 0;
      this.savedindextail = this.middleLits.length - 1;
      return super.propagate(s, p);
   }
}
