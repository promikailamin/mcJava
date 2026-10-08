package net.fabricmc.loader.impl.lib.tinyremapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.FieldRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.RecordComponentRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

final class AsmClassRemapper extends VisitTrackingClassRemapper {
   private final boolean rebuildSourceFilenames;
   private final boolean checkPackageAccess;
   private final boolean skipLocalMapping;
   private final boolean renameInvalidLocals;
   private final Pattern invalidLvNamePattern;
   private final boolean inferNameFromSameLvIndex;
   private boolean sourceNameVisited;
   private MethodNode methodNode;

   AsmClassRemapper(
      ClassVisitor cv,
      AsmRemapper remapper,
      boolean rebuildSourceFilenames,
      boolean checkPackageAccess,
      boolean skipLocalMapping,
      boolean renameInvalidLocals,
      Pattern invalidLvNamePattern,
      boolean inferNameFromSameLvIndex
   ) {
      super(cv, remapper);
      this.rebuildSourceFilenames = rebuildSourceFilenames;
      this.checkPackageAccess = checkPackageAccess;
      this.skipLocalMapping = skipLocalMapping;
      this.renameInvalidLocals = renameInvalidLocals;
      this.invalidLvNamePattern = invalidLvNamePattern;
      this.inferNameFromSameLvIndex = inferNameFromSameLvIndex;
   }

   @Override
   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      if (this.checkPackageAccess) {
         AsmRemapper remapper = (AsmRemapper)this.remapper;
         if (superName != null) {
            PackageAccessChecker.checkClass(name, superName, "super class", remapper);
         }

         if (interfaces != null) {
            for (String iface : interfaces) {
               PackageAccessChecker.checkClass(name, iface, "super interface", remapper);
            }
         }
      }

      this.sourceNameVisited = false;
      super.visit(version, access, name, signature, superName, interfaces);
   }

   @Override
   public void visitSource(String source, String debug) {
      this.sourceNameVisited = true;
      if (!this.rebuildSourceFilenames) {
         super.visitSource(source, debug);
      } else {
         String mappedClsName = this.remapper.map(this.className);
         int end = mappedClsName.indexOf(36);
         if (end <= 0) {
            end = mappedClsName.length();
         }

         int start = mappedClsName.lastIndexOf(47, end - 1) + 1;
         if (end <= start) {
            end = mappedClsName.length();
         }

         super.visitSource(mappedClsName.substring(start, end).concat(".java"), debug);
      }
   }

   @Override
   public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
      if (this.checkPackageAccess) {
         PackageAccessChecker.checkDesc(this.className, descriptor, "field descriptor", (AsmRemapper)this.remapper);
      }

      return super.visitField(access, name, descriptor, signature, value);
   }

   protected FieldVisitor createFieldRemapper(FieldVisitor fieldVisitor) {
      return new AsmClassRemapper.AsmFieldRemapper(fieldVisitor, (AsmRemapper)this.remapper);
   }

   @Override
   public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      if (this.checkPackageAccess) {
         PackageAccessChecker.checkDesc(this.className, descriptor, "method descriptor", (AsmRemapper)this.remapper);
      }

      if (!this.skipLocalMapping || this.renameInvalidLocals) {
         this.methodNode = new MethodNode(this.api, access, name, descriptor, signature, exceptions);
      }

      return super.visitMethod(access, name, descriptor, signature, exceptions);
   }

   protected MethodVisitor createMethodRemapper(MethodVisitor methodVisitor) {
      return new AsmClassRemapper.AsmMethodRemapper(
         methodVisitor,
         (AsmRemapper)this.remapper,
         this.className,
         this.methodNode,
         this.checkPackageAccess,
         this.skipLocalMapping,
         this.renameInvalidLocals,
         this.invalidLvNamePattern,
         this.inferNameFromSameLvIndex
      );
   }

   protected RecordComponentVisitor createRecordComponentRemapper(RecordComponentVisitor recordComponentVisitor) {
      return new AsmClassRemapper.AsmRecordComponentRemapper(recordComponentVisitor, (AsmRemapper)this.remapper);
   }

   public AnnotationVisitor createAnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor) {
      return new AsmClassRemapper.AsmAnnotationRemapper(descriptor, annotationVisitor, (AsmRemapper)this.remapper);
   }

   @Override
   public void visitEnd() {
      ((AsmRemapper)this.remapper).finish(this.className, this.cv);
      super.visitEnd();
   }

   @Override
   protected void onVisit(VisitTrackingClassRemapper.VisitKind kind) {
      if (this.rebuildSourceFilenames && !this.sourceNameVisited && kind.ordinal() > VisitTrackingClassRemapper.VisitKind.SOURCE.ordinal()) {
         this.visitSource(null, null);
      }
   }

   static class AsmAnnotationRemapper extends AnnotationVisitor {
      protected final String descriptor;
      protected final AsmRemapper remapper;

      AsmAnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor, AsmRemapper remapper) {
         super(589824, annotationVisitor);
         this.descriptor = descriptor;
         this.remapper = remapper;
      }

      public void visit(String name, Object value) {
         super.visit(this.mapAnnotationAttributeName(name, getDescriptor(value)), this.remapper.mapValue(value));
      }

      public void visitEnum(String name, String descriptor, String value) {
         super.visitEnum(
            this.mapAnnotationAttributeName(name, descriptor),
            this.remapper.mapDesc(descriptor),
            this.remapper.mapFieldName(Type.getType(descriptor).getInternalName(), value, descriptor)
         );
      }

      public AnnotationVisitor visitAnnotation(String name, String descriptor) {
         AnnotationVisitor annotationVisitor = super.visitAnnotation(this.mapAnnotationAttributeName(name, descriptor), this.remapper.mapDesc(descriptor));
         if (annotationVisitor == null) {
            return null;
         } else {
            return annotationVisitor == this.av ? this : this.createAnnotationRemapper(descriptor, annotationVisitor);
         }
      }

      public AnnotationVisitor createAnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor) {
         return new AsmClassRemapper.AsmAnnotationRemapper(descriptor, annotationVisitor, this.remapper);
      }

      public AnnotationVisitor visitArray(String name) {
         return new AsmClassRemapper.AsmAnnotationRemapper.AsmArrayAttributeAnnotationRemapper(
            name, desc -> super.visitArray(this.mapAnnotationAttributeName(name, desc == null ? null : "[" + desc)), this.remapper
         );
      }

      protected String mapAnnotationAttributeName(String name, String attributeDesc) {
         return this.descriptor != null && name != null ? this.remapper.mapAnnotationAttributeName(this.descriptor, name, attributeDesc) : name;
      }

      protected static String getDescriptor(Object value) {
         if (value instanceof Type) {
            return ((Type)value).getDescriptor();
         } else {
            Class<?> cls = value.getClass();
            if (Byte.class.isAssignableFrom(cls)) {
               return "B";
            } else if (Boolean.class.isAssignableFrom(cls)) {
               return "Z";
            } else if (Character.class.isAssignableFrom(cls)) {
               return "C";
            } else if (Short.class.isAssignableFrom(cls)) {
               return "S";
            } else if (Integer.class.isAssignableFrom(cls)) {
               return "I";
            } else if (Long.class.isAssignableFrom(cls)) {
               return "J";
            } else if (Float.class.isAssignableFrom(cls)) {
               return "F";
            } else {
               return Double.class.isAssignableFrom(cls) ? "D" : Type.getDescriptor(cls);
            }
         }
      }

      private static class AsmArrayAttributeAnnotationRemapper extends AsmClassRemapper.AsmAnnotationRemapper {
         protected final String arrayName;
         protected final Function<String, AnnotationVisitor> avSupplier;

         AsmArrayAttributeAnnotationRemapper(String arrayName, Function<String, AnnotationVisitor> avSupplier, AsmRemapper remapper) {
            super(null, null, remapper);
            this.arrayName = arrayName;
            this.avSupplier = Objects.requireNonNull(avSupplier);
         }

         @Override
         public void visit(String name, Object value) {
            if (this.av == null) {
               this.av = this.avSupplier.apply(getDescriptor(value));
            }

            super.visit(name, value);
         }

         @Override
         public void visitEnum(String name, String descriptor, String value) {
            if (this.av == null) {
               this.av = this.avSupplier.apply(descriptor);
            }

            super.visitEnum(name, descriptor, value);
         }

         @Override
         public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            if (this.av == null) {
               this.av = this.avSupplier.apply(descriptor);
            }

            return super.visitAnnotation(name, descriptor);
         }

         @Override
         public AnnotationVisitor visitArray(String name) {
            return new AsmClassRemapper.AsmAnnotationRemapper.AsmArrayAttributeAnnotationRemapper(name, desc -> {
               if (this.av == null) {
                  this.av = this.avSupplier.apply(desc == null ? null : "[" + desc);
               }

               return super.visitArray(this.mapAnnotationAttributeName(name, desc == null ? null : "[" + desc));
            }, this.remapper);
         }

         public void visitEnd() {
            if (this.av == null) {
               this.av = this.avSupplier.apply(null);
            }

            super.visitEnd();
         }
      }
   }

   static class AsmFieldRemapper extends FieldRemapper {
      AsmFieldRemapper(FieldVisitor fieldVisitor, AsmRemapper remapper) {
         super(fieldVisitor, remapper);
      }

      public AnnotationVisitor createAnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor) {
         return new AsmClassRemapper.AsmAnnotationRemapper(descriptor, annotationVisitor, (AsmRemapper)this.remapper);
      }
   }

   static class AsmMethodRemapper extends MethodRemapper {
      private final TinyRemapper tr;
      private static final String[] singleCharStrings = new String[]{
         "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"
      };
      private final String owner;
      private final MethodNode methodNode;
      private final MethodVisitor output;
      private final Map<String, Integer> nameCounts = new HashMap<>();
      private final boolean checkPackageAccess;
      private final boolean skipLocalMapping;
      private final boolean renameInvalidLocals;
      private final Pattern invalidLvNamePattern;
      private final boolean inferNameFromSameLvIndex;

      AsmMethodRemapper(
         MethodVisitor methodVisitor,
         AsmRemapper remapper,
         String owner,
         MethodNode methodNode,
         boolean checkPackageAccess,
         boolean skipLocalMapping,
         boolean renameInvalidLocals,
         Pattern invalidLvNamePattern,
         boolean inferNameFromSameLvIndex
      ) {
         super((MethodVisitor)(methodNode != null ? methodNode : methodVisitor), remapper);
         this.owner = owner;
         this.methodNode = methodNode;
         this.output = methodVisitor;
         this.checkPackageAccess = checkPackageAccess;
         this.skipLocalMapping = skipLocalMapping;
         this.renameInvalidLocals = renameInvalidLocals;
         this.invalidLvNamePattern = invalidLvNamePattern;
         this.inferNameFromSameLvIndex = inferNameFromSameLvIndex;
         this.tr = remapper.tr;
      }

      public AnnotationVisitor createAnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor) {
         return new AsmClassRemapper.AsmAnnotationRemapper(descriptor, annotationVisitor, (AsmRemapper)this.remapper);
      }

      public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
         if (this.checkPackageAccess) {
            PackageAccessChecker.checkClass(this.owner, type, "try-catch", (AsmRemapper)this.remapper);
         }

         super.visitTryCatchBlock(start, end, handler, type);
      }

      public void visitTypeInsn(int opcode, String type) {
         if (this.checkPackageAccess) {
            PackageAccessChecker.checkClass(this.owner, type, "type instruction", (AsmRemapper)this.remapper);
         }

         super.visitTypeInsn(opcode, type);
      }

      public void visitLdcInsn(Object value) {
         if (this.checkPackageAccess) {
            PackageAccessChecker.checkValue(this.owner, value, "ldc instruction", (AsmRemapper)this.remapper);
         }

         super.visitLdcInsn(value);
      }

      public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
         if (this.checkPackageAccess) {
            PackageAccessChecker.checkDesc(this.owner, descriptor, "multianewarray instruction", (AsmRemapper)this.remapper);
         }

         super.visitMultiANewArrayInsn(descriptor, numDimensions);
      }

      public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
         if (this.checkPackageAccess) {
            PackageAccessChecker.checkMember(this.owner, owner, name, descriptor, TrMember.MemberType.FIELD, "field instruction", (AsmRemapper)this.remapper);
         }

         super.visitFieldInsn(opcode, owner, name, descriptor);
      }

      public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
         if (this.checkPackageAccess) {
            PackageAccessChecker.checkMember(this.owner, owner, name, descriptor, TrMember.MemberType.METHOD, "method instruction", (AsmRemapper)this.remapper);
         }

         super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      }

      public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
         Handle implemented = this.getLambdaImplementedMethod(name, descriptor, bootstrapMethodHandle, this.tr.knownIndyBsm, bootstrapMethodArguments);
         if (implemented != null) {
            name = this.remapper.mapMethodName(implemented.getOwner(), implemented.getName(), implemented.getDesc());
         } else {
            name = this.remapper.mapInvokeDynamicMethodName(name, descriptor);
         }

         for (int i = 0; i < bootstrapMethodArguments.length; i++) {
            bootstrapMethodArguments[i] = this.remapper.mapValue(bootstrapMethodArguments[i]);
         }

         this.mv
            .visitInvokeDynamicInsn(
               name, this.remapper.mapMethodDesc(descriptor), (Handle)this.remapper.mapValue(bootstrapMethodHandle), bootstrapMethodArguments
            );
      }

      private Handle getLambdaImplementedMethod(String name, String desc, Handle bsm, Set<String> knownIndyBsm, Object... bsmArgs) {
         if (isJavaLambdaMetafactory(bsm)) {
            assert desc.endsWith(";");
            return new Handle(9, desc.substring(desc.lastIndexOf(41) + 2, desc.length() - 1), name, ((Type)bsmArgs[0]).getDescriptor(), true);
         }

         if (knownIndyBsm.contains(bsm.getOwner())) {
            return null;
         }

         this.tr
            .getLogger()
            .warn("unknown invokedynamic bsm: %s/%s%s (tag=%d iif=%b)", bsm.getOwner(), bsm.getName(), bsm.getDesc(), bsm.getTag(), bsm.isInterface());
         return null;
      }

      private static boolean isJavaLambdaMetafactory(Handle bsm) {
         return bsm.getTag() == 6
            && bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
            && (
               bsm.getName().equals("metafactory")
                     && bsm.getDesc()
                        .equals(
                           "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
                        )
                  || bsm.getName().equals("altMetafactory")
                     && bsm.getDesc()
                        .equals(
                           "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"
                        )
            )
            && !bsm.isInterface();
      }

      public void visitEnd() {
         if (this.methodNode != null) {
            if (!this.skipLocalMapping
               || this.renameInvalidLocals
                  && (
                     this.methodNode.localVariables != null && !this.methodNode.localVariables.isEmpty()
                        || this.methodNode.parameters != null && !this.methodNode.parameters.isEmpty()
                  )) {
               this.processLocals();
            }

            this.methodNode.visitEnd();
            this.methodNode.accept(this.output);
         } else {
            super.visitEnd();
         }
      }

      private void processLocals() {
         boolean isStatic = (this.methodNode.access & 8) != 0;
         Type[] argTypes = Type.getArgumentTypes(this.methodNode.desc);
         int argLvSize = getLvIndex(argTypes.length, isStatic, argTypes);
         String[] args = new String[argTypes.length];
         if (this.methodNode.parameters != null && this.methodNode.parameters.size() == args.length) {
            for (int i = 0; i < args.length; i++) {
               args[i] = ((ParameterNode)this.methodNode.parameters.get(i)).name;
            }
         } else {
            assert this.methodNode.parameters == null;
         }

         if (this.methodNode.localVariables != null) {
            for (int i = 0; i < this.methodNode.localVariables.size(); i++) {
               LocalVariableNode lv = (LocalVariableNode)this.methodNode.localVariables.get(i);
               if (!isStatic && lv.index == 0) {
                  lv.name = "this";
               } else if (lv.index < argLvSize) {
                  int asmIndex = getAsmIndex(lv.index, isStatic, argTypes);
                  String existingName = args[asmIndex];
                  if (existingName == null || !isValidJavaIdentifier(existingName) && isValidJavaIdentifier(lv.name)) {
                     args[asmIndex] = lv.name;
                  }
               } else if (!this.skipLocalMapping) {
                  int startOpIdx = 0;
                  AbstractInsnNode start = lv.start;

                  while ((start = start.getPrevious()) != null) {
                     if (start.getOpcode() >= 0) {
                        startOpIdx++;
                     }
                  }

                  lv.name = ((AsmRemapper)this.remapper).mapMethodVar(this.owner, this.methodNode.name, this.methodNode.desc, lv.index, startOpIdx, i, lv.name);
                  if (this.renameInvalidLocals && this.isValidLvName(lv.name)) {
                     this.nameCounts.putIfAbsent(lv.name, 1);
                  }
               }
            }
         }

         if (!this.skipLocalMapping) {
            for (int i = 0; i < args.length; i++) {
               args[i] = ((AsmRemapper)this.remapper)
                  .mapMethodArg(this.owner, this.methodNode.name, this.methodNode.desc, getLvIndex(i, isStatic, argTypes), args[i]);
               if (this.renameInvalidLocals && this.isValidLvName(args[i])) {
                  this.nameCounts.putIfAbsent(args[i], 1);
               }
            }
         }

         if (this.renameInvalidLocals) {
            for (int i = 0; i < args.length; i++) {
               if (!this.isValidLvName(args[i])) {
                  args[i] = this.getNameFromType(this.remapper.mapDesc(argTypes[i].getDescriptor()), true);
               }
            }
         }

         boolean hasAnyArgs = false;
         boolean hasAllArgs = true;

         for (String arg : args) {
            if (arg != null) {
               hasAnyArgs = true;
            } else {
               hasAllArgs = false;
            }
         }

         if (this.methodNode.localVariables != null || hasAnyArgs && (this.methodNode.access & 1024) == 0) {
            if (this.methodNode.localVariables == null) {
               this.methodNode.localVariables = new ArrayList();
            }

            boolean[] argsWritten = new boolean[args.length];

            label219:
            for (int i = 0; i < this.methodNode.localVariables.size(); i++) {
               LocalVariableNode lv = (LocalVariableNode)this.methodNode.localVariables.get(i);
               if (isStatic || lv.index != 0) {
                  if (lv.index < argLvSize) {
                     int asmIndex = getAsmIndex(lv.index, isStatic, argTypes);
                     lv.name = args[asmIndex];
                     argsWritten[asmIndex] = true;
                  } else if (this.renameInvalidLocals && !this.isValidLvName(lv.name)) {
                     if (this.inferNameFromSameLvIndex) {
                        for (int j = 0; j < this.methodNode.localVariables.size(); j++) {
                           if (j != i) {
                              LocalVariableNode otherLv = (LocalVariableNode)this.methodNode.localVariables.get(j);
                              if (otherLv.index == lv.index
                                 && otherLv.name != null
                                 && otherLv.desc.equals(lv.desc)
                                 && (j < i || this.isValidLvName(otherLv.name))) {
                                 lv.name = otherLv.name;
                                 continue label219;
                              }
                           }
                        }
                     }

                     lv.name = this.getNameFromType(lv.desc, false);
                  }
               }
            }

            LabelNode start = null;
            LabelNode end = null;

            for (int i = 0; i < args.length; i++) {
               if (!argsWritten[i] && args[i] != null) {
                  if (start == null) {
                     boolean pastStart = false;

                     for (AbstractInsnNode ain : this.methodNode.instructions) {
                        if (ain.getType() == 8) {
                           LabelNode label = (LabelNode)ain;
                           if (start == null && !pastStart) {
                              start = label;
                           }

                           end = label;
                        } else if (ain.getOpcode() >= 0) {
                           pastStart = true;
                           end = null;
                        }
                     }

                     if (start == null) {
                        start = new LabelNode();
                        this.methodNode.instructions.insert(start);
                     }

                     if (end == null) {
                        if (!pastStart) {
                           end = start;
                        } else {
                           end = new LabelNode();
                           this.methodNode.instructions.add(end);
                        }
                     }
                  }

                  this.methodNode
                     .localVariables
                     .add(
                        new LocalVariableNode(args[i], this.remapper.mapDesc(argTypes[i].getDescriptor()), null, start, end, getLvIndex(i, isStatic, argTypes))
                     );
               }
            }
         }

         if (this.methodNode.parameters != null || hasAllArgs && args.length > 0 || hasAnyArgs && (this.methodNode.access & 1024) != 0) {
            if (this.methodNode.parameters == null) {
               this.methodNode.parameters = new ArrayList(args.length);
            }

            while (this.methodNode.parameters.size() < args.length) {
               this.methodNode.parameters.add(new ParameterNode(null, 0));
            }

            for (int i = 0; i < args.length; i++) {
               ((ParameterNode)this.methodNode.parameters.get(i)).name = args[i];
            }
         }
      }

      private static int getLvIndex(int asmIndex, boolean isStatic, Type[] argTypes) {
         int ret = 0;
         if (!isStatic) {
            ret++;
         }

         for (int i = 0; i < asmIndex; i++) {
            ret += argTypes[i].getSize();
         }

         return ret;
      }

      private static int getAsmIndex(int lvIndex, boolean isStatic, Type[] argTypes) {
         if (!isStatic) {
            lvIndex--;
         }

         for (int i = 0; i < argTypes.length; i++) {
            if (lvIndex == 0) {
               return i;
            }

            lvIndex -= argTypes[i].getSize();
         }

         return -1;
      }

      private String getNameFromType(String type, boolean isArg) {
         boolean plural = false;
         if (type.charAt(0) == '[') {
            plural = true;
            type = type.substring(type.lastIndexOf(91) + 1);
         }

         boolean incrementLetter = true;
         String varName;
         switch (type.charAt(0)) {
            case 'B':
               varName = "b";
               break;
            case 'C':
               varName = "c";
               break;
            case 'D':
               varName = "d";
               break;
            case 'E':
            case 'G':
            case 'H':
            case 'K':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            default:
               throw new IllegalStateException();
            case 'F':
               varName = "f";
               break;
            case 'I':
               varName = "i";
               break;
            case 'J':
               varName = "l";
               break;
            case 'L':
               int start = type.lastIndexOf(47) + 1;
               int startDollar = type.lastIndexOf(36) + 1;
               if (startDollar > start && startDollar < type.length() - 1) {
                  start = startDollar;
               } else if (start == 0) {
                  start = 1;
               }

               char first = type.charAt(start);
               char firstLc = Character.toLowerCase(first);
               if (first == firstLc) {
                  varName = null;
               } else {
                  varName = firstLc + type.substring(start + 1, type.length() - 1);
               }

               if (!isValidJavaIdentifier(varName)) {
                  varName = isArg ? "arg" : "lv";
               }

               incrementLetter = false;
               break;
            case 'S':
               varName = "s";
               break;
            case 'Z':
               varName = "bl";
               incrementLetter = false;
         }

         boolean hasPluralS = false;
         if (plural) {
            String pluralVarName = varName + 's';
            if (!isJavaKeyword(pluralVarName)) {
               varName = pluralVarName;
               hasPluralS = true;
            }
         }

         if (incrementLetter) {
            for (int index = -1; this.nameCounts.putIfAbsent(varName, 1) != null || isJavaKeyword(varName); varName = getIndexName(++index, plural)) {
               if (index < 0) {
                  index = getNameIndex(varName, hasPluralS);
               }
            }

            return varName;
         } else {
            String baseVarName = varName;
            int count = this.nameCounts.compute(baseVarName, (k, v) -> v == null ? 1 : v + 1);
            if (count == 1) {
               if (!isJavaKeyword(baseVarName)) {
                  return varName;
               }

               varName = varName + '_';
            } else {
               varName = baseVarName + Integer.toString(count);
            }

            while (this.nameCounts.putIfAbsent(varName, 1) != null) {
               varName = baseVarName + Integer.toString(count++);
            }

            this.nameCounts.put(baseVarName, count);
            return varName;
         }
      }

      private static int getNameIndex(String name, boolean plural) {
         int ret = 0;
         int i = 0;

         for (int max = name.length() - (plural ? 1 : 0); i < max; i++) {
            ret = ret * 26 + name.charAt(i) - 97 + 1;
         }

         return ret - 1;
      }

      private static String getIndexName(int index, boolean plural) {
         if (index < 26 && !plural) {
            return singleCharStrings[index];
         }

         StringBuilder ret = new StringBuilder(2);

         do {
            int next = index / 26;
            int cur = index - next * 26;
            ret.append((char)(97 + cur));
            index = next - 1;
         } while (index >= 0);

         ret.reverse();
         if (plural) {
            ret.append('s');
         }

         return ret.toString();
      }

      private boolean isValidLvName(String s) {
         return isValidJavaIdentifier(s) && !isJavaKeyword(s) && (this.invalidLvNamePattern == null || !this.invalidLvNamePattern.matcher(s).matches());
      }

      private static boolean isValidJavaIdentifier(String s) {
         return s != null && !s.isEmpty() && SourceVersion.isIdentifier(s) && !s.codePoints().anyMatch(Character::isIdentifierIgnorable);
      }

      private static boolean isJavaKeyword(String s) {
         return SourceVersion.isKeyword(s);
      }
   }

   static class AsmRecordComponentRemapper extends RecordComponentRemapper {
      AsmRecordComponentRemapper(RecordComponentVisitor recordComponentVisitor, AsmRemapper remapper) {
         super(recordComponentVisitor, remapper);
      }

      public AnnotationVisitor createAnnotationRemapper(String descriptor, AnnotationVisitor annotationVisitor) {
         return new AsmClassRemapper.AsmAnnotationRemapper(descriptor, annotationVisitor, (AsmRemapper)this.remapper);
      }
   }
}
