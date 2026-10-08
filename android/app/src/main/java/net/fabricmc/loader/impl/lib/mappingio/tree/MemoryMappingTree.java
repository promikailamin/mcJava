package net.fabricmc.loader.impl.lib.mappingio.tree;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.loader.impl.lib.mappingio.MappedElementKind;
import net.fabricmc.loader.impl.lib.mappingio.MappingFlag;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import org.jetbrains.annotations.Nullable;

public final class MemoryMappingTree implements VisitableMappingTree {
   private boolean inVisitPass;
   private boolean indexByDstNames;
   private String srcNamespace;
   private List<String> dstNamespaces = Collections.emptyList();
   private final List<MappingTree.MetadataEntry> metadata = new ArrayList<>();
   private final Map<String, MemoryMappingTree.ClassEntry> classesBySrcName = new LinkedHashMap<>();
   private final Collection<MemoryMappingTree.ClassEntry> classesView = Collections.unmodifiableCollection(this.classesBySrcName.values());
   private Map<String, MemoryMappingTree.ClassEntry>[] classesByDstNames;
   private HierarchyInfoProvider<?> hierarchyInfo;
   private int srcNsMap;
   private int[] dstNameMap;
   private MemoryMappingTree.Entry<?> currentEntry;
   private MemoryMappingTree.ClassEntry currentClass;
   private MemoryMappingTree.MethodEntry currentMethod;
   private Map<String, MemoryMappingTree.ClassEntry> pendingClasses;
   private Map<MemoryMappingTree.GlobalMemberKey, MemoryMappingTree.MemberEntry<?>> pendingMembers;

   public MemoryMappingTree() {
      this(false);
   }

   public MemoryMappingTree(boolean indexByDstNames) {
      this.indexByDstNames = indexByDstNames;
   }

   private void initClassesByDstNames() {
      this.classesByDstNames = new Map[this.dstNamespaces.size()];

      for (int i = 0; i < this.classesByDstNames.length; i++) {
         this.classesByDstNames[i] = new HashMap<>(this.classesBySrcName.size());
      }

      for (MemoryMappingTree.ClassEntry cls : this.classesBySrcName.values()) {
         for (int i = 0; i < cls.dstNames.length; i++) {
            String dstName = cls.dstNames[i];
            if (dstName != null) {
               this.classesByDstNames[i].put(dstName, cls);
            }
         }
      }
   }

   @Nullable
   @Override
   public String getSrcNamespace() {
      return this.srcNamespace;
   }

   @Override
   public List<String> getDstNamespaces() {
      return this.dstNamespaces;
   }

   private void resizeDstNames(int newSize) {
      for (MemoryMappingTree.ClassEntry cls : this.classesBySrcName.values()) {
         cls.resizeDstNames(newSize);

         for (MemoryMappingTree.FieldEntry field : cls.getFields()) {
            field.resizeDstNames(newSize);
         }

         for (MemoryMappingTree.MethodEntry method : cls.getMethods()) {
            method.resizeDstNames(newSize);

            for (MemoryMappingTree.MethodArgEntry arg : method.getArgs()) {
               arg.resizeDstNames(newSize);
            }

            for (MemoryMappingTree.MethodVarEntry var : method.getVars()) {
               var.resizeDstNames(newSize);
            }
         }
      }
   }

   @Override
   public Collection<? extends MappingTree.ClassMapping> getClasses() {
      return this.classesView;
   }

   @Nullable
   @Override
   public MappingTree.ClassMapping getClass(String srcName) {
      return this.classesBySrcName.get(srcName);
   }

   @Nullable
   @Override
   public MappingTree.ClassMapping getClass(String name, int namespace) {
      return namespace >= 0 && this.indexByDstNames ? this.classesByDstNames[namespace].get(name) : VisitableMappingTree.super.getClass(name, namespace);
   }

   private int getSrcNsEquivalent(MappingTree.ElementMapping mapping) {
      int ret = mapping.getTree().getNamespaceId(this.srcNamespace);
      if (ret == -2) {
         throw new UnsupportedOperationException("can't find source namespace in referenced mapping tree");
      } else {
         return ret;
      }
   }

   @Override
   public void accept(MappingVisitor visitor, VisitOrder order) throws IOException {
      do {
         if (visitor.visitHeader()) {
            visitor.visitNamespaces(this.srcNamespace, this.dstNamespaces);
            Collection<MappingTree.MetadataEntry> metadataToVisit = this.metadata;
            if (visitor.getFlags().contains(MappingFlag.NEEDS_METADATA_UNIQUENESS)) {
               Deque<MappingTree.MetadataEntry> uniqueMetadata = new ArrayDeque<>();
               Set<String> addedKeys = new HashSet<>();

               for (int i = this.metadata.size() - 1; i >= 0; i--) {
                  MappingTree.MetadataEntry entry = this.metadata.get(i);
                  if (!addedKeys.contains(entry.getKey())) {
                     addedKeys.add(entry.getKey());
                     uniqueMetadata.addFirst(entry);
                  }
               }

               metadataToVisit = uniqueMetadata;
            }

            for (MappingTree.MetadataEntry entry : metadataToVisit) {
               visitor.visitMetadata(entry.getKey(), entry.getValue());
            }
         }

         if (visitor.visitContent()) {
            Set<MappingFlag> flags = visitor.getFlags();
            boolean supplyFieldDstDescs = flags.contains(MappingFlag.NEEDS_DST_FIELD_DESC);
            boolean supplyMethodDstDescs = flags.contains(MappingFlag.NEEDS_DST_METHOD_DESC);

            for (MemoryMappingTree.ClassEntry cls : order.sortClasses(this.classesBySrcName.values())) {
               cls.accept(visitor, order, supplyFieldDstDescs, supplyMethodDstDescs);
            }
         }
      } while (!visitor.visitEnd());
   }

   @Override
   public void reset() {
      this.inVisitPass = false;
      this.srcNsMap = -1;
      this.dstNameMap = null;
      this.currentEntry = null;
      this.currentClass = null;
      this.currentMethod = null;
      this.pendingClasses = null;
      this.pendingMembers = null;
   }

   @Override
   public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) {
      this.inVisitPass = true;
      this.srcNsMap = -1;
      this.dstNameMap = new int[dstNamespaces.size()];
      if (this.srcNamespace != null) {
         if (!srcNamespace.equals(this.srcNamespace)) {
            this.srcNsMap = this.dstNamespaces.indexOf(srcNamespace);
            if (this.srcNsMap < 0) {
               this.reset();
               throw new IllegalArgumentException("can't merge with disassociated src namespace");
            }
         }

         int newDstNamespaces = 0;

         for (int i = 0; i < this.dstNameMap.length; i++) {
            String dstNs = dstNamespaces.get(i);
            int idx;
            if (dstNs.equals(this.srcNamespace)) {
               idx = -1;
            } else {
               if (dstNs.equals(srcNamespace)) {
                  this.reset();
                  throw new IllegalArgumentException("namespace \"" + srcNamespace + "\" is present on both source and destination side simultaneously");
               }

               idx = this.dstNamespaces.indexOf(dstNs);
               if (idx < 0) {
                  if (newDstNamespaces == 0) {
                     this.dstNamespaces = new ArrayList<>(this.dstNamespaces);
                  }

                  idx = this.dstNamespaces.size();
                  this.dstNamespaces.add(dstNs);
                  newDstNamespaces++;
               }
            }

            this.dstNameMap[i] = idx;
         }

         if (newDstNamespaces > 0) {
            int newSize = this.dstNamespaces.size();
            this.resizeDstNames(newSize);
            if (this.indexByDstNames) {
               this.classesByDstNames = Arrays.copyOf(this.classesByDstNames, newSize);

               for (int i = newSize - newDstNamespaces; i < this.classesByDstNames.length; i++) {
                  this.classesByDstNames[i] = new HashMap<>(this.classesBySrcName.size());
               }
            }
         }
      } else {
         this.srcNamespace = srcNamespace;
         this.dstNamespaces = dstNamespaces;

         for (int i = 0; i < this.dstNameMap.length; this.dstNameMap[i] = i++) {
            if (dstNamespaces.get(i).equals(srcNamespace)) {
               this.reset();
               throw new IllegalArgumentException("namespace \"" + srcNamespace + "\" is present on both source and destination side simultaneously");
            }
         }

         if (this.indexByDstNames) {
            this.initClassesByDstNames();
         }
      }
   }

   @Override
   public void visitMetadata(String key, @Nullable String value) {
      MemoryMappingTree.MetadataEntryImpl entry = new MemoryMappingTree.MetadataEntryImpl(key, value);
      this.metadata.add(entry);
   }

   @Override
   public boolean visitClass(String srcName) {
      this.currentMethod = null;
      MemoryMappingTree.ClassEntry cls = (MemoryMappingTree.ClassEntry)this.getClass(srcName, this.srcNsMap);
      if (cls == null) {
         if (this.srcNsMap >= 0) {
            cls = this.queuePendingClass(srcName);
         } else {
            cls = new MemoryMappingTree.ClassEntry(this, srcName);
            this.classesBySrcName.put(srcName, cls);
         }
      }

      this.currentEntry = this.currentClass = cls;
      return true;
   }

   @Override
   public boolean visitField(String srcName, @Nullable String srcDesc) {
      if (this.currentClass == null) {
         throw new UnsupportedOperationException("Tried to visit field before owning class");
      }

      this.currentMethod = null;
      MemoryMappingTree.FieldEntry field = this.currentClass.getField(srcName, srcDesc, this.srcNsMap);
      if (field == null) {
         if (this.srcNsMap >= 0) {
            field = (MemoryMappingTree.FieldEntry)this.queuePendingMember(srcName, srcDesc, true);
         } else {
            field = new MemoryMappingTree.FieldEntry(this.currentClass, srcName, srcDesc);
            field = this.currentClass.addFieldInternal(field);
         }
      } else if (srcDesc != null && field.srcDesc == null) {
         if (this.srcNsMap >= 0) {
            this.queuePendingMember(srcName, srcDesc, true).setSrcName(field.getSrcName());
         } else {
            field.setSrcDescInternal(srcDesc);
         }
      }

      this.currentEntry = field;
      return true;
   }

   @Override
   public boolean visitMethod(String srcName, @Nullable String srcDesc) {
      if (this.currentClass == null) {
         throw new UnsupportedOperationException("Tried to visit method before owning class");
      }

      MemoryMappingTree.MethodEntry method = this.currentClass.getMethod(srcName, srcDesc, this.srcNsMap);
      if (method == null) {
         if (this.srcNsMap >= 0) {
            method = (MemoryMappingTree.MethodEntry)this.queuePendingMember(srcName, srcDesc, false);
         } else {
            method = new MemoryMappingTree.MethodEntry(this.currentClass, srcName, srcDesc);
            method = this.currentClass.addMethodInternal(method);
         }
      } else if (isValidDescriptor(srcDesc, true) && !isValidDescriptor(method.srcDesc, true)) {
         if (this.srcNsMap >= 0) {
            this.queuePendingMember(srcName, srcDesc, false).setSrcName(method.getSrcName());
         } else {
            method.setSrcDescInternal(srcDesc);
         }
      }

      this.currentEntry = this.currentMethod = method;
      return true;
   }

   private MemoryMappingTree.ClassEntry queuePendingClass(String name) {
      if (this.pendingClasses == null) {
         this.pendingClasses = new HashMap<>();
      }

      MemoryMappingTree.ClassEntry cls = this.pendingClasses.get(name);
      if (cls == null) {
         cls = new MemoryMappingTree.ClassEntry(this, null);
         this.pendingClasses.put(name, cls);
      }

      assert this.srcNsMap >= 0;
      cls.setDstNameInternal(name, this.srcNsMap);
      return cls;
   }

   private MemoryMappingTree.MemberEntry<?> queuePendingMember(String name, @Nullable String desc, boolean isField) {
      if (this.pendingMembers == null) {
         this.pendingMembers = new HashMap<>();
      }

      MemoryMappingTree.GlobalMemberKey key = new MemoryMappingTree.GlobalMemberKey(this.currentClass, name, desc, isField);
      MemoryMappingTree.MemberEntry<?> member = this.pendingMembers.get(key);
      if (member == null) {
         if (isField) {
            member = new MemoryMappingTree.FieldEntry(this.currentClass, null, desc);
         } else {
            member = new MemoryMappingTree.MethodEntry(this.currentClass, null, desc);
         }

         this.pendingMembers.put(key, member);
      }

      assert this.srcNsMap >= 0;
      member.setDstNameInternal(name, this.srcNsMap);
      return member;
   }

   private void addPendingClass(MemoryMappingTree.ClassEntry cls) {
      if (!cls.isSrcNameMissing()) {
         String srcName = cls.getSrcName();
         MemoryMappingTree.ClassEntry existing = this.classesBySrcName.get(srcName);
         if (existing == null) {
            this.classesBySrcName.put(srcName, cls);
         } else {
            existing.copyFrom(cls, true);
         }
      }
   }

   private void addPendingMember(MemoryMappingTree.MemberEntry<?> member) {
      if (!member.isSrcNameMissing() && !member.getOwner().isSrcNameMissing()) {
         MemoryMappingTree.ClassEntry owner = this.classesBySrcName.get(member.getOwner().getSrcName());
         member.setOwner(owner);
         boolean isField = member.getKind() == MappedElementKind.FIELD;
         String srcName = member.getSrcName();
         String dstDesc = member.getSrcDesc();
         String srcDesc = null;
         if (isValidDescriptor(dstDesc, !isField)) {
            srcDesc = this.mapDesc(dstDesc, this.srcNsMap, -1);
         }

         member.setSrcDescInternal(srcDesc);
         if (isField) {
            MemoryMappingTree.FieldEntry queuedField = (MemoryMappingTree.FieldEntry)member;
            MemoryMappingTree.FieldEntry existingField = owner.getField(srcName, srcDesc);
            if (existingField == null) {
               owner.addFieldInternal(queuedField);
            } else {
               existingField.copyFrom(queuedField, true);
            }
         } else {
            MemoryMappingTree.MethodEntry queuedMethod = (MemoryMappingTree.MethodEntry)member;
            MemoryMappingTree.MethodEntry existingMethod = owner.getMethod(srcName, srcDesc);
            if (existingMethod == null) {
               owner.addMethodInternal(queuedMethod);
            } else {
               existingMethod.copyFrom(queuedMethod, true);
            }
         }
      }
   }

   @Override
   public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) {
      if (this.currentMethod == null) {
         throw new UnsupportedOperationException("Tried to visit method argument before owning method");
      }

      MemoryMappingTree.MethodArgEntry arg = this.currentMethod.getArg(argPosition, lvIndex, srcName);
      if (arg == null) {
         arg = new MemoryMappingTree.MethodArgEntry(this.currentMethod, argPosition, lvIndex, srcName);
         arg = this.currentMethod.addArgInternal(arg);
      } else {
         if (argPosition >= 0 && arg.argPosition < 0) {
            arg.setArgPositionInternal(argPosition);
         }

         if (lvIndex >= 0 && arg.lvIndex < 0) {
            arg.setLvIndexInternal(lvIndex);
         }

         if (srcName != null) {
            assert !srcName.isEmpty();
            arg.setSrcName(srcName);
         }
      }

      this.currentEntry = arg;
      return true;
   }

   @Override
   public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) {
      if (this.currentMethod == null) {
         throw new UnsupportedOperationException("Tried to visit method variable before owning method");
      }

      MemoryMappingTree.MethodVarEntry var = this.currentMethod.getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
      if (var == null) {
         var = new MemoryMappingTree.MethodVarEntry(this.currentMethod, lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
         var = this.currentMethod.addVarInternal(var);
      } else {
         if (lvtRowIndex >= 0 && var.lvtRowIndex < 0) {
            var.setLvtRowIndexInternal(lvtRowIndex);
         }

         if (lvIndex >= 0 && startOpIdx >= 0 && (var.lvIndex < 0 || var.startOpIdx < 0)) {
            var.setLvIndexInternal(lvIndex, startOpIdx, endOpIdx);
         }

         if (srcName != null) {
            assert !srcName.isEmpty();
            var.setSrcName(srcName);
         }
      }

      this.currentEntry = var;
      return true;
   }

   @Override
   public boolean visitEnd() {
      if (this.pendingClasses != null) {
         for (MemoryMappingTree.ClassEntry cls : this.pendingClasses.values()) {
            this.addPendingClass(cls);
         }

         this.pendingClasses = null;
      }

      if (this.pendingMembers != null) {
         for (MemoryMappingTree.MemberEntry<?> member : this.pendingMembers.values()) {
            this.addPendingMember(member);
         }

         this.pendingMembers = null;
      }

      this.reset();
      if (this.hierarchyInfo != null) {
         this.propagateNames(this.hierarchyInfo);
      }

      return true;
   }

   private <T> void propagateNames(HierarchyInfoProvider<T> provider) {
      int nsId = this.getNamespaceId(provider.getNamespace());
      if (nsId != -2) {
         Set<MemoryMappingTree.MethodEntry> processed = Collections.newSetFromMap(new IdentityHashMap<>());

         for (MemoryMappingTree.ClassEntry cls : this.classesBySrcName.values()) {
            for (MemoryMappingTree.MethodEntry method : cls.getMethods()) {
               String name = method.getName(nsId);
               if (name != null && !name.startsWith("<") && processed.add(method)) {
                  T hierarchy = provider.getMethodHierarchy(method);
                  if (provider.getHierarchySize(hierarchy) > 1) {
                     Collection<? extends MappingTree.MethodMapping> hierarchyMethods = provider.getHierarchyMethods(hierarchy, this);
                     if (hierarchyMethods.size() > 1) {
                        String[] dstNames = new String[this.dstNamespaces.size()];
                        int rem = dstNames.length;

                        label82:
                        for (MappingTree.MethodMapping m : hierarchyMethods) {
                           for (int i = 0; i < dstNames.length; i++) {
                              if (dstNames[i] == null) {
                                 String curName = m.getDstName(i);
                                 if (curName != null) {
                                    dstNames[i] = curName;
                                    if (--rem == 0) {
                                       break label82;
                                    }
                                 }
                              }
                           }
                        }

                        for (MappingTree.MethodMapping m : hierarchyMethods) {
                           processed.add((MemoryMappingTree.MethodEntry)m);

                           for (int i = 0; i < dstNames.length; i++) {
                              String curName = dstNames[i];
                              if (curName != null) {
                                 m.setDstName(curName, i);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
      namespace = this.dstNameMap[namespace];
      if (this.currentEntry == null) {
         throw new UnsupportedOperationException("Tried to visit mapped name before owner");
      }

      if (namespace < 0) {
         if (!name.equals(this.currentEntry.getSrcNameUnchecked())) {
            switch (this.currentEntry.getKind()) {
               case CLASS:
                  assert this.currentClass == this.currentEntry;
               case FIELD:
               case METHOD:
                  if (this.currentEntry.isSrcNameMissing()) {
                     this.currentEntry.setSrcName(name);
                     return;
                  }
               default:
                  throw new UnsupportedOperationException("can't change src name for " + this.currentEntry.getKind());
               case METHOD_ARG:
               case METHOD_VAR:
                  this.currentEntry.setSrcName(name);
            }
         }
      } else {
         this.currentEntry.setDstNameInternal(name, namespace);
      }
   }

   @Override
   public void visitComment(MappedElementKind targetKind, String comment) {
      MemoryMappingTree.Entry<?> entry;
      switch (targetKind) {
         case CLASS:
            entry = this.currentClass;
            break;
         case METHOD:
            entry = this.currentMethod;
            break;
         default:
            entry = this.currentEntry;
      }

      if (entry == null) {
         throw new UnsupportedOperationException("Tried to visit comment before owning target");
      }

      entry.setCommentInternal(comment);
   }

   private static boolean isValidDescriptor(String descriptor, boolean possiblyMethod) {
      return descriptor == null ? false : !possiblyMethod || !descriptor.endsWith(")");
   }

   void assertNotInVisitPass() {
      if (this.inVisitPass) {
         throw new UnsupportedOperationException("Attempted illegal tree interaction via tree-API during an ongoing visitation pass");
      }
   }

   static final class ClassEntry extends MemoryMappingTree.Entry<MemoryMappingTree.ClassEntry> implements MappingTree.ClassMapping {
      private Map<MemoryMappingTree.MemberKey, MemoryMappingTree.FieldEntry> fields = null;
      private Map<MemoryMappingTree.MemberKey, MemoryMappingTree.MethodEntry> methods = null;
      private Collection<MemoryMappingTree.FieldEntry> fieldsView = null;
      private Collection<MemoryMappingTree.MethodEntry> methodsView = null;
      private byte flags;

      ClassEntry(MemoryMappingTree tree, String srcName) {
         super(tree, srcName);
      }

      @Override
      public MappedElementKind getKind() {
         return MappedElementKind.CLASS;
      }

      public MemoryMappingTree getTree() {
         return this.tree;
      }

      @Override
      void setDstNameInternal(String name, int namespace) {
         if (this.tree.indexByDstNames) {
            String oldName = this.dstNames[namespace];
            if (!Objects.equals(name, oldName)) {
               Map<String, MemoryMappingTree.ClassEntry> map = this.tree.classesByDstNames[namespace];
               if (oldName != null) {
                  map.remove(oldName);
               }

               if (name != null) {
                  map.put(name, this);
               } else {
                  map.remove(oldName);
               }
            }
         }

         super.setDstNameInternal(name, namespace);
      }

      @Override
      public Collection<MemoryMappingTree.FieldEntry> getFields() {
         return this.fields == null ? Collections.emptyList() : this.fieldsView;
      }

      @Nullable
      public MemoryMappingTree.FieldEntry getField(String srcName, @Nullable String srcDesc) {
         return getMember(srcName, srcDesc, this.fields, this.flags, 1, 2);
      }

      @Nullable
      public MemoryMappingTree.FieldEntry getField(String name, @Nullable String desc, int namespace) {
         return (MemoryMappingTree.FieldEntry)MappingTree.ClassMapping.super.getField(name, desc, namespace);
      }

      MemoryMappingTree.FieldEntry addFieldInternal(MappingTree.FieldMapping field) {
         MemoryMappingTree.FieldEntry entry = field instanceof MemoryMappingTree.FieldEntry && field.getOwner() == this
            ? (MemoryMappingTree.FieldEntry)field
            : new MemoryMappingTree.FieldEntry(this, field, this.tree.getSrcNsEquivalent(field));
         if (this.fields == null) {
            this.fields = new LinkedHashMap<>();
            this.fieldsView = Collections.unmodifiableCollection(this.fields.values());
         }

         return this.addMember(entry, this.fields, 1, 2);
      }

      @Override
      public Collection<MemoryMappingTree.MethodEntry> getMethods() {
         return this.methods == null ? Collections.emptyList() : this.methodsView;
      }

      @Nullable
      public MemoryMappingTree.MethodEntry getMethod(String srcName, @Nullable String srcDesc) {
         return getMember(srcName, srcDesc, this.methods, this.flags, 4, 8);
      }

      @Nullable
      public MemoryMappingTree.MethodEntry getMethod(String name, @Nullable String desc, int namespace) {
         return (MemoryMappingTree.MethodEntry)MappingTree.ClassMapping.super.getMethod(name, desc, namespace);
      }

      MemoryMappingTree.MethodEntry addMethodInternal(MappingTree.MethodMapping method) {
         MemoryMappingTree.MethodEntry entry = method instanceof MemoryMappingTree.MethodEntry && method.getOwner() == this
            ? (MemoryMappingTree.MethodEntry)method
            : new MemoryMappingTree.MethodEntry(this, method, this.tree.getSrcNsEquivalent(method));
         if (this.methods == null) {
            this.methods = new LinkedHashMap<>();
            this.methodsView = Collections.unmodifiableCollection(this.methods.values());
         }

         return this.addMember(entry, this.methods, 4, 8);
      }

      private static <T extends MemoryMappingTree.MemberEntry<T>> T getMember(
         String srcName, @Nullable String srcDesc, @Nullable Map<MemoryMappingTree.MemberKey, T> map, int flags, int flagHasAny, int flagMissesAny
      ) {
         if (map == null) {
            return null;
         }

         boolean hasAnyDesc = (flags & flagHasAny) != 0;
         boolean missedAnyDesc = (flags & flagMissesAny) != 0;
         if (srcDesc == null) {
            if (missedAnyDesc) {
               T ret = (T)map.get(new MemoryMappingTree.MemberKey(srcName, null));
               if (ret != null) {
                  return ret;
               }
            }

            if (hasAnyDesc) {
               for (T entry : map.values()) {
                  if (entry.getSrcName().equals(srcName)) {
                     return entry;
                  }
               }
            }
         } else if (srcDesc.endsWith(")")) {
            if (missedAnyDesc) {
               T ret = (T)map.get(new MemoryMappingTree.MemberKey(srcName, srcDesc));
               if (ret != null) {
                  return ret;
               }

               ret = (T)map.get(new MemoryMappingTree.MemberKey(srcName, null));
               if (ret != null) {
                  return ret;
               }
            }

            if (hasAnyDesc) {
               for (T entry : map.values()) {
                  if (entry.getSrcName().equals(srcName) && entry.srcDesc.startsWith(srcDesc)) {
                     return entry;
                  }
               }
            }
         } else {
            if (hasAnyDesc) {
               T ret = (T)map.get(new MemoryMappingTree.MemberKey(srcName, srcDesc));
               if (ret != null) {
                  return ret;
               }
            }

            if (missedAnyDesc) {
               T ret = (T)map.get(new MemoryMappingTree.MemberKey(srcName, null));
               if (ret != null) {
                  return ret;
               }

               if (srcDesc.indexOf(41) >= 0) {
                  for (T entry : map.values()) {
                     if (entry.getSrcName().equals(srcName) && srcDesc.startsWith(entry.srcDesc)) {
                        return entry;
                     }
                  }
               }
            }
         }

         return null;
      }

      private <T extends MemoryMappingTree.MemberEntry<T>> T addMember(T entry, Map<MemoryMappingTree.MemberKey, T> map, int flagHasAny, int flagMissesAny) {
         T ret = (T)map.putIfAbsent(entry.getKey(), entry);
         if (ret != null) {
            ret.copyFrom(entry, true);
            return ret;
         }

         if (MemoryMappingTree.isValidDescriptor(entry.srcDesc, true)) {
            this.flags = (byte)(this.flags | flagHasAny);
            if ((this.flags & flagMissesAny) != 0) {
               ret = (T)map.remove(new MemoryMappingTree.MemberKey(entry.getSrcName(), null));
               if (ret != null) {
                  ret.setKey(entry.getKey());
                  ret.srcDesc = entry.srcDesc;
                  map.put(ret.getKey(), ret);
                  ret.copyFrom(entry, true);
                  entry = ret;
               }
            }

            return entry;
         } else {
            if ((this.flags & flagHasAny) != 0) {
               for (T prevEntry : map.values()) {
                  if (prevEntry != entry
                     && prevEntry.getSrcName().equals(entry.getSrcName())
                     && (entry.srcDesc == null || prevEntry.srcDesc.startsWith(entry.srcDesc))) {
                     map.remove(entry.getKey());
                     prevEntry.copyFrom(entry, true);
                     return prevEntry;
                  }
               }
            }

            this.flags = (byte)(this.flags | flagMissesAny);
            return entry;
         }
      }

      void accept(MappingVisitor visitor, VisitOrder order, boolean supplyFieldDstDescs, boolean supplyMethodDstDescs) throws IOException {
         if (visitor.visitClass(this.getSrcName()) && this.acceptElement(visitor, null)) {
            boolean methodsFirst = order.isMethodsFirst() && this.fields != null && this.methods != null;
            if (!methodsFirst && this.fields != null) {
               for (MemoryMappingTree.FieldEntry field : order.sortFields(this.fields.values())) {
                  field.accept(visitor, supplyFieldDstDescs);
               }
            }

            if (this.methods != null) {
               for (MemoryMappingTree.MethodEntry method : order.sortMethods(this.methods.values())) {
                  method.accept(visitor, order, supplyMethodDstDescs);
               }
            }

            if (methodsFirst) {
               for (MemoryMappingTree.FieldEntry field : order.sortFields(this.fields.values())) {
                  field.accept(visitor, supplyFieldDstDescs);
               }
            }
         }
      }

      protected void copyFrom(MemoryMappingTree.ClassEntry o, boolean replace) {
         super.copyFrom(o, replace);
         if (o.fields != null) {
            for (MemoryMappingTree.FieldEntry oField : o.fields.values()) {
               MemoryMappingTree.FieldEntry field = this.getField(oField.getSrcName(), oField.srcDesc);
               if (field == null) {
                  this.addFieldInternal(oField);
               } else {
                  if (oField.srcDesc != null && field.srcDesc == null) {
                     this.fields.remove(field.getKey());
                     field.setKey(oField.getKey());
                     field.srcDesc = oField.srcDesc;
                     this.fields.put(field.getKey(), field);
                     this.flags = (byte)(this.flags | 1);
                  }

                  field.copyFrom(oField, replace);
               }
            }
         }

         if (o.methods != null) {
            for (MemoryMappingTree.MethodEntry oMethod : o.methods.values()) {
               MemoryMappingTree.MethodEntry method = this.getMethod(oMethod.getSrcName(), oMethod.srcDesc);
               if (method == null) {
                  this.addMethodInternal(oMethod);
               } else {
                  if (oMethod.srcDesc != null && method.srcDesc == null) {
                     this.methods.remove(method.getKey());
                     method.setKey(oMethod.getKey());
                     method.srcDesc = oMethod.srcDesc;
                     this.methods.put(method.getKey(), method);
                     this.flags = (byte)(this.flags | 4);
                  }

                  method.copyFrom(oMethod, replace);
               }
            }
         }
      }

      @Override
      public String toString() {
         return this.getSrcNameUnchecked();
      }
   }

   abstract static class Entry<T extends MemoryMappingTree.Entry<T>> implements MappingTree.ElementMapping {
      private final boolean missingSrcNameAllowed = this.getKind().level > MappedElementKind.METHOD.level;
      protected final MemoryMappingTree tree;
      private String srcName;
      protected String[] dstNames;
      protected String comment;

      protected Entry(MemoryMappingTree tree, String srcName) {
         this.tree = tree;
         this.srcName = srcName;
         this.dstNames = new String[tree.dstNamespaces.size()];
      }

      protected Entry(MemoryMappingTree tree, MappingTree.ElementMapping src, int srcNsEquivalent) {
         this(tree, src.getName(srcNsEquivalent));

         for (int i = 0; i < this.dstNames.length; i++) {
            int dstNsEquivalent = src.getTree().getNamespaceId(tree.dstNamespaces.get(i));
            if (dstNsEquivalent != -2) {
               this.setDstNameInternal(src.getDstName(dstNsEquivalent), i);
            }
         }

         this.setCommentInternal(src.getComment());
      }

      public abstract MappedElementKind getKind();

      final boolean isSrcNameMissing() {
         return this.srcName == null;
      }

      String getSrcNameUnchecked() {
         return this.srcName;
      }

      @Override
      public final String getSrcName() {
         if (!this.missingSrcNameAllowed) {
            this.assertSrcNamePresent();
         }

         return this.srcName;
      }

      protected final void assertSrcNamePresent() {
         if (this.isSrcNameMissing()) {
            throw new UnsupportedOperationException("Attempted illegal interaction with a pending entry still missing its tree-side source name");
         }
      }

      void setSrcName(String name) {
         if (!this.missingSrcNameAllowed && name == null) {
            throw new UnsupportedOperationException("Source name cannot be null");
         }

         this.srcName = name;
      }

      @Nullable
      @Override
      public final String getDstName(int namespace) {
         return this.dstNames[namespace];
      }

      @Override
      public final void setDstName(String name, int namespace) {
         this.tree.assertNotInVisitPass();
         this.setDstNameInternal(name, namespace);
      }

      void setDstNameInternal(String name, int namespace) {
         this.dstNames[namespace] = name;
      }

      void resizeDstNames(int newSize) {
         this.dstNames = Arrays.copyOf(this.dstNames, newSize);
      }

      @Nullable
      @Override
      public final String getComment() {
         return this.comment;
      }

      void setCommentInternal(String comment) {
         this.comment = comment;
      }

      protected final boolean acceptElement(MappingVisitor visitor, @Nullable String[] dstDescs) throws IOException {
         MappedElementKind kind = this.getKind();

         for (int i = 0; i < this.dstNames.length; i++) {
            String dstName = this.dstNames[i];
            if (dstName != null) {
               visitor.visitDstName(kind, i, dstName);
            }
         }

         if (dstDescs != null) {
            for (int i = 0; i < dstDescs.length; i++) {
               String dstDesc = dstDescs[i];
               if (dstDesc != null) {
                  visitor.visitDstDesc(kind, i, dstDesc);
               }
            }
         }

         if (!visitor.visitElementContent(kind)) {
            return false;
         }

         if (this.comment != null) {
            visitor.visitComment(kind, this.comment);
         }

         return true;
      }

      protected void copyFrom(T o, boolean replace) {
         for (int i = 0; i < this.dstNames.length; i++) {
            if (o.dstNames[i] != null && (replace || this.dstNames[i] == null)) {
               this.dstNames[i] = o.dstNames[i];
            }
         }

         if (o.comment != null && (replace || this.comment == null)) {
            this.comment = o.comment;
         }
      }
   }

   static final class FieldEntry extends MemoryMappingTree.MemberEntry<MemoryMappingTree.FieldEntry> implements MappingTree.FieldMapping {
      FieldEntry(MemoryMappingTree.ClassEntry owner, String srcName, @Nullable String srcDesc) {
         super(owner, srcName, srcDesc);
      }

      FieldEntry(MemoryMappingTree.ClassEntry owner, MappingTree.FieldMapping src, int srcNsEquivalent) {
         super(owner, src, srcNsEquivalent);
      }

      @Override
      public MappedElementKind getKind() {
         return MappedElementKind.FIELD;
      }

      @Override
      void setSrcDescInternal(@Nullable String desc) {
         if (!Objects.equals(desc, this.srcDesc)) {
            MemoryMappingTree.MemberKey newKey = new MemoryMappingTree.MemberKey(this.getSrcName(), desc);
            if (this.owner.fields != null) {
               if (this.owner.fields.containsKey(newKey)) {
                  throw new IllegalArgumentException("conflicting name+desc after changing desc to " + desc + " for " + this);
               }

               this.owner.fields.remove(this.getKey());
            }

            this.srcDesc = desc;
            this.setKey(newKey);
            if (this.owner.fields != null) {
               this.owner.fields.put(newKey, this);
            }

            if (desc != null) {
               MemoryMappingTree.ClassEntry.access$1276(this.owner, 1);
            } else {
               MemoryMappingTree.ClassEntry.access$1276(this.owner, 2);
            }
         }
      }

      void accept(MappingVisitor visitor, boolean supplyDstDescs) throws IOException {
         if (visitor.visitField(this.getSrcName(), this.srcDesc)) {
            this.acceptMember(visitor, supplyDstDescs);
         }
      }

      @Override
      public String toString() {
         return String.format("%s;;%s", this.getSrcNameUnchecked(), this.srcDesc);
      }
   }

   static final class GlobalMemberKey {
      private final MemoryMappingTree.ClassEntry owner;
      private final String name;
      private final String desc;
      private final boolean isField;

      GlobalMemberKey(MemoryMappingTree.ClassEntry owner, String name, @Nullable String desc, boolean isField) {
         this.owner = owner;
         this.name = name;
         this.desc = desc;
         this.isField = isField;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj != null && obj.getClass() == MemoryMappingTree.GlobalMemberKey.class) {
            MemoryMappingTree.GlobalMemberKey o = (MemoryMappingTree.GlobalMemberKey)obj;
            return this.owner == o.owner && this.name.equals(o.name) && Objects.equals(this.desc, o.desc) && this.isField == o.isField;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int ret = this.owner.hashCode() * 31 + this.name.hashCode();
         if (this.desc != null) {
            ret |= this.desc.hashCode();
         }

         if (this.isField) {
            ret++;
         }

         return ret;
      }

      @Override
      public String toString() {
         return String.format("%s.%s.%s", this.owner, this.name, this.desc);
      }
   }

   abstract static class MemberEntry<T extends MemoryMappingTree.MemberEntry<T>> extends MemoryMappingTree.Entry<T> implements MappingTree.MemberMapping {
      protected MemoryMappingTree.ClassEntry owner;
      protected String srcDesc;
      private MemoryMappingTree.MemberKey key;

      protected MemberEntry(MemoryMappingTree.ClassEntry owner, String srcName, @Nullable String srcDesc) {
         super(owner.tree, srcName);
         this.owner = owner;
         this.srcDesc = srcDesc;
         this.key = new MemoryMappingTree.MemberKey(srcName, srcDesc);
      }

      protected MemberEntry(MemoryMappingTree.ClassEntry owner, MappingTree.MemberMapping src, int srcNsEquivalent) {
         super(owner.tree, src, srcNsEquivalent);
         this.owner = owner;
         this.srcDesc = src.getDesc(srcNsEquivalent);
         this.key = new MemoryMappingTree.MemberKey(this.getSrcName(), this.srcDesc);
      }

      @Override
      public MappingTree getTree() {
         return this.owner.tree;
      }

      public final MemoryMappingTree.ClassEntry getOwner() {
         return this.owner;
      }

      void setOwner(MemoryMappingTree.ClassEntry owner) {
         assert this.tree.inVisitPass;
         assert owner.getSrcName().equals(this.owner.getSrcName());
         this.owner = owner;
      }

      @Override
      void setSrcName(String name) {
         assert this.tree.inVisitPass;
         super.setSrcName(name);
         this.key = new MemoryMappingTree.MemberKey(name, this.srcDesc);
      }

      @Nullable
      @Override
      public final String getSrcDesc() {
         return this.srcDesc;
      }

      abstract void setSrcDescInternal(@Nullable String var1);

      MemoryMappingTree.MemberKey getKey() {
         this.assertSrcNamePresent();
         return this.key;
      }

      void setKey(MemoryMappingTree.MemberKey key) {
         this.key = key;
      }

      protected final boolean acceptMember(MappingVisitor visitor, boolean supplyDstDescs) throws IOException {
         String[] dstDescs;
         if (supplyDstDescs && this.srcDesc != null) {
            MappingTree tree = this.owner.tree;
            dstDescs = new String[tree.getDstNamespaces().size()];

            for (int i = 0; i < dstDescs.length; i++) {
               dstDescs[i] = tree.mapDesc(this.srcDesc, i);
            }
         } else {
            dstDescs = null;
         }

         return this.acceptElement(visitor, dstDescs);
      }
   }

   static final class MemberKey {
      private final String name;
      private final String desc;
      private final int hash;

      MemberKey(@Nullable String name, @Nullable String desc) {
         this.name = name;
         this.desc = desc;
         if (name == null) {
            this.hash = super.hashCode();
         } else if (desc == null) {
            this.hash = name.hashCode();
         } else {
            this.hash = name.hashCode() * 257 + desc.hashCode();
         }
      }

      @Override
      public boolean equals(Object obj) {
         if (obj != null && obj.getClass() == MemoryMappingTree.MemberKey.class) {
            MemoryMappingTree.MemberKey o = (MemoryMappingTree.MemberKey)obj;
            return this.name != null && o.name != null ? Objects.equals(this.name, o.name) && Objects.equals(this.desc, o.desc) : false;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.hash;
      }

      @Override
      public String toString() {
         return String.format("%s.%s", this.name, this.desc);
      }
   }

   static final class MetadataEntryImpl implements MappingTree.MetadataEntry {
      final String key;
      final String value;

      MetadataEntryImpl(String key, @Nullable String value) {
         this.key = key;
         this.value = value;
      }

      @Override
      public String getKey() {
         return this.key;
      }

      @Nullable
      @Override
      public String getValue() {
         return this.value;
      }

      @Override
      public boolean equals(Object other) {
         if (other == this) {
            return true;
         }

         if (!(other instanceof MemoryMappingTree.MetadataEntryImpl)) {
            return false;
         }

         MemoryMappingTree.MetadataEntryImpl entry = (MemoryMappingTree.MetadataEntryImpl)other;
         return this.key.equals(entry.key) && Objects.equals(this.value, entry.value);
      }

      @Override
      public int hashCode() {
         int ret = this.key.hashCode();
         if (this.value != null) {
            ret |= this.value.hashCode();
         }

         return ret;
      }

      @Override
      public String toString() {
         return this.key + ":" + this.value;
      }
   }

   static final class MethodArgEntry extends MemoryMappingTree.Entry<MemoryMappingTree.MethodArgEntry> implements MappingTree.MethodArgMapping {
      private final MemoryMappingTree.MethodEntry method;
      private int argPosition;
      private int lvIndex;

      MethodArgEntry(MemoryMappingTree.MethodEntry method, int argPosition, int lvIndex, @Nullable String srcName) {
         super(method.owner.tree, srcName);
         this.method = method;
         this.argPosition = argPosition;
         this.lvIndex = lvIndex;
      }

      MethodArgEntry(MemoryMappingTree.MethodEntry method, MappingTree.MethodArgMapping src, int srcNsEquivalent) {
         super(method.owner.tree, src, srcNsEquivalent);
         this.method = method;
         this.argPosition = src.getArgPosition();
         this.lvIndex = src.getLvIndex();
      }

      @Override
      public MappingTree getTree() {
         return this.method.owner.tree;
      }

      @Override
      public MappedElementKind getKind() {
         return MappedElementKind.METHOD_ARG;
      }

      public MemoryMappingTree.MethodEntry getMethod() {
         return this.method;
      }

      @Override
      public int getArgPosition() {
         return this.argPosition;
      }

      void setArgPositionInternal(int position) {
         this.argPosition = position;
      }

      @Override
      public int getLvIndex() {
         return this.lvIndex;
      }

      void setLvIndexInternal(int index) {
         this.lvIndex = index;
      }

      void accept(MappingVisitor visitor) throws IOException {
         if (visitor.visitMethodArg(this.argPosition, this.lvIndex, this.getSrcName())) {
            this.acceptElement(visitor, null);
         }
      }

      protected void copyFrom(MemoryMappingTree.MethodArgEntry o, boolean replace) {
         if (o.argPosition >= 0 && (replace || this.argPosition < 0)) {
            this.setArgPositionInternal(o.argPosition);
         }

         if (o.lvIndex >= 0 && (replace || this.lvIndex < 0)) {
            this.setLvIndexInternal(o.getLvIndex());
         }

         if (o.getSrcName() != null && (replace || this.getSrcName() == null)) {
            this.setSrcName(o.getSrcName());
         }

         super.copyFrom(o, replace);
      }

      @Override
      public String toString() {
         return String.format("%d/%d:%s", this.argPosition, this.lvIndex, this.getSrcName());
      }
   }

   static final class MethodEntry extends MemoryMappingTree.MemberEntry<MemoryMappingTree.MethodEntry> implements MappingTree.MethodMapping {
      private List<MemoryMappingTree.MethodArgEntry> args = null;
      private List<MemoryMappingTree.MethodVarEntry> vars = null;
      private List<MemoryMappingTree.MethodArgEntry> argsView = null;
      private List<MemoryMappingTree.MethodVarEntry> varsView = null;

      MethodEntry(MemoryMappingTree.ClassEntry owner, String srcName, @Nullable String srcDesc) {
         super(owner, srcName, srcDesc);
      }

      MethodEntry(MemoryMappingTree.ClassEntry owner, MappingTree.MethodMapping src, int srcNsEquivalent) {
         super(owner, src, srcNsEquivalent);

         for (MappingTree.MethodArgMapping arg : src.getArgs()) {
            this.addArgInternal(arg);
         }

         for (MappingTree.MethodVarMapping var : src.getVars()) {
            this.addVarInternal(var);
         }
      }

      @Override
      public MappedElementKind getKind() {
         return MappedElementKind.METHOD;
      }

      @Override
      void setSrcDescInternal(@Nullable String desc) {
         if (!Objects.equals(desc, this.srcDesc)) {
            MemoryMappingTree.MemberKey newKey = new MemoryMappingTree.MemberKey(this.getSrcName(), desc);
            if (this.owner.methods != null) {
               if (this.owner.methods.containsKey(newKey)) {
                  throw new IllegalArgumentException("conflicting name+desc after changing desc to " + desc + " for " + this);
               }

               this.owner.methods.remove(this.getKey());
            }

            this.srcDesc = desc;
            this.setKey(newKey);
            if (this.owner.methods != null) {
               this.owner.methods.put(newKey, this);
            }

            if (MemoryMappingTree.isValidDescriptor(desc, true)) {
               MemoryMappingTree.ClassEntry.access$1276(this.owner, 4);
            } else {
               MemoryMappingTree.ClassEntry.access$1276(this.owner, 8);
            }
         }
      }

      @Override
      public Collection<MemoryMappingTree.MethodArgEntry> getArgs() {
         return this.args == null ? Collections.emptyList() : this.argsView;
      }

      @Nullable
      public MemoryMappingTree.MethodArgEntry getArg(int argPosition, int lvIndex, @Nullable String srcName) {
         if (this.args == null) {
            return null;
         }

         if (argPosition >= 0 || lvIndex >= 0) {
            for (MemoryMappingTree.MethodArgEntry entry : this.args) {
               if ((argPosition >= 0 && entry.argPosition == argPosition || lvIndex >= 0 && entry.lvIndex == lvIndex)
                  && (srcName == null || entry.getSrcName() == null || srcName.equals(entry.getSrcName()))) {
                  return entry;
               }
            }
         }

         if (srcName != null) {
            for (MemoryMappingTree.MethodArgEntry entry : this.args) {
               if (srcName.equals(entry.getSrcName()) && (argPosition < 0 || entry.argPosition < 0) && (lvIndex < 0 || entry.lvIndex < 0)) {
                  return entry;
               }
            }
         }

         return null;
      }

      MemoryMappingTree.MethodArgEntry addArgInternal(MappingTree.MethodArgMapping arg) {
         MemoryMappingTree.MethodArgEntry entry = arg instanceof MemoryMappingTree.MethodArgEntry && arg.getMethod() == this
            ? (MemoryMappingTree.MethodArgEntry)arg
            : new MemoryMappingTree.MethodArgEntry(this, arg, this.owner.tree.getSrcNsEquivalent(arg));
         MemoryMappingTree.MethodArgEntry prev = this.getArg(arg.getArgPosition(), arg.getLvIndex(), arg.getSrcName());
         if (prev == null) {
            if (this.args == null) {
               this.args = new ArrayList<>();
               this.argsView = Collections.unmodifiableList(this.args);
            }

            this.args.add(entry);
         } else {
            prev.copyFrom(entry, true);
         }

         return entry;
      }

      @Override
      public Collection<MemoryMappingTree.MethodVarEntry> getVars() {
         return this.vars == null ? Collections.emptyList() : this.varsView;
      }

      @Nullable
      public MemoryMappingTree.MethodVarEntry getVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) {
         if (this.vars == null) {
            return null;
         }

         if (lvtRowIndex >= 0) {
            boolean hasMissing = false;

            for (MemoryMappingTree.MethodVarEntry entry : this.vars) {
               if (entry.lvtRowIndex == lvtRowIndex) {
                  return entry;
               }

               if (entry.lvtRowIndex < 0) {
                  hasMissing = true;
               }
            }

            if (!hasMissing) {
               return null;
            }
         }

         if (lvIndex >= 0) {
            boolean hasMissing = false;
            MemoryMappingTree.MethodVarEntry bestMatch = null;

            for (MemoryMappingTree.MethodVarEntry entry : this.vars) {
               if ((lvtRowIndex < 0 || entry.lvtRowIndex < 0 || lvtRowIndex == entry.lvtRowIndex)
                  && (srcName == null || entry.getSrcName() == null || srcName.equals(entry.getSrcName()))) {
                  if (entry.lvIndex != lvIndex) {
                     if (entry.lvIndex < 0) {
                        hasMissing = true;
                     }
                  } else if (startOpIdx >= 0 && endOpIdx >= 0 && entry.startOpIdx >= 0 && entry.endOpIdx >= 0) {
                     if (startOpIdx < entry.endOpIdx && endOpIdx > entry.startOpIdx) {
                        return entry;
                     }
                  } else if ((endOpIdx < 0 || entry.startOpIdx < 0 || endOpIdx > entry.startOpIdx)
                     && (entry.endOpIdx < 0 || startOpIdx < 0 || entry.endOpIdx > startOpIdx)) {
                     if (startOpIdx < 0 || startOpIdx == entry.startOpIdx) {
                        return entry;
                     }

                     if (bestMatch == null || entry.startOpIdx >= 0 && Math.abs(entry.startOpIdx - startOpIdx) < Math.abs(bestMatch.startOpIdx - startOpIdx)) {
                        bestMatch = entry;
                     }
                  }
               }
            }

            if (!hasMissing || bestMatch != null) {
               return bestMatch;
            }
         }

         if (srcName != null) {
            for (MemoryMappingTree.MethodVarEntry entry : this.vars) {
               if (srcName.equals(entry.getSrcName()) && (lvtRowIndex < 0 || entry.lvtRowIndex < 0) && (lvIndex < 0 || entry.lvIndex < 0)) {
                  return entry;
               }
            }
         }

         return null;
      }

      MemoryMappingTree.MethodVarEntry addVarInternal(MappingTree.MethodVarMapping var) {
         MemoryMappingTree.MethodVarEntry entry = var instanceof MemoryMappingTree.MethodVarEntry && var.getMethod() == this
            ? (MemoryMappingTree.MethodVarEntry)var
            : new MemoryMappingTree.MethodVarEntry(this, var, this.owner.tree.getSrcNsEquivalent(var));
         MemoryMappingTree.MethodVarEntry prev = this.getVar(var.getLvtRowIndex(), var.getLvIndex(), var.getStartOpIdx(), var.getEndOpIdx(), var.getSrcName());
         if (prev == null) {
            if (this.vars == null) {
               this.vars = new ArrayList<>();
               this.varsView = Collections.unmodifiableList(this.vars);
            }

            this.vars.add(entry);
         } else {
            prev.copyFrom(entry, true);
         }

         return entry;
      }

      void accept(MappingVisitor visitor, VisitOrder order, boolean supplyDstDescs) throws IOException {
         if (visitor.visitMethod(this.getSrcName(), this.srcDesc) && this.acceptMember(visitor, supplyDstDescs)) {
            boolean varsFirst = order.isMethodVarsFirst() && this.args != null && this.vars != null;
            if (!varsFirst && this.args != null) {
               for (MemoryMappingTree.MethodArgEntry arg : order.sortMethodArgs(this.args)) {
                  arg.accept(visitor);
               }
            }

            if (this.vars != null) {
               for (MemoryMappingTree.MethodVarEntry var : order.sortMethodVars(this.vars)) {
                  var.accept(visitor);
               }
            }

            if (varsFirst) {
               for (MemoryMappingTree.MethodArgEntry arg : order.sortMethodArgs(this.args)) {
                  arg.accept(visitor);
               }
            }
         }
      }

      protected void copyFrom(MemoryMappingTree.MethodEntry o, boolean replace) {
         super.copyFrom(o, replace);
         if (o.args != null) {
            for (MemoryMappingTree.MethodArgEntry oArg : o.args) {
               MemoryMappingTree.MethodArgEntry arg = this.getArg(oArg.argPosition, oArg.lvIndex, oArg.getSrcName());
               if (arg == null) {
                  this.addArgInternal(oArg);
               } else {
                  arg.copyFrom(oArg, replace);
               }
            }
         }

         if (o.vars != null) {
            for (MemoryMappingTree.MethodVarEntry oVar : o.vars) {
               MemoryMappingTree.MethodVarEntry var = this.getVar(oVar.lvtRowIndex, oVar.lvIndex, oVar.startOpIdx, oVar.endOpIdx, oVar.getSrcName());
               if (var == null) {
                  this.addVarInternal(oVar);
               } else {
                  var.copyFrom(oVar, replace);
               }
            }
         }
      }

      @Override
      public String toString() {
         return String.format("%s%s", this.getSrcNameUnchecked(), this.srcDesc);
      }
   }

   static final class MethodVarEntry extends MemoryMappingTree.Entry<MemoryMappingTree.MethodVarEntry> implements MappingTree.MethodVarMapping {
      private final MemoryMappingTree.MethodEntry method;
      private int lvtRowIndex;
      private int lvIndex;
      private int startOpIdx;
      private int endOpIdx;

      MethodVarEntry(MemoryMappingTree.MethodEntry method, int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) {
         super(method.owner.tree, srcName);
         this.method = method;
         this.lvtRowIndex = lvtRowIndex;
         this.lvIndex = lvIndex;
         this.startOpIdx = startOpIdx;
         this.endOpIdx = endOpIdx;
      }

      MethodVarEntry(MemoryMappingTree.MethodEntry method, MappingTree.MethodVarMapping src, int srcNs) {
         super(method.owner.tree, src, srcNs);
         this.method = method;
         this.lvtRowIndex = src.getLvtRowIndex();
         this.lvIndex = src.getLvIndex();
         this.startOpIdx = src.getStartOpIdx();
         this.endOpIdx = src.getEndOpIdx();
      }

      @Override
      public MappingTree getTree() {
         return this.method.owner.tree;
      }

      @Override
      public MappedElementKind getKind() {
         return MappedElementKind.METHOD_VAR;
      }

      public MemoryMappingTree.MethodEntry getMethod() {
         return this.method;
      }

      @Override
      public int getLvtRowIndex() {
         return this.lvtRowIndex;
      }

      void setLvtRowIndexInternal(int index) {
         this.lvtRowIndex = index;
      }

      @Override
      public int getLvIndex() {
         return this.lvIndex;
      }

      @Override
      public int getStartOpIdx() {
         return this.startOpIdx;
      }

      @Override
      public int getEndOpIdx() {
         return this.endOpIdx;
      }

      void setLvIndexInternal(int lvIndex, int startOpIdx, int endOpIdx) {
         this.lvIndex = lvIndex;
         this.startOpIdx = startOpIdx;
         this.endOpIdx = endOpIdx;
      }

      void accept(MappingVisitor visitor) throws IOException {
         if (visitor.visitMethodVar(this.lvtRowIndex, this.lvIndex, this.startOpIdx, this.endOpIdx, this.getSrcName())) {
            this.acceptElement(visitor, null);
         }
      }

      protected void copyFrom(MemoryMappingTree.MethodVarEntry o, boolean replace) {
         if (o.lvtRowIndex >= 0 && (replace || this.lvtRowIndex < 0)) {
            this.setLvtRowIndexInternal(o.lvtRowIndex);
         }

         if (o.lvIndex >= 0 && o.startOpIdx >= 0 && (replace || this.lvIndex < 0 || this.startOpIdx < 0)) {
            this.setLvIndexInternal(o.lvIndex, o.startOpIdx, o.endOpIdx);
         }

         if (o.getSrcName() != null && (replace || this.getSrcName() == null)) {
            this.setSrcName(o.getSrcName());
         }

         super.copyFrom(o, replace);
      }

      @Override
      public String toString() {
         return String.format("%d/%d@%d-%d:%s", this.lvtRowIndex, this.lvIndex, this.startOpIdx, this.endOpIdx, this.getSrcName());
      }
   }
}
