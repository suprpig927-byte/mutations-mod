package net.minecraft.world.clock;

import net.minecraft.core.Holder;

public interface ClockManager {
   ClockInstance getInstance(Holder<WorldClock> var1);

   default void setRate(Holder<WorldClock> definition, float rate) {
   }
}
