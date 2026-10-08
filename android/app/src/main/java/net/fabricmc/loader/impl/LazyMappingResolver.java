package net.fabricmc.loader.impl;

import java.util.Collection;
import java.util.function.Supplier;
import net.fabricmc.loader.api.MappingResolver;

public class LazyMappingResolver implements MappingResolver {
   private final Supplier<MappingResolver> delegateSupplier;
   private final String currentRuntimeNamespace;
   private MappingResolver delegate = null;

   LazyMappingResolver(Supplier<MappingResolver> delegateSupplier, String currentRuntimeNamespace) {
      this.delegateSupplier = delegateSupplier;
      this.currentRuntimeNamespace = currentRuntimeNamespace;
   }

   private MappingResolver getDelegate() {
      if (this.delegate == null) {
         this.delegate = this.delegateSupplier.get();
      }

      return this.delegate;
   }

   @Override
   public Collection<String> getNamespaces() {
      return this.getDelegate().getNamespaces();
   }

   @Override
   public String getCurrentRuntimeNamespace() {
      return this.currentRuntimeNamespace;
   }

   @Override
   public String mapClassName(String namespace, String className) {
      return namespace.equals(this.currentRuntimeNamespace) ? className : this.getDelegate().mapClassName(namespace, className);
   }

   @Override
   public String unmapClassName(String targetNamespace, String className) {
      return this.getDelegate().unmapClassName(targetNamespace, className);
   }

   @Override
   public String mapFieldName(String namespace, String owner, String name, String descriptor) {
      return namespace.equals(this.currentRuntimeNamespace) ? name : this.getDelegate().mapFieldName(namespace, owner, name, descriptor);
   }

   @Override
   public String mapMethodName(String namespace, String owner, String name, String descriptor) {
      return namespace.equals(this.currentRuntimeNamespace) ? name : this.getDelegate().mapMethodName(namespace, owner, name, descriptor);
   }
}
