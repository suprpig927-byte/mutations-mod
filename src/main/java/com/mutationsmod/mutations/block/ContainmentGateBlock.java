package com.mutationsmod.mutations.block;

import com.mutationsmod.mutations.registry.ModContent;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Containment Gate: unbreakable. A Director's Keycard opens the whole gate (every connected gate block). */
public class ContainmentGateBlock extends Block {
    public ContainmentGateBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!stack.is(ModContent.DIRECTORS_KEYCARD.get())) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.literal("ACCESS DENIED - Director's Keycard required").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel sl) {
            Set<BlockPos> seen = new HashSet<>();
            ArrayDeque<BlockPos> todo = new ArrayDeque<>();
            todo.add(pos);
            while (!todo.isEmpty() && seen.size() < 128) {
                BlockPos p = todo.poll();
                if (!seen.add(p) || !sl.getBlockState(p).is(this)) continue;
                for (Direction d : Direction.values()) todo.add(p.relative(d));
            }
            for (BlockPos p : seen) {
                if (!sl.getBlockState(p).is(this)) continue;
                sl.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
            }
            sl.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.5f, 0.5f);
            player.sendOverlayMessage(Component.literal("ACCESS GRANTED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        }
        return InteractionResult.SUCCESS;
    }
}
