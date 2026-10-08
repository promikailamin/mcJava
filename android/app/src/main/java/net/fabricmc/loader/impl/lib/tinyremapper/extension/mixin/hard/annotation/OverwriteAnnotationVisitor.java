package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.annotation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.util.ConvertibleMappable;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.util.IConvertibleString;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.util.IdentityString;
import org.objectweb.asm.AnnotationVisitor;

public class OverwriteAnnotationVisitor extends AnnotationVisitor {
   private final Collection<Consumer<CommonData>> tasks;
   private final MxMember method;
   private final List<String> targets;

   public OverwriteAnnotationVisitor(Collection<Consumer<CommonData>> tasks, AnnotationVisitor delegate, MxMember method, List<String> targets) {
      super(589824, delegate);
      this.tasks = Objects.requireNonNull(tasks);
      this.method = Objects.requireNonNull(method);
      this.targets = Objects.requireNonNull(targets);
   }

   public void visitEnd() {
      this.tasks.add(data -> new OverwriteAnnotationVisitor.OverwriteMappable(data, this.method, this.targets).result());
      super.visitEnd();
   }

   private static class OverwriteMappable extends ConvertibleMappable {
      OverwriteMappable(CommonData data, MxMember self, Collection<String> targets) {
         super(data, self, targets);
      }

      @Override
      protected IConvertibleString getName() {
         return new IdentityString(this.self.getName());
      }

      @Override
      protected String getDesc() {
         return this.self.getDesc();
      }
   }
}
