package net.fabricmc.loader.impl.lib.mappingio;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum MappingFlag {
   NEEDS_MULTIPLE_PASSES,
   NEEDS_HEADER_METADATA,
   NEEDS_METADATA_UNIQUENESS,
   NEEDS_ELEMENT_UNIQUENESS,
   NEEDS_SRC_FIELD_DESC,
   NEEDS_SRC_METHOD_DESC,
   NEEDS_DST_FIELD_DESC,
   NEEDS_DST_METHOD_DESC;

   public static final Set<MappingFlag> NONE = Collections.unmodifiableSet(EnumSet.noneOf(MappingFlag.class));
}
