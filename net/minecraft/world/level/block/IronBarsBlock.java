package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IronBarsBlock extends CrossCollisionBlock {
   public IronBarsBlock(BlockBehaviour.Properties properties) {
      super(2.0F, 16.0F, 2.0F, 16.0F, 16.0F, properties);
      this.registerDefaultState(
         this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false).setValue(WATERLOGGED, false)
      );
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockGetter level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      FluidState replacedFluidState = context.getLevel().getFluidState(context.getClickedPos());
      BlockPos north = pos.north();
      BlockPos south = pos.south();
      BlockPos west = pos.west();
      BlockPos east = pos.east();
      BlockState northState = level.getBlockState(north);
      BlockState southState = level.getBlockState(south);
      BlockState westState = level.getBlockState(west);
      BlockState eastState = level.getBlockState(east);
      return this.defaultBlockState()
         .setValue(NORTH, this.attachesTo(northState, northState.isFaceSturdy(level, north, Direction.SOUTH), Direction.SOUTH))
         .setValue(SOUTH, this.attachesTo(southState, southState.isFaceSturdy(level, south, Direction.NORTH), Direction.NORTH))
         .setValue(WEST, this.attachesTo(westState, westState.isFaceSturdy(level, west, Direction.EAST), Direction.EAST))
         .setValue(EAST, this.attachesTo(eastState, eastState.isFaceSturdy(level, east, Direction.WEST), Direction.WEST))
         .setValue(WATERLOGGED, replacedFluidState.is(Fluids.WATER));
   }

   @Override
   protected BlockState updateShape(
      BlockState state,
      LevelReader level,
      ScheduledTickAccess ticks,
      BlockPos pos,
      Direction directionToNeighbour,
      BlockPos neighbourPos,
      BlockState neighbourState,
      RandomSource random
   ) {
      if (state.getValue(WATERLOGGED)) {
         ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return directionToNeighbour.getAxis().isHorizontal()
         ? state.setValue(
            PROPERTY_BY_DIRECTION.get(directionToNeighbour),
            this.attachesTo(
               neighbourState, neighbourState.isFaceSturdy(level, neighbourPos, directionToNeighbour.getOpposite()), directionToNeighbour.getOpposite()
            )
         )
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   @Override
   protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   @Override
   protected boolean skipRendering(BlockState state, BlockState neighborState, Direction direction) {
      if (neighborState.is(this)
         || neighborState.is(BlockTags.BARS) && state.is(BlockTags.BARS) && neighborState.hasProperty(PROPERTY_BY_DIRECTION.get(direction.getOpposite()))) {
         if (!direction.getAxis().isHorizontal()) {
            return true;
         }

         if (state.getValue(PROPERTY_BY_DIRECTION.get(direction)) && neighborState.getValue(PROPERTY_BY_DIRECTION.get(direction.getOpposite()))) {
            return true;
         }
      }

      return super.skipRendering(state, neighborState, direction);
   }

   public boolean attachesTo(BlockState state, boolean faceSolid, Direction direction) {
      TriState adjResult = state.canBeConnectedTo(direction, this);
      return adjResult.isTrue() || !adjResult.isFalse() && this.attachsTo(state, faceSolid);
   }

   @Deprecated
   public final boolean attachsTo(BlockState state, boolean faceSolid) {
      return !isExceptionForConnection(state) && faceSolid || state.getBlock() instanceof IronBarsBlock || state.is(BlockTags.WALLS);
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}
