package net.fabricmc.loader.impl.launch.knot;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.FabricMixinVersions;
import net.fabricmc.loader.impl.util.UrlUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.util.ReEntranceLock;

public class MixinServiceKnot implements IClassBytecodeProvider, IClassProvider, IClassTracker, IMixinService, ITransformerProvider {
   static IMixinTransformer transformer;
   private final ReEntranceLock lock = new ReEntranceLock(1);

   public byte[] getClassBytes(String name, String transformedName) throws IOException {
      return FabricLauncherBase.getLauncher().getClassByteArray(name, true);
   }

   public byte[] getClassBytes(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
      byte[] classBytes = FabricLauncherBase.getLauncher().getClassByteArray(name, runTransformers);
      if (classBytes != null) {
         return classBytes;
      } else {
         throw new ClassNotFoundException(name);
      }
   }

   public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
      return this.getClassNode(name, true);
   }

   public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
      return this.getClassNode(name, runTransformers, 0);
   }

   public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
      ClassReader reader = new ClassReader(this.getClassBytes(name, runTransformers));
      ClassNode node = new ClassNode();
      reader.accept(node, readerFlags);
      return node;
   }

   public URL[] getClassPath() {
      return new URL[0];
   }

   public Class<?> findClass(String name) throws ClassNotFoundException {
      return FabricLauncherBase.getLauncher().getTargetClassLoader().loadClass(name);
   }

   public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
      return Class.forName(name, initialize, FabricLauncherBase.getLauncher().getTargetClassLoader());
   }

   public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
      return Class.forName(name, initialize, Knot.class.getClassLoader());
   }

   public String getName() {
      return FabricLauncherBase.getLauncher() instanceof Knot ? "Knot/Fabric" : "Launchwrapper/Fabric";
   }

   public boolean isValid() {
      return true;
   }

   public void prepare() {
   }

   public Phase getInitialPhase() {
      return Phase.PREINIT;
   }

   public void offer(IMixinInternal internal) {
      if (internal instanceof IMixinTransformerFactory) {
         transformer = ((IMixinTransformerFactory)internal).createTransformer();
      }
   }

   public void init() {
   }

   public void beginPhase() {
   }

   public void checkEnv(Object bootSource) {
   }

   public ReEntranceLock getReEntranceLock() {
      return this.lock;
   }

   public IClassProvider getClassProvider() {
      return this;
   }

   public IClassBytecodeProvider getBytecodeProvider() {
      return this;
   }

   public ITransformerProvider getTransformerProvider() {
      return this;
   }

   public IClassTracker getClassTracker() {
      return this;
   }

   public IMixinAuditTrail getAuditTrail() {
      return null;
   }

   public IFeatureValidator getFeatureValidator() {
      return IFeatureValidator.ALLOW_ALL;
   }

   public IAdviceProvider getAdviceProvider() {
      return new IAdviceProvider() {
         public String higherCompatibilityNeeded(int requiredCompatibility, String requiredCompatibilityString) {
            return "Increase your Fabric Loader dependency to at least " + FabricMixinVersions.getMinLoaderVersion(requiredCompatibility);
         }
      };
   }

   public Collection<String> getPlatformAgents() {
      return Collections.singletonList("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
   }

   public IContainerHandle getPrimaryContainer() {
      return new ContainerHandleURI(UrlUtil.LOADER_CODE_SOURCE.toUri());
   }

   public Collection<IContainerHandle> getMixinContainers() {
      return Collections.emptyList();
   }

   public InputStream getResourceAsStream(String name) {
      return FabricLauncherBase.getLauncher().getResourceAsStream(name);
   }

   public void registerInvalidClass(String className) {
   }

   public boolean isClassLoaded(String className) {
      return FabricLauncherBase.getLauncher().isClassLoaded(className);
   }

   public String getClassRestrictions(String className) {
      return "";
   }

   public Collection<ITransformer> getTransformers() {
      return Collections.emptyList();
   }

   public Collection<ITransformer> getDelegatedTransformers() {
      return Collections.emptyList();
   }

   public void addTransformerExclusion(String name) {
   }

   public String getSideName() {
      return FabricLauncherBase.getLauncher().getEnvironmentType().name();
   }

   public CompatibilityLevel getMinCompatibilityLevel() {
      return CompatibilityLevel.JAVA_8;
   }

   public CompatibilityLevel getMaxCompatibilityLevel() {
      return CompatibilityLevel.JAVA_25;
   }

   public ILogger getLogger(String name) {
      return MixinLogger.get(name);
   }

   static IMixinTransformer getTransformer() {
      return transformer;
   }
}
