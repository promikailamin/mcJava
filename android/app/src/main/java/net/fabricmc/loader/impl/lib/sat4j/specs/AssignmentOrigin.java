package net.fabricmc.loader.impl.lib.sat4j.specs;

public enum AssignmentOrigin {
   UNASSIGNED("\u001b[0;37m"),
   DECIDED("\u001b[0;32m"),
   PROPAGATED_ORIGINAL("\u001b[0;31m"),
   PROPAGATED_LEARNED("\u001b[0;34m"),
   DECIDED_PROPAGATED("\u001b[0;35m"),
   DECIDED_PROPAGATED_LEARNED("\u001b[0;36m"),
   DECIDED_CYCLE("\u001b[0;42m");

   public static final String BLANK = "\u001b[0m";
   private final String color;

   AssignmentOrigin(String color) {
      this.color = color;
   }
}
