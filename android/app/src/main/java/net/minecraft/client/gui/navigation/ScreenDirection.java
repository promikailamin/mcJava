// PATTERN_SWITCHES_CONVERTED
package net.minecraft.client.gui.navigation;

import it.unimi.dsi.fastutil.ints.IntComparator;

public enum ScreenDirection {
   UP,
   DOWN,
   LEFT,
   RIGHT;

   private final IntComparator coordinateValueComparator = (k1, k2) -> k1 == k2 ? 0 : (this.isBefore(k1, k2) ? -1 : 1);

   public ScreenAxis getAxis() {
      if (this == UP || this == DOWN) {
          return ScreenAxis.VERTICAL;
      }
      else if (this == LEFT || this == RIGHT) {
          return ScreenAxis.HORIZONTAL;
      }
   }

   public ScreenDirection getOpposite() {
      return switch (this) {
         case UP -> DOWN;
         case DOWN -> UP;
         case LEFT -> RIGHT;
         case RIGHT -> LEFT;
      };
   }

   public boolean isPositive() {
      if (this == UP || this == LEFT) {
          return false;
      }
      else if (this == DOWN || this == RIGHT) {
          return true;
      }
   }

   public boolean isAfter(final int a, final int b) {
      return this.isPositive() ? a > b : b > a;
   }

   public boolean isBefore(final int a, final int b) {
      return this.isPositive() ? a < b : b < a;
   }

   public IntComparator coordinateValueComparator() {
      return this.coordinateValueComparator;
   }
}
