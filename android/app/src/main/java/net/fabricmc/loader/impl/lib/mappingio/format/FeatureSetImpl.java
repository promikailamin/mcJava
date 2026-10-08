package net.fabricmc.loader.impl.lib.mappingio.format;

class FeatureSetImpl implements FeatureSet {
   private final boolean hasNamespaces;
   private final FeatureSet.MetadataSupport fileMetadata;
   private final FeatureSet.MetadataSupport elementMetadata;
   private final FeatureSet.NameSupport packages;
   private final FeatureSet.ClassSupport classes;
   private final FeatureSet.MemberSupport fields;
   private final FeatureSet.MemberSupport methods;
   private final FeatureSet.LocalSupport args;
   private final FeatureSet.LocalSupport vars;
   private final FeatureSet.ElementCommentSupport elementComments;
   private final boolean hasFileComments;

   FeatureSetImpl(
      boolean hasNamespaces,
      FeatureSet.MetadataSupport fileMetadata,
      FeatureSet.MetadataSupport elementMetadata,
      FeatureSet.NameSupport packages,
      FeatureSet.ClassSupport classes,
      FeatureSet.MemberSupport fields,
      FeatureSet.MemberSupport methods,
      FeatureSet.LocalSupport args,
      FeatureSet.LocalSupport vars,
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

   @Override
   public boolean hasNamespaces() {
      return this.hasNamespaces;
   }

   @Override
   public FeatureSet.MetadataSupport fileMetadata() {
      return this.fileMetadata;
   }

   @Override
   public FeatureSet.MetadataSupport elementMetadata() {
      return this.elementMetadata;
   }

   @Override
   public FeatureSet.NameSupport packages() {
      return this.packages;
   }

   @Override
   public FeatureSet.ClassSupport classes() {
      return this.classes;
   }

   @Override
   public FeatureSet.MemberSupport fields() {
      return this.fields;
   }

   @Override
   public FeatureSet.MemberSupport methods() {
      return this.methods;
   }

   @Override
   public FeatureSet.LocalSupport args() {
      return this.args;
   }

   @Override
   public FeatureSet.LocalSupport vars() {
      return this.vars;
   }

   @Override
   public FeatureSet.ElementCommentSupport elementComments() {
      return this.elementComments;
   }

   @Override
   public boolean hasFileComments() {
      return this.hasFileComments;
   }

   static class ClassSupportImpl extends FeatureSetImpl.NameSupportImpl implements FeatureSet.ClassSupport {
      private final boolean hasRepackaging;

      ClassSupportImpl(FeatureSet.NameSupport names, boolean hasRepackaging) {
         super(names.srcNames(), names.dstNames());
         this.hasRepackaging = hasRepackaging;
      }

      @Override
      public boolean hasRepackaging() {
         return this.hasRepackaging;
      }
   }

   static class DescSupportImpl implements FeatureSet.DescSupport {
      private final FeatureSet.FeaturePresence srcDescriptors;
      private final FeatureSet.FeaturePresence dstDescriptors;

      DescSupportImpl(FeatureSet.FeaturePresence srcDescriptors, FeatureSet.FeaturePresence dstDescriptors) {
         this.srcDescriptors = srcDescriptors;
         this.dstDescriptors = dstDescriptors;
      }

      @Override
      public FeatureSet.FeaturePresence srcDescs() {
         return this.srcDescriptors;
      }

      @Override
      public FeatureSet.FeaturePresence dstDescs() {
         return this.dstDescriptors;
      }
   }

   static class LocalSupportImpl extends FeatureSetImpl.NameSupportImpl implements FeatureSet.LocalSupport {
      private final FeatureSet.FeaturePresence positions;
      private final FeatureSet.FeaturePresence lvIndices;
      private final FeatureSet.FeaturePresence lvtRowIndices;
      private final FeatureSet.FeaturePresence startOpIndices;
      private final FeatureSet.FeaturePresence endOpIndices;
      private final FeatureSet.DescSupport descriptors;

      LocalSupportImpl(
         FeatureSet.FeaturePresence positions,
         FeatureSet.FeaturePresence lvIndices,
         FeatureSet.FeaturePresence lvtRowIndices,
         FeatureSet.FeaturePresence startOpIndices,
         FeatureSet.FeaturePresence endOpIndices,
         FeatureSet.NameSupport names,
         FeatureSet.DescSupport descriptors
      ) {
         super(names.srcNames(), names.dstNames());
         this.positions = positions;
         this.lvIndices = lvIndices;
         this.lvtRowIndices = lvtRowIndices;
         this.startOpIndices = startOpIndices;
         this.endOpIndices = endOpIndices;
         this.descriptors = descriptors;
      }

      @Override
      public FeatureSet.FeaturePresence positions() {
         return this.positions;
      }

      @Override
      public FeatureSet.FeaturePresence lvIndices() {
         return this.lvIndices;
      }

      @Override
      public FeatureSet.FeaturePresence lvtRowIndices() {
         return this.lvtRowIndices;
      }

      @Override
      public FeatureSet.FeaturePresence startOpIndices() {
         return this.startOpIndices;
      }

      @Override
      public FeatureSet.FeaturePresence endOpIndices() {
         return this.endOpIndices;
      }

      @Override
      public FeatureSet.FeaturePresence srcDescs() {
         return this.descriptors.srcDescs();
      }

      @Override
      public FeatureSet.FeaturePresence dstDescs() {
         return this.descriptors.dstDescs();
      }
   }

   static class MemberSupportImpl extends FeatureSetImpl.NameSupportImpl implements FeatureSet.MemberSupport {
      private final FeatureSet.DescSupport descriptors;

      MemberSupportImpl(FeatureSet.NameSupport names, FeatureSet.DescSupport descriptors) {
         super(names.srcNames(), names.dstNames());
         this.descriptors = descriptors;
      }

      @Override
      public FeatureSet.FeaturePresence srcDescs() {
         return this.descriptors.srcDescs();
      }

      @Override
      public FeatureSet.FeaturePresence dstDescs() {
         return this.descriptors.dstDescs();
      }
   }

   static class NameSupportImpl implements FeatureSet.NameSupport {
      private final FeatureSet.FeaturePresence srcNames;
      private final FeatureSet.FeaturePresence dstNames;

      NameSupportImpl(FeatureSet.FeaturePresence srcNames, FeatureSet.FeaturePresence dstNames) {
         this.srcNames = srcNames;
         this.dstNames = dstNames;
      }

      @Override
      public FeatureSet.FeaturePresence srcNames() {
         return this.srcNames;
      }

      @Override
      public FeatureSet.FeaturePresence dstNames() {
         return this.dstNames;
      }
   }
}
