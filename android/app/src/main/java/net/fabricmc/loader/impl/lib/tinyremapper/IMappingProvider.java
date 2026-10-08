package net.fabricmc.loader.impl.lib.tinyremapper;

@FunctionalInterface
public interface IMappingProvider {
   void load(IMappingProvider.MappingAcceptor var1);

   interface MappingAcceptor {
      void acceptClass(String var1, String var2);

      void acceptMethod(IMappingProvider.Member var1, String var2);

      void acceptMethodArg(IMappingProvider.Member var1, int var2, String var3);

      void acceptMethodVar(IMappingProvider.Member var1, int var2, int var3, int var4, String var5);

      void acceptField(IMappingProvider.Member var1, String var2);
   }

   final class Member {
      public String owner;
      public String name;
      public String desc;

      public Member(String owner, String name, String desc) {
         this.owner = owner;
         this.name = name;
         this.desc = desc;
      }
   }
}
