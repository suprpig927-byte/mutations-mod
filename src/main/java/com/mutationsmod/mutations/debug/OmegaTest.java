package com.mutationsmod.mutations.debug;

import com.mojang.authlib.GameProfile;
import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.ability.Omega;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Ultimate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Ops-only test harness: fires every Omega ultimate from a fake player next to a few zombies and logs what happened. */
public final class OmegaTest {
    private OmegaTest() {}

    private record Run(Ultimate u, FakePlayer p, List<Zombie> mobs, long start) {}
    private static final List<Run> RUNS = new ArrayList<>();

    public static int start(ServerLevel level, BlockPos origin, Ultimate only) {
        int n = 0;
        for (Ultimate u : Ultimate.values()) {
            if (only != null && u != only) continue;
            int x = origin.getX() + n * 24, z = origin.getZ();
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(("omega-" + u).getBytes()), "Omega" + u.ordinal()));
            p.snapTo(x + 0.5, y, z + 0.5, 0, 0);
            p.setHealth(p.getMaxHealth());
            MutationData d = MutationData.EMPTY.withMutations(List.of(u.mutation().id())).withRolled(true).withMastery(80, 0, 0);
            if (u == Ultimate.OVERLOAD) d = d.withCharge(100);
            ModAttachments.set(p, d);
            List<Zombie> mobs = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Zombie zb = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
                if (zb == null) continue;
                double a = i * 2.1;
                zb.snapTo(x + 0.5 + Math.cos(a) * 4, y, z + 0.5 + Math.sin(a) * 4, 0, 0);
                zb.setPersistenceRequired();
                zb.setCustomName(net.minecraft.network.chat.Component.literal(u.name() + "_" + i));
                level.addFreshEntity(zb);
                mobs.add(zb);
            }
            boolean ok;
            try {
                ok = Omega.fire(p, ModAttachments.get(p), u);
                MutationsMod.LOGGER.info("OMEGATEST {} fired={} at {},{},{}", u, ok, x, y, z);
            } catch (Throwable t) {
                MutationsMod.LOGGER.error("OMEGATEST {} CRASHED", u, t);
                ok = false;
            }
            RUNS.add(new Run(u, p, mobs, level.getGameTime()));
            n++;
        }
        return n;
    }

    public static void tick(ServerLevel level) {
        if (RUNS.isEmpty()) return;
        long now = level.getGameTime();
        for (Run r : List.copyOf(RUNS)) {
            if (r.p().level() != level) continue;
            try {
                Omega.tick(r.p(), ModAttachments.get(r.p()));
            } catch (Throwable t) {
                MutationsMod.LOGGER.error("OMEGATEST {} TICK CRASHED", r.u(), t);
                RUNS.remove(r);
                continue;
            }
            long age = now - r.start();
            if (age == 40 || age == 200 || age == 650) {
                StringBuilder sb = new StringBuilder();
                for (Zombie zb : r.mobs()) sb.append(zb.isAlive() ? String.format("%.1f", zb.getHealth()) : "dead").append(' ');
                MutationData d = ModAttachments.get(r.p());
                MutationsMod.LOGGER.info("OMEGATEST {} t={} zombies=[{}] player={},{},{} hp={} timers={}", r.u(), age, sb.toString().trim(),
                        (int) r.p().getX(), (int) r.p().getY(), (int) r.p().getZ(), r.p().getHealth(),
                        d.cooldowns().entrySet().stream().filter(e -> e.getValue() > now).map(e -> e.getKey()).toList());
                if (r.u() == Ultimate.DOPPELGANGER) {
                    int decoys = level.getEntitiesOfClass(LivingEntity.class, r.p().getBoundingBox().inflate(30),
                            e -> e instanceof com.mutationsmod.mutations.entity.Decoy).size();
                    MutationsMod.LOGGER.info("OMEGATEST DOPPELGANGER decoys={} zombieTargets={}", decoys,
                            r.mobs().stream().map(zb -> zb.getTarget() == null ? "none" : zb.getTarget().getClass().getSimpleName()).toList());
                }
            }
            if (age >= 650) {
                for (Zombie zb : r.mobs()) zb.discard();
                Omega.forget(r.p());
                RUNS.remove(r);
                MutationsMod.LOGGER.info("OMEGATEST {} done", r.u());
            }
        }
    }
}
