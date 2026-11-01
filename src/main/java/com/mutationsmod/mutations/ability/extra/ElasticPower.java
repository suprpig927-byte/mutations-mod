package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Elastic: stretch and bounce. */
public class ElasticPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.ELASTIC; }

    static final ItemParticleOption RUBBER = new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.SLIME_BALL)));

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        double reach = 2 + (d.has(Secondary.LONG_ARMS) ? 2 : 0) + (Catalysts.holding(p, Mutation.ELASTIC) ? 2 : 0);
        Powers.add(p, Attributes.ENTITY_INTERACTION_RANGE, "elastic", reach);
        Powers.add(p, Attributes.BLOCK_INTERACTION_RANGE, "elastic", reach);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "elastic", 0.5);
        if (d.has(Secondary.SQUEEZE)) squeeze(p);
        ServerLevel level = (ServerLevel) p.level();
        if (Powers.timerActive(d, level, "rampage")) {
            Powers.effect(p, MobEffects.JUMP_BOOST, 10, 3);
            if (!p.onGround()) {
                if (d.timer("rampage_air") == 0) ModAttachments.set(p, ModAttachments.get(p).withTimer("rampage_air", 1));
            } else if (d.timer("rampage_air") > 0) {
                ModAttachments.set(p, ModAttachments.get(p).withTimer("rampage_air", 0));
                for (LivingEntity e : Kit.foes(p, 4)) {
                    Powers.hurt(p, e, 5f * Powers.power(d));
                    Powers.knockAway(e, p.position(), 1.2, 0.5);
                }
                level.sendParticles(RUBBER, p.getX(), p.getY() + 0.2, p.getZ(), 30, 1.5, 0.1, 1.5, 0.2);
                Powers.impulse(p, new Vec3(0, 1.2, 0), false);
                Powers.sound(p, SoundEvents.SLIME_JUMP, 1.5f, 0.6f);
            }
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        var src = e.getSource();
        if (src.is(DamageTypes.FALL)) {
            float amount = e.getAmount();
            e.setCanceled(true);
            if (amount >= 3) {
                double up = Math.min(1.8, Math.sqrt(amount) * 0.33 * (d.has(Secondary.SUPER_BOUNCE) ? 1.4 : 1.0));
                Powers.impulse(p, new Vec3(0, up, 0), false);
                Powers.sound(p, SoundEvents.SLIME_JUMP, 1f, 0.8f);
            }
            return;
        }
        if (d.has(Secondary.REBOUND) && src.getDirectEntity() instanceof Projectile pr && p.getRandom().nextFloat() < 0.3f) {
            e.setCanceled(true);
            if (src.getEntity() != null) pr.setDeltaMovement(src.getEntity().position().add(0, 1, 0).subtract(pr.position()).normalize().scale(1.5));
            else pr.setDeltaMovement(pr.getDeltaMovement().scale(-1));
            pr.setOwner(p);
            pr.needsSync = true;
            return;
        }
        if (d.has(Secondary.ABSORBENT) && src.is(DamageTypeTags.IS_EXPLOSION)) e.setAmount(e.getAmount() * 0.5f);
        ItemStack w = src.getWeaponItem();
        if (w != null && !Catalysts.holding(p, Mutation.ELASTIC) && (w.is(ItemTags.SWORDS) || w.is(ItemTags.AXES) || w.is(Items.TRIDENT))) {
            e.setAmount(e.getAmount() * 1.25f);
        }
    }

    @Override
    public void onMelee(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.TANGLE)) target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case STRETCH_GRAB -> {
                LivingEntity t = Kit.look(p, 12);
                if (t != null) {
                    Powers.impulse(t, p.position().subtract(t.position()).normalize().scale(1.4).add(0, 0.3, 0), true);
                    Powers.line(level, p.getEyePosition(), t.getBoundingBox().getCenter(), RUBBER, 0.3);
                } else {
                    BlockHitResult hit = Kit.lookBlock(p, 12);
                    if (hit.getType() != HitResult.Type.BLOCK) return false;
                    Vec3 to = hit.getLocation().subtract(p.position());
                    Powers.impulse(p, to.normalize().scale(Math.min(2.0, to.length() * 0.2 + 0.6)).add(0, 0.3, 0), true);
                    Powers.line(level, p.getEyePosition(), hit.getLocation(), RUBBER, 0.3);
                }
                Powers.sound(p, SoundEvents.SLIME_SQUISH, 1.5f, 0.8f);
                return true;
            }
            case SLINGSHOT -> {
                Powers.impulse(p, p.getLookAngle().scale(2.6).add(0, 0.3, 0), true);
                Powers.startRush(p, 20, 6f * Powers.power(d), 1.0);
                Powers.sound(p, SoundEvents.SLIME_JUMP, 1.5f, 0.6f);
                return true;
            }
            case COIL_PUNCH -> {
                LivingEntity t = Kit.look(p, p.entityInteractionRange() + 0.5);
                if (t == null) return false;
                float dmg = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3f;
                Powers.hurt(p, t, dmg);
                Powers.knockAway(t, p.position(), 2.5, 0.6);
                level.sendParticles(RUBBER, t.getX(), t.getY() + 1, t.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
                Powers.sound(p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5f, 0.6f);
                return true;
            }
            case PARACHUTE -> {
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                return true;
            }
            case RUBBER_WALL -> {
                Vec3 dir = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z).normalize();
                Vec3 c = p.position().add(dir.scale(2)).add(0, 1, 0);
                Zones.add(level, c, 2.5, 100, p.getUUID(), z -> {
                    ServerPlayer o = z.ownerPlayer();
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(2.5, 1.5, 2.5), e -> e != o && e.isAlive())) {
                        Powers.impulse(e, dir.scale(1.2).add(0, 0.3, 0), true);
                    }
                    for (Projectile pr : level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(2.5, 1.5, 2.5), x -> x.getOwner() != o)) {
                        if (pr.getDeltaMovement().dot(dir) < 0) { pr.setDeltaMovement(pr.getDeltaMovement().scale(-1)); pr.needsSync = true; }
                    }
                    if (z.age() % 3 == 0) {
                        Vec3 side = new Vec3(-dir.z, 0, dir.x);
                        for (double s = -2; s <= 2; s += 0.8) level.sendParticles(RUBBER, c.x + side.x * s, c.y, c.z + side.z * s, 1, 0.05, 0.6, 0.05, 0);
                    }
                }, null);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "rampage", 240);
        Powers.startRush(p, 240, 4f * Powers.power(d), 1.2);
        Powers.impulse(p, new Vec3(0, 1.2, 0), false);
        Powers.sound(p, SoundEvents.SLIME_JUMP, 2f, 0.5f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        Powers.later((ServerLevel) p.level(), 1, () -> Powers.knockAway(target, p.position(), 1.2, 0.3));
        Powers.impulse(p, new Vec3(0, 0.35, 0), false);
    }

    /** Squeeze: sneaking into a wall with a 1-block gap flattens you to crawl through. Runs on both sides. */
    public static void squeeze(net.minecraft.world.entity.player.Player p) {
        boolean standFits = p.level().noCollision(p, p.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(p.position()));
        if (p.getForcedPose() == net.minecraft.world.entity.Pose.SWIMMING) {
            if (standFits && !p.isShiftKeyDown()) p.setForcedPose(null);
        } else if (p.isShiftKeyDown() && p.horizontalCollision && !p.isInWater()) {
            p.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);
        }
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.add(p, Attributes.ENTITY_INTERACTION_RANGE, "elastic", 0);
        Powers.add(p, Attributes.BLOCK_INTERACTION_RANGE, "elastic", 0);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "elastic", 0);
    }
}
