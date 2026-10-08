package net.fabricmc.loader.impl.lib.mappingio.format.srg;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Set;
import net.fabricmc.loader.impl.lib.mappingio.MappedElementKind;
import net.fabricmc.loader.impl.lib.mappingio.MappingFlag;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import net.fabricmc.loader.impl.lib.mappingio.format.ColumnFileReader;
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;

public final class JamFileReader {
   private JamFileReader() {
   }

   public static void read(Reader reader, MappingVisitor visitor) throws IOException {
      read(reader, "source", "target", visitor);
   }

   public static void read(Reader reader, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
      read(new ColumnFileReader(reader, '\t', ' '), sourceNs, targetNs, visitor);
   }

   private static void read(ColumnFileReader reader, String sourceNs, String targetNs, MappingVisitor visitor) throws IOException {
      Set<MappingFlag> flags = visitor.getFlags();
      MappingVisitor parentVisitor = null;
      boolean readerMarked = false;
      if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS)) {
         parentVisitor = visitor;
         visitor = new MemoryMappingTree();
      } else if (flags.contains(MappingFlag.NEEDS_MULTIPLE_PASSES)) {
         reader.mark();
         readerMarked = true;
      }

      while (true) {
         if (visitor.visitHeader()) {
            visitor.visitNamespaces(sourceNs, Collections.singletonList(targetNs));
         }

         if (visitor.visitContent()) {
            String lastClassName = null;
            boolean visitClass = false;
            String lastMethodName = null;
            String lastMethodDesc = null;
            boolean visitMember = false;
            boolean visitMethodContent = false;

            do {
               boolean isArg = false;
               if (reader.nextCol("CL")) {
                  String srcName = reader.nextCol();
                  if (srcName == null || srcName.isEmpty()) {
                     throw new IOException("missing class-name-a in line " + reader.getLineNumber());
                  }

                  lastClassName = srcName;
                  visitClass = visitor.visitClass(srcName);
                  if (visitClass) {
                     String dstName = reader.nextCol();
                     if (dstName == null || dstName.isEmpty()) {
                        throw new IOException("missing class-name-b in line " + reader.getLineNumber());
                     }

                     visitor.visitDstName(MappedElementKind.CLASS, 0, dstName);
                     visitClass = visitor.visitElementContent(MappedElementKind.CLASS);
                  }
               } else {
                  boolean isMethod;
                  if ((isMethod = reader.nextCol("MD")) || reader.nextCol("FD") || (isArg = reader.nextCol("MP"))) {
                     String clsSrcName = reader.nextCol();
                     if (clsSrcName == null) {
                        throw new IOException("missing class-name-a in line " + reader.getLineNumber());
                     }

                     String memberSrcName = reader.nextCol();
                     if (memberSrcName == null || memberSrcName.isEmpty()) {
                        throw new IOException("missing member-name-a in line " + reader.getLineNumber());
                     }

                     String memberSrcDesc = reader.nextCol();
                     if (memberSrcDesc == null || memberSrcDesc.isEmpty()) {
                        throw new IOException("missing member-desc-a in line " + reader.getLineNumber());
                     }

                     String col5 = reader.nextCol();
                     String col6 = reader.nextCol();
                     String col7 = reader.nextCol();
                     int argSrcPos = -1;
                     String dstName;
                     if (!isArg) {
                        dstName = col5;
                     } else {
                        argSrcPos = Integer.parseInt(col5);
                        if (col7 != null && !col7.isEmpty()) {
                           String argSrcDesc = col6;
                           if (argSrcDesc == null || argSrcDesc.isEmpty()) {
                              throw new IOException("missing parameter-desc-a in line " + reader.getLineNumber());
                           }

                           dstName = col7;
                        } else {
                           dstName = col6;
                        }
                     }

                     if (dstName == null || dstName.isEmpty()) {
                        throw new IOException("missing name-b in line " + reader.getLineNumber());
                     }

                     if (!clsSrcName.equals(lastClassName)) {
                        lastClassName = clsSrcName;
                        lastMethodName = null;
                        lastMethodDesc = null;
                        visitClass = visitor.visitClass(clsSrcName) && visitor.visitElementContent(MappedElementKind.CLASS);
                     }

                     if (visitClass) {
                        boolean newMethod = false;
                        boolean isField = !isMethod && !isArg;
                        if (isField) {
                           visitMember = visitor.visitField(memberSrcName, memberSrcDesc);
                        } else if (!isArg || (newMethod = !memberSrcName.equals(lastMethodName) || !memberSrcDesc.equals(lastMethodDesc))) {
                           lastMethodName = memberSrcName;
                           lastMethodDesc = memberSrcDesc;
                           visitMember = visitor.visitMethod(memberSrcName, memberSrcDesc);
                           visitMethodContent = false;
                        }

                        if (visitMember) {
                           if (isField) {
                              visitor.visitDstName(MappedElementKind.FIELD, 0, dstName);
                              visitor.visitElementContent(MappedElementKind.FIELD);
                           } else {
                              if (isMethod) {
                                 visitor.visitDstName(MappedElementKind.METHOD, 0, dstName);
                              }

                              if (isMethod || newMethod) {
                                 visitMethodContent = visitor.visitElementContent(MappedElementKind.METHOD);
                              }

                              if (isArg && visitMethodContent && visitor.visitMethodArg(argSrcPos, -1, null)) {
                                 visitor.visitDstName(MappedElementKind.METHOD_ARG, 0, dstName);
                                 visitor.visitElementContent(MappedElementKind.METHOD_ARG);
                              }
                           }
                        }
                     }
                  }
               }
            } while (reader.nextLine(0));
         }

         if (visitor.visitEnd()) {
            if (parentVisitor != null) {
               ((MappingTree)visitor).accept(parentVisitor);
            }

            return;
         }

         if (!readerMarked) {
            throw new IllegalStateException("repeated visitation requested without NEEDS_MULTIPLE_PASSES");
         }

         int markIdx = reader.reset();
         assert markIdx == 1;
      }
   }
}
