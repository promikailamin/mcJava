package net.fabricmc.loader.impl.lib.sat4j.specs;

public interface UnitClauseProvider {
   UnitClauseProvider VOID = new UnitClauseProvider() {
      @Override
      public void provideUnitClauses(UnitPropagationListener upl) {
      }
   };

   void provideUnitClauses(UnitPropagationListener var1);
}
