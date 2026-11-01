package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Suppression;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Landlord: own chunks of land and be powerful on them. Homesick away from them. */
public class LandlordPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.LANDLORD; }

    static final DustParticleOptions BORDER = new DustParticleOptions(0xE0B040, 1.4f);
    static final long OFF = 1_000_000_000L;

    // ------------------------------------------------------------------ claims

    static int maxClaims(ServerPlayer p) { return Catalysts.holding(p, Mutation.LANDLORD) ? 6 : 3; }

    /** Each claim is {chunkX, chunkZ, claimedAt}. */
    public static List<long[]> claims(MutationData d) {
        List<long[]> out = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            long x = d.timer("claim" + i + "x");
            if (x == 0) continue;
            out.add(new long[]{x - OFF, d.timer("claim" + i + "z") - OFF, d.timer("claim" + i + "t"), i});
        }
        return out;
    }

    static boolean claimed(MutationData d, int cx, int cz) {
        for (long[] c : claims(d)) if (c[0] == cx && c[1] == cz) return true;
        return false;
    }

    static boolean inClaim(MutationData d, Level level, BlockPos pos) {
        return level.dimension() == Level.OVERWORLD && claimed(d, pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean onProperty(ServerPlayer p, MutationData d) {
        return Powers.timerActive(d, p.level(), "foreclosure") || inClaim(d, p.level(), p.blockPosition());
    }

    static boolean onPropertyAt(ServerPlayer p, MutationData d, Entity at) {
        if (Powers.timerActive(d, p.level(), "foreclosure") && at.distanceToSqr(p) < 24 * 24) return true;
        return inClaim(d, at.level(), at.blockPosition());
    }

    private static boolean homesick(ServerPlayer p, MutationData d) {
        List<long[]> cs = claims(d);
        if (cs.isEmpty() || d.has(Secondary.REMOTE_LANDLORD)) return false;
        if (p.level().dimension() != Level.OVERWORLD) return true;
        for (long[] c : cs) {
            double dx = c[0] * 16 + 8 - p.getX(), dz = c[1] * 16 + 8 - p.getZ();
            if (dx * dx + dz * dz <= 72 * 72) return false;
        }
        return true;
    }

    static Vec3 chunkCenter(ServerLevel level, long cx, long cz) {
        int x = (int) cx * 16 + 8, z = (int) cz * 16 + 8;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    static void showBorder(ServerLevel level, ServerPlayer p, long cx, long cz) {
        double y = p.getY() + 0.3;
        for (int i = 0; i <= 16; i++) {
            double a = cx * 16 + i, b = cz * 16 + i;
            for (double[] q : new double[][]{{a, cz * 16}, {a, cz * 16 + 16}, {cx * 16, b}, {cx * 16 + 16, b}}) {
                level.sendParticles(p, BORDER, true, false, q[0], y, q[1], 2, 0, 0.6, 0, 0);
            }
        }
    }

    static void evict(ServerPlayer p, LivingEntity e, double strength) {
        Vec3 dir = e.position().subtract(p.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() < 0.01) dir = p.getLookAngle().multiply(1, 0, 1);
        Powers.impulse(e, dir.normalize().scale(strength).add(0, 0.6, 0), true);
        Powers.particles(e, ParticleTypes.CLOUD, 10, 0.4, 0.05);
    }

    // ------------------------------------------------------------------ hooks

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        ServerLevel level = (ServerLevel) p.level();
        List<long[]> cs = claims(d);
        Powers.add(p, Attributes.MAX_HEALTH, "property_value", d.has(Secondary.PROPERTY_VALUE) ? 2.0 * cs.size() : 0);
        boolean home = onProperty(p, d);
        boolean fore = Powers.timerActive(d, level, "foreclosure");
        if (now % 20 == 0) {
            if (home) {
                Powers.effect(p, MobEffects.STRENGTH, 40, fore ? 1 : 0);
                Powers.effect(p, MobEffects.RESISTANCE, 40, fore ? 1 : 0);
                if (now % 60 == 0 && p.getHealth() < p.getMaxHealth()) p.heal(1f);
            } else if (homesick(p, d)) {
                Powers.effect(p, MobEffects.WEAKNESS, 40, 0);
            }
        }
        // Trespassers glow; banned mobs get pushed out.
        if (now % 10 == 0 && level.dimension() == Level.OVERWORLD && !cs.isEmpty()) {
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48), m -> m instanceof Enemy && !Minions.isMinion(m) && inClaim(d, level, m.blockPosition()))) {
                if (now % 40 == 0) Kit.glow(m, 50);
                if (Marks.has(m, "banned")) {
                    long[] c = null;
                    for (long[] q : cs) if (q[0] == m.blockPosition().getX() >> 4 && q[1] == m.blockPosition().getZ() >> 4) c = q;
                    if (c != null) {
                        Vec3 out = m.position().subtract(c[0] * 16 + 8, m.getY(), c[1] * 16 + 8).multiply(1, 0, 1);
                        if (out.lengthSqr() < 0.01) out = new Vec3(1, 0, 0);
                        Powers.impulse(m, out.normalize().scale(0.8).add(0, 0.3, 0), true);
                    }
                }
            }
        }
        // Rent Due: hostiles on your Property take damage each second.
        if (Powers.timerActive(d, level, "rent_due") && now % 20 == 0) {
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64), m -> m instanceof Enemy && !Minions.isMinion(m) && onPropertyAt(p, d, m))) {
                Powers.hurt(p, m, 2f * Powers.power(d));
                m.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, m.getX(), m.getY() + m.getBbHeight(), m.getZ(), 2, 0.2, 0.1, 0.2, 0);
            }
        }
        // Foreclosure: hostiles near you are evicted every second.
        if (fore && now % 20 == 0) {
            for (LivingEntity e : Kit.hostiles(p, 8)) evict(p, e, 1.6);
        }
        // Prime Real Estate: things grow faster on your land.
        if (d.has(Secondary.PRIME_REAL_ESTATE) && now % 40 == 0 && level.dimension() == Level.OVERWORLD) {
            for (long[] c : cs) {
                if (!level.hasChunk((int) c[0], (int) c[1])) continue;
                Vec3 cc = chunkCenter(level, c[0], c[1]);
                VerdantPower.growNear(level, BlockPos.containing(cc), 8, 4);
                for (Animal a : level.getEntitiesOfClass(Animal.class, new AABB(c[0] * 16, level.getMinY(), c[1] * 16, c[0] * 16 + 16, level.getMaxY(), c[1] * 16 + 16), Animal::isBaby)) {
                    a.ageUp(10);
                }
            }
        }
        // Rent day.
        if (!cs.isEmpty()) {
            long next = d.timer("rent_next");
            if (next == 0) Powers.setTimer(p, "rent_next", 12000);
            else if (now >= next) {
                collectRent(p, level, cs);
                Powers.setTimer(p, "rent_next", 12000);
            }
        }
    }

    static void collectRent(ServerPlayer p, ServerLevel level, List<long[]> cs) {
        int emeralds = 0, nuggets = 0;
        for (long[] c : cs) {
            if (!level.hasChunk((int) c[0], (int) c[1])) continue;
            AABB box = new AABB(c[0] * 16, level.getMinY(), c[1] * 16, c[0] * 16 + 16, level.getMaxY(), c[1] * 16 + 16);
            emeralds += level.getEntitiesOfClass(AbstractVillager.class, box).size();
            nuggets += level.getEntitiesOfClass(Animal.class, box, a -> !Minions.isMinion(a)).size() * 2;
        }
        int mult = Catalysts.holding(p, Mutation.LANDLORD) ? 2 : 1;
        emeralds = Math.min(8, emeralds) * mult;
        nuggets = Math.min(32, nuggets) * mult;
        if (emeralds + nuggets == 0) {
            p.sendSystemMessage(Component.literal("Rent day: nobody lives on your Property yet.").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (emeralds > 0) give(p, new ItemStack(Items.EMERALD, emeralds));
        if (nuggets > 0) give(p, new ItemStack(Items.GOLD_NUGGET, nuggets));
        p.sendSystemMessage(Component.literal("Rent collected: " + emeralds + " emeralds, " + nuggets + " gold nuggets.").withStyle(ChatFormatting.GOLD));
        Powers.sound(p, SoundEvents.VILLAGER_TRADE, 1f, 1f);
    }

    static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) {
            ItemEntity it = new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), s);
            p.level().addFreshEntity(it);
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (!onProperty(p, d)) return;
        if (d.has(Secondary.HOME_FIELD)) e.setAmount(e.getAmount() * 0.75f);
        if (d.has(Secondary.SECURITY_DEPOSIT) && e.getSource().getEntity() instanceof Mob m) Marks.set(m, "deposit", p.level().getGameTime() + 1200, p.getId());
    }

    @Override
    public void onDrops(LivingDropsEvent e, ServerPlayer p, MutationData d) {
        LivingEntity v = e.getEntity();
        if (!(Marks.has(v, "deposit") && Marks.value(v, "deposit") == p.getId())) return;
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity it : e.getDrops()) {
            ItemEntity copy = new ItemEntity(v.level(), it.getX(), it.getY(), it.getZ(), it.getItem().copy());
            copy.setDefaultPickUpDelay();
            extra.add(copy);
        }
        e.getDrops().addAll(extra);
    }

    @Override
    public float cooldownFactor(ServerPlayer p, MutationData d, ActiveAbility a) {
        return homesick(p, d) ? 1.25f : 1f;
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        long now = level.getGameTime();
        switch (a) {
            case STAKE_CLAIM -> {
                if (level.dimension() != Level.OVERWORLD) {
                    p.sendOverlayMessage(Component.literal("You can only claim land in the Overworld.").withStyle(ChatFormatting.GOLD));
                    return false;
                }
                int cx = p.blockPosition().getX() >> 4, cz = p.blockPosition().getZ() >> 4;
                if (claimed(d, cx, cz)) {
                    showBorder(level, p, cx, cz);
                    p.sendOverlayMessage(Component.literal("You already own this chunk.").withStyle(ChatFormatting.GOLD));
                    return false;
                }
                List<long[]> cs = claims(d);
                int slot = -1;
                if (cs.size() >= maxClaims(p)) {
                    long[] oldest = cs.stream().min(java.util.Comparator.comparingLong(c -> c[2])).get();
                    slot = (int) oldest[3];
                } else {
                    for (int i = 0; i < 6 && slot < 0; i++) if (d.timer("claim" + i + "x") == 0) slot = i;
                }
                // Any claim beyond the current max (catalyst removed) gets dropped too.
                MutationData nd = d.withTimer("claim" + slot + "x", cx + OFF).withTimer("claim" + slot + "z", cz + OFF).withTimer("claim" + slot + "t", now);
                ModAttachments.set(p, nd);
                showBorder(level, p, cx, cz);
                p.sendOverlayMessage(Component.literal("Claimed chunk " + cx + ", " + cz + " (" + claims(nd).size() + "/" + maxClaims(p) + ").").withStyle(ChatFormatting.GOLD));
                Powers.sound(p, SoundEvents.ANVIL_USE, 1f, 1.2f);
                return true;
            }
            case EVICTION -> {
                LivingEntity t = Kit.look(p, 16);
                if (t == null) return false;
                boolean home = onPropertyAt(p, d, t);
                evict(p, t, home ? 3.2 : 1.6);
                if (home) Marks.set(t, "banned", now + 600, p.getId());
                Powers.sound(p, SoundEvents.VILLAGER_NO, 1.5f, 0.8f);
                return true;
            }
            case RENT_DUE -> {
                if (claims(d).isEmpty() && !Powers.timerActive(d, level, "foreclosure")) {
                    p.sendOverlayMessage(Component.literal("You don't own any land yet.").withStyle(ChatFormatting.GOLD));
                    return false;
                }
                Powers.setTimer(p, "rent_due", 200);
                Powers.sound(p, SoundEvents.BELL_BLOCK, 1.5f, 0.8f);
                return true;
            }
            case RETURN_HOME -> {
                List<long[]> cs = claims(d);
                if (cs.isEmpty()) { p.sendOverlayMessage(Component.literal("You don't own any land yet.").withStyle(ChatFormatting.GOLD)); return false; }
                ServerLevel over = level.getServer().overworld();
                long[] best = cs.get(0);
                double bd = Double.MAX_VALUE;
                if (level == over) for (long[] c : cs) {
                    double dx = c[0] * 16 + 8 - p.getX(), dz = c[1] * 16 + 8 - p.getZ();
                    if (dx * dx + dz * dz < bd) { bd = dx * dx + dz * dz; best = c; }
                }
                over.getChunk((int) best[0], (int) best[1]);
                Vec3 to = chunkCenter(over, best[0], best[1]);
                Powers.particles(p, ParticleTypes.PORTAL, 40, 0.5, 0.5);
                if (level != over) p.teleportTo(over, to.x, to.y, to.z, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
                else Powers.teleport(p, to);
                Powers.sound(p, SoundEvents.ENDERMAN_TELEPORT, 1f, 1f);
                return true;
            }
            case HIRE_HELP -> {
                for (int i = 0; i < 2; i++) {
                    IronGolem g = Minions.spawn(p, EntityTypes.IRON_GOLEM, Kit.groundNear(level, p.position(), 3, p.getRandom()), 1200, "Groundskeeper");
                    if (g != null) g.setPlayerCreated(true);
                }
                Powers.sound(p, SoundEvents.IRON_GOLEM_REPAIR, 1.5f, 1f);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "foreclosure", 400);
        ServerLevel level = (ServerLevel) p.level();
        for (int i = 0; i < 64; i++) {
            double a = i * Math.PI / 32;
            level.sendParticles(BORDER, p.getX() + Math.cos(a) * 24, p.getY() + 0.5, p.getZ() + Math.sin(a) * 24, 3, 0, 0.6, 0, 0);
        }
        Powers.sound(p, SoundEvents.BELL_BLOCK, 2f, 0.5f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (onPropertyAt(p, d, target)) e.setAmount(e.getAmount() * 1.5f);
        if (Powers.timerActive(d, p.level(), "key_evict")) {
            Powers.clearTimer(p, "key_evict");
            Powers.later((ServerLevel) p.level(), 1, () -> evict(p, target, onPropertyAt(p, d, target) ? 3.2 : 1.6));
        }
    }

    @Override
    public boolean weaponUse(ServerPlayer p, MutationData d, ItemStack stack) {
        if (Powers.timerActive(d, p.level(), "key_cd")) return false;
        Powers.setTimer(p, "key_evict", 100);
        Powers.setTimer(p, "key_cd", 100);
        Powers.sound(p, SoundEvents.CHAIN_PLACE, 1.5f, 1.4f);
        p.sendOverlayMessage(Component.literal("Your next hit evicts.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.add(p, Attributes.MAX_HEALTH, "property_value", 0);
    }

    /** Gated Community: no natural hostile spawns on a Landlord's Property. */
    public static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        if (!(e.getEntity() instanceof Enemy) || e.getSpawnType() != EntitySpawnReason.NATURAL) return;
        Level level = e.getLevel().getLevel();
        if (level.dimension() != Level.OVERWORLD) return;
        BlockPos pos = BlockPos.containing(e.getX(), e.getY(), e.getZ());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            MutationData d = ModAttachments.get(p);
            if (d.has(Mutation.LANDLORD) && d.has(Secondary.GATED_COMMUNITY) && !Suppression.active(d, p.level()) && claimed(d, pos.getX() >> 4, pos.getZ() >> 4)) {
                e.setSpawnCancelled(true);
                return;
            }
        }
    }
}
