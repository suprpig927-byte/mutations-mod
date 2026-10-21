package com.mutationsmod.mutations.mastery;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Mastery levels and XP. No level cap. */
public final class Mastery {
    private Mastery() {}

    public static final int STAT_POINTS_PER_LEVEL = 3;
    /** XP for using an active ability. Passives never give XP. */
    public static final int XP_PER_ABILITY_USE = 5;
    /** Levels where a secondary mutation roll unlocks (rolling comes in a later version). */
    public static final int[] ROLL_LEVELS = {10, 25, 50};

    /** XP needed to go from {@code level} to {@code level + 1}. */
    public static int xpToNext(int level) {
        return 50 + 25 * (level - 1);
    }

    /** XP for killing a mob: based on how tough it was. */
    public static int xpForKill(float maxHealth, boolean hostile, boolean player) {
        if (player) return 40;
        int base = Math.max(1, Math.round(maxHealth / 2f));
        return hostile ? base + 4 : Math.max(1, base / 2);
    }

    public static void addXp(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        MutationData d = ModAttachments.get(player);
        if (d.mutations().isEmpty()) return;
        int level = d.level();
        int xp = d.xp() + amount;
        int points = d.statPoints();
        int gained = 0;
        while (xp >= xpToNext(level)) {
            xp -= xpToNext(level);
            level++;
            points += STAT_POINTS_PER_LEVEL;
            gained++;
        }
        ModAttachments.set(player, d.withMastery(level, xp, points));
        if (gained > 0) announceLevelUp(player, level);
    }

    /** Sets the mastery level directly. Stat points are adjusted for the levels gained or lost. */
    public static void setLevel(ServerPlayer player, int newLevel) {
        MutationData d = ModAttachments.get(player);
        newLevel = Math.max(1, newLevel);
        int points = Math.max(0, d.statPoints() + (newLevel - d.level()) * STAT_POINTS_PER_LEVEL);
        ModAttachments.set(player, d.withMastery(newLevel, 0, points));
    }

    public static void setStatPoints(ServerPlayer player, int points) {
        MutationData d = ModAttachments.get(player);
        ModAttachments.set(player, d.withMastery(d.level(), d.xp(), points));
    }

    private static void announceLevelUp(ServerPlayer player, int level) {
        player.sendSystemMessage(Component.literal("Mastery level " + level + "!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("  +" + STAT_POINTS_PER_LEVEL + " stat points").withStyle(ChatFormatting.YELLOW)));
        for (int roll : ROLL_LEVELS) {
            if (roll == level) {
                player.sendSystemMessage(Component.literal("Your mutation is evolving... a secondary mutation roll is unlocked!")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }
}
