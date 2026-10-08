package net.fabricmc.loader.impl.util.mappings;

import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import org.spongepowered.asm.mixin.extensibility.IRemapper;

public class MixinRemapper implements IRemapper {
   protected final MappingTree mappings;
   protected final int fromId;
   protected final int toId;

   public MixinRemapper(MappingTree mappings, int fromId, int toId) {
      this.mappings = mappings;
      this.fromId = fromId;
      this.toId = toId;
   }

   public String mapMethodName(String owner, String name, String desc) {
      MappingTree.MethodMapping method = this.mappings.getMethod(owner, name, desc, this.fromId);
      return method == null ? name : method.getName(this.toId);
   }

   public String mapFieldName(String owner, String name, String desc) {
      MappingTree.FieldMapping field = this.mappings.getField(owner, name, desc, this.fromId);
      return field == null ? name : field.getName(this.toId);
   }

   public String map(String typeName) {
      return this.mappings.mapClassName(typeName, this.fromId, this.toId);
   }

   public String unmap(String typeName) {
      return this.mappings.mapClassName(typeName, this.toId, this.fromId);
   }

   public String mapDesc(String desc) {
      return this.mappings.mapDesc(desc, this.fromId, this.toId);
   }

   public String unmapDesc(String desc) {
      return this.mappings.mapDesc(desc, this.toId, this.fromId);
   }
}
