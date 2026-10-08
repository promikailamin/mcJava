package net.fabricmc.loader.api.metadata;

import java.util.Map.Entry;

public interface CustomValue {
   CustomValue.CvType getType();

   CustomValue.CvObject getAsObject();

   CustomValue.CvArray getAsArray();

   String getAsString();

   Number getAsNumber();

   boolean getAsBoolean();

   interface CvArray extends Iterable<CustomValue>, CustomValue {
      int size();

      CustomValue get(int var1);
   }

   interface CvObject extends Iterable<Entry<String, CustomValue>>, CustomValue {
      int size();

      boolean containsKey(String var1);

      CustomValue get(String var1);
   }

   enum CvType {
      OBJECT,
      ARRAY,
      STRING,
      NUMBER,
      BOOLEAN,
      NULL;
   }
}
