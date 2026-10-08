package net.fabricmc.loader.api.metadata.version;

import java.util.Collection;
import java.util.List;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.impl.util.version.VersionIntervalImpl;

public interface VersionInterval {
   VersionInterval INFINITE = new VersionIntervalImpl(null, false, null, false);

   boolean isSemantic();

   Version getMin();

   boolean isMinInclusive();

   Version getMax();

   boolean isMaxInclusive();

   default VersionInterval and(VersionInterval o) {
      return and(this, o);
   }

   default List<VersionInterval> or(Collection<VersionInterval> o) {
      return or(o, this);
   }

   default List<VersionInterval> not() {
      return not(this);
   }

   static VersionInterval and(VersionInterval a, VersionInterval b) {
      return VersionIntervalImpl.and(a, b);
   }

   static List<VersionInterval> and(Collection<VersionInterval> a, Collection<VersionInterval> b) {
      return VersionIntervalImpl.and(a, b);
   }

   static List<VersionInterval> or(Collection<VersionInterval> a, VersionInterval b) {
      return VersionIntervalImpl.or(a, b);
   }

   static List<VersionInterval> not(VersionInterval interval) {
      return VersionIntervalImpl.not(interval);
   }

   static List<VersionInterval> not(Collection<VersionInterval> intervals) {
      return VersionIntervalImpl.not(intervals);
   }
}
