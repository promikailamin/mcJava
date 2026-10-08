package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common;

import java.util.Locale;
import java.util.regex.Pattern;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;

public final class StringUtility {
   private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("([A-Za-z0-9_$]+/)*[A-Za-z0-9_$]+");
   private static final Pattern CLASS_DESC_PATTERN = Pattern.compile("L" + CLASS_NAME_PATTERN + ";");
   private static final Pattern FIELD_DESC_PATTERN = Pattern.compile("\\[*(" + CLASS_DESC_PATTERN + "|[BCDFIJSZ])");
   private static final Pattern METHOD_DESC_PATTERN = Pattern.compile("\\((" + FIELD_DESC_PATTERN + ")*\\)(" + FIELD_DESC_PATTERN + "|V)");
   private static final Pattern INTERNAL_CLASS_PATTERN = Pattern.compile("java/.*");

   public static String addPrefix(String prefix, String text) {
      return prefix + text;
   }

   public static String removePrefix(String prefix, String text) {
      if (text.startsWith(prefix)) {
         return text.substring(prefix.length());
      } else {
         throw new RuntimeException(String.format("%s does not start with %s", text, prefix));
      }
   }

   public static String removeCamelPrefix(String prefix, String text) {
      text = removePrefix(prefix, text);
      if (text.isEmpty() || text.toUpperCase(Locale.ROOT).equals(text)) {
         return text;
      } else if (Character.isLowerCase(text.charAt(0))) {
         throw new RuntimeException(String.format("%s does not start with camel prefix %s", text, prefix));
      } else {
         return text.substring(0, 1).toLowerCase(Locale.ROOT) + text.substring(1);
      }
   }

   public static boolean isClassName(String text) {
      return CLASS_NAME_PATTERN.matcher(text).matches();
   }

   public static boolean isClassDesc(String text) {
      return CLASS_DESC_PATTERN.matcher(text).matches();
   }

   public static boolean isFieldDesc(String text) {
      return FIELD_DESC_PATTERN.matcher(text).matches();
   }

   public static boolean isMethodDesc(String text) {
      return METHOD_DESC_PATTERN.matcher(text).matches();
   }

   public static TrMember.MemberType getTypeByDesc(String text) {
      if (isFieldDesc(text)) {
         return TrMember.MemberType.FIELD;
      } else if (isMethodDesc(text)) {
         return TrMember.MemberType.METHOD;
      } else {
         throw new RuntimeException(String.format("%s is neither field descriptor nor method descriptor.", text));
      }
   }

   public static String classNameToDesc(String className) {
      if (!isClassName(className)) {
         throw new RuntimeException(String.format("%s is not a class name.", className));
      } else {
         return "L" + className + ";";
      }
   }

   public static String classDescToName(String classDesc) {
      if (!isClassDesc(classDesc)) {
         throw new RuntimeException(String.format("%s is not a class descriptor.", classDesc));
      } else {
         return classDesc.substring(1, classDesc.length() - 1);
      }
   }
}
