package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Radiation: a living reactor. Hits and beams give stacking Radiation Sickness. */
public class RadiationPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.RADIATION; }

    static final DustParticleOptions GREEN = new DustParticleOptions(0x7CFF4A, 1.2f);

    /** Sick mobs: stacks, expiry, and who made them sick. */
    private record Sick(LivingEntity e, int stacks, long until, UUID owner) {}
    private static final Map<UUID, Sick> SICK = new HashMap<>();

    public static void sicken(ServerPlayer owner, LivingEntity e, int stacks) {
        if (e == owner || e instanceof net.minecraft.world.entity.player.Player pl && pl == owner) return;
        MutationData d = ModAttachments.get(owner);
        int dur = d.has(Secondary.HALF_LIFE) ? 400 : 200;
        Sick s = SICK.get(e.getUUID());
        int n = Math.min(5, (s == null ? 0 : s.stacks) + stacks);
        SICK.put(e.getUUID(), new Sick(e, n, e.level().getGameTime() + dur, owner.getUUID()));
    }

    public static int stacks(LivingEntity e) {
        Sick s = SICK.get(e.getUUID());
        return s == null || s.until < e.level().getGameTime() ? 0 : s.stacks;
    }

    public static void tickSickness(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Sick> it = SICK.values().iterator();
        java.util.List<Runnable> hurts = new java.util.ArrayList<>();
        while (it.hasNext()) {
            Sick s = it.next();
            if (s.e.level() != level) continue;
            if (!s.e.isAlive() || now >= s.until) { it.remove(); continue; }
            if (now % 5 == 0) level.sendParticles(GREEN, s.e.getX(), s.e.getY() + s.e.getBbHeight() * 0.6, s.e.getZ(), 1, 0.3, 0.4, 0.3, 0);
            if (now % 40 == 0) {
                ServerPlayer o = level.getServer().getPlayerList().getPlayer(s.owner);
                float dmg = s.stacks;
                hurts.add(() -> {
                    if (o != null) Powers.hurt(o, s.e, dmg);
                    else s.e.hurtServer(level, level.damageSources().magic(), dmg);
                });
            }
        }
        hurts.forEach(Runnable::run);
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        ServerLevel level = (ServerLevel) p.level();
        if (!d.has(Secondary.CONTAINED)) {
            if (now % 20 == 0) Powers.effect(p, MobEffects.GLOWING, 40, 0);
            if (now % 40 == 0) {
                for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> Powers.isHostile(m) && m.getTarget() == null && m.hasLineOfSight(p))) m.setTarget(p);
                for (Animal a : level.getEntitiesOfClass(Animal.class, p.getBoundingBox().inflate(8))) a.resetLove();
            }
        }
        if (Powers.timerActive(d, level, "rad_beam") && now % 4 == 0) {
            boolean core = Catalysts.holding(p, Mutation.RADIATION);
            for (LivingEntity e : Kit.beam(p, 24, d.has(Secondary.GAMMA_BURST) ? 2 : 0, GREEN)) {
                Powers.hurt(p, e, 2f * Powers.power(d) * (core ? 1.3f : 1f));
                sicken(p, e, 1);
            }
            if (now % 8 == 0) Powers.sound(p, SoundEvents.BEACON_AMBIENT, 0.6f, 1.8f);
        }
        if (Powers.timerActive(d, level, "crit_mass") && now % 20 == 0) {
            for (LivingEntity e : Kit.foes(p, 8)) { Powers.hurt(p, e, 3f * Powers.power(d)); sicken(p, e, 1); }
            level.sendParticles(GREEN, p.getX(), p.getY() + 1, p.getZ(), 60, 4, 1.5, 4, 0);
        }
        if (d.has(Secondary.ISOTOPE_FEEDING) && now % 40 == 0) {
            boolean near = !level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6), e -> stacks(e) > 0).isEmpty();
            if (near) p.heal(1f);
        }
    }

    @Override
    public boolean blocksEffect(ServerPlayer p, MutationData d, Holder<MobEffect> effect) {
        return effect.equals(MobEffects.POISON) || effect.equals(MobEffects.WITHER);
    }

    @Override
    public void onMelee(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        sicken(p, target, 1);
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        sicken(p, target, 1);   // plus the normal hit's stack = 2
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (d.has(Secondary.RAD_SHIELD) && e.getSource().getDirectEntity() instanceof Projectile && e.getSource().getEntity() instanceof LivingEntity shooter) {
            sicken(p, shooter, 2);
        }
    }

    @Override
    public void onKill(ServerPlayer p, MutationData d, LivingEntity victim) {
        int s = stacks(victim);
        if (s > 0 && d.has(Secondary.FALLOUT)) {
            for (LivingEntity e : victim.level().getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(3), x -> x != victim && x != p && x.isAlive())) sicken(p, e, s);
            ((ServerLevel) victim.level()).sendParticles(GREEN, victim.getX(), victim.getY() + 1, victim.getZ(), 30, 1.5, 1, 1.5, 0);
        }
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case RADIATION_BEAM -> {
                Powers.setTimer(p, "rad_beam", Catalysts.holding(p, Mutation.RADIATION) ? 80 : 40);
                Powers.sound(p, SoundEvents.BEACON_ACTIVATE, 1f, 1.6f);
                return true;
            }
            case MELTDOWN_PULSE -> {
                for (LivingEntity e : Kit.foes(p, 6)) {
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                    sicken(p, e, 1);
                }
                level.sendParticles(GREEN, p.getX(), p.getY() + 1, p.getZ(), 80, 3, 1, 3, 0);
                Powers.sound(p, SoundEvents.WITHER_SHOOT, 0.7f, 1.2f);
                return true;
            }
            case IRRADIATE -> {
                Vec3 at = Kit.lookPoint(p, 24);
                Zones.add(level, at, 2, 600, p.getUUID(), z -> {
                    if (z.age() % 10 == 0) level.sendParticles(GREEN, z.center.x, z.center.y + 0.2, z.center.z, 6, 1, 0.1, 1, 0);
                    ServerPlayer o = z.ownerPlayer();
                    if (o == null || z.age() % 20 != 0) return;
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(2, 1.5, 2),
                            e -> e != o && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player))) sicken(o, e, 1);
                }, null);
                return true;
            }
            case DECAY_TOUCH -> {
                LivingEntity t = Kit.look(p, 8);
                if (t == null) return false;
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1));
                Powers.hurt(p, t, 6f);
                p.heal(6f);
                Powers.line(level, p.getEyePosition(), t.getBoundingBox().getCenter(), GREEN, 0.3);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "crit_mass", 200);
        ServerLevel level = (ServerLevel) p.level();
        Powers.sound(p, SoundEvents.BEACON_POWER_SELECT, 1.5f, 0.5f);
        Powers.later(level, 200, () -> {
            if (!p.isAlive() || p.level() != level) return;
            Vec3 c = p.position();
            for (LivingEntity e : Kit.foes(p, 12)) {
                Powers.hurt(p, e, 20f * Powers.power(d));
                Powers.knockAway(e, c, 1.6, 0.6);
                sicken(p, e, 3);
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 2, 1, 1, 1, 0);
            level.sendParticles(GREEN, c.x, c.y + 1, c.z, 200, 6, 2, 6, 0);
            Powers.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.6f);
            Zones.add(level, c, 8, 400, p.getUUID(), z -> {
                if (z.age() % 10 == 0) level.sendParticles(GREEN, c.x, c.y + 0.5, c.z, 20, 6, 0.5, 6, 0);
                ServerPlayer o = z.ownerPlayer();
                if (o == null || z.age() % 20 != 0) return;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(8),
                        e -> e != o && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player))) sicken(o, e, 1);
            }, null);
        });
        return true;
    }
}
