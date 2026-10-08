package net.fabricmc.loader.impl.util.version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionComparisonOperator;
import net.fabricmc.loader.api.metadata.version.VersionInterval;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public final class VersionPredicateParser {
   private static final VersionComparisonOperator[] OPERATORS = VersionComparisonOperator.values();

   public static VersionPredicate any() {
      return VersionPredicateParser.AnyVersionPredicate.INSTANCE;
   }

   public static VersionPredicate parse(String predicate) throws VersionParsingException {
      if (!predicate.isEmpty() && !predicate.equals("*")) {
         List<VersionPredicateParser.SingleVersionPredicate> predicateList = new ArrayList<>();

         for (String s : predicate.split(" ")) {
            s = s.trim();
            if (!s.isEmpty() && !s.equals("*")) {
               VersionComparisonOperator operator = VersionComparisonOperator.EQUAL;

               for (VersionComparisonOperator op : OPERATORS) {
                  if (s.startsWith(op.getSerialized())) {
                     operator = op;
                     s = s.substring(op.getSerialized().length());
                     break;
                  }
               }

               Version version = VersionParser.parse(s, true);
               if (version instanceof SemanticVersion) {
                  SemanticVersion semVer = (SemanticVersion)version;
                  if (semVer.hasWildcard()) {
                     if (operator != VersionComparisonOperator.EQUAL) {
                        throw new VersionParsingException(
                           "Invalid predicate: "
                              + predicate
                              + ", version ranges with wildcards (.X) require using the equality operator or no operator at all!"
                        );
                     }

                     assert !semVer.getPrereleaseKey().isPresent();
                     int[] newComponents = new int[semVer.getVersionComponentCount() - 1];

                     for (int i = 0; i < semVer.getVersionComponentCount() - 1; i++) {
                        newComponents[i] = semVer.getVersionComponent(i);
                     }

                     version = new SemanticVersionImpl(newComponents, "", semVer.getBuildKey().orElse(null));
                     int compCount = semVer.getVersionComponentCount();
                     if (compCount <= 1) {
                        throw new IllegalStateException("invalid component count " + compCount + " for version " + semVer);
                     }

                     if (compCount <= 3) {
                        operator = compCount == 2 ? VersionComparisonOperator.SAME_TO_NEXT_MAJOR : VersionComparisonOperator.SAME_TO_NEXT_MINOR;
                     } else {
                        predicateList.add(new VersionPredicateParser.SingleVersionPredicate(VersionComparisonOperator.GREATER_EQUAL, version));
                        newComponents = (int[])newComponents.clone();
                        newComponents[newComponents.length - 1]++;
                        version = new SemanticVersionImpl(newComponents, "", null);
                        operator = VersionComparisonOperator.LESS;
                     }
                  }
               } else {
                  if (!operator.isMinInclusive() && !operator.isMaxInclusive()) {
                     throw new VersionParsingException(
                        "Invalid predicate: " + predicate + ", version ranges need to be semantic version compatible to use operators that exclude the bound!"
                     );
                  }

                  operator = VersionComparisonOperator.EQUAL;
               }

               predicateList.add(new VersionPredicateParser.SingleVersionPredicate(operator, version));
            }
         }

         if (predicateList.isEmpty()) {
            return VersionPredicateParser.AnyVersionPredicate.INSTANCE;
         } else {
            return predicateList.size() == 1 ? predicateList.get(0) : new VersionPredicateParser.MultiVersionPredicate(predicateList);
         }
      } else {
         return VersionPredicateParser.AnyVersionPredicate.INSTANCE;
      }
   }

   public static Set<VersionPredicate> parse(Collection<String> predicates) throws VersionParsingException {
      Set<VersionPredicate> ret = new HashSet<>(predicates.size());

      for (String version : predicates) {
         ret.add(parse(version));
      }

      return ret;
   }

   public static VersionPredicate getAny() {
      return VersionPredicateParser.AnyVersionPredicate.INSTANCE;
   }

   static class AnyVersionPredicate implements VersionPredicate {
      static final VersionPredicate INSTANCE = new VersionPredicateParser.AnyVersionPredicate();

      private AnyVersionPredicate() {
      }

      public boolean test(Version t) {
         return true;
      }

      public List<? extends VersionPredicate.PredicateTerm> getTerms() {
         return Collections.emptyList();
      }

      @Override
      public VersionInterval getInterval() {
         return VersionIntervalImpl.INFINITE;
      }

      @Override
      public String toString() {
         return "*";
      }
   }

   static class MultiVersionPredicate implements VersionPredicate {
      private final List<VersionPredicateParser.SingleVersionPredicate> predicates;

      MultiVersionPredicate(List<VersionPredicateParser.SingleVersionPredicate> predicates) {
         this.predicates = predicates;
      }

      public boolean test(Version version) {
         Objects.requireNonNull(version, "null version");

         for (VersionPredicateParser.SingleVersionPredicate predicate : this.predicates) {
            if (!predicate.test(version)) {
               return false;
            }
         }

         return true;
      }

      public List<? extends VersionPredicate.PredicateTerm> getTerms() {
         return this.predicates;
      }

      @Override
      public VersionInterval getInterval() {
         if (this.predicates.isEmpty()) {
            return VersionPredicateParser.AnyVersionPredicate.INSTANCE.getInterval();
         }

         VersionInterval ret = this.predicates.get(0).getInterval();

         for (int i = 1; i < this.predicates.size(); i++) {
            ret = VersionIntervalImpl.and(ret, this.predicates.get(i).getInterval());
         }

         return ret;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj instanceof VersionPredicateParser.MultiVersionPredicate) {
            VersionPredicateParser.MultiVersionPredicate o = (VersionPredicateParser.MultiVersionPredicate)obj;
            return this.predicates.equals(o.predicates);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.predicates.hashCode();
      }

      @Override
      public String toString() {
         StringBuilder ret = new StringBuilder();

         for (VersionPredicateParser.SingleVersionPredicate predicate : this.predicates) {
            if (ret.length() > 0) {
               ret.append(' ');
            }

            ret.append(predicate.toString());
         }

         return ret.toString();
      }
   }

   static class SingleVersionPredicate implements VersionPredicate, VersionPredicate.PredicateTerm {
      private final VersionComparisonOperator operator;
      private final Version refVersion;

      SingleVersionPredicate(VersionComparisonOperator operator, Version refVersion) {
         this.operator = operator;
         this.refVersion = refVersion;
      }

      public boolean test(Version version) {
         Objects.requireNonNull(version, "null version");
         return this.operator.test(version, this.refVersion);
      }

      public List<VersionPredicate.PredicateTerm> getTerms() {
         return Collections.singletonList(this);
      }

      @Override
      public VersionInterval getInterval() {
         if (this.refVersion instanceof SemanticVersion) {
            SemanticVersion version = (SemanticVersion)this.refVersion;
            return new VersionIntervalImpl(
               this.operator.minVersion(version), this.operator.isMinInclusive(), this.operator.maxVersion(version), this.operator.isMaxInclusive()
            );
         } else {
            return new VersionIntervalImpl(this.refVersion, true, this.refVersion, true);
         }
      }

      @Override
      public VersionComparisonOperator getOperator() {
         return this.operator;
      }

      @Override
      public Version getReferenceVersion() {
         return this.refVersion;
      }

      @Override
      public boolean equals(Object obj) {
         if (!(obj instanceof VersionPredicateParser.SingleVersionPredicate)) {
            return false;
         }

         VersionPredicateParser.SingleVersionPredicate o = (VersionPredicateParser.SingleVersionPredicate)obj;
         return this.operator == o.operator && this.refVersion.equals(o.refVersion);
      }

      @Override
      public int hashCode() {
         return this.operator.ordinal() * 31 + this.refVersion.hashCode();
      }

      @Override
      public String toString() {
         return this.operator.getSerialized().concat(this.refVersion.toString());
      }
   }
}
