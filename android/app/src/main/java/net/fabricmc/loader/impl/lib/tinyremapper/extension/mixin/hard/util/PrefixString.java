package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.util;

import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.StringUtility;

public class PrefixString implements IConvertibleString {
   private final String prefix;
   private final String text;

   public PrefixString(String prefix, String text) {
      this.prefix = Objects.requireNonNull(prefix);
      this.text = StringUtility.removePrefix(prefix, text);
   }

   @Override
   public String getConverted() {
      return this.text;
   }

   @Override
   public String getReverted(String newText) {
      return StringUtility.addPrefix(this.prefix, newText);
   }
}
