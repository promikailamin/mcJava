package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLogger;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrRemapper;

public final class MapUtility {
   private final TrRemapper remapper;
   private final TrLogger logger;
   public static final List<String> IGNORED_NAME = Arrays.asList("<init>", "<clinit>");

   public MapUtility(TrRemapper remapper, TrLogger logger) {
      this.remapper = Objects.requireNonNull(remapper);
      this.logger = Objects.requireNonNull(logger);
   }

   public String mapName(TrClass _class) {
      return this.remapper.map(_class.getName());
   }

   public String mapName(TrMember member) {
      return member.isField()
         ? this.remapper.mapFieldName(member.getOwner().getName(), member.getName(), member.getDesc())
         : this.remapper.mapMethodName(member.getOwner().getName(), member.getName(), member.getDesc());
   }

   public String mapDesc(TrMember member) {
      return member.isField() ? this.remapper.mapDesc(member.getDesc()) : this.remapper.mapMethodDesc(member.getDesc());
   }

   public TrRemapper asTrRemapper() {
      return this.remapper;
   }
}
