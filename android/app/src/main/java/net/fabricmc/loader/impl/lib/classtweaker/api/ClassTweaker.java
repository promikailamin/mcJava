package net.fabricmc.loader.impl.lib.classtweaker.api;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import net.fabricmc.loader.impl.lib.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.loader.impl.lib.classtweaker.impl.ClassTweakerImpl;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.objectweb.asm.ClassVisitor;

@NonExtendable
public interface ClassTweaker extends ClassTweakerVisitor {
   static ClassTweaker newInstance() {
      return new ClassTweakerImpl();
   }

   Set<String> getTargets();

   AccessWidener getAccessWidener(String var1);

   List<InjectedInterface> getInjectedInterfaces(String var1);

   List<EnumExtension> getEnumExtensions(String var1);

   ClassVisitor createClassVisitor(int var1, @Nullable ClassVisitor var2, @Nullable BiConsumer<String, byte[]> var3);
}
