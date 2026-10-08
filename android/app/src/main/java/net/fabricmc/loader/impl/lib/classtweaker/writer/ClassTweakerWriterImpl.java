package net.fabricmc.loader.impl.lib.classtweaker.writer;

import net.fabricmc.loader.impl.lib.classtweaker.api.ClassTweakerWriter;
import net.fabricmc.loader.impl.lib.classtweaker.api.visitor.AccessWidenerVisitor;
import net.fabricmc.loader.impl.lib.classtweaker.reader.ClassTweakerReaderImpl;

public final class ClassTweakerWriterImpl implements ClassTweakerWriter {
   private final StringBuilder builder = new StringBuilder();
   private final int version;
   private String namespace;

   public ClassTweakerWriterImpl(int version) {
      this.version = version;
   }

   @Override
   public void visitHeader(String namespace) {
      if (this.namespace == null) {
         String header = "accessWidener";
         int headerVersion = this.version;
         if (this.version >= 3) {
            header = "classTweaker";
            headerVersion -= 2;
         }

         this.builder.append(header).append("\tv").append(headerVersion).append('\t').append(namespace).append('\n');
      } else if (!this.namespace.equals(namespace)) {
         throw new IllegalArgumentException("Cannot write different namespaces to the same file (" + this.namespace + " != " + namespace + ")");
      }

      this.namespace = namespace;
   }

   @Override
   public AccessWidenerVisitor visitAccessWidener(final String owner) {
      return new AccessWidenerVisitor() {
         @Override
         public void visitClass(AccessWidenerVisitor.AccessType access, boolean transitive) {
            ClassTweakerWriterImpl.this.writeAccess(access, transitive);
            ClassTweakerWriterImpl.this.builder.append("\tclass\t").append(owner).append('\n');
         }

         @Override
         public void visitMethod(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
            ClassTweakerWriterImpl.this.writeAccess(access, transitive);
            ClassTweakerWriterImpl.this.builder.append("\tmethod\t").append(owner).append('\t').append(name).append('\t').append(descriptor).append('\n');
         }

         @Override
         public void visitField(String name, String descriptor, AccessWidenerVisitor.AccessType access, boolean transitive) {
            ClassTweakerWriterImpl.this.writeAccess(access, transitive);
            ClassTweakerWriterImpl.this.builder.append("\tfield\t").append(owner).append('\t').append(name).append('\t').append(descriptor).append('\n');
         }
      };
   }

   @Override
   public void visitInjectedInterface(String owner, String iface, boolean transitive) {
      if (this.version < 3) {
         throw new IllegalArgumentException("Cannot write interface injection rule in version " + this.version);
      }

      if (transitive) {
         this.builder.append("transitive-");
      }

      this.builder.append("inject-interface\t").append(owner).append("\t").append(iface).append('\n');
   }

   @Override
   public void visitEnumExtension(String owner, String addedConstant, boolean transitive) {
      if (this.version < 4) {
         throw new IllegalArgumentException("Cannot write enum extension rule in version " + this.version);
      }

      if (transitive) {
         this.builder.append("transitive-");
      }

      this.builder.append("extend-enum\t").append(owner).append("\t").append(addedConstant).append('\n');
   }

   @Override
   public byte[] getOutput() {
      return this.getOutputAsString().getBytes(ClassTweakerReaderImpl.ENCODING);
   }

   public String getOutputAsString() {
      if (this.namespace == null) {
         throw new IllegalStateException("No namespace set. visitHeader wasn't called.");
      } else {
         return this.builder.toString();
      }
   }

   private void writeAccess(AccessWidenerVisitor.AccessType access, boolean transitive) {
      if (transitive) {
         if (this.version < 2) {
            throw new IllegalStateException("Cannot write transitive rule in version " + this.version);
         }

         this.builder.append("transitive-");
      }

      this.builder.append(access);
   }
}
