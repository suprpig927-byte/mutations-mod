package com.mutationsmod.mutations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything the mod remembers about one player. Immutable: every change makes a new copy,
 * which is then stored with player.setData(...) so it saves and syncs to that player's client.
 *
 * @param mutations  the player's mutation roster. Index 0 is the base mutation.
 * @param rolled     whether the first-spawn roll has happened
 * @param level      mastery level (starts at 1, no cap)
 * @param xp         mastery XP towards the next level
 * @param statPoints unspent stat points (spending comes in a later version)
 * @param cooldowns  ability id -> game time when that ability is ready again
 * @param speedTier  Phaser speed tier (0 = normal)
 */
public record MutationData(List<String> mutations, boolean rolled, int level, int xp, int statPoints,
                           Map<String, Long> cooldowns, int speedTier) {

    public static final MutationData EMPTY = new MutationData(List.of(), false, 1, 0, 0, Map.of(), 0);

    public static final MapCodec<MutationData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.listOf().fieldOf("mutations").forGetter(MutationData::mutations),
            Codec.BOOL.fieldOf("rolled").forGetter(MutationData::rolled),
            Codec.INT.fieldOf("level").forGetter(MutationData::level),
            Codec.INT.fieldOf("xp").forGetter(MutationData::xp),
            Codec.INT.fieldOf("stat_points").forGetter(MutationData::statPoints),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("cooldowns").forGetter(MutationData::cooldowns),
            Codec.INT.fieldOf("speed_tier").forGetter(MutationData::speedTier)
    ).apply(i, MutationData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MutationData> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buf, MutationData d) -> {
                buf.writeVarInt(d.mutations.size());
                for (String m : d.mutations) buf.writeUtf(m);
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
            },
            buf -> {
                int n = buf.readVarInt();
                List<String> muts = new ArrayList<>(n);
                for (int j = 0; j < n; j++) muts.add(buf.readUtf());
                boolean rolled = buf.readBoolean();
                int level = buf.readVarInt();
                int xp = buf.readVarInt();
                int points = buf.readVarInt();
                int c = buf.readVarInt();
                Map<String, Long> cds = new HashMap<>();
                for (int j = 0; j < c; j++) cds.put(buf.readUtf(), buf.readLong());
                int tier = buf.readVarInt();
                return new MutationData(muts, rolled, level, xp, points, cds, tier);
            });

    public MutationData {
        mutations = List.copyOf(mutations);
        cooldowns = Map.copyOf(cooldowns);
        level = Math.max(1, level);
        xp = Math.max(0, xp);
        statPoints = Math.max(0, statPoints);
        speedTier = Math.max(0, speedTier);
    }

    // ---- queries ----

    public List<Mutation> mutationList() {
        List<Mutation> out = new ArrayList<>();
        for (String id : mutations) Mutation.byId(id).ifPresent(out::add);
        return out;
    }

    public boolean has(Mutation m) {
        return mutations.contains(m.id());
    }

    /** All active abilities across the roster, in order. Keybind "Ability N" uses entry N-1. */
    public List<ActiveAbility> actives() {
        List<ActiveAbility> out = new ArrayList<>();
        for (Mutation m : mutationList()) out.addAll(m.actives());
        return out;
    }

    public long readyAt(ActiveAbility a) {
        return cooldowns.getOrDefault(a.id(), 0L);
    }

    // ---- copy-with helpers ----

    public MutationData withMutations(List<String> newMutations) {
        return new MutationData(newMutations, rolled, level, xp, statPoints, cooldowns, speedTier);
    }

    public MutationData withRolled(boolean r) {
        return new MutationData(mutations, r, level, xp, statPoints, cooldowns, speedTier);
    }

    public MutationData withMastery(int newLevel, int newXp, int newPoints) {
        return new MutationData(mutations, rolled, newLevel, newXp, newPoints, cooldowns, speedTier);
    }

    public MutationData withCooldown(ActiveAbility a, long readyAtTime) {
        Map<String, Long> m = new HashMap<>(cooldowns);
        m.put(a.id(), readyAtTime);
        return new MutationData(mutations, rolled, level, xp, statPoints, m, speedTier);
    }

    public MutationData withoutCooldowns() {
        return new MutationData(mutations, rolled, level, xp, statPoints, Map.of(), speedTier);
    }

    public MutationData withSpeedTier(int tier) {
        return new MutationData(mutations, rolled, level, xp, statPoints, cooldowns, tier);
    }
}
