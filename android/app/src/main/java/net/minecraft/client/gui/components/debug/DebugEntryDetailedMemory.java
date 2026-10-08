package net.minecraft.client.gui.components.debug;

import java.util.List;
import java.util.Locale;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class DebugEntryDetailedMemory implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("memory");

   @Override
   public void display(
      final DebugScreenDisplayer displayer,
      final @Nullable Level serverOrClientLevel,
      final @Nullable LevelChunk clientChunk,
      final @Nullable LevelChunk serverChunk
   ) {
      displayer.addToGroup(
         GROUP, List.of(printMemoryUsage(runtimeHeapMemory(), "heap"), printMemoryUsage(runtimeNonHeapMemory(), "non-heap"))
      );
   }

   private static long[] runtimeHeapMemory() {
      Runtime runtime = Runtime.getRuntime();
      long total = runtime.totalMemory();
      long used = total - runtime.freeMemory();
      return new long[]{total, used, total, runtime.maxMemory()};
   }

   private static long[] runtimeNonHeapMemory() {
      long allocated = android.os.Debug.getNativeHeapAllocatedSize();
      long size = android.os.Debug.getNativeHeapSize();
      return new long[]{0L, allocated, size, size};
   }

   private static long bytesToMebibytes(final long used) {
      return used / 1024L / 1024L;
   }

   private static String printMemoryUsage(final long[] usage, final String type) {
      return String.format(
         Locale.ROOT,
         "Memory (%s): i=%03dMiB u=%03dMiB c=%03dMiB m=%03dMiB",
         type,
         bytesToMebibytes(usage[0]),
         bytesToMebibytes(usage[1]),
         bytesToMebibytes(usage[2]),
         bytesToMebibytes(usage[3])
      );
   }

   @Override
   public boolean isAllowed(final boolean reducedDebugInfo) {
      return true;
   }
}
