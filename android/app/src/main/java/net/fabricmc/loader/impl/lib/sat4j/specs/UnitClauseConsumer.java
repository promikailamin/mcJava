package net.fabricmc.loader.impl.lib.sat4j.specs;

public interface UnitClauseConsumer {
   UnitClauseConsumer VOID = new UnitClauseConsumer() {
      @Override
      public void learnUnit(int p) {
      }
   };

   void learnUnit(int var1);
}
