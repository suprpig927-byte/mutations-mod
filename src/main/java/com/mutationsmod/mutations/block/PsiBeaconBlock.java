package com.mutationsmod.mutations.block;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Psi Beacon: right-click to list every mutant within 64 blocks (mutation, mastery level, distance). */
public class PsiBeaconBlock extends Block {
    public static final int RANGE = 64;

    public PsiBeaconBlock(Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer user)) return InteractionResult.SUCCESS;
        level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.0f, 1.6f);
        user.sendSystemMessage(Component.literal("== Psi Beacon: mutants within " + RANGE + " blocks ==").withStyle(ChatFormatting.LIGHT_PURPLE));
        int found = 0;
        for (Player other : level.players()) {
            if (!(other instanceof ServerPlayer sp) || sp.distanceToSqr(pos.getCenter()) > RANGE * RANGE) continue;
            MutationData d = ModAttachments.get(sp);
            if (d.mutations().isEmpty()) continue;
            Mutation base = d.base();
            int dist = (int) Math.sqrt(sp.distanceToSqr(pos.getCenter()));
            user.sendSystemMessage(Component.literal(" " + sp.getName().getString()).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("  " + (base == null ? "?" : base.displayName())).withColor(base == null ? 0xFFFFFF : base.color() & 0xFFFFFF))
                    .append(Component.literal("  Lv " + d.level() + "  " + dist + "m").withStyle(ChatFormatting.GRAY)));
            found++;
        }
        if (found == 0) user.sendSystemMessage(Component.literal(" No mutants detected.").withStyle(ChatFormatting.GRAY));
        return InteractionResult.SUCCESS;
    }
}
