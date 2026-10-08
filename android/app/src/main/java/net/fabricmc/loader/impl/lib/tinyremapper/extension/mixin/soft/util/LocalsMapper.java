package net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.soft.util;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLocal;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.data.CommonData;

public class LocalsMapper {
   public static String mapLocal(CommonData data, TrMethod target, String localName) {
      TrLocal[] localVariables = target.getLocals();
      if (localVariables != null && localVariables.length != 0) {
         Map<String, Integer> lvtName2Index = new HashMap<>();

         for (TrLocal variable : localVariables) {
            if (!lvtName2Index.containsKey(variable.getName())) {
               lvtName2Index.put(variable.getName(), variable.getIndex());
            } else {
               lvtName2Index.put(variable.getName(), -1);
            }
         }

         if (!lvtName2Index.containsKey(localName)) {
            return localName;
         }

         int lvIndex = lvtName2Index.get(localName);
         return lvIndex < 0
            ? localName
            : data.mapper.asTrRemapper().mapMethodArg(target.getOwner().getName(), target.getName(), target.getDesc(), lvIndex, localName);
      } else {
         return localName;
      }
   }
}
