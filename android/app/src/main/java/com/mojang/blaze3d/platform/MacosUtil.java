package com.mojang.blaze3d.platform;

import java.util.Set;
import net.minecraft.util.Util;

/**
 * Port of {@code MacosUtil}: the Objective-C bridge ({@code ca.weblite.objc}) and
 * {@code ObjCRuntime} are desktop-only, so this file is reduced to its portable surface.
 */
public final class MacosUtil {
   public static final boolean IS_MACOS = Util.getPlatform() == Util.OS.OSX;

   private MacosUtil() {
   }

   public static void disableCloseWindowMenuItem() {
   }

   public static void setFullscreenMenuVisibility(final boolean value) {
   }

   public static void setCtrlClickEmulatesRightClick(final boolean value) {
   }

   private static final class DisabledActions {
      private static final Set<Long> SELECTORS = Set.of();
   }
}
