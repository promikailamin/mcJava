package net.fabricmc.loader.impl.lib.tinyremapper.api;

public interface TrEnvironment {
   int getMrjVersion();

   TrRemapper getRemapper();

   TrLogger getLogger();

   TrClass getClass(String var1);

   void propagate(TrMember var1, String var2);
}
