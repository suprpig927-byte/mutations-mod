package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities.Item;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaInventoryCodeHooks;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class HopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {
   public static final int MOVE_ITEM_SPEED = 8;
   public static final int HOPPER_CONTAINER_SIZE = 5;
   private static final int[][] CACHED_SLOTS = new int[54][];
   private static final int NO_COOLDOWN_TIME = -1;
   private static final Component DEFAULT_NAME = Component.translatable("container.hopper");
   private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
   private int cooldownTime = -1;
   private long tickedGameTime;
   private Direction facing;
   private final SnapshotJournal<Integer> cooldownTimeJournal = new SnapshotJournal<Integer>() {
      {
         Objects.requireNonNull(HopperBlockEntity.this);
      }

      protected Integer createSnapshot() {
         return HopperBlockEntity.this.cooldownTime;
      }

      protected void revertToSnapshot(Integer snapshot) {
         HopperBlockEntity.this.cooldownTime = snapshot;
      }
   };

   public HopperBlockEntity(BlockPos worldPosition, BlockState blockState) {
      super(BlockEntityTypes.HOPPER, worldPosition, blockState);
      this.facing = blockState.getValue(HopperBlock.FACING);
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(input)) {
         ContainerHelper.loadAllItems(input, this.items);
      }

      this.cooldownTime = input.getIntOr("TransferCooldown", -1);
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      if (!this.trySaveLootTable(output)) {
         ContainerHelper.saveAllItems(output, this.items);
      }

      output.putInt("TransferCooldown", this.cooldownTime);
   }

   @Override
   public int getContainerSize() {
      return this.items.size();
   }

   @Override
   public ItemStack removeItem(int slot, int count) {
      this.unpackLootTable(null);
      return ContainerHelper.removeItem(this.getItems(), slot, count);
   }

   @Override
   public void setItem(int slot, ItemStack itemStack) {
      this.unpackLootTable(null);
      this.getItems().set(slot, itemStack);
      itemStack.limitSize(this.getMaxStackSize(itemStack));
   }

   private boolean wasEmpty(int slot, int amountChange) {
      if (this.items.get(slot).getCount() != amountChange) {
         return false;
      } else {
         for (int i = 0; i < this.items.size(); i++) {
            if (i != slot && !this.items.get(i).isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }

   public void onTransfer(int slot, int amountChange, TransactionContext transaction) {
      if (amountChange > 0 && !this.isOnCustomCooldown() && this.wasEmpty(slot, amountChange)) {
         this.cooldownTimeJournal.updateSnapshots(transaction);
         this.setCooldown(8);
      }
   }

   @Override
   public void setBlockState(BlockState blockState) {
      super.setBlockState(blockState);
      this.facing = blockState.getValue(HopperBlock.FACING);
   }

   @Override
   protected Component getDefaultName() {
      return DEFAULT_NAME;
   }

   public static void pushItemsTick(Level level, BlockPos pos, BlockState state, HopperBlockEntity entity) {
      entity.cooldownTime--;
      entity.tickedGameTime = level.getGameTime();
      if (!entity.isOnCooldown()) {
         entity.setCooldown(0);
         tryMoveItems(level, pos, state, entity, () -> suckInItems(level, entity));
      }
   }

   private static boolean tryMoveItems(Level level, BlockPos pos, BlockState state, HopperBlockEntity entity, BooleanSupplier action) {
      if (level.isClientSide()) {
         return false;
      } else {
         if (!entity.isOnCooldown() && state.getValue(HopperBlock.ENABLED)) {
            boolean changed = false;
            if (!entity.isEmpty()) {
               changed = ejectItems(level, pos, entity);
            }

            if (!entity.inventoryFull()) {
               changed |= action.getAsBoolean();
            }

            if (changed) {
               entity.setCooldown(8);
               setChanged(level, pos, state);
               return true;
            }
         }

         return false;
      }
   }

   private boolean inventoryFull() {
      for (ItemStack itemStack : this.items) {
         if (itemStack.isEmpty() || itemStack.getCount() != itemStack.getMaxStackSize()) {
            return false;
         }
      }

      return true;
   }

   private static boolean ejectItems(Level level, BlockPos blockPos, HopperBlockEntity self) {
      ContainerOrHandler containerOrHandler = getContainerOrHandlerAt(level, blockPos.relative(self.facing), self.facing.getOpposite());
      if (containerOrHandler.isEmpty()) {
         return false;
      } else if (containerOrHandler.container() != null) {
         Container container = containerOrHandler.container();
         Direction direction = self.facing.getOpposite();
         if (isFullContainer(container, direction)) {
            return false;
         } else {
            for (int slot = 0; slot < self.getContainerSize(); slot++) {
               ItemStack itemStack = self.getItem(slot);
               if (!itemStack.isEmpty()) {
                  int originalCount = itemStack.getCount();
                  ItemStack result = addItem(self, container, self.removeItem(slot, 1), direction);
                  if (result.isEmpty()) {
                     container.setChanged();
                     return true;
                  }

                  itemStack.setCount(originalCount);
                  if (originalCount == 1) {
                     self.setItem(slot, itemStack);
                  }
               }
            }

            return false;
         }
      } else {
         return VanillaInventoryCodeHooks.insertHook(self, containerOrHandler.itemHandler());
      }
   }

   private static int[] getSlots(Container container, Direction direction) {
      if (container instanceof WorldlyContainer worldlyContainer) {
         return worldlyContainer.getSlotsForFace(direction);
      } else {
         int containerSize = container.getContainerSize();
         if (containerSize < CACHED_SLOTS.length) {
            int[] cachedSlots = CACHED_SLOTS[containerSize];
            if (cachedSlots != null) {
               return cachedSlots;
            } else {
               int[] slots = createFlatSlots(containerSize);
               CACHED_SLOTS[containerSize] = slots;
               return slots;
            }
         } else {
            return createFlatSlots(containerSize);
         }
      }
   }

   private static int[] createFlatSlots(int containerSize) {
      int[] slots = new int[containerSize];
      int i = 0;

      while (i < slots.length) {
         slots[i] = i++;
      }

      return slots;
   }

   private static boolean isFullContainer(Container container, Direction direction) {
      int[] slots = getSlots(container, direction);

      for (int slot : slots) {
         ItemStack itemStack = container.getItem(slot);
         if (itemStack.getCount() < itemStack.getMaxStackSize()) {
            return false;
         }
      }

      return true;
   }

   public static boolean suckInItems(Level level, Hopper hopper) {
      BlockPos blockPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
      BlockState blockState = level.getBlockState(blockPos);
      ContainerOrHandler containerOrHandler = getSourceContainerOrHandler(level, hopper, blockPos, blockState);
      if (containerOrHandler.container() != null) {
         Container container = containerOrHandler.container();
         Direction direction = Direction.DOWN;

         for (int slot : getSlots(container, direction)) {
            if (tryTakeInItemFromSlot(hopper, container, slot, direction)) {
               return true;
            }
         }

         return false;
      } else if (containerOrHandler.itemHandler() != null) {
         return VanillaInventoryCodeHooks.extractHook(hopper, containerOrHandler.itemHandler());
      } else {
         boolean isBlocked = hopper.isGridAligned()
            && blockState.isCollisionShapeFullBlock(level, blockPos)
            && !blockState.is(BlockTags.DOES_NOT_BLOCK_HOPPERS);
         if (!isBlocked) {
            for (ItemEntity entity : getItemsAtAndAbove(level, hopper)) {
               if (addItem(hopper, entity)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static boolean tryTakeInItemFromSlot(Hopper hopper, Container container, int slot, Direction direction) {
      ItemStack itemStack = container.getItem(slot);
      if (!itemStack.isEmpty() && canTakeItemFromContainer(hopper, container, itemStack, slot, direction)) {
         int originalCount = itemStack.getCount();
         ItemStack result = addItem(container, hopper, container.removeItem(slot, 1), null);
         if (result.isEmpty()) {
            container.setChanged();
            return true;
         }

         itemStack.setCount(originalCount);
         if (originalCount == 1) {
            container.setItem(slot, itemStack);
         }
      }

      return false;
   }

   public static boolean addItem(Container container, ItemEntity entity) {
      boolean changed = false;
      ItemStack copy = entity.getItem().copy();
      ItemStack result = addItem(null, container, copy, null);
      if (result.isEmpty()) {
         changed = true;
         entity.setItem(ItemStack.EMPTY);
         entity.discard();
      } else {
         entity.setItem(result);
      }

      return changed;
   }

   public static ItemStack addItem(@Nullable Container from, Container container, ItemStack itemStack, @Nullable Direction direction) {
      if (container instanceof WorldlyContainer worldly && direction != null) {
         int[] slots = worldly.getSlotsForFace(direction);

         for (int i = 0; i < slots.length && !itemStack.isEmpty(); i++) {
            itemStack = tryMoveInItem(from, container, itemStack, slots[i], direction);
         }
      } else {
         int size = container.getContainerSize();

         for (int i = 0; i < size && !itemStack.isEmpty(); i++) {
            itemStack = tryMoveInItem(from, container, itemStack, i, direction);
         }
      }

      return itemStack;
   }

   private static boolean canPlaceItemInContainer(Container container, ItemStack itemStack, int slot, @Nullable Direction direction) {
      return !container.canPlaceItem(slot, itemStack)
         ? false
         : !(container instanceof WorldlyContainer worldly && !worldly.canPlaceItemThroughFace(slot, itemStack, direction));
   }

   private static boolean canTakeItemFromContainer(Container into, Container from, ItemStack itemStack, int slot, Direction direction) {
      return !from.canTakeItem(into, slot, itemStack)
         ? false
         : !(from instanceof WorldlyContainer worldly && !worldly.canTakeItemThroughFace(slot, itemStack, direction));
   }

   private static ItemStack tryMoveInItem(@Nullable Container from, Container container, ItemStack itemStack, int slot, @Nullable Direction direction) {
      ItemStack current = container.getItem(slot);
      if (canPlaceItemInContainer(container, itemStack, slot, direction)) {
         boolean success = false;
         boolean wasEmpty = container.isEmpty();
         if (current.isEmpty()) {
            container.setItem(slot, itemStack);
            itemStack = ItemStack.EMPTY;
            success = true;
         } else if (canMergeItems(current, itemStack)) {
            int space = itemStack.getMaxStackSize() - current.getCount();
            int count = Math.min(itemStack.getCount(), space);
            itemStack.shrink(count);
            current.grow(count);
            success = count > 0;
         }

         if (success) {
            if (wasEmpty && container instanceof HopperBlockEntity hopperBlockEntity && !hopperBlockEntity.isOnCustomCooldown()) {
               int skipTickCount = 0;
               if (from instanceof HopperBlockEntity fromHopper && hopperBlockEntity.tickedGameTime >= fromHopper.tickedGameTime) {
                  skipTickCount = 1;
               }

               hopperBlockEntity.setCooldown(8 - skipTickCount);
            }

            container.setChanged();
         }
      }

      return itemStack;
   }

   @Nullable
   private static Container getAttachedContainer(Level level, BlockPos blockPos, HopperBlockEntity self) {
      return getContainerAt(level, blockPos.relative(self.facing));
   }

   @Nullable
   private static Container getSourceContainer(Level level, Hopper hopper, BlockPos pos, BlockState state) {
      return getContainerAt(level, pos, state, hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
   }

   public static List<ItemEntity> getItemsAtAndAbove(Level level, Hopper hopper) {
      AABB aabb = hopper.getSuckAabb().move(hopper.getLevelX() - 0.5, hopper.getLevelY() - 0.5, hopper.getLevelZ() - 0.5);
      return level.getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE);
   }

   @Deprecated
   @Nullable
   public static Container getContainerAt(Level level, BlockPos pos) {
      return getContainerAt(level, pos, level.getBlockState(pos), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
   }

   @Nullable
   private static Container getContainerAt(Level level, BlockPos pos, BlockState state, double x, double y, double z) {
      Container result = getBlockContainer(level, pos, state);
      if (result == null) {
         result = getEntityContainer(level, x, y, z);
      }

      return result;
   }

   @Nullable
   private static Container getBlockContainer(Level level, BlockPos pos, BlockState state) {
      Block block = state.getBlock();
      if (block instanceof WorldlyContainerHolder worldlyContainerHolder) {
         return worldlyContainerHolder.getContainer(state, level, pos);
      } else if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof Container container) {
         if (container instanceof ChestBlockEntity && block instanceof ChestBlock chestBlock) {
            container = ChestBlock.getContainer(chestBlock, state, level, pos, true);
         }

         return container;
      } else {
         return null;
      }
   }

   @Nullable
   private static Container getEntityContainer(Level level, double x, double y, double z) {
      List<Entity> entities = level.getEntities(
         (Entity)null, new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5), EntitySelector.CONTAINER_ENTITY_SELECTOR
      );
      return !entities.isEmpty() ? (Container)entities.get(level.getRandom().nextInt(entities.size())) : null;
   }

   private static ContainerOrHandler getSourceContainerOrHandler(Level level, Hopper hopper, BlockPos pos, BlockState state) {
      return getContainerOrHandlerAt(level, pos, state, hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ(), Direction.DOWN);
   }

   public static ContainerOrHandler getContainerOrHandlerAt(Level level, BlockPos pos, @Nullable Direction side) {
      return getContainerOrHandlerAt(level, pos, level.getBlockState(pos), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, side);
   }

   private static ContainerOrHandler getContainerOrHandlerAt(
      Level level, BlockPos pos, BlockState state, double x, double y, double z, @Nullable Direction side
   ) {
      Container container = getBlockContainer(level, pos, state);
      if (container != null) {
         return new ContainerOrHandler(container, null);
      } else {
         ResourceHandler<ItemResource> blockItemHandler = (ResourceHandler<ItemResource>)level.getCapability(Item.BLOCK, pos, state, null, side);
         return blockItemHandler != null
            ? new ContainerOrHandler(null, blockItemHandler)
            : VanillaInventoryCodeHooks.getEntityContainerOrHandler(level, x, y, z, side);
      }
   }

   private static boolean canMergeItems(ItemStack a, ItemStack b) {
      return a.getCount() <= a.getMaxStackSize() && ItemStack.isSameItemSameComponents(a, b);
   }

   @Override
   public double getLevelX() {
      return this.worldPosition.getX() + 0.5;
   }

   @Override
   public double getLevelY() {
      return this.worldPosition.getY() + 0.5;
   }

   @Override
   public double getLevelZ() {
      return this.worldPosition.getZ() + 0.5;
   }

   @Override
   public boolean isGridAligned() {
      return true;
   }

   public void setCooldown(int time) {
      this.cooldownTime = time;
   }

   private boolean isOnCooldown() {
      return this.cooldownTime > 0;
   }

   public boolean isOnCustomCooldown() {
      return this.cooldownTime > 8;
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> items) {
      this.items = items;
   }

   public static void entityInside(Level level, BlockPos pos, BlockState blockState, Entity entity, HopperBlockEntity hopper) {
      if (entity instanceof ItemEntity itemEntity
         && !itemEntity.getItem().isEmpty()
         && entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()).intersects(hopper.getSuckAabb())) {
         tryMoveItems(level, pos, blockState, hopper, () -> addItem(hopper, itemEntity));
      }
   }

   @Override
   protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      return new HopperMenu(containerId, inventory, this);
   }

   public long getLastUpdateTime() {
      return this.tickedGameTime;
   }
}
