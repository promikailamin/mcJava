package net.fabricmc.loader.impl.lib.sat4j.pb.orders;

import net.fabricmc.loader.impl.lib.sat4j.minisat.core.IOrder;
import net.fabricmc.loader.impl.lib.sat4j.pb.ObjectiveFunction;

public interface IOrderObjective extends IOrder {
   void setObjectiveFunction(ObjectiveFunction var1);
}
