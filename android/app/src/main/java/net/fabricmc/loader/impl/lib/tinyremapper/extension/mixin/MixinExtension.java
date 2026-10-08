package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.fabricmc.loader.impl.lib.tinyremapper.InputTag;
import net.fabricmc.loader.impl.lib.tinyremapper.TinyRemapper;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrEnvironment;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.HardTargetMixinClassVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.SoftTargetMixinClassVisitor;
import org.objectweb.asm.ClassVisitor;

public class MixinExtension implements TinyRemapper.Extension {
   private final Map<Integer, Collection<Consumer<CommonData>>> tasks = new ConcurrentHashMap<>();
   private final Set<MixinExtension.AnnotationTarget> targets;
   private final Predicate<InputTag> inputTagFilter;

   public MixinExtension() {
      this(EnumSet.allOf(MixinExtension.AnnotationTarget.class));
   }

   public MixinExtension(Predicate<InputTag> inputTagFilter) {
      this(EnumSet.allOf(MixinExtension.AnnotationTarget.class), inputTagFilter);
   }

   public MixinExtension(Set<MixinExtension.AnnotationTarget> targets) {
      this(targets, null);
   }

   public MixinExtension(Set<MixinExtension.AnnotationTarget> targets, Predicate<InputTag> inputTagFilter) {
      this.targets = targets;
      this.inputTagFilter = inputTagFilter;
   }

   @Override
   public void attach(TinyRemapper.Builder builder) {
      if (this.targets.contains(MixinExtension.AnnotationTarget.HARD)) {
         builder.extraAnalyzeVisitor(new MixinExtension.AnalyzeVisitorProvider()).extraStateProcessor(this::stateProcessor);
      }

      if (this.targets.contains(MixinExtension.AnnotationTarget.SOFT)) {
         builder.extraPreApplyVisitor(new MixinExtension.PreApplyVisitorProvider());
      }
   }

   private void stateProcessor(TrEnvironment environment) {
      CommonData data = new CommonData(environment);

      for (Consumer<CommonData> task : this.tasks.getOrDefault(environment.getMrjVersion(), Collections.emptyList())) {
         try {
            task.accept(data);
         } catch (RuntimeException e) {
            environment.getLogger().error(e.getMessage());
         }
      }
   }

   private final class AnalyzeVisitorProvider implements TinyRemapper.AnalyzeVisitorProvider {
      private AnalyzeVisitorProvider() {
      }

      @Override
      public ClassVisitor insertAnalyzeVisitor(int mrjVersion, String className, ClassVisitor next) {
         return new HardTargetMixinClassVisitor(MixinExtension.this.tasks.computeIfAbsent(mrjVersion, k -> new ConcurrentLinkedQueue<>()), next);
      }

      @Override
      public ClassVisitor insertAnalyzeVisitor(int mrjVersion, String className, ClassVisitor next, InputTag[] inputTags) {
         if (MixinExtension.this.inputTagFilter != null && inputTags != null) {
            for (InputTag tag : inputTags) {
               if (MixinExtension.this.inputTagFilter.test(tag)) {
                  return this.insertAnalyzeVisitor(mrjVersion, className, next);
               }
            }

            return next;
         } else {
            return this.insertAnalyzeVisitor(mrjVersion, className, next);
         }
      }

      @Override
      public ClassVisitor insertAnalyzeVisitor(boolean isInput, int mrjVersion, String className, ClassVisitor next, InputTag[] inputTags) {
         return !isInput ? next : this.insertAnalyzeVisitor(mrjVersion, className, next, inputTags);
      }
   }

   public enum AnnotationTarget {
      SOFT,
      HARD;
   }

   private final class PreApplyVisitorProvider implements TinyRemapper.ApplyVisitorProvider {
      private PreApplyVisitorProvider() {
      }

      @Override
      public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next) {
         return new SoftTargetMixinClassVisitor(new CommonData(cls.getEnvironment()), next);
      }

      @Override
      public ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next, InputTag[] inputTags) {
         if (!cls.isInput()) {
            return next;
         }

         if (MixinExtension.this.inputTagFilter != null && inputTags != null) {
            for (InputTag tag : inputTags) {
               if (MixinExtension.this.inputTagFilter.test(tag)) {
                  return this.insertApplyVisitor(cls, next);
               }
            }

            return next;
         } else {
            return this.insertApplyVisitor(cls, next);
         }
      }
   }
}
