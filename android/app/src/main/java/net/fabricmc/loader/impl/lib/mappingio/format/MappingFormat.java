package net.fabricmc.loader.impl.lib.mappingio.format;

import org.jetbrains.annotations.Nullable;

public enum MappingFormat {
   TINY_FILE(
      "Tiny file",
      "tiny",
      true,
      FeatureSetBuilder.create()
         .withNamespaces(true)
         .withFileMetadata(FeatureSet.MetadataSupport.FIXED)
         .withClasses(c -> c.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.OPTIONAL).withRepackaging(true))
         .withFields(
            f -> f.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.OPTIONAL)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withMethods(
            m -> m.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.OPTIONAL)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withFileComments(true)
   ),
   TINY_2_FILE(
      "Tiny v2 file",
      "tiny",
      true,
      FeatureSetBuilder.create()
         .withNamespaces(true)
         .withFileMetadata(FeatureSet.MetadataSupport.ARBITRARY)
         .withClasses(c -> c.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.OPTIONAL).withRepackaging(true))
         .withFields(
            f -> f.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.OPTIONAL)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withMethods(
            m -> m.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.OPTIONAL)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withArgs(
            a -> a.withLvIndices(FeatureSet.FeaturePresence.REQUIRED)
               .withSrcNames(FeatureSet.FeaturePresence.OPTIONAL)
               .withDstNames(FeatureSet.FeaturePresence.OPTIONAL)
         )
         .withVars(
            v -> v.withLvIndices(FeatureSet.FeaturePresence.REQUIRED)
               .withLvtRowIndices(FeatureSet.FeaturePresence.OPTIONAL)
               .withStartOpIndices(FeatureSet.FeaturePresence.REQUIRED)
               .withSrcNames(FeatureSet.FeaturePresence.OPTIONAL)
               .withDstNames(FeatureSet.FeaturePresence.OPTIONAL)
         )
         .withElementComments(FeatureSet.ElementCommentSupport.SHARED)
         .withFileComments(true)
   ),
   ENIGMA_FILE(
      "Enigma file",
      "mapping",
      true,
      FeatureSetBuilder.create()
         .withFileMetadata(FeatureSet.MetadataSupport.ARBITRARY)
         .withElementMetadata(FeatureSet.MetadataSupport.FIXED)
         .withClasses(c -> c.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.OPTIONAL).withRepackaging(true))
         .withFields(
            f -> f.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.OPTIONAL)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withMethods(
            m -> m.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.OPTIONAL)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withArgs(a -> a.withLvIndices(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.OPTIONAL))
         .withElementComments(FeatureSet.ElementCommentSupport.SHARED)
         .withFileComments(true)
   ),
   ENIGMA_DIR,
   PROGUARD_FILE(
      "ProGuard file",
      "txt",
      true,
      FeatureSetBuilder.create()
         .withElementMetadata(FeatureSet.MetadataSupport.FIXED)
         .withClasses(c -> c.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.REQUIRED).withRepackaging(true))
         .withFields(
            f -> f.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.REQUIRED)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withMethods(
            m -> m.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.REQUIRED)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withFileComments(true)
   ),
   SRG_FILE(
      "SRG file",
      "srg",
      true,
      FeatureSetBuilder.create()
         .withPackages(p -> p.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.REQUIRED))
         .withClasses(c -> c.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.REQUIRED).withRepackaging(true))
         .withFields(f -> f.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.REQUIRED))
         .withMethods(
            m -> m.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.REQUIRED)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
               .withDstDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withFileComments(true)
   ),
   XSRG_FILE,
   JAM_FILE,
   CSRG_FILE,
   TSRG_FILE,
   TSRG_2_FILE,
   INTELLIJ_MIGRATION_MAP_FILE(
      "IntelliJ migration map file",
      "xml",
      true,
      FeatureSetBuilder.create()
         .withFileMetadata(FeatureSet.MetadataSupport.FIXED)
         .withPackages(p -> p.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.REQUIRED))
         .withClasses(c -> c.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.REQUIRED).withRepackaging(true))
         .withFileComments(true)
   ),
   RECAF_SIMPLE_FILE(
      "Recaf Simple file",
      "txt",
      true,
      FeatureSetBuilder.create()
         .withClasses(c -> c.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.REQUIRED).withRepackaging(true))
         .withFields(
            f -> f.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withSrcDescs(FeatureSet.FeaturePresence.OPTIONAL)
               .withDstNames(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withMethods(
            m -> m.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.REQUIRED)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withFileComments(true)
   ),
   JOBF_FILE(
      "JOBF file",
      "jobf",
      true,
      FeatureSetBuilder.create()
         .withPackages(p -> p.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.REQUIRED))
         .withClasses(c -> c.withSrcNames(FeatureSet.FeaturePresence.REQUIRED).withDstNames(FeatureSet.FeaturePresence.REQUIRED))
         .withFields(
            f -> f.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.REQUIRED)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withMethods(
            m -> m.withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
               .withDstNames(FeatureSet.FeaturePresence.REQUIRED)
               .withSrcDescs(FeatureSet.FeaturePresence.REQUIRED)
         )
         .withFileComments(true)
   );

   private final FeatureSet features;
   public final String name;
   public final boolean hasWriter;
   @Nullable
   public final String fileExt;
   @Deprecated
   public final boolean hasNamespaces;
   @Deprecated
   public final boolean hasFieldDescriptors;
   @Deprecated
   public final boolean supportsComments;
   @Deprecated
   public final boolean supportsArgs;
   @Deprecated
   public final boolean supportsLocals;

   MappingFormat(String name, @Nullable String fileExt, boolean hasWriter, FeatureSetBuilder featureBuilder) {
      this.name = name;
      this.fileExt = fileExt;
      this.hasWriter = hasWriter;
      this.features = featureBuilder.build();
      this.hasNamespaces = this.features.hasNamespaces();
      this.hasFieldDescriptors = this.features.fields().srcDescs() != FeatureSet.FeaturePresence.ABSENT
         || this.features.fields().dstDescs() != FeatureSet.FeaturePresence.ABSENT;
      this.supportsComments = this.features.elementComments() != FeatureSet.ElementCommentSupport.NONE;
      this.supportsArgs = this.features.supportsArgs();
      this.supportsLocals = this.features.supportsVars();
   }

   public FeatureSet features() {
      return this.features;
   }

   public boolean hasSingleFile() {
      return this.fileExt != null;
   }

   // $VF: Failed to inline enum fields
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   static {
      ENIGMA_DIR = new MappingFormat("Enigma directory", null, true, FeatureSetBuilder.createFrom(ENIGMA_FILE.features));
      XSRG_FILE = new MappingFormat(
         "XSRG file",
         "xsrg",
         true,
         FeatureSetBuilder.createFrom(SRG_FILE.features)
            .withFields(f -> f.withSrcDescs(FeatureSet.FeaturePresence.REQUIRED).withDstDescs(FeatureSet.FeaturePresence.REQUIRED))
      );
      JAM_FILE = new MappingFormat(
         "JAM file",
         "jam",
         true,
         FeatureSetBuilder.createFrom(SRG_FILE.features)
            .withPackages(p -> p.withSrcNames(FeatureSet.FeaturePresence.ABSENT).withDstNames(FeatureSet.FeaturePresence.ABSENT))
            .withFields(f -> f.withSrcDescs(FeatureSet.FeaturePresence.REQUIRED))
            .withMethods(m -> m.withDstDescs(FeatureSet.FeaturePresence.ABSENT))
            .withArgs(
               a -> a.withPositions(FeatureSet.FeaturePresence.REQUIRED)
                  .withSrcDescs(FeatureSet.FeaturePresence.OPTIONAL)
                  .withDstNames(FeatureSet.FeaturePresence.REQUIRED)
            )
      );
      CSRG_FILE = new MappingFormat(
         "CSRG file", "csrg", true, FeatureSetBuilder.createFrom(SRG_FILE.features).withMethods(m -> m.withDstDescs(FeatureSet.FeaturePresence.ABSENT))
      );
      TSRG_FILE = new MappingFormat("TSRG file", "tsrg", true, FeatureSetBuilder.createFrom(CSRG_FILE.features));
      TSRG_2_FILE = new MappingFormat(
         "TSRG v2 file",
         "tsrg",
         true,
         FeatureSetBuilder.createFrom(TSRG_FILE.features)
            .withNamespaces(true)
            .withElementMetadata(FeatureSet.MetadataSupport.FIXED)
            .withFields(f -> f.withSrcDescs(FeatureSet.FeaturePresence.OPTIONAL))
            .withArgs(
               a -> a.withLvIndices(FeatureSet.FeaturePresence.REQUIRED)
                  .withSrcNames(FeatureSet.FeaturePresence.REQUIRED)
                  .withDstNames(FeatureSet.FeaturePresence.REQUIRED)
            )
      );
   }
}
