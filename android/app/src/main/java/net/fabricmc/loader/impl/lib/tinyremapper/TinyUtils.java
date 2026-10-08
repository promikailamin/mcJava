package net.fabricmc.loader.impl.lib.tinyremapper;

import java.io.IOException;
import java.util.List;
import net.fabricmc.loader.impl.lib.mappingio.FlatMappingVisitor;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import net.fabricmc.loader.impl.lib.mappingio.adapter.FlatAsRegularMappingVisitor;
import net.fabricmc.loader.impl.lib.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.loader.impl.lib.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTreeView;

public final class TinyUtils {
   public static IMappingProvider createMappingProvider(MappingTreeView tree, String fromM, String toM) {
      return out -> {
         try {
            tree.accept(createAdapter(fromM, toM, out));
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      };
   }

   private static MappingVisitor createAdapter(String fromNs, String toNs, IMappingProvider.MappingAcceptor out) throws IOException {
      return new MappingSourceNsSwitch(new MappingDstNsReorder(new FlatAsRegularMappingVisitor(new TinyUtils.MappingAdapter(out)), toNs), fromNs, false);
   }

   private static boolean firstNullOrEqual(Object o1, Object o2) {
      return o1 == null || o1.equals(o2);
   }

   private static boolean anyNullOrEqual(Object o1, Object o2) {
      return o2 == null || firstNullOrEqual(o1, o2);
   }

   private static final class MappingAdapter implements FlatMappingVisitor {
      private final IMappingProvider.MappingAcceptor next;

      MappingAdapter(IMappingProvider.MappingAcceptor next) {
         this.next = next;
      }

      @Override
      public boolean visitClass(String srcName, String[] dstNames) throws IOException {
         String dstName = dstNames[0];
         if (!TinyUtils.anyNullOrEqual(srcName, dstName)) {
            this.next.acceptClass(srcName, dstName);
         }

         return true;
      }

      @Override
      public boolean visitField(String srcClsName, String srcName, String srcDesc, String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException {
         String dstName = dstNames[0];
         if (!TinyUtils.anyNullOrEqual(srcName, dstName)) {
            this.next.acceptField(new IMappingProvider.Member(srcClsName, srcName, srcDesc), dstName);
         }

         return false;
      }

      @Override
      public boolean visitMethod(String srcClsName, String srcName, String srcDesc, String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException {
         String dstName = dstNames[0];
         if (!TinyUtils.anyNullOrEqual(srcName, dstName)) {
            this.next.acceptMethod(new IMappingProvider.Member(srcClsName, srcName, srcDesc), dstName);
         }

         return true;
      }

      @Override
      public boolean visitMethodArg(
         String srcClsName,
         String srcMethodName,
         String srcMethodDesc,
         int argPosition,
         int lvIndex,
         String srcArgName,
         String[] dstClsNames,
         String[] dstMethodNames,
         String[] dstMethodDescs,
         String[] dstArgNames
      ) throws IOException {
         String dstName = dstArgNames[0];
         if (!TinyUtils.firstNullOrEqual(dstName, srcArgName)) {
            this.next.acceptMethodArg(new IMappingProvider.Member(srcClsName, srcMethodName, srcMethodDesc), lvIndex, dstName);
         }

         return false;
      }

      @Override
      public boolean visitMethodVar(
         String srcClsName,
         String srcMethodName,
         String srcMethodDesc,
         int lvtRowIndex,
         int lvIndex,
         int startOpIdx,
         int endOpIds,
         String srcVarName,
         String[] dstClsNames,
         String[] dstMethodNames,
         String[] dstMethodDescs,
         String[] dstVarNames
      ) throws IOException {
         String dstName = dstVarNames[0];
         if (!TinyUtils.firstNullOrEqual(dstName, srcVarName)) {
            this.next.acceptMethodVar(new IMappingProvider.Member(srcClsName, srcMethodName, srcMethodDesc), lvIndex, startOpIdx, -1, dstName);
         }

         return false;
      }

      @Override
      public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
      }

      @Override
      public void visitClassComment(String srcName, String[] dstNames, String comment) throws IOException {
      }

      @Override
      public void visitFieldComment(
         String srcClsName, String srcName, String srcDesc, String[] dstClsNames, String[] dstNames, String[] dstDescs, String comment
      ) throws IOException {
      }

      @Override
      public void visitMethodComment(
         String srcClsName, String srcName, String srcDesc, String[] dstClsNames, String[] dstNames, String[] dstDescs, String comment
      ) throws IOException {
      }

      @Override
      public void visitMethodArgComment(
         String srcClsName,
         String srcMethodName,
         String srcMethodDesc,
         int argPosition,
         int lvIndex,
         String srcArgName,
         String[] dstClsNames,
         String[] dstMethodNames,
         String[] dstMethodDescs,
         String[] dstArgNames,
         String comment
      ) throws IOException {
      }

      @Override
      public void visitMethodVarComment(
         String srcClsName,
         String srcMethodName,
         String srcMethodDesc,
         int lvtRowIndex,
         int lvIndex,
         int startOpIdx,
         int endOpIdx,
         String srcVarName,
         String[] dstClsNames,
         String[] dstMethodNames,
         String[] dstMethodDescs,
         String[] dstVarNames,
         String comment
      ) throws IOException {
      }
   }
}
