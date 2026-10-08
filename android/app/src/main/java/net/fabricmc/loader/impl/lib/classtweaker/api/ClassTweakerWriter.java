package net.fabricmc.loader.impl.lib.classtweaker.api;

import net.fabricmc.loader.impl.lib.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.loader.impl.lib.classtweaker.writer.ClassTweakerWriterImpl;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface ClassTweakerWriter extends ClassTweakerVisitor {
   static ClassTweakerWriter create(int version) {
      return new ClassTweakerWriterImpl(version);
   }

   byte[] getOutput();
}
