package net.fabricmc.loader.impl.lib.mappingio.adapter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.loader.impl.lib.mappingio.MappedElementKind;
import net.fabricmc.loader.impl.lib.mappingio.MappingFlag;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import org.jetbrains.annotations.Nullable;

public abstract class ForwardingMappingVisitor implements MappingVisitor {
   protected final MappingVisitor next;

   protected ForwardingMappingVisitor(MappingVisitor next) {
      Objects.requireNonNull(next, "null next");
      this.next = next;
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
   public boolean visitClass(String srcName) throws IOException {
      return this.next.visitClass(srcName);
   }

   @Override
   public boolean visitField(String srcName, @Nullable String srcDesc) throws IOException {
      return this.next.visitField(srcName, srcDesc);
   }

   @Override
   public boolean visitMethod(String srcName, @Nullable String srcDesc) throws IOException {
      return this.next.visitMethod(srcName, srcDesc);
   }

   @Override
   public boolean visitMethodArg(int argPosition, int lvIndex, @Nullable String srcName) throws IOException {
      return this.next.visitMethodArg(argPosition, lvIndex, srcName);
   }

   @Override
   public boolean visitMethodVar(int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, @Nullable String srcName) throws IOException {
      return this.next.visitMethodVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
   }

   @Override
   public boolean visitEnd() throws IOException {
      return this.next.visitEnd();
   }

   @Override
   public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
      this.next.visitDstName(targetKind, namespace, name);
   }

   @Override
   public void visitDstDesc(MappedElementKind targetKind, int namespace, String desc) throws IOException {
      this.next.visitDstDesc(targetKind, namespace, desc);
   }

   @Override
   public boolean visitElementContent(MappedElementKind targetKind) throws IOException {
      return this.next.visitElementContent(targetKind);
   }

   @Override
   public void visitComment(MappedElementKind targetKind, String comment) throws IOException {
      this.next.visitComment(targetKind, comment);
   }
}
