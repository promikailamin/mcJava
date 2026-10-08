package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.annotation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.ResolveUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.Pair;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.data.SoftInterface;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.util.HardTargetMappable;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.util.PrefixString;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

public class ImplementsAnnotationVisitor extends AnnotationVisitor {
   private final List<SoftInterface> interfaces;

   public ImplementsAnnotationVisitor(AnnotationVisitor delegate, List<SoftInterface> interfacesOut) {
      super(589824, delegate);
      this.interfaces = Objects.requireNonNull(interfacesOut);
   }

   public AnnotationVisitor visitArray(String name) {
      AnnotationVisitor av = super.visitArray(name);
      return name.equals("value") ? new AnnotationVisitor(589824, av) {
         public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            if (!descriptor.equals("Lorg/spongepowered/asm/mixin/Interface;")) {
               throw new RuntimeException("Unexpected annotation " + descriptor);
            }

            AnnotationVisitor av1 = super.visitAnnotation(name, descriptor);
            SoftInterface _interface = new SoftInterface();
            ImplementsAnnotationVisitor.this.interfaces.add(_interface);
            return new ImplementsAnnotationVisitor.InterfaceAnnotationVisitor(av1, _interface);
         }
      } : av;
   }

   public static void visitMethod(Collection<Consumer<CommonData>> tasks, MxMember method, List<SoftInterface> interfaces) {
      tasks.add(data -> new ImplementsAnnotationVisitor.SoftImplementsMappable(data, method, interfaces).result());
   }

   private static class InterfaceAnnotationVisitor extends AnnotationVisitor {
      private final SoftInterface _interface;

      InterfaceAnnotationVisitor(AnnotationVisitor delegate, SoftInterface _interfaceOut) {
         super(589824, delegate);
         this._interface = Objects.requireNonNull(_interfaceOut);
         this._interface.setRemap(SoftInterface.Remap.ALL);
      }

      public void visit(String name, Object value) {
         if (name.equals("iface")) {
            Type target = Objects.requireNonNull((Type)value);
            this._interface.setTarget(target.getInternalName());
         } else if (name.equals("prefix")) {
            String prefix = Objects.requireNonNull((String)value);
            this._interface.setPrefix(prefix);
         }

         super.visit(name, value);
      }

      public void visitEnum(String name, String descriptor, String value) {
         if (name.equals("remap")) {
            if (!descriptor.equals("Lorg/spongepowered/asm/mixin/Interface$Remap;")) {
               throw new RuntimeException("Incorrect enum type of Interface.Remap " + descriptor);
            }

            for (SoftInterface.Remap candidate : SoftInterface.Remap.values()) {
               if (candidate.name().equals(value)) {
                  this._interface.setRemap(candidate);
                  break;
               }
            }
         }

         super.visitEnum(name, descriptor, value);
      }
   }

   private static class SoftImplementsMappable extends HardTargetMappable {
      private final Collection<SoftInterface> interfaces;

      SoftImplementsMappable(CommonData data, MxMember self, Collection<SoftInterface> interfaces) {
         super(data, self);
         this.interfaces = Objects.requireNonNull(interfaces);
      }

      @Override
      protected Optional<String> getMappedName() {
         Stream<String> stream = Stream.empty();
         stream = Stream.concat(
            stream,
            this.interfaces
               .stream()
               .filter(iface -> iface.getRemap().compareTo(SoftInterface.Remap.ONLY_PREFIX) >= 0)
               .filter(iface -> this.self.getName().startsWith(iface.getPrefix()))
               .map(
                  iface -> Pair.of(
                     new PrefixString(iface.getPrefix(), this.self.getName()),
                     this.data
                        .resolver
                        .resolveMethod(
                           iface.getTarget(),
                           this.self.getName().substring(iface.getPrefix().length()),
                           this.self.getDesc(),
                           ResolveUtility.FLAG_UNIQUE | ResolveUtility.FLAG_RECURSIVE
                        )
                  )
               )
               .filter(pair -> pair.second().isPresent())
               .map(pair -> Pair.of(pair.first(), pair.second().get()))
               .map(pair -> Pair.of(pair.first(), this.data.mapper.mapName(pair.second())))
               .map(pair -> pair.first().getReverted(pair.second()))
         );
         stream = Stream.concat(
            stream,
            this.interfaces
               .stream()
               .filter(iface -> iface.getRemap().compareTo(SoftInterface.Remap.ALL) >= 0)
               .map(
                  iface -> this.data
                     .resolver
                     .resolveMethod(iface.getTarget(), this.self.getName(), this.self.getDesc(), ResolveUtility.FLAG_UNIQUE | ResolveUtility.FLAG_RECURSIVE)
               )
               .filter(Optional::isPresent)
               .map(Optional::get)
               .map(this.data.mapper::mapName)
         );
         List<String> collection = stream.distinct().collect(Collectors.toList());
         if (collection.size() > 1) {
            this.data.getLogger().error("Conflict mapping detected, %s -> %s.", this.self.getName(), collection);
         }

         return collection.stream().findFirst();
      }
   }
}
