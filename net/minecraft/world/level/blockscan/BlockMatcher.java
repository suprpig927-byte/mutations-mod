package net.minecraft.world.level.blockscan;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BlockMatcher {
   private static final Predicate<BlockState> NO_PREDICATE = var0 -> true;
   static final BiPredicate<BlockState, BlockPos> NO_POSITION_PREDICATE = (var0, var1) -> true;
   protected final LevelReader level;
   protected Predicate<BlockState> statePredicate = NO_PREDICATE;
   protected BiPredicate<BlockState, BlockPos> positionPredicate = NO_POSITION_PREDICATE;

   public BlockMatcher(LevelReader levelReader) {
      this.level = levelReader;
   }

   public BlockMatcher filterState(Predicate<BlockState> predicate) {
      this.statePredicate = this.statePredicate == NO_PREDICATE ? predicate : this.statePredicate.and(predicate);
      return this;
   }

   public BlockMatcher filterPosition(BiPredicate<BlockState, BlockPos> predicate) {
      this.positionPredicate = this.positionPredicate == NO_POSITION_PREDICATE ? predicate : this.positionPredicate.and(predicate);
      return this;
   }

   public abstract boolean atLeastMatched(int var1);

   public boolean atMostMatched(int n) {
      return !this.atLeastMatched(n + 1);
   }

   public boolean anyMatched() {
      return this.anyMatched(this.statePredicate, this.positionPredicate);
   }

   protected boolean anyMatched(Predicate<BlockState> statePredicate) {
      return this.anyMatched(statePredicate, NO_POSITION_PREDICATE);
   }

   protected abstract boolean anyMatched(Predicate<BlockState> var1, BiPredicate<BlockState, BlockPos> var2);

   public boolean noneMatched() {
      return !this.anyMatched();
   }

   public boolean allMatched() {
      return this.positionPredicate != NO_POSITION_PREDICATE
         ? !this.anyMatched(NO_PREDICATE, (state, pos) -> !this.statePredicate.test(state) || !this.positionPredicate.test(state, pos))
         : !this.anyMatched(state -> !this.statePredicate.test(state));
   }

   public abstract void forEach(BiConsumer<BlockPos, BlockState> var1);

   public abstract boolean forEachUntil(BlockStateConsumer var1);
}
