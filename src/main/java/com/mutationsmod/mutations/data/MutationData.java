package com.mutationsmod.mutations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import com.mutationsmod.mutations.mutation.Stat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything the mod remembers about one player. Immutable: every change makes a new copy, which
 * is stored with player.setData(...) so it saves and syncs to that player's client.
 *
 * @param mutations   base mutations. Index 0 is the rolled base; commands can add more.
 * @param rolled      whether the first-spawn roll has happened
 * @param level       mastery level (starts at 1, no cap)
 * @param xp          mastery XP towards the next level
 * @param statPoints  unspent stat points
 * @param cooldowns   ability id -> game time it's ready; also "t:..." timers (see {@link #timer})
 * @param speedTier   Phaser speed tier (0 = normal)
 * @param secondaries secondary mutation ids, in the order they were rolled
 * @param stats       stat id -> points spent
 * @param rollOptions the 3 secondary options currently on offer (empty = no roll in progress)
 * @param charge      Absorber's stored charge
 */
public record MutationData(List<String> mutations, boolean rolled, int level, int xp, int statPoints,
                           Map<String, Long> cooldowns, int speedTier, List<String> secondaries,
                           Map<String, Integer> stats, List<String> rollOptions, float charge) {

    public static final MutationData EMPTY = new MutationData(List.of(), false, 1, 0, 0, Map.of(), 0,
            List.of(), Map.of(), List.of(), 0f);

    // Newer fields are optional so worlds from older versions still load.
    public static final MapCodec<MutationData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.listOf().fieldOf("mutations").forGetter(MutationData::mutations),
            Codec.BOOL.fieldOf("rolled").forGetter(MutationData::rolled),
            Codec.INT.fieldOf("level").forGetter(MutationData::level),
            Codec.INT.fieldOf("xp").forGetter(MutationData::xp),
            Codec.INT.fieldOf("stat_points").forGetter(MutationData::statPoints),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("cooldowns").forGetter(MutationData::cooldowns),
            Codec.INT.fieldOf("speed_tier").forGetter(MutationData::speedTier),
            Codec.STRING.listOf().optionalFieldOf("secondaries", List.of()).forGetter(MutationData::secondaries),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("stats", Map.of()).forGetter(MutationData::stats),
            Codec.STRING.listOf().optionalFieldOf("roll_options", List.of()).forGetter(MutationData::rollOptions),
            Codec.FLOAT.optionalFieldOf("charge", 0f).forGetter(MutationData::charge)
    ).apply(i, MutationData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MutationData> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buf, MutationData d) -> {
                writeList(buf, d.mutations);
                buf.writeBoolean(d.rolled);
                buf.writeVarInt(d.level);
                buf.writeVarInt(d.xp);
                buf.writeVarInt(d.statPoints);
                buf.writeVarInt(d.cooldowns.size());
                d.cooldowns.forEach((k, v) -> {
                    buf.writeUtf(k);
                    buf.writeLong(v);
                });
                buf.writeVarInt(d.speedTier);
                writeList(buf, d.secondaries);
                buf.writeVarInt(d.stats.size());
                d.stats.forEach((k, v) -> {
                    buf.writeUtf(k);
                    buf.writeVarInt(v);
                });
                writeList(buf, d.rollOptions);
                buf.writeFloat(d.charge);
            },
            buf -> {
                List<String> muts = readList(buf);
                boolean rolled = buf.readBoolean();
                int level = buf.readVarInt();
                int xp = buf.readVarInt();
                int points = buf.readVarInt();
                int c = buf.readVarInt();
                Map<String, Long> cds = new HashMap<>();
                for (int j = 0; j < c; j++) cds.put(buf.readUtf(), buf.readLong());
                int tier = buf.readVarInt();
                List<String> secs = readList(buf);
                int s = buf.readVarInt();
                Map<String, Integer> stats = new HashMap<>();
                for (int j = 0; j < s; j++) stats.put(buf.readUtf(), buf.readVarInt());
                List<String> options = readList(buf);
                float charge = buf.readFloat();
                return new MutationData(muts, rolled, level, xp, points, cds, tier, secs, stats, options, charge);
            });

    private static void writeList(RegistryFriendlyByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeUtf(s);
    }

    private static List<String> readList(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<String> out = new ArrayList<>(n);
        for (int j = 0; j < n; j++) out.add(buf.readUtf());
        return out;
    }

    public MutationData {
        mutations = List.copyOf(mutations);
        cooldowns = Map.copyOf(cooldowns);
        secondaries = List.copyOf(secondaries);
        stats = Map.copyOf(stats);
        rollOptions = List.copyOf(rollOptions);
        level = Math.max(1, level);
        xp = Math.max(0, xp);
        statPoints = Math.max(0, statPoints);
        speedTier = Math.max(0, speedTier);
        charge = Math.max(0f, charge);
    }

    // ------------------------------------------------------------------ queries

    public List<Mutation> mutationList() {
        List<Mutation> out = new ArrayList<>();
        for (String id : mutations) Mutation.byId(id).ifPresent(out::add);
        return out;
    }

    public Mutation base() {
        List<Mutation> list = mutationList();
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean has(Mutation m) {
        return mutations.contains(m.id());
    }

    public boolean has(Secondary s) {
        return secondaries.contains(s.id()) && has(s.base());
    }

    public List<Secondary> secondaryList() {
        List<Secondary> out = new ArrayList<>();
        for (String id : secondaries) Secondary.byId(id).ifPresent(out::add);
        return out;
    }

    /** All active abilities: base actives first, then secondary actives. "Ability N" uses entry N-1. */
    public List<ActiveAbility> actives() {
        List<ActiveAbility> out = new ArrayList<>();
        for (Mutation m : mutationList()) out.addAll(m.actives());
        for (Secondary s : secondaryList()) {
            if (s.active() != null && has(s.base())) out.add(s.active());
        }
        return out;
    }

    public int stat(Stat s) {
        return stats.getOrDefault(s.id(), 0);
    }

    public int totalSpentStats() {
        int t = 0;
        for (int v : stats.values()) t += v;
        return t;
    }

    public long readyAt(ActiveAbility a) {
        return cooldowns.getOrDefault(a.id(), 0L);
    }

    /** Named timers share the cooldown map under a "t:" prefix. */
    public long timer(String name) {
        return cooldowns.getOrDefault("t:" + name, 0L);
    }

    // ------------------------------------------------------------------ copy-with helpers

    public MutationData withMutations(List<String> v) {
        return new MutationData(v, rolled, level, xp, statPoints, cooldowns, speedTier, secondaries, stats, rollOptions, charge);
    }

    public MutationData withRolled(boolean v) {
        return new MutationData(mutations, v, level, xp, statPoints, cooldowns, speedTier, secondaries, stats, rollOptions, charge);
    }

    public MutationData withMastery(int newLevel, int newXp, int newPoints) {
        return new MutationData(mutations, rolled, newLevel, newXp, newPoints, cooldowns, speedTier, secondaries, stats, rollOptions, charge);
    }

    public MutationData withStatPoints(int v) {
        return new MutationData(mutations, rolled, level, xp, v, cooldowns, speedTier, secondaries, stats, rollOptions, charge);
    }

    public MutationData withCooldown(ActiveAbility a, long readyAtTime) {
        return withCooldownKey(a.id(), readyAtTime);
    }

    public MutationData withTimer(String name, long time) {
        return withCooldownKey("t:" + name, time);
    }

    private MutationData withCooldownKey(String key, long value) {
        Map<String, Long> m = new HashMap<>(cooldowns);
        m.put(key, value);
        return new MutationData(mutations, rolled, level, xp, statPoints, m, speedTier, secondaries, stats, rollOptions, charge);
    }

    /** Clears ability cooldowns but keeps timers. */
    public MutationData withoutCooldowns() {
        Map<String, Long> m = new HashMap<>();
        cooldowns.forEach((k, v) -> {
            if (k.startsWith("t:")) m.put(k, v);
        });
        return new MutationData(mutations, rolled, level, xp, statPoints, m, speedTier, secondaries, stats, rollOptions, charge);
    }

    public MutationData withSpeedTier(int v) {
        return new MutationData(mutations, rolled, level, xp, statPoints, cooldowns, v, secondaries, stats, rollOptions, charge);
    }

    public MutationData withSecondaries(List<String> v) {
        return new MutationData(mutations, rolled, level, xp, statPoints, cooldowns, speedTier, v, stats, rollOptions, charge);
    }

    public MutationData withStats(Map<String, Integer> v) {
        return new MutationData(mutations, rolled, level, xp, statPoints, cooldowns, speedTier, secondaries, v, rollOptions, charge);
    }

    public MutationData withRollOptions(List<String> v) {
        return new MutationData(mutations, rolled, level, xp, statPoints, cooldowns, speedTier, secondaries, stats, v, charge);
    }

    public MutationData withCharge(float v) {
        return new MutationData(mutations, rolled, level, xp, statPoints, cooldowns, speedTier, secondaries, stats, rollOptions, v);
    }
}
