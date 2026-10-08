package net.fabricmc.loader.impl.game.minecraft;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.impl.lib.gson.JsonReader;
import net.fabricmc.loader.impl.lib.gson.JsonToken;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.SimpleClassPath;
import net.fabricmc.loader.impl.util.version.SemanticVersionImpl;
import net.fabricmc.loader.impl.util.version.VersionPredicateParser;
import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public final class McVersionLookup {
   private static final Pattern DATE_BASED_PATTERN = Pattern.compile("(\\d{2}\\.\\d+(?:\\.\\d+)?)(?:-(snapshot|pre|rc)-(\\d+))?");
   private static final Pattern RELEASE_PATTERN = Pattern.compile("(1\\.(\\d+)(?:\\.(\\d+))?)(?:-(\\d+))?(?:[ _]([Uu]nobfuscated))?");
   private static final Pattern TEST_BUILD_PATTERN = Pattern.compile(".+(?:-tb| Test Build )(\\d+)?(?:-(\\d+))?");
   private static final Pattern PRE_RELEASE_PATTERN = Pattern.compile(
      ".+(?:-pre| Pre-?[Rr]elease ?)(?:(\\d+)(?: ;\\))?)?(?:-(\\d+))?(?:[ _]([Uu]nobfuscated))?"
   );
   private static final Pattern RELEASE_CANDIDATE_PATTERN = Pattern.compile(".+(?:-rc| RC| [Rr]elease Candidate )(\\d+)(?:-(\\d+))?(?:[ _]([Uu]nobfuscated))?");
   private static final Pattern SNAPSHOT_PATTERN = Pattern.compile("(?:Snapshot )?(\\d+)w0?(0|[1-9]\\d*)([a-z])(?:-(\\d+)|[ _]([Uu]nobfuscated))?");
   private static final Pattern EXPERIMENTAL_PATTERN = Pattern.compile(
      ".+(?:-exp|(?:_deep_dark)?_experimental[_-]snapshot-|(?: Deep Dark)? [Ee]xperimental [Ss]napshot )(\\d+)"
   );
   private static final Pattern BETA_PATTERN = Pattern.compile("(?:b|Beta v?)1\\.((\\d+)(?:\\.(\\d+))?(_0\\d)?)([a-z])?(?:-(\\d+))?(?:-(launcher))?");
   private static final Pattern ALPHA_PATTERN = Pattern.compile("(?:(?:server-)?a|Alpha v?)[01]\\.(\\d+\\.\\d+(?:_0\\d)?)([a-z])?(?:-(\\d+))?(?:-(launcher))?");
   private static final Pattern INDEV_PATTERN = Pattern.compile("(?:inf?-|Inf?dev )(?:0\\.31 )?(\\d+)(?:-(\\d+))?");
   private static final Pattern CLASSIC_SERVER_PATTERN = Pattern.compile("(?:(?:server-)?c)1\\.(\\d\\d?(?:\\.\\d)?)(?:-(\\d+))?");
   private static final Pattern LATE_CLASSIC_PATTERN = Pattern.compile(
      "(?:c?0\\.)(\\d\\d?)(?:_0(\\d))?(?:_st)?(?:_0(\\d))?([a-z])?(?:-([cs]))?(?:-(\\d+))?(?:-(renew))?"
   );
   private static final Pattern EARLY_CLASSIC_PATTERN = Pattern.compile("(?:c?0\\.0\\.)(\\d\\d?)a(?:_0(\\d))?(?:-(\\d+))?(?:-(launcher))?");
   private static final Pattern PRE_CLASSIC_PATTERN = Pattern.compile("(?:rd|pc)-(\\d+)(?:-(launcher))?");
   private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(.+)(?:-(\\d+))");
   private static final String STRING_DESC = "Ljava/lang/String;";
   private static final Pattern VERSION_PATTERN = Pattern.compile(
      PRE_CLASSIC_PATTERN.pattern()
         + "|"
         + EARLY_CLASSIC_PATTERN.pattern()
         + "|"
         + LATE_CLASSIC_PATTERN.pattern()
         + "|"
         + CLASSIC_SERVER_PATTERN.pattern()
         + "|"
         + INDEV_PATTERN.pattern()
         + "|"
         + ALPHA_PATTERN.pattern()
         + "|"
         + BETA_PATTERN.pattern()
         + "("
         + TEST_BUILD_PATTERN.pattern().substring(2)
         + ")?("
         + PRE_RELEASE_PATTERN.pattern().substring(2)
         + ")?("
         + RELEASE_CANDIDATE_PATTERN.pattern().substring(2)
         + ")?|"
         + RELEASE_PATTERN.pattern()
         + "("
         + TEST_BUILD_PATTERN.pattern().substring(2)
         + ")?("
         + PRE_RELEASE_PATTERN.pattern().substring(2)
         + ")?("
         + RELEASE_CANDIDATE_PATTERN.pattern().substring(2)
         + ")?("
         + EXPERIMENTAL_PATTERN.pattern().substring(2)
         + ")?|"
         + SNAPSHOT_PATTERN.pattern()
         + "|[Cc]ombat(?: Test )?\\d[a-z]?|Minecraft RC\\d|2.0|1\\.RV-Pre1|3D Shareware v1\\.34|20w14~|22w13oneBlockAtATime|23w13a_or_b|24w14potato|25w14craftmine("
         + TIMESTAMP_PATTERN.pattern()
         + ")?"
   );

   public static McVersion getVersion(List<Path> gameJars, String entrypointClass, String versionName) {
      McVersion.Builder builder = new McVersion.Builder();
      if (versionName != null) {
         builder.setNameAndRelease(versionName);
      }

      try {
         SimpleClassPath cp = new SimpleClassPath(gameJars);

         try {
            if (entrypointClass != null) {
               InputStream is = cp.getInputStream(LoaderUtil.getClassFileName(entrypointClass));

               try {
                  DataInputStream dis = new DataInputStream(is);
                  if (dis.readInt() == -889275714) {
                     dis.readUnsignedShort();
                     builder.setClassVersion(dis.readUnsignedShort());
                  }
               } catch (Throwable var10) {
                  if (is != null) {
                     try {
                        is.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (is != null) {
                  is.close();
               }
            }

            if (versionName == null) {
               fillVersionFromJar(cp, builder);
            }
         } catch (Throwable var11) {
            try {
               cp.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
         }

         cp.close();
      } catch (IOException e) {
         throw ExceptionUtil.wrap(e);
      }

      return builder.build();
   }

   public static McVersion getVersionExceptClassVersion(Path gameJar) {
      McVersion.Builder builder = new McVersion.Builder();

      try {
         SimpleClassPath cp = new SimpleClassPath(Collections.singletonList(gameJar));

         try {
            fillVersionFromJar(cp, builder);
         } catch (Throwable var6) {
            try {
               cp.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         cp.close();
      } catch (IOException e) {
         throw ExceptionUtil.wrap(e);
      }

      return builder.build();
   }

   public static void fillVersionFromJar(SimpleClassPath cp, McVersion.Builder builder) {
      try {
         InputStream is;
         if ((is = cp.getInputStream("version.json")) != null && fromVersionJson(is, builder)) {
            return;
         }

         if ((is = cp.getInputStream("net/minecraft/realms/RealmsSharedConstants.class")) != null
            && fromAnalyzer(is, new McVersionLookup.FieldStringConstantVisitor("VERSION_STRING"), builder)) {
            return;
         }

         if ((is = cp.getInputStream("net/minecraft/realms/RealmsBridge.class")) != null
            && fromAnalyzer(is, new McVersionLookup.MethodConstantRetVisitor("getVersionString"), builder)) {
            return;
         }

         if ((is = cp.getInputStream("net/minecraft/server/MinecraftServer.class")) != null
            && fromAnalyzer(is, new McVersionLookup.MethodConstantVisitor("run"), builder)) {
            return;
         }

         SimpleClassPath.CpEntry entry = cp.getEntry("net/minecraft/client/Minecraft.class");
         if (entry != null) {
            if (fromAnalyzer(entry.getInputStream(), new McVersionLookup.MethodConstantRetVisitor(null), builder)) {
               return;
            }

            if (fromAnalyzer(entry.getInputStream(), new McVersionLookup.MethodStringConstantContainsVisitor("org/lwjgl/opengl/Display", "setTitle"), builder)) {
               return;
            }
         }

         String type;
         if ((
               (is = cp.getInputStream("net/minecraft/client/MinecraftApplet.class")) != null
                  || (is = cp.getInputStream("com/mojang/minecraft/MinecraftApplet.class")) != null
            )
            && (type = analyze(is, new McVersionLookup.FieldTypeCaptureVisitor())) != null
            && (is = cp.getInputStream(type.concat(".class"))) != null
            && fromAnalyzer(is, new McVersionLookup.MethodConstantVisitor("init"), builder)) {
            return;
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      builder.setFromFileName(cp.getPaths().get(0).getFileName().toString());
   }

   private static boolean fromVersionJson(InputStream is, McVersion.Builder builder) {
      try {
         JsonReader reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));

         boolean var14;
         label100: {
            try {
               String id = null;
               String name = null;
               String release = null;
               reader.beginObject();

               while (reader.hasNext()) {
                  switch (reader.nextName()) {
                     case "id":
                        if (reader.peek() != JsonToken.STRING) {
                           throw new IOException("\"id\" in version json must be a string");
                        }

                        id = reader.nextString();
                        break;
                     case "name":
                        if (reader.peek() != JsonToken.STRING) {
                           throw new IOException("\"name\" in version json must be a string");
                        }

                        name = reader.nextString();
                        break;
                     case "release_target":
                        if (reader.peek() != JsonToken.STRING) {
                           throw new IOException("\"release_target\" in version json must be a string");
                        }

                        release = reader.nextString();
                        break;
                     default:
                        reader.skipValue();
                  }
               }

               reader.endObject();
               if (id != null) {
                  int pos = id.indexOf(" / ");
                  if (pos > 0) {
                     id = id.substring(0, pos);
                  }
               }

               String version;
               if (name != null && (id == null || id.length() > name.length())) {
                  version = name;
               } else {
                  version = id;
               }

               if (version == null) {
                  var14 = false;
                  break label100;
               }

               builder.setId(id);
               builder.setName(name);
               if (release == null) {
                  builder.setNameAndRelease(version);
               } else {
                  builder.setVersion(version);
                  builder.setRelease(release);
               }

               var14 = true;
            } catch (Throwable var9) {
               try {
                  reader.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            reader.close();
            return var14;
         }

         reader.close();
         return var14;
      } catch (IOException e) {
         e.printStackTrace();
         return false;
      }
   }

   private static <T extends ClassVisitor & McVersionLookup.Analyzer> boolean fromAnalyzer(InputStream is, T analyzer, McVersion.Builder builder) {
      String result = analyze(is, analyzer);
      if (result != null) {
         builder.setNameAndRelease(result);
         return true;
      } else {
         return false;
      }
   }

   private static <T extends ClassVisitor & McVersionLookup.Analyzer> String analyze(InputStream is, T analyzer) {
      try {
         ClassReader cr = new ClassReader(is);
         cr.accept(analyzer, 6);
         return analyzer.getResult();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         try {
            is.close();
         } catch (IOException var12) {
         }
      }

      return null;
   }

   @VisibleForTesting
   public static String getRelease(String version) {
      Matcher matcher = DATE_BASED_PATTERN.matcher(version);
      if (matcher.matches()) {
         return matcher.group(1);
      }

      matcher = RELEASE_PATTERN.matcher(version);
      if (matcher.matches()) {
         return matcher.group(1);
      }

      int pos = version.indexOf("_deep_dark_experimental_snapshot-");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf("_experimental-snapshot-");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf("-exp");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf("-tb");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf("-pre");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf("-rc");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf(" Deep Dark Experimental Snapshot");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf(" Experimental Snapshot");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf(" experimental snapshot ");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf(" Test Build");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf(" Pre-Release");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf(" Pre-release");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf(" Prerelease");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf(" RC");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      pos = version.indexOf(" Release Candidate");
      if (pos >= 0) {
         return version.substring(0, pos);
      }

      matcher = SNAPSHOT_PATTERN.matcher(version);
      if (matcher.matches()) {
         int year = Integer.parseInt(matcher.group(1));
         int week = Integer.parseInt(matcher.group(2));
         if (year == 26 && week == 14) {
            return "26.1.1";
         }

         if (year == 25 && week >= 41 || year > 25) {
            return "1.21.11";
         }

         if (year == 25 && week >= 31 && week <= 37) {
            return "1.21.9";
         }

         if (year == 25 && week >= 15 && week <= 21) {
            return "1.21.6";
         }

         if (year == 25 && week >= 2 && week <= 10) {
            return "1.21.5";
         }

         if (year == 24 && week >= 44) {
            return "1.21.4";
         }

         if (year == 24 && week >= 33 && week <= 40) {
            return "1.21.2";
         }

         if (year == 24 && week >= 18 && week <= 21) {
            return "1.21";
         }

         if (year == 23 && week >= 51 || year == 24 && week <= 14) {
            return "1.20.5";
         }

         if (year == 23 && week >= 40 && week <= 46) {
            return "1.20.3";
         }

         if (year == 23 && week >= 31 && week <= 35) {
            return "1.20.2";
         }

         if (year == 23 && week >= 12 && week <= 18) {
            return "1.20";
         }

         if (year == 23 && week <= 7) {
            return "1.19.4";
         }

         if (year == 22 && week >= 42) {
            return "1.19.3";
         }

         if (year == 22 && week == 24) {
            return "1.19.1";
         }

         if (year == 22 && week >= 11 && week <= 19) {
            return "1.19";
         }

         if (year == 22 && week >= 3 && week <= 7) {
            return "1.18.2";
         }

         if (year == 21 && week >= 37 && week <= 44) {
            return "1.18";
         }

         if (year == 20 && week >= 45 || year == 21 && week <= 20) {
            return "1.17";
         }

         if (year == 20 && week >= 27 && week <= 30) {
            return "1.16.2";
         }

         if (year == 20 && week >= 6 && week <= 22) {
            return "1.16";
         }

         if (year == 19 && week >= 34) {
            return "1.15";
         }

         if (year == 18 && week >= 43 || year == 19 && week <= 14) {
            return "1.14";
         }

         if (year == 18 && week >= 30 && week <= 33) {
            return "1.13.1";
         }

         if (year == 17 && week >= 43 || year == 18 && week <= 22) {
            return "1.13";
         }

         if (year == 17 && week == 31) {
            return "1.12.1";
         }

         if (year == 17 && week >= 6 && week <= 18) {
            return "1.12";
         }

         if (year == 16 && week == 50) {
            return "1.11.1";
         }

         if (year == 16 && week >= 32 && week <= 44) {
            return "1.11";
         }

         if (year == 16 && week >= 20 && week <= 21) {
            return "1.10";
         }

         if (year == 16 && week >= 14 && week <= 15) {
            return "1.9.3";
         }

         if (year == 15 && week >= 31 || year == 16 && week <= 7) {
            return "1.9";
         }

         if (year == 14 && week >= 2 && week <= 34) {
            return "1.8";
         }

         if (year == 13 && week >= 47 && week <= 49) {
            return "1.7.3";
         }

         if (year == 13 && week >= 36 && week <= 43) {
            return "1.7";
         }

         if (year == 13 && week >= 16 && week <= 26) {
            return "1.6";
         }

         if (year == 13 && week >= 11 && week <= 12) {
            return "1.5.1";
         }

         if (year == 13 && week >= 1 && week <= 10) {
            return "1.5";
         }

         if (year == 12 && week >= 49 && week <= 50) {
            return "1.4.6";
         }

         if (year == 12 && week >= 32 && week <= 42) {
            return "1.4";
         }

         if (year == 12 && week >= 15 && week <= 30) {
            return "1.3";
         }

         if (year == 12 && week >= 3 && week <= 8) {
            return "1.2";
         }

         if (year == 11 && week >= 47 || year == 12 && week <= 1) {
            return "1.1";
         }
      }

      return null;
   }

   private static boolean isProbableVersion(String str) {
      return VERSION_PATTERN.matcher(str).matches();
   }

   private static String findProbableVersion(String str) {
      Matcher matcher = VERSION_PATTERN.matcher(str);
      return matcher.find() ? matcher.group() : null;
   }

   @VisibleForTesting
   public static String normalizeVersion(String name, String release) {
      if (release != null && !name.equals(release)) {
         String normalizedRelease = normalizeVersion(release);
         String timestamp = null;
         StringBuilder ret = new StringBuilder();
         ret.append(normalizedRelease);
         ret.append('-');
         Matcher matcher;
         if ((matcher = DATE_BASED_PATTERN.matcher(name)).matches()) {
            if (matcher.group(2) != null) {
               switch (matcher.group(2)) {
                  case "snapshot":
                     ret.append("alpha");
                     break;
                  default:
                     ret.append(matcher.group(2));
               }

               ret.append('.');
               ret.append(matcher.group(3));
            }
         } else if ((matcher = RELEASE_PATTERN.matcher(name)).matches()) {
            if (matcher.group(4) != null) {
               timestamp = matcher.group(4);
            } else if (matcher.group(5) != null) {
               timestamp = "unobfuscated";
            }

            ret.setLength(ret.length() - 1);
         } else if ((matcher = EXPERIMENTAL_PATTERN.matcher(name)).matches()) {
            ret.append("Experimental.");
            ret.append(matcher.group(1));
         } else if (name.startsWith(release)) {
            if ((matcher = RELEASE_CANDIDATE_PATTERN.matcher(name)).matches()) {
               String rcBuild = matcher.group(1);
               if (matcher.group(2) != null) {
                  timestamp = matcher.group(2);
               } else if (matcher.group(3) != null) {
                  timestamp = "unobfuscated";
               }

               if (release.equals("Minecraft")) {
                  ret.replace(0, "Minecraft".length(), "1.0.0");
               } else if (release.equals("1.16")) {
                  int build = Integer.parseInt(rcBuild);
                  rcBuild = Integer.toString(8 + build);
               }

               ret.append("rc.");
               ret.append(rcBuild);
            } else if ((matcher = PRE_RELEASE_PATTERN.matcher(name)).matches()) {
               Matcher releaseMatcher = BETA_PATTERN.matcher(release);
               if (releaseMatcher.matches()) {
                  if (normalizedRelease.charAt(normalizedRelease.length() - 1) != 'r') {
                     throw new IllegalStateException("improperly normalized release " + release + " to " + normalizedRelease + " for pre-release " + name);
                  }

                  String prBuild = matcher.group(1);
                  timestamp = matcher.group(2);
                  if (prBuild == null) {
                     prBuild = "1";
                  }

                  ret.setLength(ret.length() - 2);
                  ret.append(prBuild);
               } else {
                  boolean legacyVersion;
                  try {
                     legacyVersion = VersionPredicateParser.parse("<=1.16").test(new SemanticVersionImpl(release, false));
                  } catch (VersionParsingException e) {
                     throw new RuntimeException("Failed to parse version: " + release);
                  }

                  String prBuild = matcher.group(1);
                  if (matcher.group(2) != null) {
                     timestamp = matcher.group(2);
                  } else if (matcher.group(3) != null) {
                     timestamp = "unobfuscated";
                  }

                  if (prBuild == null) {
                     releaseMatcher = RELEASE_PATTERN.matcher(release);
                     if (!releaseMatcher.matches()) {
                        throw new IllegalStateException("version " + name + " is a pre-release targeting neither a Beta version, nor a release version?!");
                     }

                     int minor = Integer.parseInt(releaseMatcher.group(2));
                     int patch;
                     if (releaseMatcher.group(3) == null) {
                        patch = 0;
                     } else {
                        patch = Integer.parseInt(releaseMatcher.group(3));
                     }

                     boolean showAsRelease = minor == 2 && patch == 0
                        || minor == 3 && patch == 0
                        || minor == 4 && (patch == 0 || patch == 1 || patch == 3)
                        || minor == 6 && (patch == 0 || patch == 3)
                        || minor == 7 && (patch == 0 || patch == 1 || patch == 3);
                     if (showAsRelease) {
                        ret.setLength(ret.length() - 1);
                     } else {
                        ret.append("rc");
                     }
                  } else {
                     ret.append(legacyVersion ? "rc." : "beta.");
                     ret.append(prBuild);
                  }
               }
            } else if ((matcher = TEST_BUILD_PATTERN.matcher(name)).matches()) {
               Matcher releaseMatcher = BETA_PATTERN.matcher(release);
               if (releaseMatcher.matches()) {
                  if (normalizedRelease.charAt(normalizedRelease.length() - 1) != 'r') {
                     throw new IllegalStateException("improperly normalized release " + release + " to " + normalizedRelease + " for test build " + name);
                  }

                  String tbBuild = matcher.group(1);
                  timestamp = matcher.group(2);
                  ret.setLength(ret.length() - 2);
                  ret.append(tbBuild);
               } else {
                  String tbBuild = matcher.group(1);
                  timestamp = matcher.group(2);
                  ret.append("test.");
                  ret.append(tbBuild);
               }
            } else {
               String normalized = normalizeSpecialVersion(name);
               if (normalized != null) {
                  return normalized;
               }
            }
         } else if ((matcher = SNAPSHOT_PATTERN.matcher(name)).matches()) {
            if (matcher.group(4) != null) {
               timestamp = matcher.group(4);
            } else if (matcher.group(5) != null) {
               timestamp = "unobfuscated";
            }

            ret.append("alpha.");
            ret.append(matcher.group(1));
            ret.append('.');
            ret.append(matcher.group(2));
            ret.append('.');
            ret.append(matcher.group(3));
         } else {
            String normalized = normalizeSpecialVersion(name);
            if (normalized != null) {
               return normalized;
            }

            ret.append(normalizeVersion(name));
         }

         if (timestamp != null) {
            ret.append('+');
            ret.append(timestamp);
         }

         return ret.toString();
      } else {
         String ret = normalizeSpecialVersion(name);
         return ret != null ? ret : normalizeVersion(name);
      }
   }

   private static String normalizeVersion(String version) {
      String timestamp = null;
      String suffix = null;
      StringBuilder prep = new StringBuilder();
      Matcher matcher;
      if ((matcher = BETA_PATTERN.matcher(version)).matches()) {
         String trail = matcher.group(5);
         timestamp = matcher.group(6);
         suffix = matcher.group(7);
         prep.append("1.0.0-beta.");
         prep.append(matcher.group(1));
         if (matcher.group(3) == null && matcher.group(4) == null) {
            int maj = Integer.parseInt(matcher.group(2));
            if (maj == 6 || maj == 8 || maj == 9) {
               prep.append(".0.r");
            }
         }

         if (trail != null) {
            if (matcher.group(3) == null && matcher.group(4) == null) {
               prep.append(".0");
            }

            prep.append('.').append(trail);
         }
      } else if ((matcher = ALPHA_PATTERN.matcher(version)).matches()) {
         String trail = matcher.group(2);
         timestamp = matcher.group(3);
         suffix = matcher.group(4);
         prep.append("1.0.0-alpha.");
         prep.append(matcher.group(1));
         if (trail != null) {
            prep.append('.').append(trail);
         }
      } else if ((matcher = INDEV_PATTERN.matcher(version)).matches()) {
         String date = matcher.group(1);
         String time = matcher.group(2);
         prep.append("0.31.");
         prep.append(date);
         if (time != null) {
            prep.append('-').append(time);
         }
      } else if ((matcher = EARLY_CLASSIC_PATTERN.matcher(version)).matches() || (matcher = LATE_CLASSIC_PATTERN.matcher(version)).matches()) {
         boolean late = LATE_CLASSIC_PATTERN.matcher(version).matches();
         String minor = matcher.group(1);
         String patch = matcher.group(2);
         String trail = late ? matcher.group(4) : null;
         String type = late ? matcher.group(5) : null;
         timestamp = matcher.group(late ? 6 : 3);
         suffix = matcher.group(late ? 7 : 4);
         if (late && patch == null) {
            patch = matcher.group(3);
         }

         prep.append("0.");
         prep.append(minor);
         if (patch != null) {
            prep.append('.').append(patch);
         }

         if (trail != null) {
            prep.append('-').append(trail);
         }

         if (type != null) {
            prep.append('-').append(type);
         }
      } else if ((matcher = CLASSIC_SERVER_PATTERN.matcher(version)).matches()) {
         String release = matcher.group(1);
         timestamp = matcher.group(2);
         prep.append("0.");
         prep.append(release);
      } else if ((matcher = PRE_CLASSIC_PATTERN.matcher(version)).matches()) {
         String build = matcher.group(1);
         suffix = matcher.group(2);
         if ("20090515".equals(build)) {
            build = "150000";
         }

         prep.append("0.0.0-rd.");
         prep.append(build);
      } else {
         prep.append(version);
      }

      StringBuilder ret = new StringBuilder(prep.length() + 5);
      boolean lastIsDigit = false;
      boolean lastIsLeadingZero = false;
      boolean lastIsSeparator = false;
      int i = 0;

      for (int max = prep.length(); i < max; i++) {
         char c = prep.charAt(i);
         if (c >= '0' && c <= '9') {
            if (i > 0 && !lastIsDigit && !lastIsSeparator) {
               ret.append('.');
            } else if (lastIsDigit && lastIsLeadingZero) {
               ret.setLength(ret.length() - 1);
            }

            lastIsLeadingZero = c == '0' && (!lastIsDigit || lastIsLeadingZero);
            lastIsSeparator = false;
            lastIsDigit = true;
         } else if (c != '.' && c != '-') {
            if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')) {
               if (lastIsSeparator) {
                  continue;
               }

               c = '.';
               lastIsSeparator = true;
               lastIsDigit = false;
            } else {
               if (lastIsDigit) {
                  ret.append('.');
               }

               lastIsSeparator = false;
               lastIsDigit = false;
            }
         } else {
            if (lastIsSeparator) {
               continue;
            }

            lastIsSeparator = true;
            lastIsDigit = false;
         }

         ret.append(c);
      }

      i = 0;

      while (i < ret.length() && ret.charAt(i) == '.') {
         i++;
      }

      int end = ret.length();

      while (end > i && ret.charAt(end - 1) == '.') {
         end--;
      }

      ret.setLength(end);
      if (timestamp != null || suffix != null) {
         ret.append('+');
         if (timestamp != null) {
            ret.append(timestamp);
            if (suffix != null) {
               ret.append('.');
            }
         }

         if (suffix != null) {
            ret.append(suffix);
         }
      }

      return ret.substring(i);
   }

   private static String normalizeSpecialVersion(String version) {
      String normalized = normalizeSpecialVersionBase(version);
      if (normalized == null) {
         String timestamp = null;
         Matcher matcher = TIMESTAMP_PATTERN.matcher(version);
         if (matcher.matches()) {
            version = matcher.group(1);
            timestamp = matcher.group(2);
         }

         normalized = normalizeSpecialVersionBase(version);
         if (normalized != null && timestamp != null) {
            normalized = normalized + "+" + timestamp;
         }
      }

      return normalized;
   }

   private static String normalizeSpecialVersionBase(String version) {
      switch (version) {
         case "b1.2_02-dev":
            return "1.0.0-beta.2.dev";
         case "b1.3-demo":
            return "1.0.0-beta.3.demo";
         case "b1.6-trailer":
         case "b1.6-pre-trailer":
            return "1.0.0-beta.6.0.0";
         case "13w02a-whitetexturefix":
            return "1.5-alpha.13.2.a.whitetexturefix";
         case "13w04a-whitelinefix":
            return "1.5-alpha.13.4.a.whitelinefix";
         case "1.5-whitelinefix":
         case "1.5-pre-whitelinefix":
            return "1.5-rc.whitelinefix";
         case "13w12~":
            return "1.5.1-alpha.13.12.a";
         case "2.0":
            return "1.5.2-2.0";
         case "2.0-preview":
            return "1.5.2-2.0+preview";
         case "2.0-red":
         case "2point0_red":
         case "af-2013-red":
            return "1.5.2-2.0+red";
         case "2.0-purple":
         case "2point0_purple":
         case "af-2013-purple":
            return "1.5.2-2.0+purple";
         case "2.0-blue":
         case "2point0_blue":
         case "af-2013-blue":
            return "1.5.2-2.0+blue";
         case "15w14a":
         case "af-2015":
            return "1.8.4-alpha.15.14.a+loveandhugs";
         case "1.RV-Pre1":
         case "af-2016":
            return "1.9.2-rv+trendy";
         case "3D Shareware v1.34":
         case "af-2019":
            return "1.14-alpha.19.13.shareware";
         case "20w14infinite":
         case "20w14~":
         case "af-2020":
            return "1.16-alpha.20.13.inf";
         case "22w13oneblockatatime":
         case "22w13oneBlockAtATime":
         case "af-2022":
            return "1.18.3-alpha.22.13.oneblockatatime";
         case "23w13a_or_b":
         case "af-2023":
            return "1.20-alpha.23.13.ab";
         case "23w13a_or_b_original":
            return "1.20-alpha.23.13.ab+original";
         case "24w14potato":
         case "af-2024":
            return "1.20.5-alpha.24.12.potato";
         case "24w14potato_original":
            return "1.20.5-alpha.24.12.potato+original";
         case "25w14craftmine":
         case "af-2025":
            return "1.21.6-alpha.25.14.craftmine";
         case "1.14_combat-212796":
         case "1.14.3 - Combat Test":
         case "combat1":
            return "1.14.3-rc.4.combat.1";
         case "1.14_combat-0":
         case "Combat Test 2":
         case "combat2":
            return "1.14.5-combat.2";
         case "1.14_combat-3":
         case "Combat Test 3":
         case "combat3":
            return "1.14.5-combat.3";
         case "1.15_combat-1":
         case "Combat Test 4":
         case "combat4":
            return "1.15-rc.3.combat.4";
         case "1.15_combat-6":
         case "Combat Test 5":
         case "combat5":
            return "1.15.2-rc.2.combat.5";
         case "1.16_combat-0":
         case "Combat Test 6":
         case "combat6":
            return "1.16.2-beta.3.combat.6";
         case "1.16_combat-1":
         case "Combat Test 7":
         case "combat7":
            return "1.16.3-combat.7";
         case "1.16_combat-2":
         case "Combat Test 7b":
         case "combat7b":
            return "1.16.3-combat.7.b";
         case "combat7c":
         case "Combat Test 7c":
         case "1.16_combat-3":
            return "1.16.3-combat.7.c";
         case "combat8":
         case "Combat Test 8":
         case "1.16_combat-4":
            return "1.16.3-combat.8";
         case "combat8b":
         case "Combat Test 8b":
         case "1.16_combat-5":
            return "1.16.3-combat.8.b";
         case "combat8c":
         case "Combat Test 8c":
         case "1.16_combat-6":
            return "1.16.3-combat.8.c";
         case "26w14a":
            return "26.1.1-alpha.26.14.a";
         default:
            return null;
      }
   }

   private interface Analyzer {
      String getResult();
   }

   private static final class FieldStringConstantVisitor extends ClassVisitor implements McVersionLookup.Analyzer {
      private final String fieldName;
      private String className;
      private String result;

      FieldStringConstantVisitor(String fieldName) {
         super(589824);
         this.fieldName = fieldName;
      }

      @Override
      public String getResult() {
         return this.result;
      }

      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
         this.className = name;
      }

      public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
         if (this.result == null && name.equals(this.fieldName) && descriptor.equals("Ljava/lang/String;") && value instanceof String) {
            this.result = (String)value;
         }

         return null;
      }

      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
         return this.result == null && name.equals("<clinit>")
            ? new McVersionLookup.InsnFwdMethodVisitor() {
               String lastLdc;

               @Override
               public void visitLdcInsn(Object value) {
                  String str;
                  if (value instanceof String && McVersionLookup.isProbableVersion(str = (String)value)) {
                     this.lastLdc = str;
                  } else {
                     this.lastLdc = null;
                  }
               }

               @Override
               public void visitFieldInsn(int opcode, String owner, String namex, String descriptorx) {
                  if (FieldStringConstantVisitor.this.result == null
                     && this.lastLdc != null
                     && opcode == 179
                     && owner.equals(FieldStringConstantVisitor.this.className)
                     && namex.equals(FieldStringConstantVisitor.this.fieldName)
                     && descriptorx.equals("Ljava/lang/String;")) {
                     FieldStringConstantVisitor.this.result = this.lastLdc;
                  }

                  this.lastLdc = null;
               }

               @Override
               protected void visitAnyInsn() {
                  this.lastLdc = null;
               }
            }
            : null;
      }
   }

   private static final class FieldTypeCaptureVisitor extends ClassVisitor implements McVersionLookup.Analyzer {
      private String type;

      FieldTypeCaptureVisitor() {
         super(589824);
      }

      @Override
      public String getResult() {
         return this.type;
      }

      public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
         if (this.type == null && descriptor.startsWith("L") && !descriptor.startsWith("Ljava/")) {
            this.type = descriptor.substring(1, descriptor.length() - 1);
         }

         return null;
      }

      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
         return null;
      }
   }

   private abstract static class InsnFwdMethodVisitor extends MethodVisitor {
      InsnFwdMethodVisitor() {
         super(589824);
      }

      protected abstract void visitAnyInsn();

      public void visitLdcInsn(Object value) {
         this.visitAnyInsn();
      }

      public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
         this.visitAnyInsn();
      }

      public void visitInsn(int opcode) {
         this.visitAnyInsn();
      }

      public void visitIntInsn(int opcode, int operand) {
         this.visitAnyInsn();
      }

      public void visitVarInsn(int opcode, int var) {
         this.visitAnyInsn();
      }

      public void visitTypeInsn(int opcode, String type) {
         this.visitAnyInsn();
      }

      public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
         this.visitAnyInsn();
      }

      public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
         this.visitAnyInsn();
      }

      public void visitJumpInsn(int opcode, Label label) {
         this.visitAnyInsn();
      }

      public void visitIincInsn(int var, int increment) {
         this.visitAnyInsn();
      }

      public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
         this.visitAnyInsn();
      }

      public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
         this.visitAnyInsn();
      }

      public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
         this.visitAnyInsn();
      }
   }

   private static final class MethodConstantRetVisitor extends ClassVisitor implements McVersionLookup.Analyzer {
      private final String methodName;
      private String result;

      MethodConstantRetVisitor(String methodName) {
         super(589824);
         this.methodName = methodName;
      }

      @Override
      public String getResult() {
         return this.result;
      }

      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
         return this.result == null
               && (this.methodName == null || name.equals(this.methodName))
               && descriptor.endsWith("Ljava/lang/String;")
               && descriptor.charAt(descriptor.length() - "Ljava/lang/String;".length() - 1) == ')'
            ? new McVersionLookup.InsnFwdMethodVisitor() {
               String lastLdc;

               @Override
               public void visitLdcInsn(Object value) {
                  String str;
                  if (value instanceof String && McVersionLookup.isProbableVersion(str = (String)value)) {
                     this.lastLdc = str;
                  } else {
                     this.lastLdc = null;
                  }
               }

               @Override
               public void visitInsn(int opcode) {
                  if (MethodConstantRetVisitor.this.result == null && this.lastLdc != null && opcode == 176) {
                     MethodConstantRetVisitor.this.result = this.lastLdc;
                  }

                  this.lastLdc = null;
               }

               @Override
               protected void visitAnyInsn() {
                  this.lastLdc = null;
               }
            }
            : null;
      }
   }

   private static final class MethodConstantVisitor extends ClassVisitor implements McVersionLookup.Analyzer {
      private static final String STARTING_MESSAGE = "Starting minecraft server version ";
      private static final String CLASSIC_PREFIX = "Minecraft ";
      private final String methodNameHint;
      private String result;
      private boolean foundInMethodHint;

      MethodConstantVisitor(String methodNameHint) {
         super(589824);
         this.methodNameHint = methodNameHint;
      }

      @Override
      public String getResult() {
         return this.result;
      }

      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
         final boolean isRequestedMethod = name.equals(this.methodNameHint);
         return this.result != null && !isRequestedMethod
            ? null
            : new MethodVisitor(589824) {
               public void visitLdcInsn(Object value) {
                  if ((MethodConstantVisitor.this.result == null || !MethodConstantVisitor.this.foundInMethodHint && isRequestedMethod)
                     && value instanceof String) {
                     String str = (String)value;
                     if (str.startsWith("Starting minecraft server version ")) {
                        str = str.substring("Starting minecraft server version ".length());
                        if (!str.startsWith("Beta") && str.startsWith("0.")) {
                           str = "Alpha " + str;
                        }
                     } else if (str.startsWith("Minecraft ")) {
                        str = str.substring("Minecraft ".length());
                        if (str.startsWith("Minecraft ")) {
                           str = str.substring("Minecraft ".length());
                        }
                     }

                     if (McVersionLookup.isProbableVersion(str)) {
                        MethodConstantVisitor.this.result = str;
                        MethodConstantVisitor.this.foundInMethodHint = isRequestedMethod;
                     }
                  }
               }
            };
      }
   }

   private static final class MethodStringConstantContainsVisitor extends ClassVisitor implements McVersionLookup.Analyzer {
      private final String methodOwner;
      private final String methodName;
      private String result;

      MethodStringConstantContainsVisitor(String methodOwner, String methodName) {
         super(589824);
         this.methodOwner = methodOwner;
         this.methodName = methodName;
      }

      @Override
      public String getResult() {
         return this.result;
      }

      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
         return this.result != null
            ? null
            : new McVersionLookup.InsnFwdMethodVisitor() {
               String lastLdc;

               @Override
               public void visitLdcInsn(Object value) {
                  if (value instanceof String) {
                     this.lastLdc = McVersionLookup.findProbableVersion((String)value);
                  } else {
                     this.lastLdc = null;
                  }
               }

               @Override
               public void visitMethodInsn(int opcode, String owner, String namex, String descriptorx, boolean itf) {
                  if (MethodStringConstantContainsVisitor.this.result == null
                     && this.lastLdc != null
                     && owner.equals(MethodStringConstantContainsVisitor.this.methodOwner)
                     && namex.equals(MethodStringConstantContainsVisitor.this.methodName)
                     && descriptorx.startsWith("(Ljava/lang/String;)")) {
                     MethodStringConstantContainsVisitor.this.result = this.lastLdc;
                  }

                  this.lastLdc = null;
               }

               @Override
               protected void visitAnyInsn() {
                  this.lastLdc = null;
               }
            };
      }
   }
}
