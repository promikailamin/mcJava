package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.ResolveUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.Pair;

public abstract class ConvertibleMappable extends HardTargetMappable {
   private final Collection<TrClass> targets;

   public ConvertibleMappable(CommonData data, MxMember self, Collection<String> targets) {
      super(data, self);
      this.targets = Objects.requireNonNull(targets)
         .stream()
         .map(data.resolver::resolveClass)
         .filter(Optional::isPresent)
         .map(Optional::get)
         .collect(Collectors.toList());
   }

   protected abstract IConvertibleString getName();

   protected abstract String getDesc();

   protected Stream<String> mapMultiTarget(IConvertibleString name, String desc) {
      return this.targets
         .stream()
         .map(
            target -> Pair.of(
               name, this.data.resolver.resolveMember(target, name.getConverted(), desc, ResolveUtility.FLAG_UNIQUE | ResolveUtility.FLAG_RECURSIVE)
            )
         )
         .filter(x -> x.second().isPresent())
         .map(x -> Pair.of(x.first(), this.data.mapper.mapName(x.second().get())))
         .map(x -> x.first().getReverted(x.second()));
   }

   @Override
   protected Optional<String> getMappedName() {
      if (this.targets.isEmpty()) {
         return Optional.empty();
      }

      List<String> collection = this.mapMultiTarget(this.getName(), this.getDesc()).collect(Collectors.toList());
      if (collection.size() > 1) {
         this.data.getLogger().error("Conflict mapping detected, %s -> %s.", this.self.getName(), collection);
      } else if (collection.isEmpty()) {
         this.data.getLogger().warn("Cannot remap %s because it does not exist in any of the targets %s or their parents.", this.self.getName(), this.targets);
      }

      return collection.stream().findFirst();
   }
}
