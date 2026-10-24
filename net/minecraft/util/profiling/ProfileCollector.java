package net.minecraft.util.profiling;

import com.mojang.datafixers.util.Pair;
import java.util.Set;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.jspecify.annotations.Nullable;

public interface ProfileCollector extends ProfilerFiller {
   ProfileResults getResults();

   @Nullable
   ActiveProfiler.PathEntry getEntry(String var1);

   Set<Pair<String, MetricCategory>> getChartedPaths();
}
