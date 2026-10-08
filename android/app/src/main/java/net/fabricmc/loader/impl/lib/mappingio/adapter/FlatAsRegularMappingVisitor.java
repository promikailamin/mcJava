package net.fabricmc.loader.impl.lib.mappingio.adapter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.impl.lib.mappingio.FlatMappingVisitor;
import net.fabricmc.loader.impl.lib.mappingio.MappedElementKind;
import net.fabricmc.loader.impl.lib.mappingio.MappingFlag;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import org.jetbrains.annotations.Nullable;

public final class FlatAsRegularMappingVisitor implements MappingVisitor {
   private final FlatMappingVisitor next;
   private String srcClsName;
   private String srcMemberName;
   private String srcMemberDesc;
   private String srcMemberSubName;
   private int argIdx;
   private int lvIndex;
   private int startOpIdx;
   private int endOpIdx;
   private String[] dstNames;
   private String[] dstClassNames;
   private String[] dstMemberNames;
   private String[] dstMemberDescs;

   public FlatAsRegularMappingVisitor(FlatMappingVisitor out) {
      this.next = out;
   }

   @Override
   public Set<MappingFlag> getFlags() {
      return this.next.getFlags();
   }

   @Override
   public void reset() {
      this.next.reset();
   }

   @Override
   public boolean visitHeader() throws IOException {
      return this.next.visitHeader();
   }

   @Override
   public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
      this.next.visitNamespaces(srcNamespace, dstNamespaces);
      int count = dstNamespaces.size();
      this.dstNames = new String[count];
      Set<MappingFlag> flags = this.next.getFlags();
      if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS)) {
         this.dstClassNames = new String[count];
         this.dstMemberNames = new String[count];
      } else {
         this.dstClassNames = this.dstMemberNames = null;
      }

      this.dstMemberDescs = !flags.contains(MappingFlag.NEEDS_DST_FIELD_DESC) && !flags.contains(MappingFlag.NEEDS_DST_METHOD_DESC) ? null : new String[count];
   }

   @Override
   public void visitMetadata(String key, @Nullable String value) throws IOException {
      this.next.visitMetadata(key, value);
   }

   @Override
   public boolean visitContent() throws IOException {
      return this.next.visitContent();
   }

   @Override
   public boolean visitClass(String srcName) {
      this.srcClsName = srcName;
      Arrays.fill(this.dstNames, null);
      if (this.dstClassNames != null) {
         Arrays.fill(this.dstClassNames, null);
      }

      return true;
   }

   @Override
   public boolean visitField(String srcName, @Nullable String srcDesc) {
      this.srcMemberName = srcName;
      this.srcMemberDesc = srcDesc;
      Arrays.fill(this.dstNames, null);
      if (this.dstMemberNames != null) {
         Arrays.fill(this.dstMemberNames, null);
      }

      if (this.dstMemberDescs != null) {
         Arrays.fill(this.dstMemberDescs, null);
      }

      return true;
   }

   @Override
   public boolean visitMethod(String srcName, @Nullable String srcDesc) {
      this.srcMemberName = srcName;
      this.srcMemberDesc = srcDesc;
      Arrays.fill(this.dstNames, null);
      if (this.dstMemberNames != null) {
         Arrays.fill(this.dstMemberNames, null);
      }

      if (this.dstMemberDescs != null) {
         Arrays.fill(this.dstMemberDescs, null);
      }

      return true;
   }

   @Override
   public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) {
      this.srcMemberSubName = srcName;
      this.argIdx = argPosition;
      this.lvIndex = lvIndex;
      Arrays.fill(this.dstNames, null);
      return true;
   }

   @Override
   public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) {
      this.srcMemberSubName = srcName;
      this.argIdx = lvtRowIndex;
      this.lvIndex = lvIndex;
      this.startOpIdx = startOpIdx;
      this.endOpIdx = endOpIdx;
      Arrays.fill(this.dstNames, null);
      return true;
   }

   @Override
   public boolean visitEnd() throws IOException {
      return this.next.visitEnd();
   }

   @Override
   public void visitDstName(MappedElementKind targetKind, int namespace, String name) {
      this.dstNames[namespace] = name;
   }

   @Override
   public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) {
      if (this.dstMemberDescs != null) {
         this.dstMemberDescs[namespace] = desc;
      }
   }

   @Override
   public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
      boolean relay;
      switch (targetKind) {
         case CLASS:
            relay = this.next.visitClass(this.srcClsName, this.dstNames);
            if (relay && this.dstClassNames != null) {
               System.arraycopy(this.dstNames, 0, this.dstClassNames, 0, this.dstNames.length);
            }
            break;
         case FIELD:
            relay = this.next.visitField(this.srcClsName, this.srcMemberName, this.srcMemberDesc, this.dstClassNames, this.dstNames, this.dstMemberDescs);
            if (relay && this.dstMemberNames != null) {
               System.arraycopy(this.dstNames, 0, this.dstMemberNames, 0, this.dstNames.length);
            }
            break;
         case METHOD:
            relay = this.next.visitMethod(this.srcClsName, this.srcMemberName, this.srcMemberDesc, this.dstClassNames, this.dstNames, this.dstMemberDescs);
            if (relay && this.dstMemberNames != null) {
               System.arraycopy(this.dstNames, 0, this.dstMemberNames, 0, this.dstNames.length);
            }
            break;
         case METHOD_ARG:
            relay = this.next
               .visitMethodArg(
                  this.srcClsName,
                  this.srcMemberName,
                  this.srcMemberDesc,
                  this.argIdx,
                  this.lvIndex,
                  this.srcMemberSubName,
                  this.dstClassNames,
                  this.dstMemberNames,
                  this.dstMemberDescs,
                  this.dstNames
               );
            break;
         case METHOD_VAR:
            relay = this.next
               .visitMethodVar(
                  this.srcClsName,
                  this.srcMemberName,
                  this.srcMemberDesc,
                  this.argIdx,
                  this.lvIndex,
                  this.startOpIdx,
                  this.endOpIdx,
                  this.srcMemberSubName,
                  this.dstClassNames,
                  this.dstMemberNames,
                  this.dstMemberDescs,
                  this.dstNames
               );
            break;
         default:
            throw new IllegalStateException();
      }

      return relay;
   }

   @Override
   public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
      switch (targetKind) {
         case CLASS:
            this.next.visitClassComment(this.srcClsName, this.dstClassNames, comment);
            break;
         case FIELD:
            this.next
               .visitFieldComment(
                  this.srcClsName, this.srcMemberName, this.srcMemberDesc, this.dstClassNames, this.dstMemberNames, this.dstMemberDescs, comment
               );
            break;
         case METHOD:
            this.next
               .visitMethodComment(
                  this.srcClsName, this.srcMemberName, this.srcMemberDesc, this.dstClassNames, this.dstMemberNames, this.dstMemberDescs, comment
               );
            break;
         case METHOD_ARG:
            this.next
               .visitMethodArgComment(
                  this.srcClsName,
                  this.srcMemberName,
                  this.srcMemberDesc,
                  this.argIdx,
                  this.lvIndex,
                  this.srcMemberSubName,
                  this.dstClassNames,
                  this.dstMemberNames,
                  this.dstMemberDescs,
                  this.dstNames,
                  comment
               );
            break;
         case METHOD_VAR:
            this.next
               .visitMethodVarComment(
                  this.srcClsName,
                  this.srcMemberName,
                  this.srcMemberDesc,
                  this.argIdx,
                  this.lvIndex,
                  this.startOpIdx,
                  this.endOpIdx,
                  this.srcMemberSubName,
                  this.dstClassNames,
                  this.dstMemberNames,
                  this.dstMemberDescs,
                  this.dstNames,
                  comment
               );
      }
   }
}
