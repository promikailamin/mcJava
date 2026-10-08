package net.fabricmc.loader.impl.lib.tinyremapper;

import java.util.Collection;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrRemapper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

class AsmRemapper extends TrRemapper {
   final TinyRemapper.MrjState context;
   final TinyRemapper tr;

   AsmRemapper(TinyRemapper.MrjState context) {
      this.context = context;
      this.tr = context.tr;
   }

   public String map(String typeName) {
      String ret = this.tr.classMap.get(typeName);
      if (ret != null) {
         return ret;
      } else {
         return this.tr.extraRemapper != null ? this.tr.extraRemapper.map(typeName) : typeName;
      }
   }

   public String mapFieldName(String owner, String name, String desc) {
      ClassInstance cls = this.getClass(owner);
      return cls == null ? name : this.mapFieldName(cls, name, desc);
   }

   final String mapFieldName(ClassInstance cls, String name, String desc) {
      MemberInstance member = cls.resolve(TrMember.MemberType.FIELD, MemberInstance.getFieldId(name, desc, this.tr.ignoreFieldDesc));
      String newName;
      if (member != null && (newName = member.getNewName()) != null) {
         return newName;
      }

      assert (newName = this.tr.fieldMap.get(cls.getName() + "/" + MemberInstance.getFieldId(name, desc, this.tr.ignoreFieldDesc))) == null
         || newName.equals(name);
      return this.tr.extraRemapper != null ? this.tr.extraRemapper.mapFieldName(cls.getName(), name, desc) : name;
   }

   public String mapRecordComponentName(String owner, String name, String descriptor) {
      return this.mapFieldName(owner, name, descriptor);
   }

   public String mapMethodName(String owner, String name, String desc) {
      if (!desc.startsWith("(")) {
         return this.mapFieldName(owner, name, desc);
      }

      ClassInstance cls = this.getClass(owner);
      return cls == null ? name : this.mapMethodName(cls, name, desc);
   }

   final String mapMethodName(ClassInstance cls, String name, String desc) {
      MemberInstance member = cls.resolve(TrMember.MemberType.METHOD, MemberInstance.getMethodId(name, desc));
      String newName;
      if (member != null && (newName = member.getNewName()) != null) {
         return newName;
      }

      assert (newName = this.tr.methodMap.get(cls.getName() + "/" + MemberInstance.getMethodId(name, desc))) == null || newName.equals(name);
      return this.tr.extraRemapper != null ? this.tr.extraRemapper.mapMethodName(cls.getName(), name, desc) : name;
   }

   public String mapMethodNamePrefixDesc(String owner, String name, String descPrefix) {
      ClassInstance cls = this.getClass(owner);
      if (cls == null) {
         return name;
      }

      Collection<TrMethod> members = cls.resolveMethods(name, descPrefix, true, null, null);
      MemberInstance member = members.size() == 1 ? (MemberInstance)members.iterator().next() : null;
      String newName;
      return member != null && (newName = member.getNewName()) != null ? newName : name;
   }

   @Override
   public String mapMethodArg(String methodOwner, String methodName, String methodDesc, int lvIndex, String name) {
      String newName = this.tr.methodArgMap.get(methodOwner + "/" + MemberInstance.getMethodId(methodName, methodDesc) + lvIndex);
      if (newName != null) {
         return newName;
      }

      ClassInstance cls = this.getClass(methodOwner);
      if (cls == null) {
         return name;
      }

      MemberInstance originatingMethod = cls.resolve(TrMember.MemberType.METHOD, MemberInstance.getMethodId(methodName, methodDesc));
      if (originatingMethod == null) {
         return name;
      }

      String originatingNewName = this.tr
         .methodArgMap
         .get(originatingMethod.newNameOriginatingCls + "/" + MemberInstance.getMethodId(originatingMethod.name, originatingMethod.desc) + lvIndex);
      return originatingNewName != null ? originatingNewName : name;
   }

   public String mapMethodVar(String methodOwner, String methodName, String methodDesc, int lvIndex, int startOpIdx, int asmIndex, String name) {
      String newName = this.tr.methodVarMap.get(methodOwner + "/" + MemberInstance.getMethodId(methodName, methodDesc) + lvIndex);
      return newName != null ? newName : name;
   }

   @Override
   public String mapAnnotationAttributeName(String descriptor, String name) {
      throw new RuntimeException("Deprecated function");
   }

   public String mapAnnotationAttributeName(String annotationDesc, String name, String attributeDesc) {
      String annotationClass = Type.getType(annotationDesc).getInternalName();
      return attributeDesc == null
         ? this.mapMethodNamePrefixDesc(annotationClass, name, "()")
         : this.mapMethodName(annotationClass, name, "()" + attributeDesc);
   }

   void finish(String className, ClassVisitor cv) {
      ClassInstance cls = null;
      if (this.tr.propagateBridges == TinyRemapper.LinkedMethodPropagation.COMPATIBLE
         || this.tr.propagateRecordComponents == TinyRemapper.LinkedMethodPropagation.COMPATIBLE) {
         cls = this.getClass(className);
         if (cls != null) {
            BridgeHandler.generateCompatBridges(cls, this, cv);
         }
      }
   }

   ClassInstance getClass(String owner) {
      return this.context.getClass(owner);
   }
}
