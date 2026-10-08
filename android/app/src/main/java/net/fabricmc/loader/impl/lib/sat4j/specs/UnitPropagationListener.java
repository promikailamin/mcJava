package net.fabricmc.loader.impl.lib.sat4j.specs;

public interface UnitPropagationListener {
   boolean enqueue(int var1);

   boolean enqueue(int var1, Constr var2);

   void unset(int var1);

   int getPropagationLevel();
}
