package net.fabricmc.loader.impl.lib.sat4j.pb.constraints.pb;

public enum AutoDivisionStrategy {
   DISABLED {},
   ENABLED {};

   AutoDivisionStrategy() {
   }

   @Override
   public String toString() {
      return "Auto-division on coefficients is " + this.name().toLowerCase();
   }
}
