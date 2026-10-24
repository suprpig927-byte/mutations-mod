package net.minecraft.world.level.block;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.BlockRelocability;
import net.neoforged.neoforge.common.util.BlockRelocability.Multiblock;
import net.neoforged.neoforge.common.util.BlockRelocability.No;
import net.neoforged.neoforge.common.util.BlockRelocability.Yes;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

public abstract class AbstractBedBlock extends HorizontalDirectionalBlock {
   public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
   public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

   public AbstractBedBlock(BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(PART, BedPart.FOOT).setValue(OCCUPIED, false));
   }

   @Nullable
   public static Direction getBedOrientation(BlockGetter level, BlockPos pos) {
      BlockState blockState = level.getBlockState(pos);
      return blockState.getBlock() instanceof AbstractBedBlock ? blockState.getValue(FACING) : null;
   }

   protected abstract EnvironmentAttribute<BedRule> getBedEnvironmentAttribute();

   protected abstract InteractionResult destroyOnUse(BlockState var1, Level var2, BlockPos var3, Player var4);

   protected abstract void destroyOnLeave(Level var1, BlockPos var2);

   public BedRule getBedRule(Level level, BlockPos pos) {
      return level.environmentAttributes().getValue(this.getBedEnvironmentAttribute(), pos);
   }

   public Identifier getSleptInBedStatType() {
      return Stats.SLEEP_IN_BED;
   }

   public OptionalDouble getSleepHeight(BlockState state, Level level, BlockPos pos) {
      if (!state.is(this)) {
         return OptionalDouble.empty();
      } else {
         VoxelShape shape = state.getShape(level, pos);
         return shape.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(shape.max(Direction.Axis.Y));
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS_SERVER;
      } else {
         BedPart part = state.getValue(PART);
         BlockState otherState = level.getBlockState(pos.relative(getNeighbourDirection(part, state.getValue(FACING))));
         if (otherState.is(this) && otherState.getValue(PART) != part) {
            if (part != BedPart.HEAD) {
               pos = pos.relative(state.getValue(FACING));
               state = level.getBlockState(pos);
            }

            BedRule bedRule = this.getBedRule(level, pos);
            if (bedRule.destroyOnUse()) {
               bedRule.errorMessage().ifPresent(player::sendOverlayMessage);
               return this.destroyOnUse(state, level, pos, player);
            } else if (state.getValue(OCCUPIED)) {
               if (!this.kickVillagerOutOfBed(level, pos)) {
                  player.sendOverlayMessage(Component.translatable("block.minecraft.bed.occupied"));
               }

               return InteractionResult.SUCCESS_SERVER;
            } else {
               player.startSleepInBed(this, state, bedRule, pos).ifLeft(problem -> {
                  if (problem.message() != null) {
                     player.sendOverlayMessage(problem.message());
                  }
               });
               return InteractionResult.SUCCESS_SERVER;
            }
         } else {
            return InteractionResult.CONSUME;
         }
      }
   }

   public void onStopSleeping(Level level, BlockPos pos) {
      BedRule bedRule = this.getBedRule(level, pos);
      if (bedRule.destroyOnLeave()) {
         this.destroyOnLeave(level, pos);
      }
   }

   private boolean kickVillagerOutOfBed(Level level, BlockPos pos) {
      List<Villager> villagers = level.getEntitiesOfClass(Villager.class, new AABB(pos), LivingEntity::isSleeping);
      if (villagers.isEmpty()) {
         return false;
      } else {
         villagers.get(0).stopSleeping();
         return true;
      }
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
      if (directionToNeighbour != getNeighbourDirection(state.getValue(PART), state.getValue(FACING))) {
         return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
      } else {
         return neighbourState.is(this) && neighbourState.getValue(PART) != state.getValue(PART)
            ? state.setValue(OCCUPIED, neighbourState.getValue(OCCUPIED))
            : Blocks.AIR.defaultBlockState();
      }
   }

   private static Direction getNeighbourDirection(BedPart part, Direction facing) {
      return part == BedPart.FOOT ? facing : facing.getOpposite();
   }

   @Override
   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide() && player.preventsBlockDrops()) {
         BedPart part = state.getValue(PART);
         if (part == BedPart.FOOT) {
            BlockPos headPos = pos.relative(getNeighbourDirection(part, state.getValue(FACING)));
            BlockState headState = level.getBlockState(headPos);
            if (headState.is(this) && headState.getValue(PART) == BedPart.HEAD) {
               level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 35);
               level.levelEvent(player, 2001, headPos, Block.getId(headState));
            }
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction facing = context.getHorizontalDirection();
      BlockPos pos = context.getClickedPos();
      BlockPos relative = pos.relative(facing);
      Level level = context.getLevel();
      return level.getBlockState(relative).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(relative)
         ? this.defaultBlockState().setValue(FACING, facing)
         : null;
   }

   public static Direction getConnectedDirection(BlockState state) {
      Direction facing = state.getValue(FACING);
      return state.getValue(PART) == BedPart.HEAD ? facing.getOpposite() : facing;
   }

   public static DoubleBlockCombiner.BlockType getBlockType(BlockState state) {
      BedPart part = state.getValue(PART);
      return part == BedPart.HEAD ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
   }

   private static boolean isBunkBed(BlockGetter level, BlockPos pos) {
      return level.getBlockState(pos.below()).getBlock() instanceof AbstractBedBlock;
   }

   public static Optional<Vec3> findStandUpPosition(EntityType<?> type, CollisionGetter level, BlockPos pos, Direction forward, float yaw) {
      Direction right = forward.getClockWise();
      Direction side = right.isFacingAngle(yaw) ? right.getOpposite() : right;
      if (isBunkBed(level, pos)) {
         return findBunkBedStandUpPosition(type, level, pos, forward, side);
      } else {
         int[][] offsets = bedStandUpOffsets(forward, side);
         Optional<Vec3> safePosition = findStandUpPositionAtOffset(type, level, pos, offsets, true);
         return safePosition.isPresent() ? safePosition : findStandUpPositionAtOffset(type, level, pos, offsets, false);
      }
   }

   private static Optional<Vec3> findBunkBedStandUpPosition(EntityType<?> type, CollisionGetter level, BlockPos pos, Direction forward, Direction side) {
      int[][] offsets = bedSurroundStandUpOffsets(forward, side);
      Optional<Vec3> safePosition = findStandUpPositionAtOffset(type, level, pos, offsets, true);
      if (safePosition.isPresent()) {
         return safePosition;
      } else {
         BlockPos below = pos.below();
         Optional<Vec3> belowSafePosition = findStandUpPositionAtOffset(type, level, below, offsets, true);
         if (belowSafePosition.isPresent()) {
            return belowSafePosition;
         } else {
            int[][] aboveOffsets = bedAboveStandUpOffsets(forward);
            Optional<Vec3> aboveSafePosition = findStandUpPositionAtOffset(type, level, pos, aboveOffsets, true);
            if (aboveSafePosition.isPresent()) {
               return aboveSafePosition;
            } else {
               Optional<Vec3> unsafePosition = findStandUpPositionAtOffset(type, level, pos, offsets, false);
               if (unsafePosition.isPresent()) {
                  return unsafePosition;
               } else {
                  Optional<Vec3> belowUnsafePosition = findStandUpPositionAtOffset(type, level, below, offsets, false);
                  return belowUnsafePosition.isPresent() ? belowUnsafePosition : findStandUpPositionAtOffset(type, level, pos, aboveOffsets, false);
               }
            }
         }
      }
   }

   private static Optional<Vec3> findStandUpPositionAtOffset(EntityType<?> type, CollisionGetter level, BlockPos pos, int[][] offsets, boolean checkDangerous) {
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

      for (int[] offset : offsets) {
         blockPos.set(pos.getX() + offset[0], pos.getY(), pos.getZ() + offset[1]);
         Vec3 position = DismountHelper.findSafeDismountLocation(type, level, blockPos, checkDangerous);
         if (position != null) {
            return Optional.of(position);
         }
      }

      return Optional.empty();
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, PART, OCCUPIED);
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
      super.setPlacedBy(level, pos, state, by, itemStack);
      BlockPos otherPos = pos.relative(state.getValue(FACING));
      level.setBlockAndUpdate(otherPos, state.setValue(PART, BedPart.HEAD));
   }

   @Override
   protected long getSeed(BlockState state, BlockPos pos) {
      BlockPos sourcePos = pos.relative(state.getValue(FACING), state.getValue(PART) == BedPart.HEAD ? 0 : 1);
      return Mth.getSeed(sourcePos.getX(), pos.getY(), sourcePos.getZ());
   }

   @Override
   protected boolean isPathfindable(BlockState state, PathComputationType type) {
      return false;
   }

   private static int[][] bedStandUpOffsets(Direction forward, Direction side) {
      return (int[][])ArrayUtils.addAll(bedSurroundStandUpOffsets(forward, side), bedAboveStandUpOffsets(forward));
   }

   private static int[][] bedSurroundStandUpOffsets(Direction forward, Direction side) {
      return new int[][]{
         {side.getStepX(), side.getStepZ()},
         {side.getStepX() - forward.getStepX(), side.getStepZ() - forward.getStepZ()},
         {side.getStepX() - forward.getStepX() * 2, side.getStepZ() - forward.getStepZ() * 2},
         {-forward.getStepX() * 2, -forward.getStepZ() * 2},
         {-side.getStepX() - forward.getStepX() * 2, -side.getStepZ() - forward.getStepZ() * 2},
         {-side.getStepX() - forward.getStepX(), -side.getStepZ() - forward.getStepZ()},
         {-side.getStepX(), -side.getStepZ()},
         {-side.getStepX() + forward.getStepX(), -side.getStepZ() + forward.getStepZ()},
         {forward.getStepX(), forward.getStepZ()},
         {side.getStepX() + forward.getStepX(), side.getStepZ() + forward.getStepZ()}
      };
   }

   private static int[][] bedAboveStandUpOffsets(Direction forward) {
      return new int[][]{{0, 0}, {-forward.getStepX(), -forward.getStepZ()}};
   }

   public BlockRelocability getRelocability(LevelReader level, BlockPos pos, BlockState state) {
      if (state.is(net.neoforged.neoforge.common.Tags.Blocks.RELOCATION_NOT_SUPPORTED)) {
         return No.INSTANCE;
      } else {
         BedPart thisPart = state.getValue(PART);
         BlockPos neighborPos = pos.relative(getConnectedDirection(state));
         BlockState neighborState = level.getBlockState(neighborPos);
         BlockState expectedState = state.setValue(PART, thisPart == BedPart.HEAD ? BedPart.FOOT : BedPart.HEAD);
         return (BlockRelocability)(neighborState != expectedState ? Yes.INSTANCE : new Multiblock(Set.of(pos, neighborPos)));
      }
   }
}
