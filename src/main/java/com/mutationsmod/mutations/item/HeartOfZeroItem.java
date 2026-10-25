package com.mutationsmod.mutations.item;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mastery.Mastery;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Heart of Zero: absorb it to permanently gain one extra secondary mutation slot (once per player). */
public class HeartOfZeroItem extends Item {
    public HeartOfZeroItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        MutationData d = ModAttachments.get(sp);
        if (d.mutations().isEmpty()) {
            sp.sendOverlayMessage(Component.literal("The heart beats, but finds nothing in you to change.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (Mastery.heartAbsorbed(d)) {
            sp.sendOverlayMessage(Component.literal("You've already absorbed a Heart of Zero.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        ModAttachments.set(sp, d.withTimer("heart_of_zero", 1));
        if (!sp.isCreative()) player.getItemInHand(hand).shrink(1);
        sp.sendSystemMessage(Component.literal("You absorb the Heart of Zero. Your mutation can hold one more secondary mutation!")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.4f);
        Mastery.startRoll(sp, false);
        return InteractionResult.SUCCESS;
    }
}
