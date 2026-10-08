package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.StringUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.util.NamedMappable;
import org.objectweb.asm.AnnotationVisitor;

public class AccessorAnnotationVisitor extends AnnotationVisitor {
   private final CommonData data;
   private final MxMember method;
   private final List<String> targets;
   private final String fieldDesc;
   private boolean isSoftTarget;
   private static final Pattern GETTER_PATTERN = Pattern.compile("(?<=\\(\\)).*");
   private static final Pattern SETTER_PATTERN = Pattern.compile("(?<=\\().*(?=\\)V)");

   public AccessorAnnotationVisitor(CommonData data, AnnotationVisitor delegate, MxMember method, List<String> targets) {
      super(589824, Objects.requireNonNull(delegate));
      this.data = Objects.requireNonNull(data);
      Objects.requireNonNull(method);
      this.method = method;
      this.targets = Objects.requireNonNull(targets);
      Matcher getterMatcher = GETTER_PATTERN.matcher(method.getDesc());
      Matcher setterMatcher = SETTER_PATTERN.matcher(method.getDesc());
      if (getterMatcher.find()) {
         this.fieldDesc = getterMatcher.group();
      } else {
         if (!setterMatcher.find()) {
            throw new RuntimeException(method.getDesc() + " is not getter or setter");
         }

         this.fieldDesc = setterMatcher.group();
      }
   }

   public void visit(String name, Object value) {
      if (name.equals("value")) {
         this.isSoftTarget = true;
         String fieldName = Objects.requireNonNull((String)value);
         this.setAnnotationValue(fieldName);
      } else {
         super.visit(name, value);
      }
   }

   public void visitEnd() {
      if (!this.isSoftTarget) {
         this.setAnnotationValue(this.inferFieldName());
      }

      super.visitEnd();
   }

   private void setAnnotationValue(String fieldName) {
      super.visit("value", new NamedMappable(this.data, fieldName, this.fieldDesc, this.targets).result());
   }

   private String inferFieldName() {
      String prefix;
      if (this.method.getName().startsWith("get")) {
         prefix = "get";
      } else if (this.method.getName().startsWith("set")) {
         prefix = "set";
      } else {
         if (!this.method.getName().startsWith("is")) {
            throw new RuntimeException(String.format("%s does not start with get, set or is.", this.method.getName()));
         }

         prefix = "is";
      }

      return StringUtility.removeCamelPrefix(prefix, this.method.getName());
   }
}
