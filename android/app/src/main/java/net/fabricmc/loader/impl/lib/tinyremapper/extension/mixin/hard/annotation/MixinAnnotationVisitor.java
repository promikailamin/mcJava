package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.annotation;

import java.util.List;
import java.util.Objects;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

public class MixinAnnotationVisitor extends AnnotationVisitor {
   private final List<String> targets;

   public MixinAnnotationVisitor(AnnotationVisitor delegate, List<String> targetsOut) {
      super(589824, delegate);
      this.targets = Objects.requireNonNull(targetsOut);
   }

   public AnnotationVisitor visitArray(String name) {
      AnnotationVisitor visitor = super.visitArray(name);
      if (name.equals("targets")) {
         return new AnnotationVisitor(589824, visitor) {
            public void visit(String name, Object value) {
               String srcName = ((String)value).replaceAll("\\s", "").replace('.', '/');
               String dstName = srcName;
               MixinAnnotationVisitor.this.targets.add(srcName);
               Object var5 = dstName;
               super.visit(name, var5);
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
