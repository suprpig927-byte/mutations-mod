package com.mutationsmod.mutations.ability.extra;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Short-lived areas with a per-tick effect (acid pools, black holes, waves...). */
public final class Zones {
    private Zones() {}

    public static final class Zone {
        public final ServerLevel level;
        public Vec3 center;
        public final double radius;
        public final long start, until;
        public final UUID owner;
        public Vec3 dir = Vec3.ZERO;
        public final java.util.Set<Integer> touched = new java.util.HashSet<>();
        final Consumer<Zone> tick;
        final Consumer<Zone> end;

        Zone(ServerLevel level, Vec3 center, double radius, long start, long until, UUID owner, Consumer<Zone> tick, Consumer<Zone> end) {
            this.level = level; this.center = center; this.radius = radius; this.start = start; this.until = until;
            this.owner = owner; this.tick = tick; this.end = end;
        }

        public long age() { return level.getGameTime() - start; }
        public net.minecraft.server.level.ServerPlayer ownerPlayer() { return level.getServer().getPlayerList().getPlayer(owner); }
    }

    private static final List<Zone> ZONES = new ArrayList<>();

    public static Zone add(ServerLevel level, Vec3 center, double radius, int ticks, UUID owner, Consumer<Zone> tick, Consumer<Zone> end) {
        Zone z = new Zone(level, center, radius, level.getGameTime(), level.getGameTime() + ticks, owner, tick, end);
        ZONES.add(z);
        return z;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (Zone z : List.copyOf(ZONES)) {
            if (z.level != level) continue;
            if (now >= z.until) {
                ZONES.remove(z);
                if (z.end != null) z.end.accept(z);
            } else if (z.tick != null) {
                z.tick.accept(z);
            }
        }
    }
}
