package net.fabricmc.loader.impl.lib.mappingio.format.tiny;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.impl.lib.mappingio.MappedElementKind;
import net.fabricmc.loader.impl.lib.mappingio.MappingFlag;
import net.fabricmc.loader.impl.lib.mappingio.MappingVisitor;
import net.fabricmc.loader.impl.lib.mappingio.format.ColumnFileReader;
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import net.fabricmc.loader.impl.lib.mappingio.tree.MemoryMappingTree;

public final class Tiny1FileReader {
   private Tiny1FileReader() {
   }

   public static List<String> getNamespaces(Reader reader) throws IOException {
      return getNamespaces(new ColumnFileReader(reader, '\t', '\t'));
   }

   private static List<String> getNamespaces(ColumnFileReader reader) throws IOException {
      if (!reader.nextCol("v1")) {
         throw new IOException("invalid/unsupported tiny file: no tiny 1 header");
      }

      List<String> ret = new ArrayList<>();

      String ns;
      while ((ns = reader.nextCol()) != null) {
         ret.add(ns);
      }

      return ret;
   }

   public static void read(Reader reader, MappingVisitor visitor) throws IOException {
      read(new ColumnFileReader(reader, '\t', '\t'), visitor);
   }

   private static void read(ColumnFileReader reader, MappingVisitor visitor) throws IOException {
      if (!reader.nextCol("v1")) {
         throw new IOException("invalid/unsupported tiny file: no tiny 1 header");
      }

      String srcNamespace = reader.nextCol();
      if (srcNamespace != null && !srcNamespace.isEmpty()) {
         List<String> dstNamespaces = new ArrayList<>();

         while (!reader.isAtEol()) {
            String dstNamespace = reader.nextCol();
            if (dstNamespace == null || dstNamespace.isEmpty()) {
               throw new IOException("empty destination namespace in Tiny v1 header");
            }

            dstNamespaces.add(dstNamespace);
         }

         int dstNsCount = dstNamespaces.size();
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
               visitor.visitNamespaces(srcNamespace, dstNamespaces);
            }

            if (visitor.visitContent()) {
               String lastClass = null;
               boolean visitLastClass = false;

               while (reader.nextLine(0)) {
                  if (reader.nextCol("CLASS")) {
                     String srcName = reader.nextCol();
                     if (srcName == null || srcName.isEmpty()) {
                        throw new IOException("missing class-name-a in line " + reader.getLineNumber());
                     }

                     lastClass = srcName;
                     visitLastClass = visitor.visitClass(srcName);
                     if (visitLastClass) {
                        readDstNames(reader, MappedElementKind.CLASS, dstNsCount, visitor);
                        visitLastClass = visitor.visitElementContent(MappedElementKind.CLASS);
                     }
                  } else {
                     boolean isMethod;
                     if (!(isMethod = reader.nextCol("METHOD")) && !reader.nextCol("FIELD")) {
                        String line = reader.nextCol();
                        String prefix = "# INTERMEDIARY-COUNTER ";
                        String[] parts;
                        if (line.startsWith("# INTERMEDIARY-COUNTER ") && (parts = line.substring("# INTERMEDIARY-COUNTER ".length()).split(" ")).length == 2) {
                           String property = null;
                           switch (parts[0]) {
                              case "class":
                                 property = "next-intermediary-class";
                                 break;
                              case "field":
                                 property = "next-intermediary-field";
                                 break;
                              case "method":
                                 property = "next-intermediary-method";
                           }

                           if (property != null) {
                              visitor.visitMetadata(property, parts[1]);
                           }
                        }
                     } else {
                        String srcOwner = reader.nextCol();
                        if (srcOwner == null || srcOwner.isEmpty()) {
                           throw new IOException("missing class-name-a in line " + reader.getLineNumber());
                        }

                        if (!srcOwner.equals(lastClass)) {
                           lastClass = srcOwner;
                           visitLastClass = visitor.visitClass(srcOwner) && visitor.visitElementContent(MappedElementKind.CLASS);
                        }

                        if (visitLastClass) {
                           String srcDesc = reader.nextCol();
                           if (srcDesc == null || srcDesc.isEmpty()) {
                              throw new IOException("missing member-desc-a in line " + reader.getLineNumber());
                           }

                           String srcName = reader.nextCol();
                           if (srcName == null || srcName.isEmpty()) {
                              throw new IOException("missing member-name-a in line " + reader.getLineNumber());
                           }

                           if (isMethod && visitor.visitMethod(srcName, srcDesc) || !isMethod && visitor.visitField(srcName, srcDesc)) {
                              MappedElementKind kind = isMethod ? MappedElementKind.METHOD : MappedElementKind.FIELD;
                              readDstNames(reader, kind, dstNsCount, visitor);
                              visitor.visitElementContent(kind);
                           }
                        }
                     }
                  }
               }
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
      } else {
         throw new IOException("no source namespace in Tiny v1 header");
      }
   }

   private static void readDstNames(ColumnFileReader reader, MappedElementKind subjectKind, int dstNsCount, MappingVisitor visitor) throws IOException {
      for (int dstNs = 0; dstNs < dstNsCount; dstNs++) {
         String name = reader.nextCol();
         if (name == null) {
            throw new IOException("missing name columns in line " + reader.getLineNumber());
         }

         if (!name.isEmpty()) {
            visitor.visitDstName(subjectKind, dstNs, name);
         }
      }
   }
}
