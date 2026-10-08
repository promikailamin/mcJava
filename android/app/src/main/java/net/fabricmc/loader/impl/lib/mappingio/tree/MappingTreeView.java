package net.fabricmc.loader.impl.lib.mappingio.tree;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import org.jetbrains.annotations.Nullable;

public interface MappingTreeView {
   @Nullable
   String getSrcNamespace();

   List<String> getDstNamespaces();

   default int getNamespaceId(String namespace) {
      if (namespace.equals(this.getSrcNamespace())) {
         return -1;
      }

      int ret = this.getDstNamespaces().indexOf(namespace);
      return ret >= 0 ? ret : -2;
   }

   Collection<? extends MappingTreeView.ClassMappingView> getClasses();

   @Nullable
   MappingTreeView.ClassMappingView getClass(String var1);

   @Nullable
   default MappingTreeView.ClassMappingView getClass(String name, int namespace) {
      if (namespace < 0) {
         return this.getClass(name);
      }

      for (MappingTreeView.ClassMappingView cls : this.getClasses()) {
         if (name.equals(cls.getDstName(namespace))) {
            return cls;
         }
      }

      return null;
   }

   @Nullable
   default MappingTreeView.FieldMappingView getField(String clsName, String name, @Nullable String desc, int namespace) {
      MappingTreeView.ClassMappingView owner = this.getClass(clsName, namespace);
      return owner != null ? owner.getField(name, desc, namespace) : null;
   }

   @Nullable
   default MappingTreeView.MethodMappingView getMethod(String clsName, String name, @Nullable String desc, int namespace) {
      MappingTreeView.ClassMappingView owner = this.getClass(clsName, namespace);
      return owner != null ? owner.getMethod(name, desc, namespace) : null;
   }

   default void accept(MappingVisitor visitor) throws IOException {
      this.accept(visitor, VisitOrder.createByInputOrder());
   }

   void accept(MappingVisitor var1, VisitOrder var2) throws IOException;

   default String mapClassName(String name, int srcNamespace, int dstNamespace) {
      assert name.indexOf(46) < 0;
      if (srcNamespace == dstNamespace) {
         return name;
      }

      MappingTreeView.ClassMappingView cls = this.getClass(name, srcNamespace);
      if (cls == null) {
         return name;
      }

      String ret = cls.getName(dstNamespace);
      return ret != null ? ret : name;
   }

   default String mapDesc(CharSequence desc, int namespace) {
      return this.mapDesc(desc, 0, desc.length(), -1, namespace);
   }

   default String mapDesc(CharSequence desc, int srcNamespace, int dstNamespace) {
      return this.mapDesc(desc, 0, desc.length(), srcNamespace, dstNamespace);
   }

   default String mapDesc(CharSequence desc, int start, int end, int srcNamespace, int dstNamespace) {
      if (srcNamespace == dstNamespace) {
         return desc.subSequence(start, end).toString();
      }

      StringBuilder ret = null;
      int copyOffset = start;
      int offset = start;

      while (offset < end) {
         char c = desc.charAt(offset++);
         if (c == 'L') {
            int idEnd;
            for (idEnd = offset; idEnd < end; idEnd++) {
               c = desc.charAt(idEnd);
               if (c == ';') {
                  break;
               }
            }

            if (idEnd >= end) {
               throw new IllegalArgumentException("invalid descriptor: " + desc.subSequence(start, end));
            }

            String cls = desc.subSequence(offset, idEnd).toString();
            String mappedCls = this.mapClassName(cls, srcNamespace, dstNamespace);
            if (mappedCls != null && !mappedCls.equals(cls)) {
               if (ret == null) {
                  ret = new StringBuilder(end - start);
               }

               ret.append(desc, copyOffset, offset);
               ret.append(mappedCls);
               copyOffset = idEnd;
            }

            offset = idEnd + 1;
         }
      }

      if (ret == null) {
         return desc.subSequence(start, end).toString();
      }

      ret.append(desc, copyOffset, end);
      return ret.toString();
   }

   interface ClassMappingView extends MappingTreeView.ElementMappingView {
      Collection<? extends MappingTreeView.FieldMappingView> getFields();

      @Nullable
      MappingTreeView.FieldMappingView getField(String var1, @Nullable String var2);

      @Nullable
      default MappingTreeView.FieldMappingView getField(String name, @Nullable String desc, int namespace) {
         if (namespace < 0) {
            return this.getField(name, desc);
         }

         for (MappingTreeView.FieldMappingView field : this.getFields()) {
            String mDesc;
            if (name.equals(field.getDstName(namespace)) && (desc == null || (mDesc = field.getDesc(namespace)) == null || desc.equals(mDesc))) {
               return field;
            }
         }

         return null;
      }

      Collection<? extends MappingTreeView.MethodMappingView> getMethods();

      @Nullable
      MappingTreeView.MethodMappingView getMethod(String var1, @Nullable String var2);

      @Nullable
      default MappingTreeView.MethodMappingView getMethod(String name, @Nullable String desc, int namespace) {
         if (namespace < 0) {
            return this.getMethod(name, desc);
         }

         for (MappingTreeView.MethodMappingView method : this.getMethods()) {
            String mDesc;
            if (name.equals(method.getDstName(namespace))
               && (desc == null || (mDesc = method.getDesc(namespace)) == null || desc.equals(mDesc) || desc.endsWith(")") && mDesc.startsWith(desc))) {
               return method;
            }
         }

         return null;
      }
   }

   interface ElementMappingView {
      MappingTreeView getTree();

      String getSrcName();

      @Nullable
      String getDstName(int var1);

      @Nullable
      default String getName(int namespace) {
         return namespace < 0 ? this.getSrcName() : this.getDstName(namespace);
      }

      @Nullable
      default String getName(String namespace) {
         int nsId = this.getTree().getNamespaceId(namespace);
         return nsId == -2 ? null : this.getName(nsId);
      }

      @Nullable
      String getComment();
   }

   interface FieldMappingView extends MappingTreeView.MemberMappingView {
   }

   interface MemberMappingView extends MappingTreeView.ElementMappingView {
      MappingTreeView.ClassMappingView getOwner();

      @Nullable
      String getSrcDesc();

      @Nullable
      default String getDesc(int namespace) {
         String srcDesc = this.getSrcDesc();
         return namespace >= 0 && srcDesc != null ? this.getTree().mapDesc(srcDesc, namespace) : srcDesc;
      }
   }

   interface MetadataEntryView {
      String getKey();

      @Nullable
      String getValue();
   }

   interface MethodArgMappingView extends MappingTreeView.ElementMappingView {
      int getArgPosition();

      int getLvIndex();
   }

   interface MethodMappingView extends MappingTreeView.MemberMappingView {
   }

   interface MethodVarMappingView extends MappingTreeView.ElementMappingView {
      int getLvtRowIndex();

      int getLvIndex();

      int getStartOpIdx();

      int getEndOpIdx();
   }
}
