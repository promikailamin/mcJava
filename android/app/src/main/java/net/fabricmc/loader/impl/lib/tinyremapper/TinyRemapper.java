package net.fabricmc.loader.impl.lib.tinyremapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipError;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrEnvironment;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLocal;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLogger;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;

public class TinyRemapper {
   private final boolean check = false;
   private final boolean keepInputData;
   final Set<String> forcePropagation;
   final Set<String> knownIndyBsm;
   final boolean propagatePrivate;
   final TinyRemapper.LinkedMethodPropagation propagateBridges;
   final TinyRemapper.LinkedMethodPropagation propagateRecordComponents;
   private final boolean removeFrames;
   private final boolean ignoreConflicts;
   private final boolean resolveMissing;
   private final boolean checkPackageAccess;
   private final boolean fixPackageAccess;
   private final boolean rebuildSourceFilenames;
   private final boolean skipLocalMapping;
   private final boolean renameInvalidLocals;
   private final Pattern invalidLvNamePattern;
   private final boolean inferNameFromSameLvIndex;
   private final boolean disableLocalVariableTracking;
   private final List<TinyRemapper.AnalyzeVisitorProvider> analyzeVisitors;
   private final List<TinyRemapper.StateProcessor> stateProcessors;
   private final List<TinyRemapper.ApplyVisitorProvider> preApplyVisitors;
   private final List<TinyRemapper.ApplyVisitorProvider> postApplyVisitors;
   private final TrLogger logger;
   final Remapper extraRemapper;
   final AtomicReference<Map<InputTag, InputTag[]>> singleInputTags = new AtomicReference<>(Collections.emptyMap());
   final List<CompletableFuture<?>> pendingReads = new ArrayList<>();
   final Map<String, ClassInstance> readClasses = new ConcurrentHashMap<>();
   final TinyRemapper.MrjState defaultState = new TinyRemapper.MrjState(this, -1);
   final Map<Integer, TinyRemapper.MrjState> mrjStates = new HashMap<>();
   final Map<String, String> classMap;
   final Map<String, String> methodMap;
   final Map<String, String> methodArgMap;
   final Map<String, String> methodVarMap;
   final Map<String, String> fieldMap;
   final Map<MemberInstance, Set<String>> conflicts;
   final Set<ClassInstance> classesToMakePublic;
   final Set<MemberInstance> membersToMakePublic;
   private final Collection<IMappingProvider> mappingProviders;
   final boolean ignoreFieldDesc;
   private final int threadCount;
   private final ExecutorService threadPool;
   private volatile boolean dirty;
   private Map<ClassInstance, byte[]> outputBuffer;

   private TinyRemapper(
      Collection<IMappingProvider> mappingProviders,
      boolean ignoreFieldDesc,
      int threadCount,
      boolean keepInputData,
      Set<String> forcePropagation,
      Set<String> knownIndyBsm,
      boolean propagatePrivate,
      TinyRemapper.LinkedMethodPropagation propagateBridges,
      TinyRemapper.LinkedMethodPropagation propagateRecordComponents,
      boolean removeFrames,
      boolean ignoreConflicts,
      boolean resolveMissing,
      boolean checkPackageAccess,
      boolean fixPackageAccess,
      boolean rebuildSourceFilenames,
      boolean skipLocalMapping,
      boolean renameInvalidLocals,
      Pattern invalidLvNamePattern,
      boolean inferNameFromSameLvIndex,
      boolean disableLocalVariableTracking,
      List<TinyRemapper.AnalyzeVisitorProvider> analyzeVisitors,
      List<TinyRemapper.StateProcessor> stateProcessors,
      List<TinyRemapper.ApplyVisitorProvider> preApplyVisitors,
      List<TinyRemapper.ApplyVisitorProvider> postApplyVisitors,
      Remapper extraRemapper,
      TrLogger logger
   ) {
      this.mrjStates.put(this.defaultState.version, this.defaultState);
      this.classMap = new HashMap<>();
      this.methodMap = new HashMap<>();
      this.methodArgMap = new HashMap<>();
      this.methodVarMap = new HashMap<>();
      this.fieldMap = new HashMap<>();
      this.conflicts = new ConcurrentHashMap<>();
      this.classesToMakePublic = Collections.newSetFromMap(new ConcurrentHashMap<>());
      this.membersToMakePublic = Collections.newSetFromMap(new ConcurrentHashMap<>());
      this.dirty = true;
      this.logger = logger;
      this.mappingProviders = mappingProviders;
      this.ignoreFieldDesc = ignoreFieldDesc;
      this.threadCount = threadCount > 0 ? threadCount : Math.max(Runtime.getRuntime().availableProcessors(), 2);
      this.keepInputData = keepInputData;
      this.threadPool = Executors.newFixedThreadPool(this.threadCount);
      this.forcePropagation = forcePropagation;
      this.knownIndyBsm = knownIndyBsm;
      this.propagatePrivate = propagatePrivate;
      this.propagateBridges = propagateBridges;
      this.propagateRecordComponents = propagateRecordComponents;
      this.removeFrames = removeFrames;
      this.ignoreConflicts = ignoreConflicts;
      this.resolveMissing = resolveMissing;
      this.checkPackageAccess = checkPackageAccess;
      this.fixPackageAccess = fixPackageAccess;
      this.rebuildSourceFilenames = rebuildSourceFilenames;
      this.skipLocalMapping = skipLocalMapping;
      this.renameInvalidLocals = renameInvalidLocals;
      this.invalidLvNamePattern = invalidLvNamePattern;
      this.inferNameFromSameLvIndex = inferNameFromSameLvIndex;
      this.disableLocalVariableTracking = disableLocalVariableTracking;
      this.analyzeVisitors = analyzeVisitors;
      this.stateProcessors = stateProcessors;
      this.preApplyVisitors = preApplyVisitors;
      this.postApplyVisitors = postApplyVisitors;
      this.extraRemapper = extraRemapper;
      this.knownIndyBsm.add("java/lang/invoke/StringConcatFactory");
      this.knownIndyBsm.add("java/lang/runtime/ObjectMethods");
      this.knownIndyBsm.add("java/lang/runtime/SwitchBootstraps");
   }

   public static TinyRemapper.Builder newRemapper(TrLogger logger) {
      return new TinyRemapper.Builder(logger);
   }

   public void finish() {
      this.threadPool.shutdown();

      try {
         this.threadPool.awaitTermination(20L, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      this.outputBuffer = null;
      this.defaultState.classes.clear();
      this.mrjStates.clear();
   }

   public InputTag createInputTag() {
      InputTag ret = new InputTag();
      InputTag[] array = new InputTag[]{ret};

      Map<InputTag, InputTag[]> oldTags;
      Map<InputTag, InputTag[]> newTags;
      do {
         oldTags = this.singleInputTags.get();
         newTags = new IdentityHashMap<>(oldTags.size() + 1);
         newTags.putAll(oldTags);
         newTags.put(ret, array);
      } while (!this.singleInputTags.compareAndSet(oldTags, newTags));

      return ret;
   }

   public CompletableFuture<?> readInputsAsync(InputTag tag, Path... inputs) {
      CompletableFuture<?> ret = this.read(inputs, true, tag);
      if (!ret.isDone()) {
         this.pendingReads.add(ret);
      } else {
         ret.join();
      }

      return ret;
   }

   public CompletableFuture<?> readClassPathAsync(Path... inputs) {
      CompletableFuture<?> ret = this.read(inputs, false, null);
      if (!ret.isDone()) {
         this.pendingReads.add(ret);
      } else {
         ret.join();
      }

      return ret;
   }

   private CompletableFuture<List<ClassInstance>> read(Path[] inputs, boolean isInput, InputTag tag) {
      InputTag[] tags = this.singleInputTags.get().get(tag);
      List<CompletableFuture<List<ClassInstance>>> futures = new ArrayList<>();
      List<FileSystemReference> fsToClose = Collections.synchronizedList(new ArrayList<>());

      for (Path input : inputs) {
         futures.addAll(this.read(input, isInput, tags, true, fsToClose));
      }

      if (futures.isEmpty()) {
         return CompletableFuture.completedFuture(Collections.emptyList());
      }

      CompletableFuture<List<ClassInstance>> ret;
      if (futures.size() == 1) {
         ret = futures.get(0);
      } else {
         ret = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(ignore -> futures.stream().flatMap(f -> f.join().stream()).collect(Collectors.toList()));
      }

      if (!this.dirty) {
         this.dirty = true;

         for (TinyRemapper.MrjState state : this.mrjStates.values()) {
            state.dirty = true;
         }
      }

      return ret.whenComplete((res, exc) -> {
         for (FileSystemReference fs : fsToClose) {
            try {
               fs.close();
            } catch (IOException var7) {
            }
         }

         if (res != null) {
            for (ClassInstance node : res) {
               addClass(node, this.readClasses, true);
            }
         }

         assert this.dirty;
      });
   }

   private static void addClass(ClassInstance cls, Map<String, ClassInstance> out, boolean isVersionAware) {
      String name = isVersionAware ? ClassInstance.getMrjName(cls.getName(), cls.getMrjVersion()) : cls.getName();

      while (true) {
         ClassInstance prev = out.putIfAbsent(name, cls);
         if (prev == null) {
            return;
         }

         if (prev.isMrjCopy() && prev.getMrjVersion() < cls.getMrjVersion()) {
            if (out.replace(name, prev, cls)) {
               return;
            }
         } else {
            if (!cls.isInput) {
               prev.addInputTags(cls.getInputTags());
               return;
            }

            if (prev.isInput) {
               cls.tr.getLogger().warn("duplicate input class %s, from %s and %s", name, prev.srcPath, cls.srcPath);
               prev.addInputTags(cls.getInputTags());
               return;
            }

            if (out.replace(name, prev, cls)) {
               cls.addInputTags(prev.getInputTags());
               return;
            }
         }
      }
   }

   private List<CompletableFuture<List<ClassInstance>>> read(Path file, boolean isInput, InputTag[] tags, boolean saveData, List<FileSystemReference> fsToClose) {
      try {
         return this.read(file, isInput, tags, file, saveData, fsToClose);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private List<CompletableFuture<List<ClassInstance>>> read(
      Path file, final boolean isInput, final InputTag[] tags, final Path srcPath, boolean saveData, final List<FileSystemReference> fsToClose
   ) throws IOException {
      final List<CompletableFuture<List<ClassInstance>>> ret = new ArrayList<>();
      Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
         public FileVisitResult visitFile(final Path file, BasicFileAttributes attrs) throws IOException {
            String name = file.getFileName().toString();
            if (name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".class")) {
               ret.add(CompletableFuture.supplyAsync(new Supplier<List<ClassInstance>>() {
                  public List<ClassInstance> get() {
                     try {
                        return TinyRemapper.this.readFile(file, isInput, tags, srcPath, fsToClose);
                     } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                     } catch (IOException | ZipError e) {
                        throw new RuntimeException("Error reading file " + file, e);
                     }
                  }
               }, TinyRemapper.this.threadPool));
            }

            return FileVisitResult.CONTINUE;
         }
      });
      return ret;
   }

   private List<ClassInstance> readFile(Path file, final boolean isInput, final InputTag[] tags, final Path srcPath, List<FileSystemReference> fsToClose) throws IOException, URISyntaxException {
      final List<ClassInstance> ret = new ArrayList<>();
      if (file.toString().endsWith(".class")) {
         ClassInstance res = this.analyze(isInput, tags, srcPath, file);
         if (res != null) {
            ret.add(res);
         }
      } else {
         FileSystemReference fs = FileSystemReference.openJar(file);
         fsToClose.add(fs);
         Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
               if (file.toString().endsWith(".class")) {
                  ClassInstance res = TinyRemapper.this.analyze(isInput, tags, srcPath, file);
                  if (res != null) {
                     ret.add(res);
                  }
               }

               return FileVisitResult.CONTINUE;
            }
         });
      }

      return ret;
   }

   private static int analyzeMrjVersion(Path file, String name) {
      assert file.getFileName().toString().endsWith(".class");
      int pkgCount = 0;
      int pos = 0;

      while ((pos = name.indexOf(47, pos) + 1) > 0) {
         pkgCount++;
      }

      int pathNameCount = file.getNameCount();
      int pathNameOffset = pathNameCount - pkgCount - 1;
      if (pathNameOffset >= 3
         && file.getName(pathNameOffset - 3).toString().equals("META-INF")
         && file.getName(pathNameOffset - 2).toString().equals("versions")
         && file.subpath(pathNameOffset, pathNameCount).toString().replace('\\', '/').regionMatches(0, name, 0, name.length())) {
         try {
            return Integer.parseInt(file.getName(pathNameOffset - 1).toString());
         } catch (NumberFormatException var7) {
         }
      }

      return -1;
   }

   private ClassInstance analyze(boolean isInput, InputTag[] tags, Path srcPath, Path file) throws IOException {
      byte[] data = Files.readAllBytes(file);

      ClassReader reader;
      try {
         reader = new ClassReader(data);
      } catch (Throwable t) {
         throw new RuntimeException("error analyzing " + file + " from " + srcPath, t);
      }

      if ((reader.getAccess() & 32768) != 0) {
         return null;
      }

      String name = reader.getClassName();
      final int mrjVersion = analyzeMrjVersion(file, name);
      final ClassInstance ret = new ClassInstance(this, isInput, tags, srcPath, isInput ? data : null);
      ClassVisitor cv = new ClassVisitor(589824) {
         public void visit(int version, int access, String namex, String signature, String superName, String[] interfaces) {
            ret.init(namex, version, mrjVersion, signature, superName, access, interfaces);
            super.visit(version, access, namex, signature, superName, interfaces);
         }

         public MethodVisitor visitMethod(int access, String namex, String desc, String signature, String[] exceptions) {
            final MemberInstance member = new MemberInstance(TrMember.MemberType.METHOD, ret, namex, desc, access, ret.getMembers().size());
            MemberInstance prev = ret.addMember(member);
            if (prev != null) {
               throw new RuntimeException(String.format("duplicate method %s/%s%s in inputs", ret.getName(), namex, desc));
            } else {
               return TinyRemapper.this.disableLocalVariableTracking
                  ? super.visitMethod(access, namex, desc, signature, exceptions)
                  : new MethodVisitor(589824, super.visitMethod(access, namex, desc, signature, exceptions)) {
                     final List<TrLocal> locals = new ArrayList<>();

                     public void visitLocalVariable(String name, String descriptor, String signaturex, Label start, Label end, int index) {
                        this.locals.add(new LocalInstance(member, name, descriptor, index));
                        super.visitLocalVariable(name, descriptor, signaturex, start, end, index);
                     }

                     public void visitEnd() {
                        member.setLocals(this.locals.toArray(new TrLocal[0]));
                        super.visitEnd();
                     }
                  };
            }
         }

         public FieldVisitor visitField(int access, String namex, String desc, String signature, Object value) {
            MemberInstance prev = ret.addMember(new MemberInstance(TrMember.MemberType.FIELD, ret, namex, desc, access, ret.getMembers().size()));
            if (prev != null) {
               throw new RuntimeException(String.format("duplicate field %s/%s;;%s in inputs", ret.getName(), namex, desc));
            } else {
               return super.visitField(access, namex, desc, signature, value);
            }
         }
      };

      for (int i = this.analyzeVisitors.size() - 1; i >= 0; i--) {
         cv = this.analyzeVisitors.get(i).insertAnalyzeVisitor(isInput, mrjVersion, name, cv, tags);
      }

      if (this.disableLocalVariableTracking) {
         reader.accept(cv, 7);
      } else {
         reader.accept(cv, 4);
      }

      return ret;
   }

   private void loadMappings() {
      IMappingProvider.MappingAcceptor acceptor = new IMappingProvider.MappingAcceptor() {
         @Override
         public void acceptClass(String srcName, String dstName) {
            if (srcName == null) {
               throw new NullPointerException("null src name");
            }

            if (dstName == null) {
               throw new NullPointerException("null dst name");
            }

            TinyRemapper.this.classMap.put(srcName, dstName);
         }

         @Override
         public void acceptMethod(IMappingProvider.Member method, String dstName) {
            if (method == null) {
               throw new NullPointerException("null src method");
            }

            if (method.owner == null) {
               throw new NullPointerException("null src method owner");
            }

            if (method.name == null) {
               throw new NullPointerException("null src method name");
            }

            if (method.desc == null) {
               throw new NullPointerException("null src method desc");
            }

            if (dstName == null) {
               throw new NullPointerException("null dst name");
            }

            TinyRemapper.this.methodMap.put(method.owner + "/" + MemberInstance.getMethodId(method.name, method.desc), dstName);
         }

         @Override
         public void acceptMethodArg(IMappingProvider.Member method, int lvIndex, String dstName) {
            if (method == null) {
               throw new NullPointerException("null src method");
            }

            if (method.owner == null) {
               throw new NullPointerException("null src method owner");
            }

            if (method.name == null) {
               throw new NullPointerException("null src method name");
            }

            if (method.desc == null) {
               throw new NullPointerException("null src method desc");
            }

            if (dstName == null) {
               throw new NullPointerException("null dst name");
            }

            TinyRemapper.this.methodArgMap.put(method.owner + "/" + MemberInstance.getMethodId(method.name, method.desc) + lvIndex, dstName);
         }

         @Override
         public void acceptMethodVar(IMappingProvider.Member method, int lvIndex, int startOpIdx, int asmIndex, String dstName) {
            if (method == null) {
               throw new NullPointerException("null src method");
            }

            if (method.owner == null) {
               throw new NullPointerException("null src method owner");
            }

            if (method.name == null) {
               throw new NullPointerException("null src method name");
            }

            if (method.desc == null) {
               throw new NullPointerException("null src method desc");
            }

            if (dstName == null) {
               throw new NullPointerException("null dst name");
            }

            TinyRemapper.this.methodVarMap.put(method.owner + "/" + MemberInstance.getMethodId(method.name, method.desc) + lvIndex, dstName);
         }

         @Override
         public void acceptField(IMappingProvider.Member field, String dstName) {
            if (field == null) {
               throw new NullPointerException("null src field");
            }

            if (field.owner == null) {
               throw new NullPointerException("null src field owner");
            }

            if (field.name == null) {
               throw new NullPointerException("null src field name");
            }

            if (field.desc == null && !TinyRemapper.this.ignoreFieldDesc) {
               throw new NullPointerException("null src field desc");
            }

            if (dstName == null) {
               throw new NullPointerException("null dst name");
            }

            TinyRemapper.this.fieldMap.put(field.owner + "/" + MemberInstance.getFieldId(field.name, field.desc, TinyRemapper.this.ignoreFieldDesc), dstName);
         }
      };

      for (IMappingProvider provider : this.mappingProviders) {
         provider.load(acceptor);
      }
   }

   private void checkClassMappings() {
      Set<String> testSet = new HashSet<>(this.classMap.values());
      if (testSet.size() != this.classMap.size()) {
         Set<String> duplicates = new HashSet<>();

         for (String name : this.classMap.values()) {
            if (!testSet.remove(name)) {
               duplicates.add(name);
            }
         }

         this.logger.warn("non-unique class target name mappings:");

         for (String target : duplicates) {
            StringBuilder sb = new StringBuilder();
            sb.append("  [");
            boolean first = true;

            for (Entry<String, String> e : this.classMap.entrySet()) {
               if (e.getValue().equals(target)) {
                  if (first) {
                     first = false;
                  } else {
                     sb.append(", ");
                  }

                  sb.append(e.getKey());
               }
            }

            sb.append(String.format("] -> %s", target));
            this.getLogger().warn(sb.toString());
         }

         throw new RuntimeException("duplicate class target name mappings detected");
      }
   }

   private void merge(TinyRemapper.MrjState state) {
      for (ClassInstance node : state.classes.values()) {
         assert node.getSuperName() != null;
         ClassInstance parent = state.getClass(node.getSuperName());
         if (parent != null) {
            node.parents.add(parent);
            parent.children.add(node);
         }

         for (String iface : node.getInterfaceNames0()) {
            parent = state.getClass(iface);
            if (parent != null) {
               node.parents.add(parent);
               parent.children.add(node);
            }
         }
      }
   }

   private void propagate(TinyRemapper.MrjState state) {
      List<Future<?>> futures = new ArrayList<>();
      List<Entry<String, String>> tasks = new ArrayList<>();
      int maxTasks = this.methodMap.size() / this.threadCount / 4;

      for (Entry<String, String> entry : this.methodMap.entrySet()) {
         tasks.add(entry);
         if (tasks.size() >= maxTasks) {
            futures.add(this.threadPool.submit(new TinyRemapper.Propagation(state, TrMember.MemberType.METHOD, tasks)));
            tasks.clear();
         }
      }

      futures.add(this.threadPool.submit(new TinyRemapper.Propagation(state, TrMember.MemberType.METHOD, tasks)));
      tasks.clear();

      for (Entry<String, String> entry : this.fieldMap.entrySet()) {
         tasks.add(entry);
         if (tasks.size() >= maxTasks) {
            futures.add(this.threadPool.submit(new TinyRemapper.Propagation(state, TrMember.MemberType.FIELD, tasks)));
            tasks.clear();
         }
      }

      futures.add(this.threadPool.submit(new TinyRemapper.Propagation(state, TrMember.MemberType.FIELD, tasks)));
      tasks.clear();
      waitForAll(futures);
      this.handleConflicts(state);
   }

   private void handleConflicts(TinyRemapper.MrjState state) {
      Set<String> testSet = new HashSet<>();
      boolean targetNameCheckFailed = false;

      for (ClassInstance cls : state.classes.values()) {
         for (MemberInstance member : cls.getMembers()) {
            String name = member.getNewMappedName();
            if (name == null) {
               name = member.name;
            }

            testSet.add(MemberInstance.getId(member.type, name, member.desc, this.ignoreFieldDesc));
         }

         if (testSet.size() != cls.getMembers().size()) {
            if (!targetNameCheckFailed) {
               targetNameCheckFailed = true;
               this.getLogger().warn("Mapping target name conflicts detected:");
            }

            Map<String, List<MemberInstance>> duplicates = new HashMap<>();

            for (MemberInstance member : cls.getMembers()) {
               String name = member.getNewMappedName();
               if (name == null) {
                  name = member.name;
               }

               duplicates.computeIfAbsent(MemberInstance.getId(member.type, name, member.desc, this.ignoreFieldDesc), ignore -> new ArrayList<>()).add(member);
            }

            for (Entry<String, List<MemberInstance>> e : duplicates.entrySet()) {
               String nameDesc = e.getKey();
               List<MemberInstance> members = e.getValue();
               if (members.size() >= 2) {
                  MemberInstance anyMember = members.get(0);
                  StringBuilder sb = new StringBuilder(String.format("  %ss %s/[", anyMember.type, cls.getName()));

                  for (int i = 0; i < members.size(); i++) {
                     if (i != 0) {
                        sb.append(", ");
                     }

                     MemberInstance member = members.get(i);
                     if (member.newNameOriginatingCls != null && !member.newNameOriginatingCls.equals(cls.getName())) {
                        sb.append(member.newNameOriginatingCls);
                        sb.append('/');
                     }

                     sb.append(member.name);
                  }

                  sb.append(
                     String.format(
                        "]%s -> %s",
                        MemberInstance.getId(anyMember.type, "", anyMember.desc, this.ignoreFieldDesc),
                        MemberInstance.getNameFromId(anyMember.type, nameDesc, this.ignoreFieldDesc)
                     )
                  );
                  this.getLogger().warn(sb.toString());
               }
            }
         }

         testSet.clear();
      }

      boolean unfixableConflicts = false;
      if (!this.conflicts.isEmpty()) {
         this.getLogger().warn("Mapping source name conflicts detected:");

         for (Entry<MemberInstance, Set<String>> entry : this.conflicts.entrySet()) {
            MemberInstance member = entry.getKey();
            String newName = member.getNewMappedName();
            Set<String> names = entry.getValue();
            names.add(member.cls.getName() + "/" + newName);
            this.getLogger().warn("  %s %s %s (%s) -> %s", member.cls.getName(), member.type.name(), member.name, member.desc, names);
            if (this.ignoreConflicts) {
               Map<String, String> mappings = member.type == TrMember.MemberType.METHOD ? this.methodMap : this.fieldMap;
               String mappingName = mappings.get(member.cls.getName() + "/" + member.getId());
               if (mappingName == null) {
                  Queue<ClassInstance> queue = new ArrayDeque<>(member.cls.parents);

                  ClassInstance cls;
                  while ((cls = queue.poll()) != null) {
                     mappingName = mappings.get(cls.getName() + "/" + member.getId());
                     if (mappingName != null) {
                        break;
                     }

                     queue.addAll(cls.parents);
                  }
               }

               if (mappingName == null) {
                  unfixableConflicts = true;
               } else {
                  member.forceSetNewName(mappingName);
                  this.getLogger().warn("    fixable: replaced with " + mappingName);
               }
            }
         }
      }

      if (!this.conflicts.isEmpty() && !this.ignoreConflicts || unfixableConflicts || targetNameCheckFailed) {
         if (this.ignoreConflicts || targetNameCheckFailed) {
            this.getLogger().error("There were unfixable conflicts.");
         }

         throw new RuntimeException("Unfixable conflicts");
      }
   }

   public void apply(BiConsumer<String, byte[]> outputConsumer, InputTag... inputTags) {
      boolean hasInputTags = !this.singleInputTags.get().isEmpty();
      synchronized (this) {
         this.refresh();
         if (this.outputBuffer == null) {
            BiConsumer<ClassInstance, byte[]> immediateOutputConsumer;
            if (!this.fixPackageAccess && !hasInputTags) {
               immediateOutputConsumer = (clsx, datax) -> outputConsumer.accept(
                  ClassInstance.getMrjName(clsx.getContext().remapper.map(clsx.getName()), clsx.getMrjVersion()), datax
               );
            } else {
               this.outputBuffer = new ConcurrentHashMap<>();
               immediateOutputConsumer = this.outputBuffer::put;
            }

            List<Future<?>> futures = new ArrayList<>();

            for (TinyRemapper.MrjState state : this.mrjStates.values()) {
               this.mrjRefresh(state);

               for (ClassInstance cls : state.classes.values()) {
                  if (cls.isInput) {
                     if (cls.data == null) {
                        if (!hasInputTags && !this.keepInputData) {
                           throw new IllegalStateException("invoking apply multiple times without input tags or hasInputData");
                        }

                        throw new IllegalStateException("data for input class " + cls + " is missing?!");
                     }

                     futures.add(this.threadPool.submit(() -> immediateOutputConsumer.accept(cls, this.apply(cls))));
                  }
               }
            }

            waitForAll(futures);
            boolean needsFixes = !this.classesToMakePublic.isEmpty() || !this.membersToMakePublic.isEmpty();
            if (this.fixPackageAccess) {
               if (needsFixes) {
                  this.getLogger().warn("Fixing access for %d classes and %d members.", this.classesToMakePublic.size(), this.membersToMakePublic.size());
               }

               for (Entry<ClassInstance, byte[]> entry : this.outputBuffer.entrySet()) {
                  ClassInstance cls = entry.getKey();
                  byte[] data = entry.getValue();
                  if (needsFixes) {
                     data = this.fixClass(cls, data);
                  }

                  if (hasInputTags) {
                     entry.setValue(data);
                  } else {
                     outputConsumer.accept(ClassInstance.getMrjName(cls.getContext().remapper.map(cls.getName()), cls.getMrjVersion()), data);
                  }
               }

               if (!hasInputTags) {
                  this.outputBuffer = null;
               }

               this.classesToMakePublic.clear();
               this.membersToMakePublic.clear();
            } else if (needsFixes) {
               throw new RuntimeException(
                  String.format("%d classes and %d members need access fixes", this.classesToMakePublic.size(), this.membersToMakePublic.size())
               );
            }
         }

         assert hasInputTags == (this.outputBuffer != null);
         if (this.outputBuffer != null) {
            for (Entry<ClassInstance, byte[]> entry : this.outputBuffer.entrySet()) {
               ClassInstance cls = entry.getKey();
               if (inputTags == null || cls.hasAnyInputTag(inputTags)) {
                  outputConsumer.accept(ClassInstance.getMrjName(cls.getContext().remapper.map(cls.getName()), cls.getMrjVersion()), entry.getValue());
               }
            }
         }
      }
   }

   private void fixMrjClasses(Set<Integer> newVersions) {
      for (int newVersion : newVersions.stream().sorted().collect(Collectors.toList())) {
         TinyRemapper.MrjState newState = new TinyRemapper.MrjState(this, newVersion);
         if (this.mrjStates.put(newVersion, newState) != null) {
            throw new RuntimeException("internal error: duplicate versions in mrjClasses");
         }

         Optional<Integer> fromVersion = this.mrjStates.keySet().stream().filter(v -> v < newVersion).max(Integer::compare);
         if (fromVersion.isPresent()) {
            Map<String, ClassInstance> fromClasses = this.mrjStates.get(fromVersion.get()).classes;

            for (ClassInstance cls : fromClasses.values()) {
               addClass(cls.constructMrjCopy(newState), newState.classes, false);
            }
         }
      }
   }

   private void refresh() {
      if (!this.dirty) {
         assert this.pendingReads.isEmpty();
         assert this.readClasses.isEmpty();
      } else {
         this.outputBuffer = null;
         if (!this.pendingReads.isEmpty()) {
            for (CompletableFuture<?> future : this.pendingReads) {
               future.join();
            }

            this.pendingReads.clear();
         }

         if (!this.readClasses.isEmpty()) {
            Set<Integer> versions = this.readClasses.values().stream().map(ClassInstance::getMrjVersion).collect(Collectors.toSet());
            versions.removeAll(this.mrjStates.keySet());
            this.fixMrjClasses(versions);

            for (ClassInstance cls : this.readClasses.values()) {
               int clsVersion = cls.getMrjVersion();
               TinyRemapper.MrjState state = this.mrjStates.get(clsVersion);
               cls.setContext(state);
               addClass(cls, state.classes, false);

               for (int version : this.mrjStates.keySet()) {
                  if (version > clsVersion) {
                     TinyRemapper.MrjState newState = this.mrjStates.get(version);
                     addClass(cls.constructMrjCopy(newState), newState.classes, false);
                  }
               }
            }

            this.readClasses.clear();
         }

         this.loadMappings();
         this.checkClassMappings();
         assert this.dirty;
         this.dirty = false;
      }
   }

   private void mrjRefresh(TinyRemapper.MrjState state) {
      if (state.dirty) {
         assert new HashSet<>(state.classes.values()).size() == state.classes.size();
         assert state.classes.values().stream().map(ClassInstance::getName).distinct().count() == state.classes.size();
         this.merge(state);
         this.propagate(state);

         for (TinyRemapper.StateProcessor processor : this.stateProcessors) {
            processor.process(state);
         }

         state.dirty = false;
      }
   }

   private byte[] apply(ClassInstance cls) {
      ClassReader reader = new ClassReader(cls.data);
      ClassWriter writer = new ClassWriter(0);
      int flags = this.removeFrames ? 4 : 8;
      ClassVisitor visitor = writer;

      for (int i = this.postApplyVisitors.size() - 1; i >= 0; i--) {
         visitor = this.postApplyVisitors.get(i).insertApplyVisitor(cls, visitor, cls.getInputTags());
      }

      visitor = new AsmClassRemapper(
         visitor,
         cls.getContext().remapper,
         this.rebuildSourceFilenames,
         this.checkPackageAccess,
         this.skipLocalMapping,
         this.renameInvalidLocals,
         this.invalidLvNamePattern,
         this.inferNameFromSameLvIndex
      );

      for (int i = this.preApplyVisitors.size() - 1; i >= 0; i--) {
         visitor = this.preApplyVisitors.get(i).insertApplyVisitor(cls, visitor, cls.getInputTags());
      }

      reader.accept(visitor, flags);
      if (!this.keepInputData) {
         cls.data = null;
      }

      return writer.toByteArray();
   }

   private byte[] fixClass(ClassInstance cls, byte[] data) {
      final boolean makeClsPublic = this.classesToMakePublic.contains(cls);
      Set<String> clsMembersToMakePublic = null;

      for (MemberInstance member : cls.getMembers()) {
         if (this.membersToMakePublic.contains(member)) {
            if (clsMembersToMakePublic == null) {
               clsMembersToMakePublic = new HashSet<>();
            }

            AsmRemapper remapper = cls.getContext().remapper;
            String mappedName;
            String mappedDesc;
            if (member.type == TrMember.MemberType.FIELD) {
               mappedName = remapper.mapFieldName(cls, member.name, member.desc);
               mappedDesc = remapper.mapDesc(member.desc);
            } else {
               mappedName = remapper.mapMethodName(cls, member.name, member.desc);
               mappedDesc = remapper.mapMethodDesc(member.desc);
            }

            clsMembersToMakePublic.add(MemberInstance.getId(member.type, mappedName, mappedDesc, this.ignoreFieldDesc));
         }
      }

      if (!makeClsPublic && clsMembersToMakePublic == null) {
         return data;
      }

      final Set<String> finalClsMembersToMakePublic = clsMembersToMakePublic;
      ClassReader reader = new ClassReader(data);
      ClassWriter writer = new ClassWriter(0);
      reader.accept(
         new ClassVisitor(589824, writer) {
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
               if (makeClsPublic) {
                  access = access & -7 | 1;
               }

               super.visit(version, access, name, signature, superName, interfaces);
            }

            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
               if (finalClsMembersToMakePublic != null
                  && finalClsMembersToMakePublic.contains(MemberInstance.getFieldId(name, descriptor, TinyRemapper.this.ignoreFieldDesc))) {
                  access = access & -7 | 1;
               }

               return super.visitField(access, name, descriptor, signature, value);
            }

            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
               if (finalClsMembersToMakePublic != null && finalClsMembersToMakePublic.contains(MemberInstance.getMethodId(name, descriptor))) {
                  access = access & -7 | 1;
               }

               return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
         },
         0
      );
      return writer.toByteArray();
   }

   public synchronized TrEnvironment getEnvironment() {
      this.refresh();
      this.mrjRefresh(this.defaultState);
      return this.defaultState;
   }

   public TrLogger getLogger() {
      return this.logger;
   }

   private static void waitForAll(Iterable<Future<?>> futures) {
      try {
         for (Future<?> future : futures) {
            future.get();
         }
      } catch (InterruptedException | ExecutionException e) {
         throw new RuntimeException(e);
      }
   }

   private static String getClassName(String nameDesc, TrMember.MemberType type) {
      int descStart = getDescStart(nameDesc, type);
      int nameStart = nameDesc.lastIndexOf(47, descStart - 1);
      if (nameStart == -1) {
         nameStart = 0;
      }

      return nameDesc.substring(0, nameStart);
   }

   private static String stripClassName(String nameDesc, TrMember.MemberType type) {
      int descStart = getDescStart(nameDesc, type);
      int nameStart = nameDesc.lastIndexOf(47, descStart - 1);
      if (nameStart == -1) {
         nameStart = 0;
      }

      return nameDesc.substring(nameStart + 1);
   }

   private static int getDescStart(String nameDesc, TrMember.MemberType type) {
      int ret;
      if (type == TrMember.MemberType.METHOD) {
         ret = nameDesc.indexOf(40);
      } else {
         ret = nameDesc.indexOf(";;");
      }

      if (ret == -1) {
         ret = nameDesc.length();
      }

      return ret;
   }

   public interface AnalyzeVisitorProvider {
      @Deprecated
      ClassVisitor insertAnalyzeVisitor(int var1, String var2, ClassVisitor var3);

      @Deprecated
      default ClassVisitor insertAnalyzeVisitor(int mrjVersion, String className, ClassVisitor next, InputTag[] inputTags) {
         return this.insertAnalyzeVisitor(mrjVersion, className, next);
      }

      default ClassVisitor insertAnalyzeVisitor(boolean isInput, int mrjVersion, String className, ClassVisitor next, InputTag[] inputTags) {
         return this.insertAnalyzeVisitor(mrjVersion, className, next, inputTags);
      }
   }

   public interface ApplyVisitorProvider {
      ClassVisitor insertApplyVisitor(TrClass var1, ClassVisitor var2);

      default ClassVisitor insertApplyVisitor(TrClass cls, ClassVisitor next, InputTag[] inputTags) {
         return this.insertApplyVisitor(cls, next);
      }
   }

   public static class Builder {
      private final TrLogger logger;
      private final Set<IMappingProvider> mappingProviders = new HashSet<>();
      private boolean ignoreFieldDesc;
      private int threadCount;
      private final Set<String> forcePropagation = new HashSet<>();
      private final Set<String> knownIndyBsm = new HashSet<>();
      private boolean keepInputData = false;
      private boolean propagatePrivate = false;
      private TinyRemapper.LinkedMethodPropagation propagateBridges = TinyRemapper.LinkedMethodPropagation.DISABLED;
      private TinyRemapper.LinkedMethodPropagation propagateRecordComponents = TinyRemapper.LinkedMethodPropagation.DISABLED;
      private boolean removeFrames = false;
      private boolean ignoreConflicts = false;
      private boolean resolveMissing = false;
      private boolean checkPackageAccess = false;
      private boolean fixPackageAccess = false;
      private boolean rebuildSourceFilenames = false;
      private boolean skipLocalMapping = false;
      private boolean renameInvalidLocals = false;
      private Pattern invalidLvNamePattern;
      private boolean inferNameFromSameLvIndex;
      private boolean disableLocalVariableTracking = false;
      private final List<TinyRemapper.AnalyzeVisitorProvider> analyzeVisitors = new ArrayList<>();
      private final List<TinyRemapper.StateProcessor> stateProcessors = new ArrayList<>();
      private final List<TinyRemapper.ApplyVisitorProvider> preApplyVisitors = new ArrayList<>();
      private final List<TinyRemapper.ApplyVisitorProvider> postApplyVisitors = new ArrayList<>();
      private Remapper extraRemapper;

      private Builder(TrLogger logger) {
         this.logger = Objects.requireNonNull(logger, "logger");
      }

      public TinyRemapper.Builder withMappings(IMappingProvider provider) {
         this.mappingProviders.add(provider);
         return this;
      }

      public TinyRemapper.Builder rebuildSourceFilenames(boolean value) {
         this.rebuildSourceFilenames = value;
         return this;
      }

      public TinyRemapper.Builder renameInvalidLocals(boolean value) {
         this.renameInvalidLocals = value;
         return this;
      }

      public TinyRemapper.Builder extraAnalyzeVisitor(TinyRemapper.AnalyzeVisitorProvider provider) {
         this.analyzeVisitors.add(provider);
         return this;
      }

      public TinyRemapper.Builder extraStateProcessor(TinyRemapper.StateProcessor processor) {
         this.stateProcessors.add(processor);
         return this;
      }

      public TinyRemapper.Builder extraPreApplyVisitor(TinyRemapper.ApplyVisitorProvider provider) {
         this.preApplyVisitors.add(provider);
         return this;
      }

      public TinyRemapper.Builder extension(TinyRemapper.Extension extension) {
         extension.attach(this);
         return this;
      }

      public TinyRemapper build() {
         return new TinyRemapper(
            this.mappingProviders,
            this.ignoreFieldDesc,
            this.threadCount,
            this.keepInputData,
            this.forcePropagation,
            this.knownIndyBsm,
            this.propagatePrivate,
            this.propagateBridges,
            this.propagateRecordComponents,
            this.removeFrames,
            this.ignoreConflicts,
            this.resolveMissing,
            this.checkPackageAccess || this.fixPackageAccess,
            this.fixPackageAccess,
            this.rebuildSourceFilenames,
            this.skipLocalMapping,
            this.renameInvalidLocals,
            this.invalidLvNamePattern,
            this.inferNameFromSameLvIndex,
            this.disableLocalVariableTracking || this.skipLocalMapping,
            this.analyzeVisitors,
            this.stateProcessors,
            this.preApplyVisitors,
            this.postApplyVisitors,
            this.extraRemapper,
            this.logger
         );
      }
   }

   enum Direction {
      ANY,
      UP,
      DOWN;
   }

   public interface Extension {
      void attach(TinyRemapper.Builder var1);
   }

   public enum LinkedMethodPropagation {
      DISABLED,
      ENABLED,
      COMPATIBLE;
   }

   static final class MrjState implements TrEnvironment {
      final TinyRemapper tr;
      final int version;
      final Map<String, ClassInstance> classes = new HashMap<>();
      final AsmRemapper remapper;
      volatile boolean dirty = true;

      MrjState(TinyRemapper tr, int version) {
         Objects.requireNonNull(tr);
         this.tr = tr;
         this.version = version;
         this.remapper = new AsmRemapper(this);
      }

      @Override
      public int getMrjVersion() {
         return this.version;
      }

      public AsmRemapper getRemapper() {
         return this.remapper;
      }

      @Override
      public TrLogger getLogger() {
         return this.tr.logger;
      }

      public ClassInstance getClass(String internalName) {
         return this.classes.get(internalName);
      }

      @Override
      public void propagate(TrMember m, String newName) {
         MemberInstance member = (MemberInstance)m;
         Set<ClassInstance> visitedUp = Collections.newSetFromMap(new IdentityHashMap<>());
         Set<ClassInstance> visitedDown = Collections.newSetFromMap(new IdentityHashMap<>());
         Propagator.propagate(member, member.getId(), newName, visitedUp, visitedDown);
      }
   }

   class Propagation implements Runnable {
      private final TinyRemapper.MrjState state;
      private final TrMember.MemberType type;
      private final List<Entry<String, String>> tasks = new ArrayList<>();

      Propagation(TinyRemapper.MrjState state, TrMember.MemberType type, List<Entry<String, String>> tasks) {
         this.state = state;
         this.type = type;
         this.tasks.addAll(tasks);
      }

      @Override
      public void run() {
         Set<ClassInstance> visitedUp = Collections.newSetFromMap(new IdentityHashMap<>());
         Set<ClassInstance> visitedDown = Collections.newSetFromMap(new IdentityHashMap<>());

         for (Entry<String, String> entry : this.tasks) {
            String className = TinyRemapper.getClassName(entry.getKey(), this.type);
            ClassInstance cls = this.state.getClass(className);
            if (cls != null) {
               String idSrc = TinyRemapper.stripClassName(entry.getKey(), this.type);
               String nameDst = entry.getValue();
               assert nameDst.indexOf(47) < 0;
               if (!MemberInstance.getNameFromId(this.type, idSrc, TinyRemapper.this.ignoreFieldDesc).equals(nameDst)) {
                  MemberInstance member = TinyRemapper.this.resolveMissing ? cls.resolve(this.type, idSrc) : cls.getMember(this.type, idSrc);
                  if (member != null) {
                     Propagator.propagate(member, idSrc, nameDst, visitedUp, visitedDown);
                  }
               }
            }
         }
      }
   }

   public interface StateProcessor {
      void process(TrEnvironment var1);
   }
}
