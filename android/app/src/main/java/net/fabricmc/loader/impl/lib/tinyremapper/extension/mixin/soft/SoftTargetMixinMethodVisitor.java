package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.MxMember;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.AccessorAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.InvokerAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.SugarLocalAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.DefinitionAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.DefinitionsAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.InjectAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.ModifyArgAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.ModifyArgsAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.ModifyConstantAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.ModifyExpressionValueAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.ModifyReceiverAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.ModifyReturnValueAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.ModifyVariableAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.RedirectAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.WrapMethodAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.WrapOperationAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.WrapWithConditionAnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection.WrapWithConditionV2AnnotationVisitor;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.data.MemberInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

class SoftTargetMixinMethodVisitor extends MethodVisitor {
   private final CommonData data;
   private final MxMember method;
   private final List<String> targets;
   private final Set<MemberInfo> knownTargetMethods;

   SoftTargetMixinMethodVisitor(CommonData data, MethodVisitor delegate, MxMember method, List<String> targets) {
      super(589824, delegate);
      this.data = Objects.requireNonNull(data);
      this.method = Objects.requireNonNull(method);
      this.targets = Objects.requireNonNull(targets);
      this.knownTargetMethods = new HashSet<>();
   }

   public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
      switch (descriptor) {
         case "Lorg/spongepowered/asm/mixin/gen/Accessor;":
            return new AccessorAnnotationVisitor(this.data, av, this.method, this.targets);
         case "Lorg/spongepowered/asm/mixin/gen/Invoker;":
            return new InvokerAnnotationVisitor(this.data, av, this.method, this.targets);
         case "Lorg/spongepowered/asm/mixin/injection/Inject;":
            return new InjectAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lorg/spongepowered/asm/mixin/injection/ModifyArg;":
            return new ModifyArgAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;":
            return new ModifyArgsAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;":
            return new ModifyConstantAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;":
            return new ModifyVariableAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lorg/spongepowered/asm/mixin/injection/Redirect;":
            return new RedirectAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;":
            return new ModifyExpressionValueAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lcom/llamalad7/mixinextras/injector/ModifyReceiver;":
            return new ModifyReceiverAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;":
            return new ModifyReturnValueAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;":
            return new WrapMethodAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;":
            return new WrapOperationAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;":
            return new WrapWithConditionAnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;":
            return new WrapWithConditionV2AnnotationVisitor(this.data, av, this.targets, this.knownTargetMethods);
         case "Lcom/llamalad7/mixinextras/expression/Definitions;":
            return new DefinitionsAnnotationVisitor(this.data, av);
         case "Lcom/llamalad7/mixinextras/expression/Definition;":
            return new DefinitionAnnotationVisitor(this.data, av);
         default:
            return av;
      }
   }

   public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
      AnnotationVisitor av = super.visitParameterAnnotation(parameter, descriptor, visible);
      switch (descriptor) {
         case "Lcom/llamalad7/mixinextras/sugar/Local;":
            return new SugarLocalAnnotationVisitor(this.data, av, this.knownTargetMethods);
         default:
            return av;
      }
   }
}
