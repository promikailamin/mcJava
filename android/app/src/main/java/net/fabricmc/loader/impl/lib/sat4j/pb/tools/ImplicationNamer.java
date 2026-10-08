package net.fabricmc.loader.impl.lib.sat4j.pb.tools;

import java.util.Iterator;
import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.fabricmc.loader.impl.lib.sat4j.specs.IConstr;
import net.fabricmc.loader.impl.lib.sat4j.specs.IVec;

public class ImplicationNamer<T, C> {
   private final DependencyHelper<T, C> helper;
   private IVec<IConstr> toName = new Vec<>();

   public ImplicationNamer(DependencyHelper<T, C> helper, IVec<IConstr> toName) {
      this.toName = toName;
      this.helper = helper;
   }

   public void named(C name) {
      Iterator<IConstr> it = this.toName.iterator();

      while (it.hasNext()) {
         this.helper.descs.put(it.next(), name);
      }
   }
}
