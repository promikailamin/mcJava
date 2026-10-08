package net.fabricmc.loader.util.version;

import java.util.function.Predicate;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

@Deprecated
public final class SemanticVersionPredicateParser {
   public static Predicate<SemanticVersionImpl> create(String text) throws net.fabricmc.loader.api.VersionParsingException {
      VersionPredicate predicate = VersionPredicate.parse(text);
      return v -> predicate.test(v);
   }
}
