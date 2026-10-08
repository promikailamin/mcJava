package net.fabricmc.loader.impl.metadata;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.lib.gson.JsonReader;
import net.fabricmc.loader.impl.lib.gson.JsonToken;
import net.fabricmc.loader.impl.util.LoaderUtil;

public final class DependencyOverrides {
   private final Map<String, List<DependencyOverrides.Entry>> dependencyOverrides;

   public DependencyOverrides(Path configDir) {
      Path path = configDir.resolve("fabric_loader_dependencies.json");
      if (!Files.exists(path)) {
         this.dependencyOverrides = Collections.emptyMap();
      } else {
         try {
            JsonReader reader = new JsonReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8));

            try {
               this.dependencyOverrides = parse(reader);
            } catch (Throwable var7) {
               try {
                  reader.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }

               throw var7;
            }

            reader.close();
         } catch (IOException | ParseMetadataException e) {
            throw FormattedException.ofLocalized("exception.parsingOverride", "Failed to parse " + LoaderUtil.normalizePath(path), e);
         }
      }
   }

   private static Map<String, List<DependencyOverrides.Entry>> parse(JsonReader reader) throws ParseMetadataException, IOException {
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         throw new ParseMetadataException("Root must be an object", reader);
      }

      Map<String, List<DependencyOverrides.Entry>> ret = new HashMap<>();
      reader.beginObject();
      if (!reader.nextName().equals("version")) {
         throw new ParseMetadataException("First key must be \"version\"", reader);
      }

      if (reader.peek() == JsonToken.NUMBER && reader.nextInt() == 1) {
         while (reader.hasNext()) {
            String key = reader.nextName();
            if (!"overrides".equals(key)) {
               throw new ParseMetadataException("Unsupported root key: " + key, reader);
            }

            reader.beginObject();

            while (reader.hasNext()) {
               String modId = reader.nextName();
               ret.put(modId, readKeys(reader));
            }

            reader.endObject();
         }

         reader.endObject();
         return ret;
      } else {
         throw new ParseMetadataException("Unsupported \"version\", must be 1", reader);
      }
   }

   private static List<DependencyOverrides.Entry> readKeys(JsonReader reader) throws IOException, ParseMetadataException {
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         throw new ParseMetadataException("Dependency container must be an object!", reader);
      }

      Map<ModDependency.Kind, Map<DependencyOverrides.Operation, List<ModDependency>>> modOverrides = new EnumMap<>(ModDependency.Kind.class);
      reader.beginObject();

      while (reader.hasNext()) {
         String key = reader.nextName();
         DependencyOverrides.Operation op = null;

         for (DependencyOverrides.Operation o : DependencyOverrides.Operation.VALUES) {
            if (key.startsWith(o.operator)) {
               op = o;
               key = key.substring(o.operator.length());
               break;
            }
         }

         assert op != null;
         ModDependency.Kind kind = ModDependency.Kind.parse(key);
         if (kind == null) {
            throw new ParseMetadataException(
               String.format(
                  "%s is not an allowed dependency key, must be one of: %s",
                  key,
                  Arrays.stream(ModDependency.Kind.values()).map(ModDependency.Kind::getKey).collect(Collectors.joining(", "))
               ),
               reader
            );
         }

         List<ModDependency> deps = readDependencies(reader, kind);
         if (!deps.isEmpty() || op == DependencyOverrides.Operation.REPLACE) {
            modOverrides.computeIfAbsent(kind, ignore -> new EnumMap<>(DependencyOverrides.Operation.class)).put(op, deps);
         }
      }

      reader.endObject();
      List<DependencyOverrides.Entry> ret = new ArrayList<>();

      for (Map.Entry<ModDependency.Kind, Map<DependencyOverrides.Operation, List<ModDependency>>> entry : modOverrides.entrySet()) {
         ModDependency.Kind kind = entry.getKey();
         Map<DependencyOverrides.Operation, List<ModDependency>> map = entry.getValue();
         List<ModDependency> values = map.get(DependencyOverrides.Operation.REPLACE);
         if (values != null) {
            ret.add(new DependencyOverrides.Entry(DependencyOverrides.Operation.REPLACE, kind, values));
         } else {
            values = map.get(DependencyOverrides.Operation.REMOVE);
            if (values != null) {
               ret.add(new DependencyOverrides.Entry(DependencyOverrides.Operation.REMOVE, kind, values));
            }

            values = map.get(DependencyOverrides.Operation.ADD);
            if (values != null) {
               ret.add(new DependencyOverrides.Entry(DependencyOverrides.Operation.ADD, kind, values));
            }
         }
      }

      return ret;
   }

   private static List<ModDependency> readDependencies(JsonReader reader, ModDependency.Kind kind) throws IOException, ParseMetadataException {
      if (reader.peek() != JsonToken.BEGIN_OBJECT) {
         throw new ParseMetadataException("Dependency container must be an object!", reader);
      }

      List<ModDependency> ret = new ArrayList<>();
      reader.beginObject();

      while (reader.hasNext()) {
         String modId = reader.nextName();
         List<String> matcherStringList = new ArrayList<>();
         switch (reader.peek()) {
            case STRING:
               matcherStringList.add(reader.nextString());
               break;
            case BEGIN_ARRAY:
               reader.beginArray();

               while (reader.hasNext()) {
                  if (reader.peek() != JsonToken.STRING) {
                     throw new ParseMetadataException("Dependency version range array must only contain string values", reader);
                  }

                  matcherStringList.add(reader.nextString());
               }

               reader.endArray();
               break;
            default:
               throw new ParseMetadataException("Dependency version range must be a string or string array!", reader);
         }

         try {
            ret.add(new ModDependencyImpl(kind, modId, matcherStringList));
         } catch (VersionParsingException e) {
            throw new ParseMetadataException(e);
         }
      }

      reader.endObject();
      return ret;
   }

   public void apply(LoaderModMetadata metadata) {
      if (!this.dependencyOverrides.isEmpty()) {
         List<DependencyOverrides.Entry> modOverrides = this.dependencyOverrides.get(metadata.getId());
         if (modOverrides != null) {
            List<ModDependency> deps = new ArrayList<>(metadata.getDependencies());

            for (DependencyOverrides.Entry entry : modOverrides) {
               switch (entry.operation) {
                  case ADD:
                     deps.addAll(entry.values);
                     break;
                  case REMOVE:
                     Iterator<ModDependency> it = deps.iterator();

                     while (it.hasNext()) {
                        ModDependency dep = it.next();
                        if (dep.getKind() == entry.kind) {
                           for (ModDependency value : entry.values) {
                              if (value.getModId().equals(dep.getModId())) {
                                 it.remove();
                                 break;
                              }
                           }
                        }
                     }
                     break;
                  case REPLACE:
                     Iterator<ModDependency> it = deps.iterator();

                     while (it.hasNext()) {
                        ModDependency dep = it.next();
                        if (dep.getKind() == entry.kind) {
                           it.remove();
                        }
                     }

                     deps.addAll(entry.values);
               }
            }

            metadata.setDependencies(deps);
         }
      }
   }

   public Collection<String> getAffectedModIds() {
      return this.dependencyOverrides.keySet();
   }

   private static final class Entry {
      final DependencyOverrides.Operation operation;
      final ModDependency.Kind kind;
      final List<ModDependency> values;

      Entry(DependencyOverrides.Operation operation, ModDependency.Kind kind, List<ModDependency> values) {
         this.operation = operation;
         this.kind = kind;
         this.values = values;
      }
   }

   private enum Operation {
      ADD("+"),
      REMOVE("-"),
      REPLACE("");

      static final DependencyOverrides.Operation[] VALUES = values();
      final String operator;

      Operation(String operator) {
         this.operator = operator;
      }
   }
}
