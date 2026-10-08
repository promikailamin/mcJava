package net.fabricmc.loader.impl.launch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.lib.mappingio.MappingReader;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;
import net.fabricmc.loader.impl.util.ManifestUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.mappings.FilteringMappingVisitor;
import org.jetbrains.annotations.Nullable;

public final class MappingConfiguration {
   private static final boolean FIX_PACKAGE_ACCESS = SystemProperties.isSet("fabric.fixPackageAccess");
   public static final String OFFICIAL_NAMESPACE = "official";
   public static final String CLIENT_OFFICIAL_NAMESPACE = "clientOfficial";
   public static final String SERVER_OFFICIAL_NAMESPACE = "serverOfficial";
   public static final String INTERMEDIARY_NAMESPACE = "intermediary";
   public static final String NAMED_NAMESPACE = "named";
   private boolean initializedMetadata;
   private boolean initializedMappings;
   private MappingConfiguration.MappingSource mappingSource;
   private String namespace;
   @Nullable
   private String gameId;
   @Nullable
   private String gameVersion;
   @Nullable
   private String mappingName;
   @Nullable
   private List<String> namespaces;
   @Nullable
   private MemoryMappingTree mappings;

   @Nullable
   public String getGameId() {
      this.initializeMappings(true);
      return this.gameId;
   }

   @Nullable
   public String getGameVersion() {
      this.initializeMappings(true);
      return this.gameVersion;
   }

   @Nullable
   public String getMappingName() {
      this.initializeMappings(true);
      return this.mappingName;
   }

   @Nullable
   public List<String> getNamespaces() {
      this.initializeMappings(true);
      return this.namespaces;
   }

   public boolean matches(String gameId, String gameVersion) {
      this.initializeMappings(true);
      return (this.gameId == null || gameId == null || gameId.equals(this.gameId))
         && (this.gameVersion == null || gameVersion == null || gameVersion.equals(this.gameVersion));
   }

   public MappingTree getMappings() {
      this.initializeMappings(false);
      assert this.mappings != null;
      return this.mappings;
   }

   public boolean hasAnyMappings() {
      MappingTree tree = this.getMappings();
      return !tree.getClasses().isEmpty();
   }

   public String getRuntimeNamespace() {
      if (this.namespace == null) {
         this.namespace = this.computeRuntimeNamespace();
      }

      return this.namespace;
   }

   private String computeRuntimeNamespace() {
      String ret = System.getProperty("fabric.runtimeMappingNamespace");
      if (ret != null) {
         return ret;
      }

      ret = "official";
      if (this.hasAnyMappings()) {
         String newNs = FabricLauncherBase.getLauncher().isDevelopment() ? "named" : "intermediary";
         if (this.getNamespaces().contains(newNs)) {
            ret = newNs;
         }
      }

      return FabricLoaderImpl.INSTANCE.getGameProvider().getRuntimeNamespace(ret);
   }

   public String getDefaultModDistributionNamespace() {
      String ret = System.getProperty("fabric.defaultModDistributionNamespace");
      if (ret != null) {
         return ret;
      }

      ret = this.getRuntimeNamespace();
      if (!ret.equals("official")) {
         ret = "intermediary";
      }

      return FabricLoaderImpl.INSTANCE.getGameProvider().getDefaultModDistributionNamespace(ret);
   }

   public boolean requiresPackageAccessHack() {
      return FIX_PACKAGE_ACCESS || this.getRuntimeNamespace().equals("named");
   }

   private void initializeMappings(boolean metaOnly) {
      if (!this.initializedMappings && (!this.initializedMetadata || !metaOnly)) {
         long time = System.nanoTime();
         MappingConfiguration.MappingSource source = this.getMappingSource();
         MappingVisitor out;
         if (metaOnly) {
            out = null;
         } else {
            this.mappings = new MemoryMappingTree();
            out = new FilteringMappingVisitor(this.mappings);
         }

         try {
            if (source.path != null) {
               if (metaOnly) {
                  this.namespaces = MappingReader.getNamespaces(source.path);
               } else {
                  MappingReader.read(source.path, out);
               }
            } else if (source.url != null) {
               URLConnection connection = source.url.openConnection();
               if (!this.initializedMetadata && connection instanceof JarURLConnection) {
                  Manifest manifest = ((JarURLConnection)connection).getManifest();
                  if (manifest != null) {
                     this.gameId = ManifestUtil.getManifestValue(manifest, new Name("Game-Id"));
                     this.gameVersion = ManifestUtil.getManifestValue(manifest, new Name("Game-Version"));
                     this.mappingName = ManifestUtil.getManifestValue(manifest, new Name("Mapping-Name"));
                  }
               }

               BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

               try {
                  if (metaOnly) {
                     this.namespaces = MappingReader.getNamespaces(reader);
                  } else {
                     MappingReader.read(reader, out);
                  }
               } catch (Throwable var11) {
                  try {
                     reader.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               reader.close();
            } else {
               Log.info(LogCategory.MAPPINGS, "Mappings not present!");
               this.mappings = new MemoryMappingTree();
               this.initializedMappings = true;
            }
         } catch (IOException e) {
            throw new RuntimeException("Error reading mappings", e);
         }

         if (!this.initializedMetadata && !metaOnly && this.mappings.getSrcNamespace() != null) {
            this.namespaces = new ArrayList<>(this.mappings.getDstNamespaces().size() + 1);
            this.namespaces.add(this.mappings.getSrcNamespace());
            this.namespaces.addAll(this.mappings.getDstNamespaces());
         }

         Log.debug(LogCategory.MAPPINGS, "Loading mappings%s took %.2f ms", metaOnly ? " (meta only)" : "", (System.nanoTime() - time) * 1.0E-6);
         this.initializedMetadata = true;
         if (!metaOnly) {
            this.initializedMappings = true;
         }
      }
   }

   private MappingConfiguration.MappingSource getMappingSource() {
      if (this.mappingSource != null) {
         return this.mappingSource;
      }

      String zipMappingPath = "mappings/mappings.tiny";
      String pathStr = System.getProperty("fabric.mappingPath");
      URL url = null;
      Path path = null;
      if (pathStr == null) {
         url = MappingConfiguration.class.getClassLoader().getResource("mappings/mappings.tiny");
      } else {
         path = Paths.get(pathStr).toAbsolutePath();
         if (!Files.exists(path)) {
            Log.warn(LogCategory.MAPPINGS, "Mapping file %s supplied by the system property doesn't exist", path);
            path = null;
         } else if (!Files.isDirectory(path)) {
            try {
               ZipFile zf = new ZipFile(path.toFile());

               try {
                  ZipEntry entry = zf.getEntry("mappings/mappings.tiny");
                  if (entry == null) {
                     Log.warn(LogCategory.MAPPINGS, "Mapping file %s supplied by the system property doesn't contain mappings at mappings/mappings.tiny", path);
                     path = null;
                  } else {
                     url = new URI("jar", path.toUri() + "!/" + "mappings/mappings.tiny", null).toURL();
                     path = null;
                  }
               } catch (Throwable var9) {
                  try {
                     zf.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }

                  throw var9;
               }

               zf.close();
            } catch (ZipException var10) {
            } catch (IOException | URISyntaxException e) {
               throw new RuntimeException(e);
            }
         }
      }

      this.mappingSource = new MappingConfiguration.MappingSource(url, path);
      return this.mappingSource;
   }

   private static final class MappingSource {
      final URL url;
      final Path path;

      MappingSource(URL url, Path path) {
         this.url = url;
         this.path = path;
      }
   }
}
