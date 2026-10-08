package net.fabricmc.loader.impl.lib.mappingio.format;

import java.util.function.Consumer;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;

@Experimental
public class FeatureSetBuilder {
   private boolean hasNamespaces;
   private FeatureSet.MetadataSupport fileMetadata;
   private FeatureSet.MetadataSupport elementMetadata;
   private FeatureSetBuilder.NameFeatureBuilder packages;
   private FeatureSetBuilder.ClassSupportBuilder classes;
   private FeatureSetBuilder.MemberSupportBuilder fields;
   private FeatureSetBuilder.MemberSupportBuilder methods;
   private FeatureSetBuilder.LocalSupportBuilder args;
   private FeatureSetBuilder.LocalSupportBuilder vars;
   private FeatureSet.ElementCommentSupport elementComments;
   private boolean hasFileComments;

   public static FeatureSetBuilder create() {
      return new FeatureSetBuilder(false);
   }

   public static FeatureSetBuilder createFrom(FeatureSet featureSet) {
      return new FeatureSetBuilder(
         featureSet.hasNamespaces(),
         featureSet.fileMetadata(),
         featureSet.elementMetadata(),
         new FeatureSetBuilder.NameFeatureBuilder(featureSet.packages()),
         new FeatureSetBuilder.ClassSupportBuilder(featureSet.classes()),
         new FeatureSetBuilder.MemberSupportBuilder(featureSet.fields()),
         new FeatureSetBuilder.MemberSupportBuilder(featureSet.methods()),
         new FeatureSetBuilder.LocalSupportBuilder(featureSet.args()),
         new FeatureSetBuilder.LocalSupportBuilder(featureSet.vars()),
         featureSet.elementComments(),
         featureSet.hasFileComments()
      );
   }

   @Internal
   public FeatureSetBuilder(boolean initWithFullSupport) {
      this(
         initWithFullSupport,
         initWithFullSupport ? FeatureSet.MetadataSupport.ARBITRARY : FeatureSet.MetadataSupport.NONE,
         initWithFullSupport ? FeatureSet.MetadataSupport.ARBITRARY : FeatureSet.MetadataSupport.NONE,
         new FeatureSetBuilder.NameFeatureBuilder(initWithFullSupport),
         new FeatureSetBuilder.ClassSupportBuilder(initWithFullSupport),
         new FeatureSetBuilder.MemberSupportBuilder(initWithFullSupport),
         new FeatureSetBuilder.MemberSupportBuilder(initWithFullSupport),
         new FeatureSetBuilder.LocalSupportBuilder(initWithFullSupport),
         new FeatureSetBuilder.LocalSupportBuilder(initWithFullSupport),
         initWithFullSupport ? FeatureSet.ElementCommentSupport.NAMESPACED : FeatureSet.ElementCommentSupport.NONE,
         initWithFullSupport
      );
   }

   FeatureSetBuilder(
      boolean hasNamespaces,
      FeatureSet.MetadataSupport fileMetadata,
      FeatureSet.MetadataSupport elementMetadata,
      FeatureSetBuilder.NameFeatureBuilder packages,
      FeatureSetBuilder.ClassSupportBuilder classes,
      FeatureSetBuilder.MemberSupportBuilder fields,
      FeatureSetBuilder.MemberSupportBuilder methods,
      FeatureSetBuilder.LocalSupportBuilder args,
      FeatureSetBuilder.LocalSupportBuilder vars,
      FeatureSet.ElementCommentSupport elementComments,
      boolean hasFileComments
   ) {
      this.hasNamespaces = hasNamespaces;
      this.fileMetadata = fileMetadata;
      this.elementMetadata = elementMetadata;
      this.packages = packages;
      this.classes = classes;
      this.fields = fields;
      this.methods = methods;
      this.args = args;
      this.vars = vars;
      this.elementComments = elementComments;
      this.hasFileComments = hasFileComments;
   }

   public FeatureSetBuilder withNamespaces(boolean value) {
      this.hasNamespaces = value;
      return this;
   }

   public FeatureSetBuilder withFileMetadata(FeatureSet.MetadataSupport featurePresence) {
      this.fileMetadata = featurePresence;
      return this;
   }

   public FeatureSetBuilder withElementMetadata(FeatureSet.MetadataSupport featurePresence) {
      this.elementMetadata = featurePresence;
      return this;
   }

   public FeatureSetBuilder withPackages(Consumer<FeatureSetBuilder.NameFeatureBuilder> featureApplier) {
      featureApplier.accept(this.packages);
      return this;
   }

   public FeatureSetBuilder withClasses(Consumer<FeatureSetBuilder.ClassSupportBuilder> featureApplier) {
      featureApplier.accept(this.classes);
      return this;
   }

   public FeatureSetBuilder withFields(Consumer<FeatureSetBuilder.MemberSupportBuilder> featureApplier) {
      featureApplier.accept(this.fields);
      return this;
   }

   public FeatureSetBuilder withMethods(Consumer<FeatureSetBuilder.MemberSupportBuilder> featureApplier) {
      featureApplier.accept(this.methods);
      return this;
   }

   public FeatureSetBuilder withArgs(Consumer<FeatureSetBuilder.LocalSupportBuilder> featureApplier) {
      featureApplier.accept(this.args);
      return this;
   }

   public FeatureSetBuilder withVars(Consumer<FeatureSetBuilder.LocalSupportBuilder> featureApplier) {
      featureApplier.accept(this.vars);
      return this;
   }

   public FeatureSetBuilder withElementComments(FeatureSet.ElementCommentSupport featurePresence) {
      this.elementComments = featurePresence;
      return this;
   }

   public FeatureSetBuilder withFileComments(boolean value) {
      this.hasFileComments = value;
      return this;
   }

   public FeatureSet build() {
      return new FeatureSetImpl(
         this.hasNamespaces,
         this.fileMetadata,
         this.elementMetadata,
         this.packages.build(),
         this.classes.build(),
         this.fields.build(),
         this.methods.build(),
         this.args.build(),
         this.vars.build(),
         this.elementComments,
         this.hasFileComments
      );
   }

   public static class ClassSupportBuilder {
      private FeatureSetBuilder.NameFeatureBuilder names;
      private boolean hasRepackaging;

      ClassSupportBuilder() {
         this(false);
      }

      ClassSupportBuilder(boolean initWithFullSupport) {
         this(new FeatureSetBuilder.NameFeatureBuilder(initWithFullSupport), initWithFullSupport);
      }

      ClassSupportBuilder(FeatureSet.ClassSupport classFeature) {
         this(new FeatureSetBuilder.NameFeatureBuilder(classFeature), classFeature.hasRepackaging());
      }

      private ClassSupportBuilder(FeatureSetBuilder.NameFeatureBuilder names, boolean hasRepackaging) {
         this.names = names;
         this.hasRepackaging = hasRepackaging;
      }

      public FeatureSetBuilder.ClassSupportBuilder withSrcNames(FeatureSet.FeaturePresence featurePresence) {
         this.names.withSrcNames(featurePresence);
         return this;
      }

      public FeatureSetBuilder.ClassSupportBuilder withDstNames(FeatureSet.FeaturePresence featurePresence) {
         this.names.withDstNames(featurePresence);
         return this;
      }

      public FeatureSetBuilder.ClassSupportBuilder withRepackaging(boolean value) {
         this.hasRepackaging = value;
         return this;
      }

      public FeatureSet.ClassSupport build() {
         return new FeatureSetImpl.ClassSupportImpl(this.names.build(), this.hasRepackaging);
      }
   }

   public static class DescFeatureBuilder {
      private FeatureSet.FeaturePresence srcDescriptors;
      private FeatureSet.FeaturePresence dstDescriptors;

      DescFeatureBuilder() {
         this(false);
      }

      DescFeatureBuilder(boolean initWithFullSupport) {
         this(
            initWithFullSupport ? FeatureSet.FeaturePresence.OPTIONAL : FeatureSet.FeaturePresence.ABSENT,
            initWithFullSupport ? FeatureSet.FeaturePresence.OPTIONAL : FeatureSet.FeaturePresence.ABSENT
         );
      }

      private DescFeatureBuilder(FeatureSet.DescSupport descFeature) {
         this(descFeature.srcDescs(), descFeature.dstDescs());
      }

      private DescFeatureBuilder(FeatureSet.FeaturePresence srcDescriptors, FeatureSet.FeaturePresence dstDescriptors) {
         this.srcDescriptors = srcDescriptors;
         this.dstDescriptors = dstDescriptors;
      }

      public FeatureSetBuilder.DescFeatureBuilder withSrcDescs(FeatureSet.FeaturePresence featurePresence) {
         this.srcDescriptors = featurePresence;
         return this;
      }

      public FeatureSetBuilder.DescFeatureBuilder withDstDescs(FeatureSet.FeaturePresence featurePresence) {
         this.dstDescriptors = featurePresence;
         return this;
      }

      public FeatureSet.DescSupport build() {
         return new FeatureSetImpl.DescSupportImpl(this.srcDescriptors, this.dstDescriptors);
      }
   }

   public static class LocalSupportBuilder {
      private FeatureSet.FeaturePresence positions;
      private FeatureSet.FeaturePresence lvIndices;
      private FeatureSet.FeaturePresence lvtRowIndices;
      private FeatureSet.FeaturePresence startOpIndices;
      private FeatureSet.FeaturePresence endOpIndices;
      private FeatureSetBuilder.NameFeatureBuilder names;
      private FeatureSetBuilder.DescFeatureBuilder descriptors;

      LocalSupportBuilder() {
         this(false);
      }

      LocalSupportBuilder(boolean initWithFullSupport) {
         this(
            initWithFullSupport ? FeatureSet.FeaturePresence.OPTIONAL : FeatureSet.FeaturePresence.ABSENT,
            initWithFullSupport ? FeatureSet.FeaturePresence.OPTIONAL : FeatureSet.FeaturePresence.ABSENT,
            initWithFullSupport ? FeatureSet.FeaturePresence.OPTIONAL : FeatureSet.FeaturePresence.ABSENT,
            initWithFullSupport ? FeatureSet.FeaturePresence.OPTIONAL : FeatureSet.FeaturePresence.ABSENT,
            initWithFullSupport ? FeatureSet.FeaturePresence.OPTIONAL : FeatureSet.FeaturePresence.ABSENT,
            new FeatureSetBuilder.NameFeatureBuilder(),
            new FeatureSetBuilder.DescFeatureBuilder()
         );
      }

      LocalSupportBuilder(FeatureSet.LocalSupport localSupport) {
         this(
            localSupport.positions(),
            localSupport.lvIndices(),
            localSupport.lvtRowIndices(),
            localSupport.startOpIndices(),
            localSupport.endOpIndices(),
            new FeatureSetBuilder.NameFeatureBuilder(localSupport),
            new FeatureSetBuilder.DescFeatureBuilder(localSupport)
         );
      }

      private LocalSupportBuilder(
         FeatureSet.FeaturePresence positions,
         FeatureSet.FeaturePresence lvIndices,
         FeatureSet.FeaturePresence lvtRowIndices,
         FeatureSet.FeaturePresence startOpIndices,
         FeatureSet.FeaturePresence endOpIndices,
         FeatureSetBuilder.NameFeatureBuilder names,
         FeatureSetBuilder.DescFeatureBuilder descriptors
      ) {
         this.positions = positions;
         this.lvIndices = lvIndices;
         this.lvtRowIndices = lvtRowIndices;
         this.startOpIndices = startOpIndices;
         this.endOpIndices = endOpIndices;
         this.names = names;
         this.descriptors = descriptors;
      }

      public FeatureSetBuilder.LocalSupportBuilder withPositions(FeatureSet.FeaturePresence featurePresence) {
         this.positions = featurePresence;
         return this;
      }

      public FeatureSetBuilder.LocalSupportBuilder withLvIndices(FeatureSet.FeaturePresence featurePresence) {
         this.lvIndices = featurePresence;
         return this;
      }

      public FeatureSetBuilder.LocalSupportBuilder withLvtRowIndices(FeatureSet.FeaturePresence featurePresence) {
         this.lvtRowIndices = featurePresence;
         return this;
      }

      public FeatureSetBuilder.LocalSupportBuilder withStartOpIndices(FeatureSet.FeaturePresence featurePresence) {
         this.startOpIndices = featurePresence;
         return this;
      }

      public FeatureSetBuilder.LocalSupportBuilder withSrcNames(FeatureSet.FeaturePresence featurePresence) {
         this.names.withSrcNames(featurePresence);
         return this;
      }

      public FeatureSetBuilder.LocalSupportBuilder withDstNames(FeatureSet.FeaturePresence featurePresence) {
         this.names.withDstNames(featurePresence);
         return this;
      }

      public FeatureSetBuilder.LocalSupportBuilder withSrcDescs(FeatureSet.FeaturePresence featurePresence) {
         this.descriptors.withSrcDescs(featurePresence);
         return this;
      }

      public FeatureSet.LocalSupport build() {
         return new FeatureSetImpl.LocalSupportImpl(
            this.positions, this.lvIndices, this.lvtRowIndices, this.startOpIndices, this.endOpIndices, this.names.build(), this.descriptors.build()
         );
      }
   }

   public static class MemberSupportBuilder {
      private FeatureSetBuilder.NameFeatureBuilder names;
      private FeatureSetBuilder.DescFeatureBuilder descriptors;

      MemberSupportBuilder() {
         this(false);
      }

      MemberSupportBuilder(boolean initWithFullSupport) {
         this(new FeatureSetBuilder.NameFeatureBuilder(initWithFullSupport), new FeatureSetBuilder.DescFeatureBuilder(initWithFullSupport));
      }

      MemberSupportBuilder(FeatureSet.MemberSupport memberSupport) {
         this(new FeatureSetBuilder.NameFeatureBuilder(memberSupport), new FeatureSetBuilder.DescFeatureBuilder(memberSupport));
      }

      private MemberSupportBuilder(FeatureSetBuilder.NameFeatureBuilder names, FeatureSetBuilder.DescFeatureBuilder descriptors) {
         this.names = names;
         this.descriptors = descriptors;
      }

      public FeatureSetBuilder.MemberSupportBuilder withSrcNames(FeatureSet.FeaturePresence featurePresence) {
         this.names.withSrcNames(featurePresence);
         return this;
      }

      public FeatureSetBuilder.MemberSupportBuilder withDstNames(FeatureSet.FeaturePresence featurePresence) {
         this.names.withDstNames(featurePresence);
         return this;
      }

      public FeatureSetBuilder.MemberSupportBuilder withSrcDescs(FeatureSet.FeaturePresence featurePresence) {
         this.descriptors.withSrcDescs(featurePresence);
         return this;
      }

      public FeatureSetBuilder.MemberSupportBuilder withDstDescs(FeatureSet.FeaturePresence featurePresence) {
         this.descriptors.withDstDescs(featurePresence);
         return this;
      }

      public FeatureSet.MemberSupport build() {
         return new FeatureSetImpl.MemberSupportImpl(this.names.build(), this.descriptors.build());
      }
   }

   public static class NameFeatureBuilder {
      private FeatureSet.FeaturePresence srcNames;
      private FeatureSet.FeaturePresence dstNames;

      NameFeatureBuilder() {
         this(false);
      }

      NameFeatureBuilder(boolean initWithFullSupport) {
         this(
            initWithFullSupport ? FeatureSet.FeaturePresence.OPTIONAL : FeatureSet.FeaturePresence.ABSENT,
            initWithFullSupport ? FeatureSet.FeaturePresence.OPTIONAL : FeatureSet.FeaturePresence.ABSENT
         );
      }

      private NameFeatureBuilder(FeatureSet.NameSupport nameFeature) {
         this(nameFeature.srcNames(), nameFeature.dstNames());
      }

      private NameFeatureBuilder(FeatureSet.FeaturePresence srcNames, FeatureSet.FeaturePresence dstNames) {
         this.srcNames = srcNames;
         this.dstNames = dstNames;
      }

      public FeatureSetBuilder.NameFeatureBuilder withSrcNames(FeatureSet.FeaturePresence featurePresence) {
         this.srcNames = featurePresence;
         return this;
      }

      public FeatureSetBuilder.NameFeatureBuilder withDstNames(FeatureSet.FeaturePresence featurePresence) {
         this.dstNames = featurePresence;
         return this;
      }

      public FeatureSet.NameSupport build() {
         return new FeatureSetImpl.NameSupportImpl(this.srcNames, this.dstNames);
      }
   }
}
