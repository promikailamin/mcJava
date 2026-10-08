package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data;

import java.util.Objects;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrEnvironment;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLogger;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.MapUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.ResolveUtility;

public final class CommonData {
   private final TrEnvironment environment;
   public final ResolveUtility resolver;
   public final MapUtility mapper;

   public CommonData(TrEnvironment environment) {
      this.environment = Objects.requireNonNull(environment);
      this.resolver = new ResolveUtility(environment);
      this.mapper = new MapUtility(environment.getRemapper(), environment.getLogger());
   }

   public TrLogger getLogger() {
      return this.environment.getLogger();
   }

   public void propagate(TrMember member, String newName) {
      this.environment.propagate(member, newName);
   }
}
