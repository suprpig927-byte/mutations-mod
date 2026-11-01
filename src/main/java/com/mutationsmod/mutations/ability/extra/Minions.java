package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ability.Powers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Summoned allies (spirit wolves, raised dead, bees, golems...). They hunt hostile mobs near their owner,
 * never hurt players, and vanish when their time is up. Tagged so stray ones are removed on reload.
 */
public final class Minions {
    private Minions() {}

    public static final String TAG = "mutations_minion";

    private record Minion(Mob mob, UUID owner, long until) {}
    private static final Map<UUID, Minion> MINIONS = new HashMap<>();

    public static <T extends Mob> T spawn(ServerPlayer owner, EntityType<T> type, Vec3 at, int ticks, String name) {
        ServerLevel level = (ServerLevel) owner.level();
        T mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) return null;
        mob.snapTo(at.x, at.y, at.z, owner.getYRot(), 0);
        mob.addTag(TAG);
        mob.setPersistenceRequired();
        if (name != null) {
            mob.setCustomName(Component.literal(name).withStyle(s -> s.withColor(0xA0E0FF)));
            mob.setCustomNameVisible(false);
        }
        mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, ticks, 0, false, false));
        MINIONS.put(mob.getUUID(), new Minion(mob, owner.getUUID(), level.getGameTime() + ticks));
        level.addFreshEntity(mob);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.8, at.z, 12, 0.3, 0.5, 0.3, 0.03);
        return mob;
    }

    public static boolean isMinion(Entity e) {
        return MINIONS.containsKey(e.getUUID());
    }

    public static UUID ownerOf(Entity e) {
        Minion m = MINIONS.get(e.getUUID());
        return m == null ? null : m.owner;
    }

    public static List<Mob> of(ServerPlayer owner) {
        List<Mob> out = new ArrayList<>();
        for (Minion m : MINIONS.values()) if (m.owner.equals(owner.getUUID()) && m.mob.isAlive()) out.add(m.mob);
        return out;
    }

    public static int count(ServerPlayer owner, EntityType<?> type) {
        int n = 0;
        for (Minion m : MINIONS.values()) if (m.owner.equals(owner.getUUID()) && m.mob.isAlive() && (type == null || m.mob.getType() == type)) n++;
        return n;
    }

    /** Sends every minion of {@code owner} after {@code target}. */
    public static void attack(ServerPlayer owner, LivingEntity target) {
        for (Mob m : of(owner)) if (m != target) m.setTarget(target);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (Minion m : List.copyOf(MINIONS.values())) {
            if (m.mob.level() != level) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(m.owner);
            if (!m.mob.isAlive() || now >= m.until || owner == null || owner.level() != level) {
                MINIONS.remove(m.mob.getUUID());
                if (m.mob.isAlive()) {
                    level.sendParticles(ParticleTypes.SOUL, m.mob.getX(), m.mob.getY() + 0.8, m.mob.getZ(), 10, 0.3, 0.5, 0.3, 0.03);
                    m.mob.discard();
                }
                continue;
            }
            if (now % 10 != 0) continue;
            if (m.mob.distanceToSqr(owner) > 30 * 30) {
                m.mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            }
            LivingEntity t = m.mob.getTarget();
            if (t == null || !t.isAlive() || t instanceof Player || isMinion(t) || t.distanceToSqr(owner) > 24 * 24) {
                LivingEntity best = null;
                double bd = 16 * 16;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(16),
                        e -> e.isAlive() && !(e instanceof Player) && !isMinion(e) && Powers.isHostile(e))) {
                    double ds = e.distanceToSqr(owner);
                    if (ds < bd) { bd = ds; best = e; }
                }
                m.mob.setTarget(best);
                if (best == null && m.mob.distanceToSqr(owner) > 36) m.mob.getNavigation().moveTo(owner, 1.2);
            }
        }
    }

    /** Minions never target players or each other. */
    public static void onChangeTarget(LivingChangeTargetEvent e) {
        if (!isMinion(e.getEntity())) return;
        LivingEntity t = e.getNewAboutToBeSetTarget();
        if (t instanceof Player || (t != null && isMinion(t))) e.setCanceled(true);
    }

    /** Minions don't hurt players, and players don't hurt their own minions. */
    public static void onDamage(LivingIncomingDamageEvent e) {
        Entity src = e.getSource().getEntity();
        if (src != null && isMinion(src) && (e.getEntity() instanceof Player || isMinion(e.getEntity()))) e.setCanceled(true);
        else if (src instanceof Player p && isMinion(e.getEntity()) && p.getUUID().equals(ownerOf(e.getEntity()))) e.setCanceled(true);
    }

    public static void forget(ServerPlayer owner) {
        for (Mob m : of(owner)) {
            MINIONS.remove(m.getUUID());
            m.discard();
        }
    }
}
