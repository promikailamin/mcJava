package com.mojang.text2speech;

/**
 * Android stub for Mojang's desktop text-to-speech library. Narrator speech is not
 * available on Android yet, so {@link #active()} reports {@code false} and all calls
 * are no-ops.
 */
public class Narrator {

   private static final Narrator INSTANCE = new Narrator();

   protected Narrator() {
   }

   public static Narrator getNarrator() {
      return INSTANCE;
   }

   public void say(String message, boolean interrupt) {
   }

   public void say(String message, boolean interrupt, float volume) {
   }

   public boolean active() {
      return false;
   }

   public void clear() {
   }

   public void destroy() {
   }
}