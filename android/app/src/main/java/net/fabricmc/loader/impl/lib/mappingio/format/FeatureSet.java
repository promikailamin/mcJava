package net.fabricmc.loader.impl.lib.mappingio.format;

public interface FeatureSet {
   boolean hasNamespaces();

   FeatureSet.MetadataSupport fileMetadata();

   FeatureSet.MetadataSupport elementMetadata();

   FeatureSet.NameSupport packages();

   FeatureSet.ClassSupport classes();

   FeatureSet.MemberSupport fields();

   FeatureSet.MemberSupport methods();

   FeatureSet.LocalSupport args();

   FeatureSet.LocalSupport vars();

   FeatureSet.ElementCommentSupport elementComments();

   boolean hasFileComments();

   default boolean supportsArgs() {
      return FeatureSetUtil.isSupported(this.args());
   }

   default boolean supportsVars() {
      return FeatureSetUtil.isSupported(this.vars());
   }

   interface ClassSupport extends FeatureSet.NameSupport {
      boolean hasRepackaging();
   }

   interface DescSupport {
      FeatureSet.FeaturePresence srcDescs();

      FeatureSet.FeaturePresence dstDescs();
   }

   enum ElementCommentSupport {
      NAMESPACED,
      SHARED,
      NONE;
   }

   enum FeaturePresence {
      REQUIRED,
      OPTIONAL,
      ABSENT;
   }

   interface LocalSupport extends FeatureSet.DescSupport, FeatureSet.NameSupport {
      FeatureSet.FeaturePresence positions();

      FeatureSet.FeaturePresence lvIndices();

      FeatureSet.FeaturePresence lvtRowIndices();

      FeatureSet.FeaturePresence startOpIndices();

      FeatureSet.FeaturePresence endOpIndices();
   }

   interface MemberSupport extends FeatureSet.DescSupport, FeatureSet.NameSupport {
   }

   enum MetadataSupport {
      NONE,
      FIXED,
      ARBITRARY;
   }

   interface NameSupport {
      FeatureSet.FeaturePresence srcNames();

      FeatureSet.FeaturePresence dstNames();
   }
}
