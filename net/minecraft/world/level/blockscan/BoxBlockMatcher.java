package net.minecraft.world.level.blockscan;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Continuation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class BoxBlockMatcher extends BlockMatcher {
   private final BlockPos from;
   private final BlockPos to;

   public BoxBlockMatcher(LevelReader level, BlockPos from, BlockPos to) {
      super(level);
      this.from = from;
      this.to = to;
   }

   public BoxBlockMatcher filterState(Predicate<BlockState> predicate) {
      super.filterState(predicate);
      return this;
   }

   @Override
   public BlockMatcher filterPosition(BiPredicate<BlockState, BlockPos> predicate) {
      super.filterPosition(predicate);
      return this;
   }

   @Override
   public boolean atLeastMatched(final int n) {
      return BlockScanUtils.findBlocks(this.level, this.from, this.to, this.statePredicate, this.positionPredicate, new BlockStateConsumer() {
         private int count;

         {
            Objects.requireNonNull(BoxBlockMatcher.this);
            this.count = 0;
         }

         @Override
         public Continuation apply(BlockPos pos, BlockState state) {
            return Continuation.abortIf(++this.count >= n);
         }
      });
   }

   @Override
   protected boolean anyMatched(Predicate<BlockState> statePredicate, BiPredicate<BlockState, BlockPos> positionPredicate) {
      return BlockScanUtils.findBlocks(this.level, this.from, this.to, statePredicate, positionPredicate, (var0, var1x) -> Continuation.ABORT);
   }

   @Override
   public void forEach(BiConsumer<BlockPos, BlockState> consumer) {
      BlockScanUtils.findBlocks(this.level, this.from, this.to, this.statePredicate, this.positionPredicate, (pos, state) -> {
         consumer.accept(pos, state);
         return Continuation.CONTINUE;
      });
   }

   @Override
   public boolean forEachUntil(BlockStateConsumer consumer) {
      return BlockScanUtils.findBlocks(this.level, this.from, this.to, this.statePredicate, this.positionPredicate, consumer);
   }
}
