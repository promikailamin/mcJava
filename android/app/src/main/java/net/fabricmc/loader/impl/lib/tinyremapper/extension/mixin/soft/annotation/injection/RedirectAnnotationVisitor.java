package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation.injection;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.data.MemberInfo;
import org.objectweb.asm.AnnotationVisitor;

public class RedirectAnnotationVisitor extends CommonInjectionAnnotationVisitor {
   public RedirectAnnotationVisitor(CommonData data, AnnotationVisitor delegate, List<String> targets, Set<MemberInfo> knownTargetMethods) {
      super(data, delegate, targets, knownTargetMethods);
   }
}
