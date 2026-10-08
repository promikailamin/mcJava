package net.fabricmc.loader.impl.lib.mappingio.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.impl.lib.mappingio.MappedElementKind;
import net.fabricmc.loader.impl.lib.mappingio.MappingFlag;
import net.fabricmc.loader.impl.lib.mappingio.MappingUtil;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import org.jetbrains.annotations.Nullable;

public final class MappingSourceNsSwitch extends ForwardingMappingVisitor {
   private final String newSourceNsName;
   private final boolean dropMissingNewSrcName;
   private int newSourceNs;
   private String oldSourceNsName;
   private final Map<String, String> classMap = new HashMap<>();
   private boolean classMapReady;
   private boolean passThrough;
   private boolean relayHeaderOrMetadata;
   private String srcName;
   private String srcDesc;
   private int argIdx;
   private int lvIndex;
   private int startOpIdx;
   private int endOpIdx;
   private String[] dstNames;
   private String[] dstDescs;

   public MappingSourceNsSwitch(MappingVisitor next, String newSourceNs, boolean dropMissingNewSrcName) {
      super(next);
      this.newSourceNsName = newSourceNs;
      this.dropMissingNewSrcName = dropMissingNewSrcName;
   }

   @Override
   public Set<MappingFlag> getFlags() {
      if (this.passThrough) {
         return this.next.getFlags();
      }

      Set<MappingFlag> ret = EnumSet.noneOf(MappingFlag.class);
      ret.addAll(this.next.getFlags());
      ret.add(MappingFlag.NEEDS_MULTIPLE_PASSES);
      ret.add(MappingFlag.NEEDS_ELEMENT_UNIQUENESS);
      return ret;
   }

   @Override
   public void reset() {
      this.classMapReady = false;
      this.passThrough = false;
      this.classMap.clear();
      this.next.reset();
   }

   @Override
   public boolean visitHeader() throws IOException {
      return !this.classMapReady ? true : this.next.visitHeader();
   }

   @Override
   public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
      if (!this.classMapReady) {
         if (srcNamespace.equals(this.newSourceNsName)) {
            this.classMapReady = true;
            this.passThrough = true;
            this.relayHeaderOrMetadata = this.next.visitHeader();
            if (this.relayHeaderOrMetadata) {
               this.next.visitNamespaces(srcNamespace, dstNamespaces);
            }
         } else {
            this.newSourceNs = dstNamespaces.indexOf(this.newSourceNsName);
            if (this.newSourceNs < 0) {
               throw new RuntimeException("invalid new source ns " + this.newSourceNsName + ": not in " + dstNamespaces + " or " + srcNamespace);
            }

            this.oldSourceNsName = srcNamespace;
            int count = dstNamespaces.size();
            this.dstNames = new String[count];
         }
      } else {
         this.relayHeaderOrMetadata = true;
         List<String> newDstNamespaces = new ArrayList<>(dstNamespaces);
         newDstNamespaces.set(this.newSourceNs, this.oldSourceNsName);
         this.next.visitNamespaces(this.newSourceNsName, newDstNamespaces);
         Set<MappingFlag> flags = this.next.getFlags();
         if (!flags.contains(MappingFlag.NEEDS_DST_FIELD_DESC) && !flags.contains(MappingFlag.NEEDS_DST_METHOD_DESC)) {
            this.dstDescs = null;
         } else {
            this.dstDescs = new String[dstNamespaces.size()];
         }
      }
   }

   @Override
   public void visitMetadata(String key, @Nullable String value) throws IOException {
      if (this.classMapReady && this.relayHeaderOrMetadata) {
         this.next.visitMetadata(key, value);
      }
   }

   @Override
   public boolean visitContent() throws IOException {
      if (!this.classMapReady) {
         return true;
      }

      this.relayHeaderOrMetadata = true;
      return this.next.visitContent();
   }

   @Override
   public boolean visitClass(String srcName) throws IOException {
      if (this.passThrough) {
         return this.next.visitClass(srcName);
      }

      this.srcName = srcName;
      return true;
   }

   @Override
   public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
      assert this.classMapReady;
      if (this.passThrough) {
         return this.next.visitField(srcName, srcDesc);
      }

      this.srcName = srcName;
      this.srcDesc = srcDesc;
      return true;
   }

   @Override
   public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
      assert this.classMapReady;
      if (this.passThrough) {
         return this.next.visitMethod(srcName, srcDesc);
      }

      this.srcName = srcName;
      this.srcDesc = srcDesc;
      return true;
   }

   @Override
   public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
      assert this.classMapReady;
      if (this.passThrough) {
         return this.next.visitMethodArg(argPosition, lvIndex, srcName);
      }

      this.srcName = srcName;
      this.argIdx = argPosition;
      this.lvIndex = lvIndex;
      return true;
   }

   @Override
   public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
      assert this.classMapReady;
      if (this.passThrough) {
         return this.next.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
      }

      this.srcName = srcName;
      this.argIdx = lvtRowIndex;
      this.lvIndex = lvIndex;
      this.startOpIdx = startOpIdx;
      this.endOpIdx = endOpIdx;
      return true;
   }

   @Override
   public boolean visitEnd() throws IOException {
      if (!this.classMapReady) {
         this.classMapReady = true;
         return false;
      } else {
         return this.next.visitEnd();
      }
   }

   @Override
   public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
      if (!this.classMapReady) {
         if (namespace == this.newSourceNs) {
            this.classMap.put(this.srcName, name);
         }
      } else if (this.passThrough) {
         this.next.visitDstName(targetKind, namespace, name);
      } else {
         if (namespace >= this.dstNames.length) {
            throw new IllegalArgumentException("out of bounds namespace");
         }

         this.dstNames[namespace] = name;
      }
   }

   @Override
   public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
      if (this.passThrough) {
         this.next.visitDstDesc(targetKind, namespace, desc);
      } else if (this.classMapReady && this.dstDescs != null) {
         this.dstDescs[namespace] = desc;
      }
   }

   @Override
   public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
      if (!this.classMapReady) {
         return false;
      }

      if (this.passThrough) {
         return this.next.visitElementContent(targetKind);
      }

      String dstName = this.dstNames[this.newSourceNs];
      if (dstName == null && targetKind != MappedElementKind.METHOD_ARG && targetKind != MappedElementKind.METHOD_VAR) {
         if (this.dropMissingNewSrcName && !this.srcName.startsWith("<")) {
            Arrays.fill(this.dstNames, null);
            if (this.dstDescs != null) {
               Arrays.fill(this.dstDescs, null);
            }

            return false;
         }

         dstName = this.srcName;
      }

      boolean relay;
      switch (targetKind) {
         case CLASS:
            relay = this.next.visitClass(dstName);
            break;
         case FIELD:
            relay = this.next.visitField(dstName, this.srcDesc != null ? MappingUtil.mapDesc(this.srcDesc, this.classMap) : null);
            break;
         case METHOD:
            relay = this.next.visitMethod(dstName, this.srcDesc != null ? MappingUtil.mapDesc(this.srcDesc, this.classMap) : null);
            break;
         case METHOD_ARG:
            relay = this.next.visitMethodArg(this.argIdx, this.lvIndex, dstName);
            break;
         case METHOD_VAR:
            relay = this.next.visitMethodVar(this.argIdx, this.lvIndex, this.startOpIdx, this.endOpIdx, dstName);
            break;
         default:
            throw new IllegalStateException();
      }

      if (relay) {
         boolean sendDesc = this.dstDescs != null && this.srcDesc != null && (targetKind == MappedElementKind.FIELD || targetKind == MappedElementKind.METHOD);

         for (int i = 0; i < this.dstNames.length; i++) {
            if (i == this.newSourceNs) {
               this.next.visitDstName(targetKind, i, this.srcName);
               if (sendDesc) {
                  this.next.visitDstDesc(targetKind, i, this.srcDesc);
               }
            } else {
               String name = this.dstNames[i];
               if (name != null) {
                  this.next.visitDstName(targetKind, i, name);
               }

               if (sendDesc) {
                  String desc = this.dstDescs[i];
                  if (desc != null) {
                     this.next.visitDstDesc(targetKind, i, desc);
                  }
               }
            }
         }

         relay = this.next.visitElementContent(targetKind);
      }

      Arrays.fill(this.dstNames, null);
      if (this.dstDescs != null) {
         Arrays.fill(this.dstDescs, null);
      }

      return relay;
   }
}
