package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation;

import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

public class MixinAnnotationVisitor extends AnnotationVisitor {
   private final CommonData data;
   private final List<String> targets;

   public MixinAnnotationVisitor(CommonData data, AnnotationVisitor delegate, List<String> targetsOut) {
      super(589824, Objects.requireNonNull(delegate));
      this.data = Objects.requireNonNull(data);
      this.targets = Objects.requireNonNull(targetsOut);
   }

   public AnnotationVisitor visitArray(String name) {
      AnnotationVisitor visitor = super.visitArray(name);
      if (name.equals("targets")) {
         return new AnnotationVisitor(589824, visitor) {
            public void visit(String name, Object value) {
               String srcName = ((String)value).replaceAll("\\s", "").replace('.', '/');
               MixinAnnotationVisitor.this.targets.add(srcName);
               Object var4 = MixinAnnotationVisitor.this.data.mapper.asTrRemapper().map(srcName);
               super.visit(name, var4);
            }
         };
      } else {
         return name.equals("value") ? new AnnotationVisitor(589824, visitor) {
            public void visit(String name, Object value) {
               Type srcType = Objects.requireNonNull((Type)value);
               MixinAnnotationVisitor.this.targets.add(srcType.getInternalName());
               super.visit(name, value);
            }
         } : visitor;
      }
   }
}
