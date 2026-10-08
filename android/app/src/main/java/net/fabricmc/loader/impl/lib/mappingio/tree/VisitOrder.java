package net.fabricmc.loader.impl.lib.mappingio.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class VisitOrder {
   private static final AlphanumericComparator ALPHANUM = new AlphanumericComparator();
   private Comparator<MappingTreeView.ClassMappingView> classComparator;
   private Comparator<MappingTreeView.FieldMappingView> fieldComparator;
   private Comparator<MappingTreeView.MethodMappingView> methodComparator;
   private Comparator<MappingTreeView.MethodArgMappingView> methodArgComparator;
   private Comparator<MappingTreeView.MethodVarMappingView> methodVarComparator;
   private boolean methodsFirst;
   private boolean methodVarsFirst;

   private VisitOrder() {
   }

   public static VisitOrder createByInputOrder() {
      return new VisitOrder();
   }

   public <T extends MappingTreeView.ClassMappingView> Collection<T> sortClasses(Collection<T> classes) {
      return sort(classes, this.classComparator);
   }

   public <T extends MappingTreeView.FieldMappingView> Collection<T> sortFields(Collection<T> fields) {
      return sort(fields, this.fieldComparator);
   }

   public <T extends MappingTreeView.MethodMappingView> Collection<T> sortMethods(Collection<T> methods) {
      return sort(methods, this.methodComparator);
   }

   public <T extends MappingTreeView.MethodArgMappingView> Collection<T> sortMethodArgs(Collection<T> args) {
      return sort(args, this.methodArgComparator);
   }

   public <T extends MappingTreeView.MethodVarMappingView> Collection<T> sortMethodVars(Collection<T> vars) {
      return sort(vars, this.methodVarComparator);
   }

   private static <T> Collection<T> sort(Collection<T> inputs, Comparator<? super T> comparator) {
      if (comparator != null && inputs.size() >= 2) {
         List<T> ret = new ArrayList<>(inputs);
         ret.sort(comparator);
         return ret;
      } else {
         return inputs;
      }
   }

   public boolean isMethodsFirst() {
      return this.methodsFirst;
   }

   public boolean isMethodVarsFirst() {
      return this.methodVarsFirst;
   }
}
