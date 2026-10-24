package com.mutationsmod.mutations.mastery;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Secondary;
import com.mutationsmod.mutations.registry.ModContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Gene Splicer actions. Costs are paid in Mutagen Crystals plus XP levels. */
public final class GeneSplicer {
    private GeneSplicer() {}

    public static final int RESPEC_CRYSTALS = 2, RESPEC_LEVELS = 10;
    public static final int REROLL_CRYSTALS = 4, REROLL_LEVELS = 20;

    /** The player has to be standing at a Gene Splicer. */
    private static boolean nearSplicer(ServerPlayer p) {
        BlockPos at = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-6, -4, -6), at.offset(6, 4, 6))) {
            if (p.level().getBlockState(pos).is(ModContent.GENE_SPLICER.get())) return true;
        }
        return false;
    }

    public static int crystals(net.minecraft.world.entity.player.Player p) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModContent.MUTAGEN_CRYSTAL.get())) n += s.getCount();
        }
        return n;
    }

    /** Checks and takes the cost. Creative players pay nothing. */
    private static boolean pay(ServerPlayer p, int crystals, int levels) {
        if (p.isCreative()) return true;
        if (crystals(p) < crystals || p.experienceLevel < levels) {
            p.sendSystemMessage(Component.literal("You need " + crystals + " Mutagen Crystals and " + levels + " XP levels.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        int left = crystals;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(ModContent.MUTAGEN_CRYSTAL.get())) continue;
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
        p.giveExperienceLevels(-levels);
        return true;
    }

    public static void respec(ServerPlayer p) {
        if (!nearSplicer(p)) return;
        if (ModAttachments.get(p).totalSpentStats() == 0) {
            p.sendSystemMessage(Component.literal("You haven't spent any stat points.").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!pay(p, RESPEC_CRYSTALS, RESPEC_LEVELS)) return;
        Mastery.respec(p, true);
        sound(p);
    }

    /** Removes one secondary and offers 3 new ones from your base's list (never the one you removed). */
    public static void reroll(ServerPlayer p, String secondaryId) {
        if (!nearSplicer(p)) return;
        MutationData d = ModAttachments.get(p);
        if (!d.rollOptions().isEmpty()) {
            p.sendSystemMessage(Component.literal("Choose your pending secondary mutation first.").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!d.secondaries().contains(secondaryId)) return;
        Secondary old = Secondary.byId(secondaryId).orElse(null);
        if (old == null || Mastery.rerollPool(d, secondaryId).isEmpty()) {
            p.sendSystemMessage(Component.literal("There's nothing left to splice into.").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!pay(p, REROLL_CRYSTALS, REROLL_LEVELS)) return;
        List<String> secs = new ArrayList<>(d.secondaries());
        secs.remove(secondaryId);
        ModAttachments.set(p, d.withSecondaries(secs));
        Mastery.startRoll(p, true, secondaryId);
        p.sendSystemMessage(Component.literal("You splice out " + old.displayName() + ". Choose its replacement.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        sound(p);
    }

    private static void sound(ServerPlayer p) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 1.2f);
    }
}
