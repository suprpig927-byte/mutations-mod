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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Mastery Tome: read it for two mastery levels' worth of XP (scales with your level). */
public class MasteryTomeItem extends Item {
    public MasteryTomeItem(Properties props) {
        super(props);
    }

    /** XP a tome gives at this level: enough for the next two levels. */
    public static int xpFor(int level) {
        return Mastery.xpToNext(level) + Mastery.xpToNext(level + 1);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        MutationData d = ModAttachments.get(sp);
        if (d.mutations().isEmpty()) {
            sp.sendOverlayMessage(Component.literal("The tome's knowledge means nothing to someone without a mutation.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        int xp = xpFor(d.level());
        Mastery.addXp(sp, xp);
        ItemStack stack = player.getItemInHand(hand);
        if (!sp.isCreative()) stack.shrink(1);
        sp.sendSystemMessage(Component.literal("You study the Mastery Tome. +" + xp + " mastery XP").withStyle(ChatFormatting.LIGHT_PURPLE));
        level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
        return InteractionResult.SUCCESS;
    }
}
