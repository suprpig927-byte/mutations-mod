package com.mutationsmod.mutations.debug;

import com.mojang.authlib.GameProfile;
import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.ability.extra.ExtraPower;
import com.mutationsmod.mutations.ability.extra.ExtraPowers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import com.mutationsmod.mutations.registry.ModWeapons;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * Ops-only smoke test for the 18 v0.19 mutations: each gets a fake player with every secondary, a row of zombies,
 * and then every active, the ultimate, the weapon and a few hooks are run. Exceptions and results are logged as T18.
 */
public final class Test18 {
    private Test18() {}

    private record Run(Mutation m, FakePlayer p, List<Zombie> zs, int step, String last) {}
    private static final List<Run> RUNS = new ArrayList<>();
    private static long nextStep = 0;

    public static void start(ServerLevel level, BlockPos o) {
        RUNS.clear();
        int k = 0;
        for (Mutation m : Mutation.values()) {
            if (ExtraPowers.of(m) == null) continue;
            int x = o.getX() + (k % 6) * 14, z = o.getZ() + (k / 6) * 16;
            k++;
            try {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                for (int dx = -4; dx <= 4; dx++) for (int dz = -3; dz <= 10; dz++) {
                    level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.STONE.defaultBlockState(), 2);
                    for (int dy = 0; dy <= 4; dy++) level.setBlock(new BlockPos(x + dx, y + dy, z + dz), Blocks.AIR.defaultBlockState(), 2);
                }
                String name = "T18_" + m.id();
                if (name.length() > 16) name = name.substring(0, 16);
                FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()), name));
                p.getInventory().clearContent();
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                p.snapTo(x + 0.5, y, z + 0.5, 0, 0);
                p.setHealth(p.getMaxHealth());
                List<String> secs = Secondary.forBase(m).stream().map(Secondary::id).toList();
                ModAttachments.set(p, MutationData.EMPTY.withMutations(List.of(m.id())).withRolled(true).withMastery(80, 0, 0).withSecondaries(secs)
                        .withTimer("m_radiance", 100).withTimer("m_souls", 10).withTimer("m_revenue", 100));
                p.getInventory().add(new ItemStack(Items.IRON_NUGGET, 32));
                List<Zombie> zs = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    Zombie zb = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
                    zb.snapTo(x + 0.5 + (i - 1) * 1.5, y, z + 4.5, 180, 0);
                    zb.setNoAi(true);
                    zb.setPersistenceRequired();
                    zb.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                    level.addFreshEntity(zb);
                    zs.add(zb);
                }
                RUNS.add(new Run(m, p, zs, 0, "-"));
            } catch (Throwable t) {
                MutationsMod.LOGGER.error("T18 {} SETUP CRASHED", m, t);
            }
        }
        nextStep = level.getGameTime() + 5;
        MutationsMod.LOGGER.info("T18 started {} runs", RUNS.size());
    }

    private static String hp(List<Zombie> zs) {
        StringBuilder sb = new StringBuilder();
        for (Zombie z : zs) sb.append(z.isAlive() ? String.format("%.1f", z.getHealth()) : "dead").append(' ');
        return sb.toString().trim();
    }

    private static void heal(List<Zombie> zs) {
        for (Zombie z : zs) if (z.isAlive()) { z.setHealth(z.getMaxHealth()); z.setDeltaMovement(0, 0, 0); }
    }

    /** Every 20 ticks each run does its next step; ticks power hooks every tick in between. */
    public static void tick(ServerLevel level) {
        if (RUNS.isEmpty()) return;
        long now = level.getGameTime();
        for (Run r : RUNS) {
            try { ExtraPowers.tick(r.p(), ModAttachments.get(r.p())); }
            catch (Throwable t) { MutationsMod.LOGGER.error("T18 {} TICK CRASHED", r.m(), t); }
        }
        if (now < nextStep) return;
        nextStep = now + 20;
        List<Run> next = new ArrayList<>();
        for (Run r : RUNS) {
            FakePlayer p = r.p();
            Mutation m = r.m();
            ExtraPower x = ExtraPowers.of(m);
            List<ActiveAbility> all = new ArrayList<>(m.actives());
            for (Secondary s : Secondary.forBase(m)) if (s.active() != null) all.add(s.active());
            int step = r.step();
            String what = "?";
            MutationsMod.LOGGER.info("T18 {} RESULT {} -> zombies=[{}] me={}", m, r.last(), hp(r.zs()), p.getHealth());
            heal(r.zs());
            try {
                // face the middle zombie
                Zombie target = r.zs().get(1).isAlive() ? r.zs().get(1) : r.zs().stream().filter(LivingEntity::isAlive).findFirst().orElse(null);
                if (target != null) p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
                MutationData d = ModAttachments.get(p);
                if (step < all.size()) {
                    ActiveAbility a = all.get(step);
                    what = a.name();
                    String why = x.blocked(p, d);
                    boolean ok = why == null && ExtraPowers.use(p, d, a);
                    MutationsMod.LOGGER.info("T18 {} {} used={} blocked={} zombies=[{}] hp={}", m, a, ok, why, hp(r.zs()), p.getHealth());
                } else if (step == all.size()) {
                    what = "ULT";
                    boolean ok = ExtraPowers.ultimate(p, d, m);
                    MutationsMod.LOGGER.info("T18 {} ULTIMATE used={} zombies=[{}]", m, ok, hp(r.zs()));
                } else if (step == all.size() + 1) {
                    what = "WEAPON";
                    ItemStack w = new ItemStack(ModWeapons.BY_MUTATION.get(m).get());
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, w);
                    boolean ok = ExtraPowers.weaponUse(p, d, m, w);
                    boolean hit = target != null && target.hurtServer(level, level.damageSources().playerAttack(p), 6f);
                    MutationsMod.LOGGER.info("T18 {} WEAPON {} use={} hit={} zombies=[{}]", m, w.getHoverName().getString(), ok, hit, hp(r.zs()));
                } else if (step == all.size() + 2) {
                    what = "KILL";
                    if (target != null) {
                        int inv = target.hurtTime;
                        boolean ok = target.hurtServer(level, level.damageSources().playerAttack(p), 1000f);
                        MutationsMod.LOGGER.info("T18 {} KILL ok={} inv={} invuln={} removed={} hp={} dist={}", m, ok, inv, target.isInvulnerable(), target.isRemoved(), target.getHealth(), target.distanceTo(p));
                    }
                    p.hurtServer(level, level.damageSources().generic(), 3f);
                    MutationsMod.LOGGER.info("T18 {} after kill+hurt hp={} timers={}", m, p.getHealth(), ModAttachments.get(p).toString().length());
                } else {
                    ExtraPowers.reset(p);
                    for (Zombie z : r.zs()) z.discard();
                    MutationsMod.LOGGER.info("T18 {} DONE", m);
                    continue;
                }
            } catch (Throwable t) {
                MutationsMod.LOGGER.error("T18 {} step {} ({}) CRASHED", m, step, what, t);
            }
            next.add(new Run(m, p, r.zs(), step + 1, what));
        }
        RUNS.clear();
        RUNS.addAll(next);
        if (RUNS.isEmpty()) MutationsMod.LOGGER.info("T18 ALL DONE");
    }
}
