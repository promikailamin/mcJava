package net.fabricmc.loader.impl.lib.mappingio.tree;

import java.util.Collection;
import org.jetbrains.annotations.Nullable;

public interface MappingTree extends MappingTreeView {
   @Override
   Collection<? extends MappingTree.ClassMapping> getClasses();

   @Nullable
   MappingTree.ClassMapping getClass(String var1);

   @Nullable
   default MappingTree.ClassMapping getClass(String name, int namespace) {
      return (MappingTree.ClassMapping)MappingTreeView.super.getClass(name, namespace);
   }

   @Nullable
   default MappingTree.FieldMapping getField(String clsName, String name, @Nullable String desc, int namespace) {
      return (MappingTree.FieldMapping)MappingTreeView.super.getField(clsName, name, desc, namespace);
   }

   @Nullable
   default MappingTree.MethodMapping getMethod(String clsName, String name, @Nullable String desc, int namespace) {
      return (MappingTree.MethodMapping)MappingTreeView.super.getMethod(clsName, name, desc, namespace);
   }

   interface ClassMapping extends MappingTree.ElementMapping, MappingTreeView.ClassMappingView {
      @Override
      Collection<? extends MappingTree.FieldMapping> getFields();

      @Nullable
      MappingTree.FieldMapping getField(String var1, @Nullable String var2);

      @Nullable
      default MappingTree.FieldMapping getField(String name, @Nullable String desc, int namespace) {
         return (MappingTree.FieldMapping)MappingTreeView.ClassMappingView.super.getField(name, desc, namespace);
      }

      @Override
      Collection<? extends MappingTree.MethodMapping> getMethods();

      @Nullable
      MappingTree.MethodMapping getMethod(String var1, @Nullable String var2);

      @Nullable
      default MappingTree.MethodMapping getMethod(String name, @Nullable String desc, int namespace) {
         return (MappingTree.MethodMapping)MappingTreeView.ClassMappingView.super.getMethod(name, desc, namespace);
      }
   }

   interface ElementMapping extends MappingTreeView.ElementMappingView {
      MappingTree getTree();

      void setDstName(String var1, int var2);
   }

   interface FieldMapping extends MappingTree.MemberMapping, MappingTreeView.FieldMappingView {
   }

   interface MemberMapping extends MappingTree.ElementMapping, MappingTreeView.MemberMappingView {
      MappingTree.ClassMapping getOwner();
   }

   interface MetadataEntry extends MappingTreeView.MetadataEntryView {
   }

   interface MethodArgMapping extends MappingTree.ElementMapping, MappingTreeView.MethodArgMappingView {
      MappingTree.MethodMapping getMethod();
   }

   interface MethodMapping extends MappingTree.MemberMapping, MappingTreeView.MethodMappingView {
      Collection<? extends MappingTree.MethodArgMapping> getArgs();

      Collection<? extends MappingTree.MethodVarMapping> getVars();
   }

   interface MethodVarMapping extends MappingTree.ElementMapping, MappingTreeView.MethodVarMappingView {
      MappingTree.MethodMapping getMethod();
   }
}
