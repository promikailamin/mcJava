package net.fabricmc.loader.impl.lib.sat4j.specs;

public interface ILogAble {
   ILogAble CONSOLE = new ILogAble() {
      @Override
      public void log(String message) {
         System.out.println(message);
      }
   };

   void log(String var1);
}
