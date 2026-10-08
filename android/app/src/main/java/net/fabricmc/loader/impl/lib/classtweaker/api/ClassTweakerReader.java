package net.fabricmc.loader.impl.lib.classtweaker.api;

import java.io.BufferedReader;
import java.io.IOException;
import net.fabricmc.loader.impl.lib.classtweaker.api.visitor.ClassTweakerVisitor;
import net.fabricmc.loader.impl.lib.classtweaker.reader.ClassTweakerReaderImpl;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface ClassTweakerReader {
   static ClassTweakerReader create(ClassTweakerVisitor visitor) {
      return new ClassTweakerReaderImpl(visitor);
   }

   void read(byte[] var1, String var2);

   void read(BufferedReader var1, String var2) throws IOException;
}
