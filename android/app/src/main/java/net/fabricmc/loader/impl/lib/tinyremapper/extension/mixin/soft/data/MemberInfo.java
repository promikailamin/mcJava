package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.data;

import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.StringUtility;

public final class MemberInfo {
   private final String owner;
   private final String name;
   private final String quantifier;
   private final String desc;

   public MemberInfo(String owner, String name, String quantifier, String desc) {
      this.owner = Objects.requireNonNull(owner);
      this.name = Objects.requireNonNull(name);
      this.quantifier = Objects.requireNonNull(quantifier);
      this.desc = Objects.requireNonNull(desc);
   }

   public String getOwner() {
      return this.owner;
   }

   public String getName() {
      return this.name;
   }

   public String getQuantifier() {
      return this.quantifier;
   }

   public String getDesc() {
      return this.desc;
   }

   public TrMember.MemberType getType() {
      if (this.desc.isEmpty()) {
         return null;
      } else {
         return StringUtility.isMethodDesc(this.desc) ? TrMember.MemberType.METHOD : TrMember.MemberType.FIELD;
      }
   }

   public boolean isFullyQualified() {
      return !this.owner.isEmpty() && !this.name.isEmpty() && !this.desc.isEmpty();
   }

   public static boolean isRegex(String str) {
      return str.endsWith("/");
   }

   public static boolean isDynamic(String str) {
      return str.startsWith("@");
   }

   public static MemberInfo parse(String str) {
      if (!isRegex(str) && !isDynamic(str)) {
         str = str.replaceAll("\\s", "");
         String descriptor = "";
         String quantifier = "";
         String name = "";
         String owner = "";
         int sep;
         if ((sep = str.indexOf(40)) >= 0) {
            descriptor = str.substring(sep);
            str = str.substring(0, sep);
         } else if ((sep = str.indexOf(":")) >= 0) {
            descriptor = str.substring(sep + 1);
            str = str.substring(0, sep);
         }

         if ((sep = str.indexOf(42)) >= 0) {
            quantifier = str.substring(sep);
            str = str.substring(0, sep);
         } else if ((sep = str.indexOf(43)) >= 0) {
            quantifier = str.substring(sep);
            str = str.substring(0, sep);
         } else if ((sep = str.indexOf(123)) >= 0) {
            quantifier = str.substring(sep);
            str = str.substring(0, sep);
         }

         if ((sep = str.indexOf(59)) >= 0) {
            owner = StringUtility.classDescToName(str.substring(0, sep + 1));
            str = str.substring(sep + 1);
         } else if ((sep = str.lastIndexOf(46)) >= 0) {
            owner = str.substring(0, sep).replace('.', '/');
            str = str.substring(sep + 1);
         }

         if (!str.contains("/") && !str.contains(".")) {
            name = str;
         } else {
            owner = str.replace('.', '/');
         }

         return new MemberInfo(owner, name, quantifier, descriptor);
      } else {
         return null;
      }
   }

   @Override
   public String toString() {
      String owner = this.getOwner().isEmpty() ? "" : StringUtility.classNameToDesc(this.getOwner());
      return owner + this.name + this.quantifier + this.formattedDesc();
   }

   private String formattedDesc() {
      String desc = this.getDesc();
      if (desc.isEmpty()) {
         return "";
      } else {
         return Objects.equals(this.getType(), TrMember.MemberType.FIELD) ? ":" + desc : desc;
      }
   }
}
