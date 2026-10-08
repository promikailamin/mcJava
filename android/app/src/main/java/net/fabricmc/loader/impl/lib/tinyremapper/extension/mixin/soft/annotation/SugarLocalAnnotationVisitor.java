package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.annotation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.ResolveUtility;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.data.MemberInfo;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.util.LocalsMapper;
import org.objectweb.asm.AnnotationVisitor;

public class SugarLocalAnnotationVisitor extends AnnotationVisitor {
   private final CommonData data;
   private final Set<MemberInfo> knownTargetMethods;

   public SugarLocalAnnotationVisitor(CommonData data, AnnotationVisitor delegate, Set<MemberInfo> knownTargetMethods) {
      super(589824, delegate);
      this.data = Objects.requireNonNull(data);
      this.knownTargetMethods = Objects.requireNonNull(knownTargetMethods);
   }

   public AnnotationVisitor visitArray(String name) {
      AnnotationVisitor av = super.visitArray(name);
      return name.equals("name")
         ? new AnnotationVisitor(589824, av) {
            public void visit(String name, Object value) {
               String localName = ((String)Objects.requireNonNull((String)value)).replaceAll("\\s", "");
               List<TrMethod> targetMethods = SugarLocalAnnotationVisitor.this.knownTargetMethods
                  .stream()
                  .map(
                     memberInfo -> SugarLocalAnnotationVisitor.this.data
                        .resolver
                        .resolveMethod(memberInfo.getOwner(), memberInfo.getName(), memberInfo.getDesc(), ResolveUtility.FLAG_UNIQUE)
                  )
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .collect(Collectors.toList());
               List<String> collection = targetMethods.stream()
                  .map(m -> LocalsMapper.mapLocal(SugarLocalAnnotationVisitor.this.data, m, localName))
                  .sorted()
                  .distinct()
                  .collect(Collectors.toList());
               if (collection.size() > 1) {
                  SugarLocalAnnotationVisitor.this.data.getLogger().error("Conflict mapping detected, %s -> %s.", localName, collection);
               } else if (collection.isEmpty()) {
                  SugarLocalAnnotationVisitor.this.data
                     .getLogger()
                     .warn("Cannot remap %s because it does not exist in any of the targets %s", localName, targetMethods);
               }

               super.visit(name, collection.stream().findFirst().orElse(localName));
            }
         }
         : av;
   }
}
