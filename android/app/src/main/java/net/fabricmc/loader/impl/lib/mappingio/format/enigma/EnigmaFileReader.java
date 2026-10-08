package net.fabricmc.loader.impl.lib.mappingio.format.enigma;

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

public final class EnigmaFileReader {
   private EnigmaFileReader() {
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
      if (flags.contains(MappingFlag.NEEDS_ELEMENT_UNIQUENESS) || flags.contains(MappingFlag.NEEDS_HEADER_METADATA)) {
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
            StringBuilder commentSb = new StringBuilder(200);

            do {
               if (reader.nextCol("CLASS")) {
                  readClass(reader, 0, null, null, commentSb, visitor);
               } else if (reader.nextCol("METADATA")) {
                  readMetadata(reader, visitor);
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

   private static void readClass(
      ColumnFileReader reader, int indent, String outerSrcClass, String outerDstClass, StringBuilder commentSb, MappingVisitor visitor
   ) throws IOException {
      String srcInnerName = reader.nextCol();
      if (srcInnerName != null && !srcInnerName.isEmpty()) {
         String srcName = srcInnerName;
         if (outerSrcClass != null && srcInnerName.indexOf(36) < 0) {
            srcName = String.format("%s$%s", outerSrcClass, srcInnerName);
         }

         String dstInnerName = reader.nextCol();
         String dstName = dstInnerName;
         if (outerDstClass != null || dstInnerName != null && outerSrcClass != null) {
            if (dstInnerName == null) {
               dstInnerName = srcInnerName;
            }

            if (outerDstClass == null) {
               outerDstClass = outerSrcClass;
            }

            dstName = String.format("%s$%s", outerDstClass, dstInnerName);
         }

         readClassBody(reader, indent, srcName, dstName, commentSb, visitor);
      } else {
         throw new IOException("missing class-name-a in line " + reader.getLineNumber());
      }
   }

   private static void readClassBody(ColumnFileReader reader, int indent, String srcClass, String dstClass, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
      boolean visited = false;
      int state = 0;

      while (reader.nextLine(indent + 1)) {
         if (!reader.nextCol("CLASS")) {
            if (reader.nextCol("COMMENT")) {
               readComment(reader, commentSb);
            } else {
               boolean isMethod;
               if ((isMethod = reader.nextCol("METHOD")) || reader.nextCol("FIELD")) {
                  state = visitClass(srcClass, dstClass, state, commentSb, visitor);
                  visited = true;
                  if (state >= 0) {
                     String srcName = reader.nextCol();
                     if (srcName != null && !srcName.isEmpty()) {
                        String dstNameOrSrcDesc = reader.nextCol();
                        if (dstNameOrSrcDesc != null && !dstNameOrSrcDesc.isEmpty()) {
                           String srcDesc = reader.nextCol();
                           String dstName;
                           if (srcDesc == null) {
                              dstName = null;
                              srcDesc = dstNameOrSrcDesc;
                           } else {
                              dstName = dstNameOrSrcDesc;
                           }

                           if (isMethod && visitor.visitMethod(srcName, srcDesc)) {
                              if (dstName != null && !dstName.isEmpty()) {
                                 visitor.visitDstName(MappedElementKind.METHOD, 0, dstName);
                              }

                              readMethod(reader, indent, commentSb, visitor);
                              continue;
                           }

                           if (!isMethod && visitor.visitField(srcName, srcDesc)) {
                              if (dstName != null && !dstName.isEmpty()) {
                                 visitor.visitDstName(MappedElementKind.FIELD, 0, dstName);
                              }

                              readElement(reader, MappedElementKind.FIELD, indent, commentSb, visitor);
                           }
                           continue;
                        }

                        throw new IOException("missing member-name-b/member-desc-a in line " + reader.getLineNumber());
                     }

                     throw new IOException("missing member-name-a in line " + reader.getLineNumber());
                  }
               }
            }
         } else {
            if (!visited || commentSb.length() > 0) {
               visitClass(srcClass, dstClass, state, commentSb, visitor);
               visited = true;
            }

            readClass(reader, indent + 1, srcClass, dstClass, commentSb, visitor);
            state = 0;
         }
      }

      if (!visited || commentSb.length() > 0) {
         visitClass(srcClass, dstClass, state, commentSb, visitor);
      }
   }

   private static int visitClass(String srcClass, String dstClass, int state, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
      if (state == 0) {
         boolean visitContent = visitor.visitClass(srcClass);
         if (visitContent) {
            if (dstClass != null && !dstClass.isEmpty()) {
               visitor.visitDstName(MappedElementKind.CLASS, 0, dstClass);
            }

            visitContent = visitor.visitElementContent(MappedElementKind.CLASS);
         }

         state = visitContent ? 1 : -1;
         if (commentSb.length() > 0) {
            if (state > 0) {
               visitor.visitComment(MappedElementKind.CLASS, commentSb.toString());
            }

            commentSb.setLength(0);
         }
      }

      return state;
   }

   private static void readMethod(ColumnFileReader reader, int indent, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
      if (visitor.visitElementContent(MappedElementKind.METHOD)) {
         while (reader.nextLine(indent + 2)) {
            if (reader.nextCol("COMMENT")) {
               readComment(reader, commentSb);
            } else {
               submitComment(MappedElementKind.METHOD, commentSb, visitor);
               if (reader.nextCol("ARG")) {
                  int lvIndex = reader.nextIntCol();
                  if (lvIndex < 0) {
                     throw new IOException("missing/invalid parameter-lv-index in line " + reader.getLineNumber());
                  }

                  if (visitor.visitMethodArg(-1, lvIndex, null)) {
                     String dstName = reader.nextCol();
                     if (dstName != null && !dstName.isEmpty()) {
                        visitor.visitDstName(MappedElementKind.METHOD_ARG, 0, dstName);
                     }

                     readElement(reader, MappedElementKind.METHOD_ARG, indent, commentSb, visitor);
                  }
               }
            }
         }

         submitComment(MappedElementKind.METHOD, commentSb, visitor);
      }
   }

   private static void readElement(ColumnFileReader reader, MappedElementKind kind, int indent, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
      if (visitor.visitElementContent(kind)) {
         while (reader.nextLine(indent + kind.level + 1)) {
            if (reader.nextCol("COMMENT")) {
               readComment(reader, commentSb);
            }
         }

         submitComment(kind, commentSb, visitor);
      }
   }

   private static void readComment(ColumnFileReader reader, StringBuilder commentSb) throws IOException {
      if (commentSb.length() > 0) {
         commentSb.append('\n');
      }

      String comment = reader.nextCols(true);
      if (comment != null) {
         commentSb.append(comment);
      }
   }

   private static void submitComment(MappedElementKind kind, StringBuilder commentSb, MappingVisitor visitor) throws IOException {
      if (commentSb.length() != 0) {
         visitor.visitComment(kind, commentSb.toString());
         commentSb.setLength(0);
      }
   }

   private static void readMetadata(ColumnFileReader reader, MappingVisitor visitor) throws IOException {
      String key = reader.nextCol();
      if (key != null && !key.isEmpty()) {
         String value = reader.nextCols(true);
         visitor.visitMetadata(key, value);
      } else {
         throw new IOException("missing metadata key in line " + reader.getLineNumber());
      }
   }
}
