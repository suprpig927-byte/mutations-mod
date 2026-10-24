package net.minecraft.world.level.block.entity;

import java.util.Collections;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityType<T extends BlockEntity> {
   private final BlockEntityType.BlockEntitySupplier<? extends T> factory;
   private final Set<Block> validBlocks;
   private final Holder.Reference<BlockEntityType<?>> builtInRegistryHolder = BuiltInRegistries.BLOCK_ENTITY_TYPE.createIntrusiveHolder(this);
   private final boolean onlyOpCanSetNbt;

   public BlockEntityType(BlockEntityType.BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks) {
      this(factory, validBlocks, false);
   }

   public BlockEntityType(BlockEntityType.BlockEntitySupplier<? extends T> factory, Set<Block> validBlocks, boolean onlyOpCanSetNbt) {
      this.factory = factory;
      this.validBlocks = validBlocks;
      this.onlyOpCanSetNbt = onlyOpCanSetNbt;
   }

   public BlockEntityType(BlockEntityType.BlockEntitySupplier<? extends T> factory, Block... validBlocks) {
      this(factory, false, validBlocks);
   }

   public BlockEntityType(BlockEntityType.BlockEntitySupplier<? extends T> factory, boolean onlyOpCanSetNbt, Block... validBlocks) {
      this(factory, Set.of(validBlocks), onlyOpCanSetNbt);
      if (validBlocks.length == 0) {
         throw new IllegalArgumentException(
            "Block entity type instantiated without valid blocks. If this is intentional, pass Set.of() instead of an empty varag."
         );
      }
   }

   public T create(BlockPos worldPosition, BlockState blockState) {
      return (T)this.factory.create(worldPosition, blockState);
   }

   public Set<Block> getValidBlocks() {
      return Collections.unmodifiableSet(this.validBlocks);
   }

   public boolean isValid(BlockState state) {
      return this.validBlocks.contains(state.getBlock());
   }

   @Deprecated
   public Holder.Reference<BlockEntityType<?>> builtInRegistryHolder() {
      return this.builtInRegistryHolder;
   }

   @Nullable
   public T getBlockEntity(BlockGetter level, BlockPos pos) {
      BlockEntity entity = level.getBlockEntity(pos);
      return (T)(entity != null && entity.getType() == this ? entity : null);
   }

   public boolean onlyOpCanSetNbt() {
      return this.onlyOpCanSetNbt ? true : BlockEntityTypes.OP_ONLY_CUSTOM_DATA.contains(this);
   }

   @FunctionalInterface
   public interface BlockEntitySupplier<T extends BlockEntity> {
      T create(BlockPos var1, BlockState var2);
   }
}
