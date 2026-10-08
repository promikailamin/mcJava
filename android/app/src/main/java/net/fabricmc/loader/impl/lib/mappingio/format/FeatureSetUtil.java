package net.fabricmc.loader.impl.lib.mappingio.format;

final class FeatureSetUtil {
   static boolean isSupported(FeatureSet.LocalSupport locals) {
      return locals.positions() != FeatureSet.FeaturePresence.ABSENT
         || locals.lvIndices() != FeatureSet.FeaturePresence.ABSENT
         || locals.lvtRowIndices() != FeatureSet.FeaturePresence.ABSENT
         || locals.startOpIndices() != FeatureSet.FeaturePresence.ABSENT
         || locals.endOpIndices() != FeatureSet.FeaturePresence.ABSENT
         || locals.srcNames() != FeatureSet.FeaturePresence.ABSENT
         || locals.dstNames() != FeatureSet.FeaturePresence.ABSENT
         || locals.srcDescs() != FeatureSet.FeaturePresence.ABSENT
         || locals.dstDescs() != FeatureSet.FeaturePresence.ABSENT;
   }
}
