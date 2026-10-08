package net.fabricmc.loader.impl.lib.tinyremapper;

import java.nio.file.Path;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Predicate;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrEnvironment;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrField;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;

public final class ClassInstance implements TrClass {
   private static final MemberInstance nullMember = new MemberInstance(null, null, null, null, 0, 0);
   private static final AtomicReferenceFieldUpdater<ClassInstance, InputTag[]> inputTagsUpdater = AtomicReferenceFieldUpdater.newUpdater(
      ClassInstance.class, InputTag[].class, "inputTags"
   );
   final TinyRemapper tr;
   private TinyRemapper.MrjState context;
   final boolean isInput;
   private volatile InputTag[] inputTags;
   final Path srcPath;
   byte[] data;
   private ClassInstance mrjOrigin;
   private final Map<String, MemberInstance> members = new LinkedHashMap<>();
   private final ConcurrentMap<String, MemberInstance> resolvedMembers = new ConcurrentHashMap<>();
   final Set<ClassInstance> parents = new HashSet<>();
   final Set<ClassInstance> children = new HashSet<>();
   private String name;
   private int classVersion;
   private int mrjVersion;
   private String superName;
   private String signature;
   private int access;
   private String[] interfaces;

   ClassInstance(TinyRemapper tr, boolean isInput, InputTag[] inputTags, Path srcFile, byte[] data) {
      assert !isInput || data != null;
      this.tr = tr;
      this.isInput = isInput;
      this.inputTags = inputTags;
      this.srcPath = srcFile;
      this.data = data;
      this.mrjOrigin = this;
   }

   void init(String name, int classVersion, int mrjVersion, String signature, String superName, int access, String[] interfaces) {
      this.name = name;
      this.classVersion = classVersion;
      this.mrjVersion = mrjVersion;
      this.superName = superName;
      this.signature = signature;
      this.access = access;
      this.interfaces = interfaces;
   }

   void setContext(TinyRemapper.MrjState context) {
      this.context = context;
   }

   TinyRemapper.MrjState getContext() {
      return this.context;
   }

   MemberInstance addMember(MemberInstance member) {
      return this.members.put(member.getId(), member);
   }

   void addInputTags(InputTag[] tags) {
      if (tags != null && tags.length != 0) {
         InputTag[] oldTags;
         InputTag[] newTags;
         do {
            oldTags = this.inputTags;
            if (oldTags == null) {
               newTags = tags;
            } else {
               int missingTags = 0;

               for (InputTag newTag : tags) {
                  boolean found = false;

                  for (InputTag oldTag : oldTags) {
                     if (newTag == oldTag) {
                        found = true;
                        break;
                     }
                  }

                  if (!found) {
                     missingTags++;
                  }
               }

               if (missingTags == 0) {
                  return;
               }

               newTags = Arrays.copyOf(oldTags, oldTags.length + missingTags);

               for (InputTag newTag : tags) {
                  boolean found = false;

                  for (InputTag oldTag : oldTags) {
                     if (newTag == oldTag) {
                        found = true;
                        break;
                     }
                  }

                  if (!found) {
                     newTags[newTags.length - missingTags] = newTag;
                     missingTags--;
                  }
               }

               assert missingTags == 0;
            }
         } while (!inputTagsUpdater.compareAndSet(this, oldTags, newTags));
      }
   }

   InputTag[] getInputTags() {
      return this.inputTags;
   }

   boolean hasAnyInputTag(InputTag[] reqTags) {
      InputTag[] availTags = this.inputTags;
      if (availTags == null) {
         return true;
      }

      for (InputTag reqTag : reqTags) {
         for (InputTag availTag : availTags) {
            if (availTag == reqTag) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public TrEnvironment getEnvironment() {
      return this.context;
   }

   @Override
   public int getAccess() {
      return this.access;
   }

   @Override
   public String getName() {
      return this.name;
   }

   public int getClassVersion() {
      return this.classVersion;
   }

   public int getMrjVersion() {
      return this.mrjVersion;
   }

   public String getSuperName() {
      return this.superName;
   }

   public ClassInstance getSuperClass() {
      for (ClassInstance cls : this.parents) {
         if (!cls.isInterface()) {
            return cls;
         }
      }

      return null;
   }

   String[] getInterfaceNames0() {
      return this.interfaces;
   }

   public boolean isPublicOrPrivate() {
      return (this.access & 3) != 0;
   }

   public boolean isMrjCopy() {
      return this.mrjOrigin != this;
   }

   public ClassInstance getMrjOrigin() {
      return this.mrjOrigin;
   }

   void propagate(
      TrMember.MemberType type,
      String originatingCls,
      String idSrc,
      String nameDst,
      TinyRemapper.Direction dir,
      boolean isVirtual,
      boolean fromBridge,
      boolean first,
      Set<ClassInstance> visitedUp,
      Set<ClassInstance> visitedDown
   ) {
      MemberInstance member = this.getMember(type, idSrc);
      if (member != null) {
         if (!first && !isVirtual) {
            return;
         }

         if (first
            || (member.access & 10) == 0
            || this.tr.propagatePrivate
            || !this.tr.forcePropagation.isEmpty() && this.tr.forcePropagation.contains(this.name.replace('/', '.') + "." + member.name)) {
            if (!member.setNewName(nameDst, fromBridge)) {
               this.tr.conflicts.computeIfAbsent(member, x -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(originatingCls + "/" + nameDst);
            } else {
               member.newNameOriginatingCls = originatingCls;
            }
         }

         if (first && ((member.access & 2) != 0 || type == TrMember.MemberType.METHOD && this.isInterface() && !isVirtual)) {
            return;
         }

         if (this.tr.propagateBridges != TinyRemapper.LinkedMethodPropagation.DISABLED && member.cls.isInput && isVirtual && (member.access & 64) != 0) {
            assert member.type == TrMember.MemberType.METHOD;
            MemberInstance bridgeTarget = BridgeHandler.getTarget(member);
            if (bridgeTarget != null) {
               Set<ClassInstance> visitedUpBridge = Collections.newSetFromMap(new IdentityHashMap<>());
               Set<ClassInstance> visitedDownBridge = Collections.newSetFromMap(new IdentityHashMap<>());
               visitedUpBridge.add(member.cls);
               visitedDownBridge.add(member.cls);
               this.propagate(
                  TrMember.MemberType.METHOD,
                  originatingCls,
                  bridgeTarget.getId(),
                  nameDst,
                  TinyRemapper.Direction.DOWN,
                  true,
                  this.tr.propagateBridges == TinyRemapper.LinkedMethodPropagation.COMPATIBLE,
                  false,
                  visitedUpBridge,
                  visitedDownBridge
               );
            }
         }
      } else {
         assert !first && (type == TrMember.MemberType.FIELD || !this.isInterface() || isVirtual);
      }

      assert isVirtual || dir == TinyRemapper.Direction.DOWN;
      if (dir == TinyRemapper.Direction.ANY || dir == TinyRemapper.Direction.UP || isVirtual && member != null && (member.access & 10) == 0) {
         for (ClassInstance node : this.parents) {
            if (visitedUp.add(node)) {
               node.propagate(type, originatingCls, idSrc, nameDst, TinyRemapper.Direction.UP, isVirtual, fromBridge, false, visitedUp, visitedDown);
            }
         }
      }

      if (dir == TinyRemapper.Direction.ANY || dir == TinyRemapper.Direction.DOWN || isVirtual && member != null && (member.access & 10) == 0) {
         for (ClassInstance node : this.children) {
            if (visitedDown.add(node)) {
               node.propagate(type, originatingCls, idSrc, nameDst, TinyRemapper.Direction.DOWN, isVirtual, fromBridge, false, visitedUp, visitedDown);
            }
         }
      }
   }

   @Override
   public boolean isAssignableFrom(TrClass cls) {
      return cls instanceof ClassInstance && this.isAssignableFrom((ClassInstance)cls);
   }

   public boolean isAssignableFrom(ClassInstance cls) {
      if (cls == this) {
         return true;
      }

      if (this.isInterface()) {
         Set<ClassInstance> visited = Collections.newSetFromMap(new IdentityHashMap<>());
         Deque<ClassInstance> queue = new ArrayDeque<>();
         visited.add(cls);

         do {
            for (ClassInstance parent : cls.parents) {
               if (parent == this) {
                  return true;
               }

               if (visited.add(parent)) {
                  queue.addLast(parent);
               }
            }
         } while ((cls = queue.pollFirst()) != null);
      } else {
         do {
            ClassInstance superCls = null;

            for (ClassInstance c : cls.parents) {
               if (!c.isInterface()) {
                  if (c == this) {
                     return true;
                  }

                  superCls = c;
                  break;
               }
            }

            cls = superCls;
         } while (cls != null);
      }

      return false;
   }

   static boolean isAssignableFrom(String superDesc, int superDescStart, String subDesc, int subDescStart, TinyRemapper.MrjState context) {
      char superType = superDesc.charAt(superDescStart);
      char subType = subDesc.charAt(subDescStart);
      if (superType == '[') {
         while (subType == '[') {
            superType = superDesc.charAt(++superDescStart);
            subType = subDesc.charAt(++subDescStart);
            if (superType != '[') {
               return superType == subType
                  && (superType != 'L' || superDesc.regionMatches(superDescStart + 1, subDesc, subDescStart + 1, superDesc.indexOf(59, superDescStart + 1) + 1));
            }
         }

         return false;
      } else {
         if (superType != 'L') {
            return superType == subType;
         }

         if (subType != 'L' && subType != '[') {
            return false;
         }

         superDescStart++;
         subDescStart++;
         if (superDesc.startsWith("java/lang/Object;", superDescStart)) {
            return true;
         }

         if (subType != 'L') {
            return false;
         }

         int superDescEnd = superDesc.indexOf(59, superDescStart);
         int subDescEnd = subDesc.indexOf(59, subDescStart);
         int superDescLen = superDescEnd - superDescStart;
         if (superDescLen == subDescEnd - subDescStart && superDesc.regionMatches(superDescStart, subDesc, subDescStart, superDescLen)) {
            return true;
         }

         String superName = superDesc.substring(superDescStart, superDescEnd);
         String subName = subDesc.substring(subDescStart, subDescEnd);
         ClassInstance superCls = context.getClass(superName);
         if (superCls != null && superCls.children.isEmpty()) {
            return false;
         }

         ClassInstance subCls = context.getClass(subName);
         if (subCls != null) {
            if (superCls != null && !superCls.isInterface()) {
               do {
                  String curSuperName = subCls.superName;
                  if (curSuperName.equals(superName)) {
                     return true;
                  }

                  if (curSuperName.equals("java/lang/Object")) {
                     return false;
                  }

                  subCls = context.getClass(curSuperName);
               } while (subCls != null);
            } else {
               Set<ClassInstance> visited = Collections.newSetFromMap(new IdentityHashMap<>());
               Deque<ClassInstance> queue = new ArrayDeque<>();
               visited.add(subCls);

               do {
                  for (ClassInstance parent : subCls.parents) {
                     if (parent.name.equals(superName)) {
                        return true;
                     }

                     if (visited.add(parent)) {
                        queue.addLast(parent);
                     }
                  }
               } while ((subCls = queue.pollFirst()) != null);
            }
         } else if (superCls != null) {
            Set<ClassInstance> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            Deque<ClassInstance> queue = new ArrayDeque<>();
            visited.add(superCls);

            do {
               for (ClassInstance child : superCls.children) {
                  if (child.name.equals(subName)) {
                     return true;
                  }

                  if (visited.add(child)) {
                     queue.addLast(child);
                  }
               }
            } while ((superCls = queue.pollFirst()) != null);
         }

         return false;
      }
   }

   public MemberInstance getMethod(String name, String desc) {
      return this.members.get(MemberInstance.getMethodId(name, desc));
   }

   public MemberInstance getMember(TrMember.MemberType type, String id) {
      return this.members.get(id);
   }

   @Override
   public Collection<? extends TrMethod> getMethods() {
      List<TrMethod> ret = new ArrayList<>(this.members.size());

      for (MemberInstance m : this.members.values()) {
         if (m.isMethod()) {
            ret.add(m);
         }
      }

      return ret;
   }

   public Collection<MemberInstance> getMembers() {
      return this.members.values();
   }

   @Override
   public Collection<TrField> getFields(String name, String desc, boolean isDescPrefix, Predicate<TrField> filter, Collection<TrField> out) {
      if (out == null) {
         out = new ArrayList<>(this.members.size());
      }

      for (MemberInstance m : this.members.values()) {
         if (m.isField() && matches(m, name, desc, isDescPrefix, filter)) {
            out.add(m);
         }
      }

      return out;
   }

   @Override
   public Collection<TrMethod> getMethods(String name, String desc, boolean isDescPrefix, Predicate<TrMethod> filter, Collection<TrMethod> out) {
      if (out == null) {
         out = new ArrayList<>(this.members.size());
      }

      for (MemberInstance m : this.members.values()) {
         if (m.isMethod() && matches(m, name, desc, isDescPrefix, filter)) {
            out.add(m);
         }
      }

      return out;
   }

   public MemberInstance resolve(TrMember.MemberType type, String id) {
      MemberInstance member = this.getMember(type, id);
      if (member != null) {
         return member;
      }

      member = this.resolvedMembers.get(id);
      if (member == null) {
         member = type == TrMember.MemberType.FIELD ? this.resolveField(id) : this.resolveMethod(id);
         assert member != null;
         MemberInstance prev = this.resolvedMembers.putIfAbsent(id, member);
         if (prev != null) {
            member = prev;
         }
      }

      return member != nullMember ? member : null;
   }

   private MemberInstance resolveField(String id) {
      Deque<ClassInstance> queue = new ArrayDeque<>();
      Set<ClassInstance> visited = Collections.newSetFromMap(new IdentityHashMap<>());
      visited.add(this);
      ClassInstance context = this;

      MemberInstance parentMember;
      do {
         ClassInstance cls = context;

         do {
            for (ClassInstance parent : cls.parents) {
               if (parent.isInterface() && visited.add(parent)) {
                  MemberInstance ret = parent.getMember(TrMember.MemberType.FIELD, id);
                  if (ret != null) {
                     return ret;
                  }

                  queue.addLast(parent);
               }
            }
         } while ((cls = queue.pollLast()) != null);

         cls = context;
         context = cls.getSuperClass();
         if (context == null) {
            return nullMember;
         }

         parentMember = context.getMember(TrMember.MemberType.FIELD, id);
      } while (parentMember == null);

      return parentMember;
   }

   @Override
   public Collection<TrField> resolveFields(String name, String desc, boolean isDescPrefix, Predicate<TrField> filter, Collection<TrField> out) {
      if (name == null || (desc == null || isDescPrefix) && !this.tr.ignoreFieldDesc) {
         if (out == null) {
            out = new ArrayList<>();
         }

         for (MemberInstance member : this.getMembers()) {
            if (member.isField()) {
               addMatching(member, name, desc, isDescPrefix, filter, out);
            }
         }

         Deque<ClassInstance> queue = new ArrayDeque<>();
         Set<ClassInstance> visited = Collections.newSetFromMap(new IdentityHashMap<>());
         visited.add(this);
         ClassInstance context = this;

         while (true) {
            ClassInstance cls = context;

            do {
               for (ClassInstance parent : cls.parents) {
                  if (parent.isInterface() && visited.add(parent)) {
                     for (MemberInstance member : parent.getMembers()) {
                        if (member.isField()) {
                           addMatching(member, name, desc, isDescPrefix, filter, out);
                        }
                     }

                     queue.addLast(parent);
                  }
               }
            } while ((cls = queue.pollLast()) != null);

            cls = context;
            context = cls.getSuperClass();
            if (context == null) {
               return out;
            }

            for (MemberInstance member : context.getMembers()) {
               if (member.isField()) {
                  addMatching(member, name, desc, isDescPrefix, filter, out);
               }
            }
         }
      } else {
         MemberInstance ret = this.resolve(TrMember.MemberType.FIELD, MemberInstance.getFieldId(name, desc, this.tr.ignoreFieldDesc));
         if (ret != null && filter != null && !filter.test(ret)) {
            ret = null;
         }

         if (out != null) {
            if (ret != null) {
               out.add(ret);
            }

            return out;
         } else {
            return ret != null && filter == null ? Collections.singletonList(ret) : Collections.emptyList();
         }
      }
   }

   private MemberInstance resolveMethod(String id) {
      ClassInstance cls = this;

      while ((cls = cls.getSuperClass()) != null) {
         MemberInstance ret = cls.getMember(TrMember.MemberType.METHOD, id);
         if (ret != null) {
            return ret;
         }
      }

      Deque<ClassInstance> queue = new ArrayDeque<>();
      Set<ClassInstance> visited = Collections.newSetFromMap(new IdentityHashMap<>());
      visited.add(this);
      List<MemberInstance> matchedMethods = new ArrayList<>();
      boolean hasNonAbstract = false;
      cls = this;

      do {
         for (ClassInstance parent : cls.parents) {
            if (visited.add(parent)) {
               if (parent.isInterface()) {
                  MemberInstance parentMember = parent.getMember(TrMember.MemberType.METHOD, id);
                  if (parentMember != null && parentMember.isVirtual()) {
                     if (!parentMember.isAbstract()) {
                        hasNonAbstract = true;
                     }

                     matchedMethods.add(parentMember);
                     continue;
                  }
               }

               queue.addLast(parent);
            }
         }
      } while ((cls = queue.pollFirst()) != null);

      if (hasNonAbstract && matchedMethods.size() > 1) {
         label58:
         for (MemberInstance member : matchedMethods) {
            if (!member.isAbstract()) {
               for (MemberInstance m : matchedMethods) {
                  if (m != member && member.cls.isAssignableFrom(m.cls)) {
                     continue label58;
                  }
               }

               return member;
            }
         }
      }

      return !matchedMethods.isEmpty() ? matchedMethods.get(0) : nullMember;
   }

   @Override
   public Collection<TrMethod> resolveMethods(String name, String desc, boolean isDescPrefix, Predicate<TrMethod> filter, Collection<TrMethod> out) {
      if (name != null && desc != null && !isDescPrefix) {
         MemberInstance ret = this.resolve(TrMember.MemberType.METHOD, MemberInstance.getMethodId(name, desc));
         if (ret != null && filter != null && !filter.test(ret)) {
            ret = null;
         }

         if (out != null) {
            if (ret != null) {
               out.add(ret);
            }

            return out;
         } else {
            return ret != null && filter == null ? Collections.singletonList(ret) : Collections.emptyList();
         }
      } else {
         if (out == null) {
            out = new ArrayList<>();
         }

         for (MemberInstance member : this.getMembers()) {
            if (member.isMethod()) {
               addMatching(member, name, desc, isDescPrefix, filter, out);
            }
         }

         ClassInstance cls = this;

         while ((cls = cls.getSuperClass()) != null) {
            for (MemberInstance member : cls.getMembers()) {
               if (member.isMethod()) {
                  addMatching(member, name, desc, isDescPrefix, filter, out);
               }
            }
         }

         Deque<ClassInstance> queue = new ArrayDeque<>();
         Set<ClassInstance> visited = Collections.newSetFromMap(new IdentityHashMap<>());
         visited.add(this);
         Map<String, List<TrMethod>> matchedMethodsMap = new HashMap<>();
         boolean hasNonAbstract = false;
         cls = this;

         do {
            for (ClassInstance parent : cls.parents) {
               if (visited.add(parent)) {
                  if (parent.isInterface()) {
                     for (MemberInstance member : parent.getMembers()) {
                        if (member.isMethod()
                           && matches(member, name, desc, isDescPrefix, filter)
                           && addUnique(member, matchedMethodsMap.computeIfAbsent(member.getId(), ignore -> new ArrayList<>()))
                           && !member.isAbstract()) {
                           hasNonAbstract = true;
                        }
                     }
                  }

                  queue.addLast(parent);
               }
            }
         } while ((cls = queue.pollFirst()) != null);

         label124:
         for (List<TrMethod> matchedMethods : matchedMethodsMap.values()) {
            if (!matchedMethods.isEmpty()) {
               if (hasNonAbstract && matchedMethods.size() > 1) {
                  label116:
                  for (TrMethod member : matchedMethods) {
                     if (!member.isAbstract()) {
                        for (TrMember m : matchedMethods) {
                           if (m != member && member.getOwner().isAssignableFrom(m.getOwner())) {
                              continue label116;
                           }
                        }

                        addUnique(member, out);
                        continue label124;
                     }
                  }
               }

               addUnique(matchedMethods.get(0), out);
            }
         }

         return out;
      }
   }

   private static <T extends TrMember> boolean matches(T member, String name, String desc, boolean isDescPrefix, Predicate<T> filter) {
      return (name == null || name.equals(member.getName()))
         && (desc == null || !isDescPrefix && member.getDesc().equals(desc) || isDescPrefix && member.getDesc().startsWith(desc))
         && (filter == null || filter.test(member));
   }

   private static <T extends TrMember> boolean addUnique(T member, Collection<T> out) {
      for (T m : out) {
         if (m.getName().equals(member.getName()) && m.getDesc().equals(member.getDesc())) {
            return false;
         }
      }

      out.add(member);
      return true;
   }

   private static <T extends TrMember> void addMatching(T member, String name, String desc, boolean isDescPrefix, Predicate<T> filter, Collection<T> out) {
      if (matches(member, name, desc, isDescPrefix, filter)) {
         addUnique(member, out);
      }
   }

   ClassInstance constructMrjCopy(TinyRemapper.MrjState newContext) {
      ClassInstance copy = new ClassInstance(this.tr, false, this.inputTags, this.srcPath, this.data);
      copy.init(this.name, this.classVersion, this.mrjVersion, this.signature, this.superName, this.access, this.interfaces);
      copy.setContext(newContext);

      for (MemberInstance member : this.members.values()) {
         copy.addMember(new MemberInstance(member.type, copy, member.name, member.desc, member.access, member.index));
      }

      copy.mrjOrigin = this.mrjOrigin;
      return copy;
   }

   @Override
   public boolean isInput() {
      return this.isInput;
   }

   @Override
   public String toString() {
      return this.name;
   }

   public static String getMrjName(String clsName, int mrjVersion) {
      return mrjVersion != -1 ? "/META-INF/versions/" + mrjVersion + "/" + clsName : clsName;
   }
}
