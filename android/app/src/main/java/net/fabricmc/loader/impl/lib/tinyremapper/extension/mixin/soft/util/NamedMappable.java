package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.util;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.MapUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.ResolveUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;

public class NamedMappable {
   private final CommonData data;
   private final String name;
   private final String desc;
   private final Collection<TrClass> targets;

   public NamedMappable(CommonData data, String name, String desc, Collection<String> targets) {
      this.data = Objects.requireNonNull(data);
      this.name = Objects.requireNonNull(name);
      this.desc = Objects.requireNonNull(desc);
      this.targets = Objects.requireNonNull(targets)
         .stream()
         .map(data.resolver::resolveClass)
         .filter(Optional::isPresent)
         .map(Optional::get)
         .collect(Collectors.toList());
   }

   public String result() {
      if (MapUtility.IGNORED_NAME.contains(this.name)) {
         return this.name;
      }

      Collection<String> collection = this.targets
         .stream()
         .map(target -> this.data.resolver.resolveMember(target, this.name, this.desc, ResolveUtility.FLAG_UNIQUE | ResolveUtility.FLAG_RECURSIVE))
         .filter(Optional::isPresent)
         .map(Optional::get)
         .map(this.data.mapper::mapName)
         .distinct()
         .collect(Collectors.toList());
      if (collection.size() > 1) {
         this.data.getLogger().error("Conflict mapping detected, %s -> %s.", this.name, collection);
      } else if (collection.isEmpty()) {
         this.data.getLogger().warn("Cannot remap %s because it does not exist in any of the targets %s or their parents.", this.name, this.targets);
      }

      return collection.stream().findFirst().orElse(this.name);
   }
}
