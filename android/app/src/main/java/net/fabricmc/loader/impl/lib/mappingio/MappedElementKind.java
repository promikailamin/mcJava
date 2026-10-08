package net.fabricmc.loader.impl.lib.mappingio;

public enum MappedElementKind {
   CLASS(0),
   FIELD(1),
   METHOD(1),
   METHOD_ARG(2),
   METHOD_VAR(2);

   public final int level;

   MappedElementKind(int level) {
      this.level = level;
   }
}
