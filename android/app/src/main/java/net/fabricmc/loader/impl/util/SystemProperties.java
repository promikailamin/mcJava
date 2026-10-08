package net.fabricmc.loader.impl.util;

public final class SystemProperties {
   public static final String DEVELOPMENT = "fabric.development";
   public static final String USE_COMPAT_CL = "fabric.loader.useCompatibilityClassLoader";
   public static final String SIDE = "fabric.side";
   public static final String MAPPING_PATH = "fabric.mappingPath";
   public static final String GAME_MAPPING_NAMESPACE = "fabric.gameMappingNamespace";
   public static final String RUNTIME_MAPPING_NAMESPACE = "fabric.runtimeMappingNamespace";
   public static final String DEFAULT_MOD_DISTRIBUTION_NAMESPACE = "fabric.defaultModDistributionNamespace";
   public static final String DEFAULT_MIXIN_REMAP_TYPE = "fabric.defaultMixinRemapType";
   public static final String SKIP_MC_PROVIDER = "fabric.skipMcProvider";
   public static final String GAME_JAR_PATH = "fabric.gameJarPath";
   public static final String GAME_JAR_PATH_CLIENT = "fabric.gameJarPath.client";
   public static final String GAME_JAR_PATH_SERVER = "fabric.gameJarPath.server";
   public static final String GAME_LIBRARIES = "fabric.gameLibraries";
   public static final String GAME_VERSION = "fabric.gameVersion";
   public static final String LOG_FILE = "fabric.log.file";
   public static final String LOG_LEVEL = "fabric.log.level";
   public static final String MODS_FOLDER = "fabric.modsFolder";
   public static final String ADD_MODS = "fabric.addMods";
   public static final String DISABLE_MOD_IDS = "fabric.debug.disableModIds";
   public static final String REMAP_CLASSPATH_FILE = "fabric.remapClasspathFile";
   public static final String PATH_GROUPS = "fabric.classPathGroups";
   public static final String FIX_PACKAGE_ACCESS = "fabric.fixPackageAccess";
   public static final String DISABLE_ENVIRONMENT_STRIP = "fabric.disableEnvironmentStrip";
   public static final String SYSTEM_LIBRARIES = "fabric.systemLibraries";
   public static final String DEBUG_THROW_DIRECTLY = "fabric.debug.throwDirectly";
   public static final String DEBUG_LOG_LIB_CLASSIFICATION = "fabric.debug.logLibClassification";
   public static final String DEBUG_LOG_CLASS_LOAD = "fabric.debug.logClassLoad";
   public static final String DEBUG_LOG_CLASS_LOAD_ERRORS = "fabric.debug.logClassLoadErrors";
   public static final String DEBUG_LOG_TRANSFORM_ERRORS = "fabric.debug.logTransformErrors";
   public static final String DEBUG_DISABLE_CLASS_PATH_ISOLATION = "fabric.debug.disableClassPathIsolation";
   public static final String DEBUG_DISABLE_MOD_SHUFFLE = "fabric.debug.disableModShuffle";
   public static final String DEBUG_LOAD_LATE = "fabric.debug.loadLate";
   public static final String DEBUG_DISCOVERY_TIMEOUT = "fabric.debug.discoveryTimeout";
   public static final String DEBUG_RESOLUTION_TIMEOUT = "fabric.debug.resolutionTimeout";
   public static final String DEBUG_REPLACE_VERSION = "fabric.debug.replaceVersion";
   public static final String DEBUG_DEOBFUSCATE_WITH_CLASSPATH = "fabric.debug.deobfuscateWithClasspath";
   public static final String UNIT_TEST = "fabric.unitTest";
   public static final String NO_GUI = "fabric.noGui";

   public static boolean isSet(String property) {
      String val = System.getProperty(property);
      return val != null && !val.equalsIgnoreCase("false");
   }
}
