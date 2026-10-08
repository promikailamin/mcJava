package net.fabricmc.loader.impl.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.impl.lib.gson.JsonReader;

abstract class CustomValueImpl implements CustomValue {
   static final CustomValue BOOLEAN_TRUE = new CustomValueImpl.BooleanImpl(true);
   static final CustomValue BOOLEAN_FALSE = new CustomValueImpl.BooleanImpl(false);
   static final CustomValue NULL = new CustomValueImpl.NullImpl();

   public static CustomValue readCustomValue(JsonReader reader) throws IOException, ParseMetadataException {
      switch (reader.peek()) {
         case BEGIN_OBJECT:
            reader.beginObject();
            Map<String, CustomValue> values = new LinkedHashMap<>();

            while (reader.hasNext()) {
               values.put(reader.nextName(), readCustomValue(reader));
            }

            reader.endObject();
            return new CustomValueImpl.ObjectImpl(values);
         case BEGIN_ARRAY:
            reader.beginArray();
            List<CustomValue> entries = new ArrayList<>();

            while (reader.hasNext()) {
               entries.add(readCustomValue(reader));
            }

            reader.endArray();
            return new CustomValueImpl.ArrayImpl(entries);
         case STRING:
            return new CustomValueImpl.StringImpl(reader.nextString());
         case NUMBER:
            return new CustomValueImpl.NumberImpl(reader.nextDouble());
         case BOOLEAN:
            if (reader.nextBoolean()) {
               return BOOLEAN_TRUE;
            }

            return BOOLEAN_FALSE;
         case NULL:
            reader.nextNull();
            return NULL;
         default:
            throw new ParseMetadataException(Objects.toString(reader.nextName()), reader);
      }
   }

   @Override
   public final CustomValue.CvObject getAsObject() {
      if (this instanceof CustomValueImpl.ObjectImpl) {
         return (CustomValueImpl.ObjectImpl)this;
      } else {
         throw new ClassCastException("can't convert " + this.getType().name() + " to Object");
      }
   }

   @Override
   public final CustomValue.CvArray getAsArray() {
      if (this instanceof CustomValueImpl.ArrayImpl) {
         return (CustomValueImpl.ArrayImpl)this;
      } else {
         throw new ClassCastException("can't convert " + this.getType().name() + " to Array");
      }
   }

   @Override
   public final String getAsString() {
      if (this instanceof CustomValueImpl.StringImpl) {
         return ((CustomValueImpl.StringImpl)this).value;
      } else {
         throw new ClassCastException("can't convert " + this.getType().name() + " to String");
      }
   }

   @Override
   public final Number getAsNumber() {
      if (this instanceof CustomValueImpl.NumberImpl) {
         return ((CustomValueImpl.NumberImpl)this).value;
      } else {
         throw new ClassCastException("can't convert " + this.getType().name() + " to Number");
      }
   }

   @Override
   public final boolean getAsBoolean() {
      if (this instanceof CustomValueImpl.BooleanImpl) {
         return ((CustomValueImpl.BooleanImpl)this).value;
      } else {
         throw new ClassCastException("can't convert " + this.getType().name() + " to Boolean");
      }
   }

   private static final class ArrayImpl extends CustomValueImpl implements CustomValue.CvArray {
      private final List<CustomValue> entries;

      ArrayImpl(List<CustomValue> entries) {
         this.entries = Collections.unmodifiableList(entries);
      }

      @Override
      public CustomValue.CvType getType() {
         return CustomValue.CvType.ARRAY;
      }

      @Override
      public int size() {
         return this.entries.size();
      }

      @Override
      public CustomValue get(int index) {
         return this.entries.get(index);
      }

      @Override
      public Iterator<CustomValue> iterator() {
         return this.entries.iterator();
      }
   }

   private static final class BooleanImpl extends CustomValueImpl {
      final boolean value;

      BooleanImpl(boolean value) {
         this.value = value;
      }

      @Override
      public CustomValue.CvType getType() {
         return CustomValue.CvType.BOOLEAN;
      }
   }

   private static final class NullImpl extends CustomValueImpl {
      private NullImpl() {
      }

      @Override
      public CustomValue.CvType getType() {
         return CustomValue.CvType.NULL;
      }
   }

   private static final class NumberImpl extends CustomValueImpl {
      final Number value;

      NumberImpl(Number value) {
         this.value = value;
      }

      @Override
      public CustomValue.CvType getType() {
         return CustomValue.CvType.NUMBER;
      }
   }

   private static final class ObjectImpl extends CustomValueImpl implements CustomValue.CvObject {
      private final Map<String, CustomValue> entries;

      ObjectImpl(Map<String, CustomValue> entries) {
         this.entries = Collections.unmodifiableMap(entries);
      }

      @Override
      public CustomValue.CvType getType() {
         return CustomValue.CvType.OBJECT;
      }

      @Override
      public int size() {
         return this.entries.size();
      }

      @Override
      public boolean containsKey(String key) {
         return this.entries.containsKey(key);
      }

      @Override
      public CustomValue get(String key) {
         return this.entries.get(key);
      }

      @Override
      public Iterator<Entry<String, CustomValue>> iterator() {
         return this.entries.entrySet().iterator();
      }
   }

   private static final class StringImpl extends CustomValueImpl {
      final String value;

      StringImpl(String value) {
         this.value = value;
      }

      @Override
      public CustomValue.CvType getType() {
         return CustomValue.CvType.STRING;
      }
   }
}
