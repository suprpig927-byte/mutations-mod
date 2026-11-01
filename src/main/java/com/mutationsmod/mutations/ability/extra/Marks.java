package com.mutationsmod.mutations.ability.extra;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;

/** Timed labels on any entity (levied, rooted, banned...). Not saved: they last seconds. */
public final class Marks {
    private Marks() {}

    private static final Map<UUID, Map<String, long[]>> MARKS = new HashMap<>();

    /** Marks {@code e} with {@code key} until {@code until} (game time); {@code value} is free data (owner hash, stacks...). */
    public static void set(Entity e, String key, long until, long value) {
        MARKS.computeIfAbsent(e.getUUID(), k -> new HashMap<>()).put(key, new long[]{until, value});
    }

    public static boolean has(Entity e, String key) {
        long[] m = get(e, key);
        return m != null;
    }

    public static long[] get(Entity e, String key) {
        Map<String, long[]> m = MARKS.get(e.getUUID());
        if (m == null) return null;
        long[] v = m.get(key);
        if (v == null) return null;
        if (v[0] < e.level().getGameTime()) {
            m.remove(key);
            return null;
        }
        return v;
    }

    public static long value(Entity e, String key) {
        long[] v = get(e, key);
        return v == null ? 0 : v[1];
    }

    public static void clear(Entity e, String key) {
        Map<String, long[]> m = MARKS.get(e.getUUID());
        if (m != null) m.remove(key);
    }

    /** Drops expired entries now and then. */
    public static void sweep(long now) {
        Iterator<Map.Entry<UUID, Map<String, long[]>>> it = MARKS.entrySet().iterator();
        while (it.hasNext()) {
            Map<String, long[]> m = it.next().getValue();
            m.values().removeIf(v -> v[0] < now);
            if (m.isEmpty()) it.remove();
        }
    }
}
