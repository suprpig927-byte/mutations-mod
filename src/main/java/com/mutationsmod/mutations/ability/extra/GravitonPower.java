package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Graviton: bend gravity. Low gravity for you, heavy fields and black holes for them. */
public class GravitonPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.GRAVITON; }

    static final DustParticleOptions VIOLET = new DustParticleOptions(0x8060E0, 1.2f);

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        ServerLevel level = (ServerLevel) p.level();
        Powers.mult(p, Attributes.GRAVITY, "graviton", -0.5);
        Powers.add(p, Attributes.JUMP_STRENGTH, "graviton", d.has(Secondary.MOONWALK) ? 0.4 : 0.2);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "mass_shift", d.has(Secondary.MASS_SHIFT) ? 1 : 0);
        // The unstable field: gravity flips now and then.
        if (!Catalysts.holding(p, Mutation.GRAVITON)) {
            long next = d.timer("next_flip");
            if (next == 0) Powers.setTimer(p, "next_flip", 600 + p.getRandom().nextInt(600));
            else if (now >= next) {
                Powers.impulse(p, new Vec3(0, 0.9, 0), false);
                level.sendParticles(VIOLET, p.getX(), p.getY() + 0.5, p.getZ(), 20, 0.4, 0.4, 0.4, 0);
                p.sendOverlayMessage(Component.literal("Your gravity flips!").withStyle(ChatFormatting.LIGHT_PURPLE));
                Powers.setTimer(p, "next_flip", 600 + p.getRandom().nextInt(600));
            }
        }
        if (d.has(Secondary.ORBIT)) {
            for (ItemEntity it : level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(6), i -> !i.hasPickUpDelay())) {
                it.setDeltaMovement(p.position().add(0, 0.6, 0).subtract(it.position()).normalize().scale(0.2));
            }
        }
        // Gravity Slam landing.
        long slam = d.timer("gslam");
        if (slam > 0 && (p.onGround() || p.isInWater())) {
            ModAttachments.set(p, ModAttachments.get(p).withTimer("gslam", 0));
            double fell = Math.max(0, slam / 10.0 - 1000 - p.getY());
            for (LivingEntity e : Kit.foes(p, 4)) {
                Powers.hurt(p, e, (float) Math.min(20, 5 + fell * 0.7) * Powers.power(d));
                Powers.knockAway(e, p.position(), 0.9, 0.4);
            }
            level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 2, 1, 0, 1, 0);
            level.sendParticles(VIOLET, p.getX(), p.getY() + 0.2, p.getZ(), 40, 2, 0.1, 2, 0);
            Powers.sound(p, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1f, 0.8f);
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (e.getSource().is(DamageTypes.FALL)) { e.setCanceled(true); return; }
        if (d.has(Secondary.REPULSOR_FIELD) && e.getSource().getDirectEntity() instanceof Projectile pr) {
            e.setCanceled(true);
            pr.setDeltaMovement(new Vec3(0, -0.1, 0));
        }
    }

    @Override
    public void onMelee(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.MASS_SHIFT)) Powers.later((ServerLevel) p.level(), 1, () -> Powers.knockAway(target, p.position(), 0.8, 0.2));
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case HEAVY_FIELD -> {
                Vec3 at = Kit.lookPoint(p, 24);
                boolean crush = d.has(Secondary.CRUSHING_WEIGHT);
                Zones.add(level, at, 5, 120, p.getUUID(), z -> {
                    ServerPlayer o = z.ownerPlayer();
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(5), e -> e != o && e.isAlive()
                            && !(e instanceof net.minecraft.world.entity.player.Player) && !Minions.isMinion(e))) {
                        Vec3 v = e.getDeltaMovement();
                        e.setDeltaMovement(v.x * 0.5, Math.min(v.y, e.onGround() ? 0 : -0.6), v.z * 0.5);
                        if (z.age() % 10 == 0) e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 15, 3, false, false));
                        if (crush && o != null && z.age() % 20 == 0) Powers.hurt(o, e, 1f);
                    }
                    if (z.age() % 4 == 0) level.sendParticles(VIOLET, z.center.x, z.center.y + 0.3, z.center.z, 12, 3.5, 0.3, 3.5, 0);
                }, null);
                Powers.sound(p, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 1f, 0.6f);
                return true;
            }
            case LIFT -> {
                LivingEntity t = Kit.look(p, 20);
                if (t == null) return false;
                t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 1));
                level.sendParticles(VIOLET, t.getX(), t.getY(), t.getZ(), 20, 0.4, 0.2, 0.4, 0);
                return true;
            }
            case GRAVITY_SLAM -> {
                if (p.onGround()) { p.sendOverlayMessage(Component.literal("You need to be in the air.").withStyle(ChatFormatting.GRAY)); return false; }
                Powers.impulse(p, new Vec3(0, -2.5, 0), true);
                ModAttachments.set(p, ModAttachments.get(p).withTimer("gslam", (long) ((p.getY() + 1000) * 10)));
                return true;
            }
            case REVERSE -> {
                for (LivingEntity e : Kit.foes(p, 8)) e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 3));
                level.sendParticles(VIOLET, p.getX(), p.getY() + 1, p.getZ(), 60, 4, 1, 4, 0);
                return true;
            }
            case GRAVITY_LASH -> {
                BlockHitResult hit = Kit.lookBlock(p, 24);
                if (hit.getType() != HitResult.Type.BLOCK) return false;
                Vec3 to = hit.getLocation().subtract(p.position());
                Powers.impulse(p, to.normalize().scale(Math.min(2.2, to.length() * 0.2 + 0.6)), true);
                Powers.line(level, p.getEyePosition(), hit.getLocation(), VIOLET, 0.5);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 at = Kit.lookPoint(p, 20);
        Zones.add(level, at, 20, 160, p.getUUID(), z -> {
            ServerPlayer o = z.ownerPlayer();
            AABB box = new AABB(z.center, z.center).inflate(20);
            for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> e != o && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player)
                    && (e instanceof LivingEntity || e instanceof ItemEntity) && !Minions.isMinion(e))) {
                Vec3 to = z.center.subtract(e.position());
                if (to.length() > 1) {
                    Vec3 pull = to.normalize().scale(Math.min(0.6, 6 / (to.length() + 4)));
                    if (e instanceof LivingEntity le) Powers.impulse(le, pull, true); else e.setDeltaMovement(pull);
                }
                if (e instanceof LivingEntity le && to.length() < 2.5 && o != null && z.age() % 10 == 0) Powers.hurt(o, le, 4f * Powers.power(d));
            }
            if (z.age() % 2 == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, z.center.x, z.center.y, z.center.z, 20, 2, 2, 2, 0.1);
                level.sendParticles(ParticleTypes.SQUID_INK, z.center.x, z.center.y, z.center.z, 6, 0.4, 0.4, 0.4, 0);
            }
        }, z -> {
            ServerPlayer o = z.ownerPlayer();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(8), e -> e != o && e.isAlive()
                    && !(e instanceof net.minecraft.world.entity.player.Player))) Powers.knockAway(e, z.center, 2.0, 0.8);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, z.center.x, z.center.y, z.center.z, 1, 0, 0, 0, 0);
            level.playSound(null, z.center.x, z.center.y, z.center.z, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 2f, 0.6f);
        });
        Powers.sound(p, SoundEvents.PORTAL_TRIGGER, 1f, 0.5f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
        target.setDeltaMovement(target.getDeltaMovement().multiply(1, 0, 1).add(0, -0.4, 0));
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.mult(p, Attributes.GRAVITY, "graviton", 0);
        Powers.add(p, Attributes.JUMP_STRENGTH, "graviton", 0);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "mass_shift", 0);
    }
}
