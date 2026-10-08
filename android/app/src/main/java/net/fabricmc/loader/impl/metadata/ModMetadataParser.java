package net.fabricmc.loader.impl.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.impl.lib.gson.JsonReader;
import net.fabricmc.loader.impl.lib.gson.JsonToken;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

public final class ModMetadataParser {
   public static final int LATEST_VERSION = 1;
   public static final Set<String> IGNORED_KEYS = Collections.singleton("$schema");

   public static LoaderModMetadata parseMetadata(
      InputStream is, String modPath, List<String> modParentPaths, VersionOverrides versionOverrides, DependencyOverrides depOverrides, boolean isDevelopment
   ) throws ParseMetadataException {
      try {
         LoaderModMetadata ret = readModMetadata(is, isDevelopment);
         versionOverrides.apply(ret);
         depOverrides.apply(ret);
         MetadataVerifier.verify(ret, isDevelopment);
         return ret;
      } catch (ParseMetadataException e) {
         e.setModPaths(modPath, modParentPaths);
         throw e;
      } catch (Throwable t) {
         ParseMetadataException e = new ParseMetadataException(t);
         e.setModPaths(modPath, modParentPaths);
         throw e;
      }
   }

   private static LoaderModMetadata readModMetadata(InputStream is, boolean isDevelopment) throws IOException, ParseMetadataException {
      int schemaVersion = 0;
      JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));

      LoaderModMetadata var7;
      label55: {
         LoaderModMetadata ret;
         try {
            reader.setRewindEnabled(true);
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
               throw new ParseMetadataException("Root of \"fabric.mod.json\" must be an object", reader);
            }

            reader.beginObject();
            boolean firstField = true;

            while (reader.hasNext()) {
               String key = reader.nextName();
               if (key.equals("schemaVersion")) {
                  if (reader.peek() != JsonToken.NUMBER) {
                     throw new ParseMetadataException("\"schemaVersion\" must be a number.", reader);
                  }

                  schemaVersion = reader.nextInt();
                  if (firstField) {
                     reader.setRewindEnabled(false);
                     ret = readModMetadata(reader, schemaVersion);
                     reader.endObject();
                     var7 = ret;
                     break label55;
                  }
                  break;
               }

               reader.skipValue();
               if (!IGNORED_KEYS.contains(key)) {
                  firstField = false;
               }
            }

            reader.rewind();
            reader.setRewindEnabled(false);
            reader.beginObject();
            LoaderModMetadata retx = readModMetadata(reader, schemaVersion);
            reader.endObject();
            if (isDevelopment) {
               Log.warn(LogCategory.METADATA, "\"fabric.mod.json\" from mod %s did not have \"schemaVersion\" as first field.", retx.getId());
            }

            ret = retx;
         } catch (Throwable var9) {
            try {
               reader.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         reader.close();
         return ret;
      }

      reader.close();
      return var7;
   }

   private static LoaderModMetadata readModMetadata(JsonReader reader, int schemaVersion) throws IOException, ParseMetadataException {
      switch (schemaVersion) {
         case 0:
            return V0ModMetadataParser.parse(reader);
         case 1:
            return V1ModMetadataParser.parse(reader);
         default:
            if (schemaVersion > 0) {
               throw new ParseMetadataException(
                  String.format(
                     "This version of fabric-loader doesn't support the newer schema version of \"%s\"\nPlease update fabric-loader to be able to read this.",
                     schemaVersion
                  )
               );
            } else {
               throw new ParseMetadataException(String.format("Invalid/Unsupported schema version \"%s\" was found", schemaVersion));
            }
      }
   }

   static void logWarningMessages(String id, List<ParseWarning> warnings) {
      if (!warnings.isEmpty()) {
         StringBuilder message = new StringBuilder();
         message.append(String.format("The mod \"%s\" contains invalid entries in its mod json:", id));

         for (ParseWarning warning : warnings) {
            message.append(String.format("\n- %s \"%s\" at line %d column %d", warning.getReason(), warning.getKey(), warning.getLine(), warning.getColumn()));
         }

         Log.warn(LogCategory.METADATA, message.toString());
      }
   }

   private ModMetadataParser() {
   }
}
