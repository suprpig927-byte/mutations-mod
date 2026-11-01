package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Swarm: your body is home to a swarm of insects. Smoke and fire scatter it. */
public class SwarmPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.SWARM; }

    static final DustParticleOptions BUG = new DustParticleOptions(0x2A2A20, 0.6f);

    private static boolean jelly(ServerPlayer p) { return Catalysts.holding(p, Mutation.SWARM); }

    private static boolean scattered(MutationData d, ServerPlayer p) {
        return Powers.timerActive(d, p.level(), "swarm_scattered");
    }

    private static void scatterSwarm(ServerPlayer p) {
        if (jelly(p)) return;
        Powers.setTimer(p, "swarm_scattered", 300);
        Powers.particles(p, BUG, 40, 1.5, 0.2);
        p.sendOverlayMessage(Component.literal("The smoke scatters your swarm!").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public String blocked(ServerPlayer p, MutationData d) {
        return scattered(d, p) ? "Your swarm is scattered. It will regather soon." : null;
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        ServerLevel level = (ServerLevel) p.level();
        if (now % 20 == 0 && !jelly(p) && !scattered(d, p)) {
            BlockPos c = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-3, -2, -3), c.offset(3, 2, 3))) {
                var s = level.getBlockState(q);
                if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) { scatterSwarm(p); break; }
            }
        }
        if (scattered(d, p)) return;
        if (now % 3 == 0) level.sendParticles(BUG, p.getX(), p.getY() + 1.2, p.getZ(), 3, 0.8, 0.6, 0.8, 0);
        int every = jelly(p) ? 20 : 40;
        if (now % every == 0) {
            float dmg = d.has(Secondary.KILLER_BEES) ? 2f : 1f;
            for (LivingEntity e : Kit.hostiles(p, 3)) Powers.hurt(p, e, dmg);
        }
        if (d.has(Secondary.POLLINATOR) && now % 20 == 0) VerdantPower.growNear(level, p.blockPosition(), 8, 4);
    }

    @Override
    public boolean ignoredBy(ServerPlayer p, MutationData d, Mob mob) {
        return mob instanceof Bee;
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (e.getSource().is(DamageTypeTags.IS_FIRE) && !scattered(d, p)) scatterSwarm(p);
        if (scattered(d, p)) return;
        if (d.has(Secondary.BUZZING_SHIELD) && e.getSource().getDirectEntity() instanceof Projectile pr && p.getRandom().nextFloat() < 0.3f) {
            e.setCanceled(true);
            pr.setDeltaMovement(new Vec3(0, -0.1, 0));
            return;
        }
        if (d.has(Secondary.CARRIER_SWARM) && e.getSource().is(DamageTypes.FALL)) e.setAmount(e.getAmount() * 0.5f);
    }

    @Override
    public boolean cheatDeath(LivingDeathEvent e, ServerPlayer p, MutationData d) {
        if (!d.has(Secondary.LARVAL_REBIRTH) || Powers.timerActive(d, p.level(), "larval_rebirth")) return false;
        if (e.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || e.getSource().is(DamageTypes.GENERIC_KILL)) return false;
        e.setCanceled(true);
        p.setHealth(6f);
        Powers.setTimer(p, "larval_rebirth", 12000);
        scatterTo(p, Kit.groundNear((ServerLevel) p.level(), p.position(), 6, p.getRandom()));
        return true;
    }

    private static boolean scatterTo(ServerPlayer p, Vec3 target) {
        Vec3 spot = Powers.safeSpot(p, target);
        if (spot == null) return false;
        ServerLevel level = (ServerLevel) p.level();
        level.sendParticles(BUG, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 0.9, 0.6, 0.1);
        Powers.line(level, p.position().add(0, 1, 0), spot.add(0, 1, 0), BUG, 0.3);
        Powers.teleport(p, spot);
        level.sendParticles(BUG, spot.x, spot.y + 1, spot.z, 60, 0.6, 0.9, 0.6, 0.1);
        Powers.sound(p, SoundEvents.BEEHIVE_EXIT, 1.5f, 1f);
        return true;
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case STING_CLOUD -> {
                LivingEntity t = Kit.look(p, 16);
                if (t == null) return false;
                float dmg = d.has(Secondary.KILLER_BEES) ? 2f : 1.5f;
                Zones.add(level, t.position(), 1, 120, p.getUUID(), z -> {
                    if (!t.isAlive()) return;
                    z.center = t.position();
                    if (z.age() % 3 == 0) level.sendParticles(BUG, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 4, 0.4, 0.4, 0.4, 0.05);
                    ServerPlayer o = z.ownerPlayer();
                    if (o != null && z.age() % 20 == 0) {
                        Powers.hurt(o, t, dmg);
                        t.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
                    }
                }, null);
                Powers.sound(p, SoundEvents.BEE_LOOP_AGGRESSIVE, 1.5f, 1f);
                return true;
            }
            case SCATTER -> {
                Vec3 look = p.getLookAngle();
                for (int dist = 10; dist >= 2; dist--) if (scatterTo(p, p.position().add(look.scale(dist)))) return true;
                return false;
            }
            case HIVE_MIND -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24), e -> e != p && e.isAlive() && e instanceof Mob)) Kit.glow(e, 200);
                level.sendParticles(BUG, p.getX(), p.getY() + 1, p.getZ(), 80, 6, 2, 6, 0.1);
                return true;
            }
            case HIVE_SUMMON -> {
                LivingEntity target = Kit.look(p, 24);
                for (int i = 0; i < 4; i++) {
                    Bee bee = Minions.spawn(p, EntityTypes.BEE, p.position().add(p.getRandom().nextGaussian(), 1.5, p.getRandom().nextGaussian()), 400, "Swarm Bee");
                    if (bee != null && target != null) bee.setTarget(target);
                }
                Powers.sound(p, SoundEvents.BEEHIVE_EXIT, 1.5f, 0.8f);
                return true;
            }
            case WASP_BARRAGE -> {
                for (LivingEntity e : Powers.cone(p, 8, 0.6)) {
                    Powers.hurt(p, e, 3f * Powers.power(d));
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                }
                Vec3 look = p.getLookAngle();
                for (int i = 1; i <= 8; i++) {
                    Vec3 at = p.getEyePosition().add(look.scale(i));
                    level.sendParticles(BUG, at.x, at.y, at.z, 8, 0.2 + i * 0.12, 0.2 + i * 0.1, 0.2 + i * 0.12, 0.05);
                }
                Powers.sound(p, SoundEvents.BEE_STING, 1.5f, 1f);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        float dmg = d.has(Secondary.KILLER_BEES) ? 3f : 2f;
        Zones.add(level, p.position(), 16, 200, p.getUUID(), z -> {
            ServerPlayer o = z.ownerPlayer();
            if (o == null) return;
            z.center = o.position();
            if (z.age() % 2 == 0) level.sendParticles(BUG, z.center.x, z.center.y + 2, z.center.z, 30, 10, 3, 10, 0.1);
            if (z.age() % 20 != 0) return;
            for (LivingEntity e : Kit.foes(o, 16)) Powers.hurt(o, e, dmg * Powers.power(d));
            var r = o.getRandom();
            for (int i = 0; i < 20; i++) {
                BlockPos q = o.blockPosition().offset(r.nextInt(21) - 10, r.nextInt(12) - 2, r.nextInt(21) - 10);
                if (level.getBlockState(q).is(BlockTags.LEAVES)) Powers.freezeTemporary(level, q, Blocks.AIR.defaultBlockState(), 600);
            }
        }, null);
        Powers.sound(p, SoundEvents.BEE_LOOP_AGGRESSIVE, 2f, 0.6f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        ServerLevel level = (ServerLevel) p.level();
        for (int t = 20; t <= 60; t += 20) Powers.later(level, t, () -> { if (target.isAlive()) Powers.hurt(p, target, 1f); });
    }
}
