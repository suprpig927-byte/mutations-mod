package com.mutationsmod.mutations.mastery;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import com.mutationsmod.mutations.mutation.Stat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Mastery levels, XP, stat points, respecs and secondary mutation rolls. */
public final class Mastery {
    private Mastery() {}

    public static final int STAT_POINTS_PER_LEVEL = 3;
    /** XP for using an active ability. Passives never give XP. */
    public static final int XP_PER_ABILITY_USE = 5;
    /** Mastery levels that unlock a secondary mutation roll. */
    public static final int[] ROLL_LEVELS = {10, 25, 50};
    public static final int MAX_SECONDARIES = ROLL_LEVELS.length;
    public static final int RESPEC_COST_LEVELS = 30;
    public static final int ROLL_CHOICES = 3;

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
        if (gained > 0) announceLevelUp(player, d.level(), level);
    }

    /** Sets the mastery level directly. Stat points are adjusted for the levels gained or lost. */
    public static void setLevel(ServerPlayer player, int newLevel) {
        MutationData d = ModAttachments.get(player);
        newLevel = Math.max(1, newLevel);
        int points = Math.max(0, d.statPoints() + (newLevel - d.level()) * STAT_POINTS_PER_LEVEL);
        ModAttachments.set(player, d.withMastery(newLevel, 0, points));
    }

    public static void setStatPoints(ServerPlayer player, int points) {
        ModAttachments.set(player, ModAttachments.get(player).withStatPoints(points));
    }

    private static void announceLevelUp(ServerPlayer player, int oldLevel, int level) {
        player.sendSystemMessage(Component.literal("Mastery level " + level + "!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("  +" + (level - oldLevel) * STAT_POINTS_PER_LEVEL + " stat points").withStyle(ChatFormatting.YELLOW)));
        ServerLevel sl = (ServerLevel) player.level();
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
        for (int roll : ROLL_LEVELS) {
            if (oldLevel < roll && level >= roll) {
                player.sendSystemMessage(Component.literal("Your mutation is evolving... a secondary mutation roll is ready! Open your mutation screen (M).")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    // ------------------------------------------------------------------ stats

    public static void spendStat(ServerPlayer player, Stat stat, int amount) {
        MutationData d = ModAttachments.get(player);
        amount = Math.min(amount, d.statPoints());
        if (amount <= 0) return;
        Map<String, Integer> stats = new HashMap<>(d.stats());
        stats.merge(stat.id(), amount, Integer::sum);
        ModAttachments.set(player, d.withStats(stats).withStatPoints(d.statPoints() - amount));
    }

    /** Refunds every spent stat point. Costs 30 XP levels unless {@code free}. */
    public static boolean respec(ServerPlayer player, boolean free) {
        MutationData d = ModAttachments.get(player);
        int spent = d.totalSpentStats();
        if (spent == 0) {
            if (!free) player.sendSystemMessage(Component.literal("You haven't spent any stat points.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (!free && !player.isCreative()) {
            if (player.experienceLevel < RESPEC_COST_LEVELS) {
                player.sendSystemMessage(Component.literal("A respec costs " + RESPEC_COST_LEVELS + " XP levels.").withStyle(ChatFormatting.RED));
                return false;
            }
            player.giveExperienceLevels(-RESPEC_COST_LEVELS);
        }
        ModAttachments.set(player, d.withStats(Map.of()).withStatPoints(d.statPoints() + spent));
        player.sendSystemMessage(Component.literal("Your stats have been reset. " + spent + " points refunded.").withStyle(ChatFormatting.AQUA));
        return true;
    }

    // ------------------------------------------------------------------ secondary rolls

    public static int rollsUnlocked(int level) {
        int n = 0;
        for (int r : ROLL_LEVELS) if (level >= r) n++;
        return n;
    }

    /** Whether the player has an unused roll available right now. */
    public static boolean canRoll(MutationData d) {
        return d.base() != null && d.secondaries().size() < Math.min(MAX_SECONDARIES, rollsUnlocked(d.level()))
                && !available(d).isEmpty();
    }

    private static List<Secondary> available(MutationData d) {
        Mutation base = d.base();
        if (base == null) return List.of();
        List<Secondary> out = new ArrayList<>();
        for (Secondary s : base.secondaries()) if (!d.secondaries().contains(s.id())) out.add(s);
        return out;
    }

    /** What a Gene Splicer reroll can offer: anything you don't have, except the one being replaced. */
    public static List<Secondary> rerollPool(MutationData d, String excluded) {
        List<Secondary> pool = available(d);
        pool.removeIf(s -> s.id().equals(excluded));
        return pool;
    }

    /** Offers 3 random secondaries. If an offer is already open, it's kept (no re-rolling the offer). */
    public static void startRoll(ServerPlayer player, boolean ignoreLevel) {
        startRoll(player, ignoreLevel, null);
    }

    public static void startRoll(ServerPlayer player, boolean ignoreLevel, String excluded) {
        MutationData d = ModAttachments.get(player);
        if (!d.rollOptions().isEmpty()) return;
        if (!ignoreLevel && !canRoll(d)) return;
        List<Secondary> pool = rerollPool(d, excluded);
        if (pool.isEmpty()) return;
        Collections.shuffle(pool, new java.util.Random(player.getRandom().nextLong()));
        List<String> options = new ArrayList<>();
        for (int i = 0; i < Math.min(ROLL_CHOICES, pool.size()); i++) options.add(pool.get(i).id());
        ModAttachments.set(player, d.withRollOptions(options));
    }

    public static void chooseRoll(ServerPlayer player, String id) {
        MutationData d = ModAttachments.get(player);
        if (!d.rollOptions().contains(id)) return;
        Secondary s = Secondary.byId(id).orElse(null);
        if (s == null) return;
        List<String> secs = new ArrayList<>(d.secondaries());
        if (!secs.contains(id)) secs.add(id);
        ModAttachments.set(player, d.withSecondaries(secs).withRollOptions(List.of()));
        player.sendSystemMessage(Component.literal("Secondary mutation gained: ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(s.displayName()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                .append(Component.literal(" - " + s.description()).withStyle(ChatFormatting.GRAY)));
        ServerLevel sl = (ServerLevel) player.level();
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
    }
}
