package net.fabricmc.loader.api.metadata.version;

import java.util.Collection;
import java.util.function.Predicate;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.impl.util.version.VersionPredicateParser;

public interface VersionPredicate extends Predicate<Version> {
   Collection<? extends VersionPredicate.PredicateTerm> getTerms();

   VersionInterval getInterval();

   static VersionPredicate parse(String predicate) throws VersionParsingException {
      return VersionPredicateParser.parse(predicate);
   }

   static Collection<VersionPredicate> parse(Collection<String> predicates) throws VersionParsingException {
      return VersionPredicateParser.parse(predicates);
   }

   interface PredicateTerm {
      VersionComparisonOperator getOperator();

      Version getReferenceVersion();
   }
}
