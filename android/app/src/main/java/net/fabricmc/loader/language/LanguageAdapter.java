package net.fabricmc.loader.language;

import java.io.IOException;

@Deprecated
public interface LanguageAdapter {
   default Object createInstance(String classString, LanguageAdapter.Options options) throws ClassNotFoundException, LanguageAdapterException {
      try {
         Class<?> c = JavaLanguageAdapter.getClass(classString, options);
         return c != null ? this.createInstance(c, options) : null;
      } catch (IOException e) {
         throw new LanguageAdapterException("I/O error!", e);
      }
   }

   Object createInstance(Class<?> var1, LanguageAdapter.Options var2) throws LanguageAdapterException;

   enum MissingSuperclassBehavior {
      RETURN_NULL,
      CRASH;
   }

   class Options {
      private LanguageAdapter.MissingSuperclassBehavior missingSuperclassBehavior;

      public LanguageAdapter.MissingSuperclassBehavior getMissingSuperclassBehavior() {
         return this.missingSuperclassBehavior;
      }

      public static class Builder {
         private final LanguageAdapter.Options options = new LanguageAdapter.Options();

         private Builder() {
         }

         public static LanguageAdapter.Options.Builder create() {
            return new LanguageAdapter.Options.Builder();
         }

         public LanguageAdapter.Options.Builder missingSuperclassBehaviour(LanguageAdapter.MissingSuperclassBehavior value) {
            this.options.missingSuperclassBehavior = value;
            return this;
         }

         public LanguageAdapter.Options build() {
            return this.options;
         }
      }
   }
}
