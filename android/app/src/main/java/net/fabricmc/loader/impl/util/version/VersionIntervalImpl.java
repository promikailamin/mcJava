package net.fabricmc.loader.impl.util.version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionInterval;

public final class VersionIntervalImpl implements VersionInterval {
   private final Version min;
   private final boolean minInclusive;
   private final Version max;
   private final boolean maxInclusive;

   public VersionIntervalImpl(Version min, boolean minInclusive, Version max, boolean maxInclusive) {
      this.min = min;
      this.minInclusive = min != null ? minInclusive : false;
      this.max = max;
      this.maxInclusive = max != null ? maxInclusive : false;
      assert min != null || !minInclusive;
      assert max != null || !maxInclusive;
      assert min == null || min instanceof SemanticVersion || minInclusive;
      assert max == null || max instanceof SemanticVersion || maxInclusive;
      assert min == null || max == null || min instanceof SemanticVersion && max instanceof SemanticVersion || min.equals(max);
   }

   @Override
   public boolean isSemantic() {
      return (this.min == null || this.min instanceof SemanticVersion) && (this.max == null || this.max instanceof SemanticVersion);
   }

   @Override
   public Version getMin() {
      return this.min;
   }

   @Override
   public boolean isMinInclusive() {
      return this.minInclusive;
   }

   @Override
   public Version getMax() {
      return this.max;
   }

   @Override
   public boolean isMaxInclusive() {
      return this.maxInclusive;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof VersionInterval)) {
         return false;
      }

      VersionInterval o = (VersionInterval)obj;
      return Objects.equals(this.min, o.getMin())
         && this.minInclusive == o.isMinInclusive()
         && Objects.equals(this.max, o.getMax())
         && this.maxInclusive == o.isMaxInclusive();
   }

   @Override
   public int hashCode() {
      return (Objects.hashCode(this.min) + (this.minInclusive ? 1 : 0)) * 31 + (Objects.hashCode(this.max) + (this.maxInclusive ? 1 : 0)) * 31;
   }

   @Override
   public String toString() {
      if (this.min == null) {
         return this.max == null ? "(-∞,∞)" : String.format("(-∞,%s%c", this.max, Character.valueOf((char)(this.maxInclusive ? ']' : ')')));
      } else {
         return this.max == null
            ? String.format("%c%s,∞)", Character.valueOf((char)(this.minInclusive ? '[' : '(')), this.min)
            : String.format(
               "%c%s,%s%c",
               Character.valueOf((char)(this.minInclusive ? '[' : '(')),
               this.min,
               this.max,
               Character.valueOf((char)(this.maxInclusive ? ']' : ')'))
            );
      }
   }

   public static VersionInterval and(VersionInterval a, VersionInterval b) {
      if (a == null || b == null) {
         return null;
      } else {
         return a.isSemantic() && b.isSemantic() ? andSemantic(a, b) : andPlain(a, b);
      }
   }

   private static VersionInterval andPlain(VersionInterval a, VersionInterval b) {
      Version aMin = a.getMin();
      Version aMax = a.getMax();
      Version bMin = b.getMin();
      Version bMax = b.getMax();
      if (aMin != null) {
         if ((bMin == null || aMin.equals(bMin)) && (bMax == null || aMin.equals(bMax))) {
            if (aMax == null && bMax != null) {
               return new VersionIntervalImpl(aMin, true, bMax, b.isMaxInclusive());
            }

            assert Objects.equals(aMax, bMax) || bMax == null;
            return a;
         } else {
            return null;
         }
      } else {
         if (aMax == null) {
            return b;
         }

         if ((bMin == null || aMax.equals(bMin)) && (bMax == null || aMax.equals(bMax))) {
            if (bMin == null) {
               return a;
            } else {
               return bMax != null ? b : new VersionIntervalImpl(bMin, true, aMax, true);
            }
         } else {
            return null;
         }
      }
   }

   private static VersionInterval andSemantic(VersionInterval a, VersionInterval b) {
      int minCmp = compareMin(a, b);
      int maxCmp = compareMax(a, b);
      if (minCmp == 0) {
         if (maxCmp == 0) {
            return a;
         } else {
            return maxCmp < 0 ? a : b;
         }
      } else {
         if (maxCmp == 0) {
            return minCmp < 0 ? b : a;
         }

         if (minCmp < 0) {
            if (maxCmp > 0) {
               return b;
            }

            SemanticVersion aMax = (SemanticVersion)a.getMax();
            SemanticVersion bMin = (SemanticVersion)b.getMin();
            int cmp = bMin.compareTo(aMax);
            return cmp >= 0 && (cmp != 0 || !b.isMinInclusive() || !a.isMaxInclusive())
               ? null
               : new VersionIntervalImpl(bMin, b.isMinInclusive(), aMax, a.isMaxInclusive());
         } else {
            if (maxCmp < 0) {
               return a;
            }

            SemanticVersion aMin = (SemanticVersion)a.getMin();
            SemanticVersion bMax = (SemanticVersion)b.getMax();
            int cmp = aMin.compareTo(bMax);
            return cmp >= 0 && (cmp != 0 || !a.isMinInclusive() || !b.isMaxInclusive())
               ? null
               : new VersionIntervalImpl(aMin, a.isMinInclusive(), bMax, b.isMaxInclusive());
         }
      }
   }

   public static List<VersionInterval> and(Collection<VersionInterval> a, Collection<VersionInterval> b) {
      if (!a.isEmpty() && !b.isEmpty()) {
         if (a.size() == 1 && b.size() == 1) {
            VersionInterval merged = and(a.iterator().next(), b.iterator().next());
            return merged != null ? Collections.singletonList(merged) : Collections.emptyList();
         }

         List<VersionInterval> allMerged = new ArrayList<>();

         for (VersionInterval intervalA : a) {
            for (VersionInterval intervalB : b) {
               VersionInterval merged = and(intervalA, intervalB);
               if (merged != null) {
                  allMerged.add(merged);
               }
            }
         }

         if (allMerged.isEmpty()) {
            return Collections.emptyList();
         }

         if (allMerged.size() == 1) {
            return allMerged;
         }

         List<VersionInterval> ret = new ArrayList<>(allMerged.size());

         for (VersionInterval v : allMerged) {
            merge(v, ret);
         }

         return ret;
      } else {
         return Collections.emptyList();
      }
   }

   public static List<VersionInterval> or(Collection<VersionInterval> a, VersionInterval b) {
      if (a.isEmpty()) {
         return b == null ? Collections.emptyList() : Collections.singletonList(b);
      }

      List<VersionInterval> ret = new ArrayList<>(a.size() + 1);

      for (VersionInterval v : a) {
         merge(v, ret);
      }

      merge(b, ret);
      return ret;
   }

   private static void merge(VersionInterval a, List<VersionInterval> out) {
      if (a != null) {
         if (out.isEmpty()) {
            out.add(a);
         } else {
            if (out.size() == 1) {
               VersionInterval e = out.get(0);
               if (e.getMin() == null && e.getMax() == null) {
                  return;
               }
            }

            if (!a.isSemantic()) {
               mergePlain(a, out);
            } else {
               mergeSemantic(a, out);
            }
         }
      }
   }

   private static void mergePlain(VersionInterval a, List<VersionInterval> out) {
      Version aMin = a.getMin();
      Version aMax = a.getMax();
      Version v = aMin != null ? aMin : aMax;
      assert v != null;

      for (int i = 0; i < out.size(); i++) {
         VersionInterval c = out.get(i);
         if (v.equals(c.getMin())) {
            if (aMin == null) {
               assert aMax.equals(c.getMin());
               out.clear();
               out.add(INFINITE);
            } else if (aMax == null && c.getMax() != null) {
               out.set(i, a);
            }

            return;
         }

         if (v.equals(c.getMax())) {
            assert c.getMin() == null;
            if (aMax == null) {
               assert aMin.equals(c.getMax());
               out.clear();
               out.add(INFINITE);
            }

            return;
         }
      }

      out.add(a);
   }

   private static void mergeSemantic(VersionInterval a, List<VersionInterval> out) {
      SemanticVersion aMin = (SemanticVersion)a.getMin();
      SemanticVersion aMax = (SemanticVersion)a.getMax();
      if (aMin == null && aMax == null) {
         out.clear();
         out.add(INFINITE);
      } else {
         for (int i = 0; i < out.size(); i++) {
            VersionInterval c = out.get(i);
            if (c.isSemantic()) {
               SemanticVersion cMin = (SemanticVersion)c.getMin();
               SemanticVersion cMax = (SemanticVersion)c.getMax();
               if (aMin == null) {
                  if (cMax == null) {
                     int cmp = aMax.compareTo(cMin);
                     if (cmp >= 0 && (cmp != 0 || a.isMaxInclusive() || c.isMinInclusive())) {
                        out.clear();
                        out.add(INFINITE);
                     } else {
                        out.add(i, a);
                     }

                     return;
                  }

                  int cmp = compareMax(a, c);
                  if (cmp < 0) {
                     if (cMin == null) {
                        return;
                     }

                     cmp = aMax.compareTo(cMin);
                     if (cmp >= 0 && (cmp != 0 || a.isMaxInclusive() || c.isMinInclusive())) {
                        out.set(i, new VersionIntervalImpl(null, false, cMax, c.isMaxInclusive()));
                     } else {
                        out.add(i, a);
                     }

                     return;
                  }

                  out.remove(i);
                  i--;
               } else {
                  if (cMax == null) {
                     int cmp = compareMin(a, c);
                     if (cmp < 0) {
                        if (aMax != null) {
                           cmp = aMax.compareTo(cMin);
                           if (cmp >= 0 && (cmp != 0 || a.isMaxInclusive() || c.isMinInclusive())) {
                              out.set(i, new VersionIntervalImpl(aMin, a.isMinInclusive(), null, false));
                           } else {
                              out.add(i, a);
                           }
                        } else {
                           while (out.size() > i) {
                              out.remove(i);
                           }

                           out.add(a);
                        }
                     }

                     return;
                  }

                  int cmp;
                  if ((cmp = aMin.compareTo(cMax)) < 0 || cmp == false && (a.isMinInclusive() || c.isMaxInclusive())) {
                     int cmp2;
                     if (aMax != null && cMin != null && (cmp2 = aMax.compareTo(cMin)) <= 0 && (cmp2 != false || !a.isMaxInclusive() && !c.isMinInclusive())) {
                        out.add(i, a);
                        return;
                     }

                     int cmpMin = compareMin(a, c);
                     int cmpMax = compareMax(a, c);
                     if (cmpMax <= 0) {
                        if (cmpMin < 0) {
                           out.set(i, new VersionIntervalImpl(aMin, a.isMinInclusive(), cMax, c.isMaxInclusive()));
                        }

                        return;
                     }

                     if (cmpMin > 0) {
                        a = new VersionIntervalImpl(cMin, c.isMinInclusive(), aMax, a.isMaxInclusive());
                     }

                     out.remove(i);
                     i--;
                  }
               }
            }
         }

         out.add(a);
      }
   }

   private static int compareMin(VersionInterval a, VersionInterval b) {
      SemanticVersion aMin = (SemanticVersion)a.getMin();
      SemanticVersion bMin = (SemanticVersion)b.getMin();
      if (aMin == null) {
         return bMin == null ? 0 : -1;
      } else {
         int cmp;
         if (bMin != null && (cmp = aMin.compareTo(bMin)) <= 0 && (cmp != false || a.isMinInclusive() || !b.isMinInclusive())) {
            return cmp >= 0 && (!a.isMinInclusive() || b.isMinInclusive()) ? 0 : -1;
         } else {
            return 1;
         }
      }
   }

   private static int compareMax(VersionInterval a, VersionInterval b) {
      SemanticVersion aMax = (SemanticVersion)a.getMax();
      SemanticVersion bMax = (SemanticVersion)b.getMax();
      if (aMax == null) {
         return bMax == null ? 0 : 1;
      } else {
         int cmp;
         if (bMax != null && (cmp = aMax.compareTo(bMax)) >= 0 && (cmp != false || a.isMaxInclusive() || !b.isMaxInclusive())) {
            return cmp <= 0 && (!a.isMaxInclusive() || b.isMaxInclusive()) ? 0 : 1;
         } else {
            return -1;
         }
      }
   }

   public static List<VersionInterval> not(VersionInterval interval) {
      if (interval == null) {
         return Collections.singletonList(INFINITE);
      }

      if (interval.getMin() == null) {
         return interval.getMax() == null
            ? Collections.emptyList()
            : Collections.singletonList(new VersionIntervalImpl(interval.getMax(), !interval.isMaxInclusive(), null, false));
      }

      if (interval.getMax() == null) {
         return Collections.singletonList(new VersionIntervalImpl(null, false, interval.getMin(), !interval.isMinInclusive()));
      }

      if (interval.getMin().equals(interval.getMax()) && !interval.isMinInclusive() && !interval.isMaxInclusive()) {
         return Collections.singletonList(INFINITE);
      }

      List<VersionInterval> ret = new ArrayList<>(2);
      ret.add(new VersionIntervalImpl(null, false, interval.getMin(), !interval.isMinInclusive()));
      ret.add(new VersionIntervalImpl(interval.getMax(), !interval.isMaxInclusive(), null, false));
      return ret;
   }

   public static List<VersionInterval> not(Collection<VersionInterval> intervals) {
      if (intervals.isEmpty()) {
         return Collections.singletonList(INFINITE);
      }

      if (intervals.size() == 1) {
         return not(intervals.iterator().next());
      }

      List<VersionInterval> ret = null;

      for (VersionInterval v : intervals) {
         List<VersionInterval> inverted = not(v);
         if (ret == null) {
            ret = inverted;
         } else {
            ret = and(ret, inverted);
         }

         if (ret.isEmpty()) {
            break;
         }
      }

      return ret;
   }
}
