package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SulfurSpikeBlock extends SpeleothemBlock {
   private static final int MAX_GROWING_LENGTH = 2;

   public SulfurSpikeBlock(BlockState blockToGrowOn, BlockBehaviour.Properties properties) {
      super(blockToGrowOn, properties);
   }

   @LevelEvent.Value
   @Override
   protected int getStalactiteLandingSound() {
      return 1052;
   }

   @Override
   protected int getMaxGrowthLength() {
      return 2;
   }
}
