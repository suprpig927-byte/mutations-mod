package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Micro: shrink tiny or grow big at will. */
public class MicroPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.MICRO; }

    public static final int NORMAL = 0, TINY = 1, BIG = 2;

    public static int size(MutationData d) { return (int) d.timer("size"); }

    static void sizeMods(LivingEntity e, String id, double scale, double health, double attack, double speed, double reach) {
        Powers.mult(e, Attributes.SCALE, id, scale);
        Powers.mult(e, Attributes.MAX_HEALTH, id, health);
        Powers.mult(e, Attributes.ATTACK_DAMAGE, id, attack);
        Powers.mult(e, Attributes.MOVEMENT_SPEED, id, speed);
        Powers.add(e, Attributes.ENTITY_INTERACTION_RANGE, id, reach);
        Powers.add(e, Attributes.BLOCK_INTERACTION_RANGE, id, reach);
        if (e.getHealth() > e.getMaxHealth()) e.setHealth(e.getMaxHealth());
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        long pendingAt = d.timer("size_at");
        if (pendingAt > 0 && now >= pendingAt) {
            ModAttachments.set(p, ModAttachments.get(p).withTimer("size", d.timer("size_next")).withTimer("size_at", 0));
            d = ModAttachments.get(p);
            Powers.particles(p, ParticleTypes.POOF, 20, 0.5, 0.05);
            Powers.sound(p, SoundEvents.PUFFER_FISH_BLOW_UP, 1f, size(d) == TINY ? 1.8f : 0.6f);
        }
        boolean giant = Powers.timerActive(d, p.level(), "giant_slayer");
        int s = size(d);
        if (giant) sizeMods(p, "micro", 3.0, 2.0, 1.0, 0, 4);
        else if (s == TINY) sizeMods(p, "micro", -0.75, -0.5, d.has(Secondary.TINY_TERROR) ? 0 : -0.5, 0.5, 0);
        else if (s == BIG) sizeMods(p, "micro", 0.75, 0.5, 0.5, -0.15, 1.5);
        else sizeMods(p, "micro", 0, 0, 0, 0, 0);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "heavyweight", (s == BIG && d.has(Secondary.HEAVYWEIGHT)) || giant ? 1 : 0);
        if ((s == BIG || giant) && !Catalysts.holding(p, Mutation.MICRO)) p.causeFoodExhaustion(0.01f);
        if (giant && now % 10 == 0 && p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.002) {
            ServerLevel level = (ServerLevel) p.level();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(p.blockPosition().below())), p.getX(), p.getY(), p.getZ(), 30, 2, 0.1, 2, 0.2);
            Powers.sound(p, SoundEvents.ANVIL_LAND, 0.5f, 0.3f);
            for (LivingEntity e : Kit.foes(p, 5)) { Powers.hurt(p, e, 5f * Powers.power(d)); Powers.knockAway(e, p.position(), 1.2, 0.5); }
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (size(d) != TINY) return;
        if (e.getSource().is(DamageTypes.FALL)) { e.setCanceled(true); return; }
        if (p.getRandom().nextFloat() < 0.2f) { e.setCanceled(true); Powers.particles(p, ParticleTypes.CLOUD, 4, 0.2, 0.02); }
    }

    @Override
    public boolean ignoredBy(ServerPlayer p, MutationData d, Mob mob) {
        return d.has(Secondary.UNSEEN) && size(d) == TINY && mob.distanceToSqr(p) > 36;
    }

    @Override
    public float cooldownFactor(ServerPlayer p, MutationData d, ActiveAbility a) {
        return a == ActiveAbility.SIZE_SHIFT && d.has(Secondary.QUICK_SHIFT) ? 0.5f : 1f;
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case SIZE_SHIFT -> {
                int next = (size(d) + 1) % 3;
                int delay = Catalysts.holding(p, Mutation.MICRO) ? 1 : 20;
                ModAttachments.set(p, d.withTimer("size_next", next).withTimer("size_at", level.getGameTime() + delay));
                p.sendOverlayMessage(Component.literal(next == TINY ? "Shrinking..." : next == BIG ? "Growing..." : "Returning to normal size...").withStyle(ChatFormatting.AQUA));
                return true;
            }
            case SHRINK_RAY -> {
                LivingEntity t = Kit.look(p, 16);
                if (t == null) return false;
                sizeMods(t, "shrink_ray", -0.5, -0.5, -0.5, 0, 0);
                Powers.later(level, 200, () -> sizeMods(t, "shrink_ray", 0, 0, 0, 0, 0));
                Powers.line(level, p.getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 0.4);
                Powers.sound(p, SoundEvents.PUFFER_FISH_BLOW_OUT, 1.5f, 1.6f);
                return true;
            }
            case GROWTH_STOMP -> {
                Powers.mult(p, Attributes.SCALE, "stomp", 1.0);
                Powers.later(level, 10, () -> Powers.mult(p, Attributes.SCALE, "stomp", 0));
                for (LivingEntity e : Kit.foes(p, 4.5)) { Powers.hurt(p, e, 6f * Powers.power(d)); Powers.knockAway(e, p.position(), 1.2, 0.5); }
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(p.blockPosition().below())), p.getX(), p.getY(), p.getZ(), 50, 2.5, 0.1, 2.5, 0.2);
                Powers.sound(p, SoundEvents.ANVIL_LAND, 1f, 0.5f);
                return true;
            }
            case ENLARGE_ALLY -> {
                LivingEntity t = Kit.look(p, 16);
                if (!(t instanceof Player ally)) return false;
                sizeMods(ally, "enlarged", 0.75, 0.5, 0.5, -0.1, 1.5);
                Powers.later(level, 400, () -> sizeMods(ally, "enlarged", 0, 0, 0, 0, 0));
                return true;
            }
            case MICROSCOPIC -> {
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
                Powers.mult(p, Attributes.SCALE, "microscopic", -0.9);
                Powers.later(level, 100, () -> Powers.mult(p, Attributes.SCALE, "microscopic", 0));
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "giant_slayer", 300);
        Powers.sound(p, SoundEvents.RAVAGER_ROAR, 1.5f, 0.6f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (Powers.timerActive(d, p.level(), "giant_slayer") || size(d) == BIG) e.setAmount(e.getAmount() * 1.6f);
        else if (size(d) == TINY) e.setAmount(e.getAmount() * 0.7f);
    }

    @Override
    public void reset(ServerPlayer p) {
        sizeMods(p, "micro", 0, 0, 0, 0, 0);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "heavyweight", 0);
    }

    /** Pocket Rider and Giant's Grip. */
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        MutationData d = ModAttachments.get(p);
        if (!d.has(Mutation.MICRO) || com.mutationsmod.mutations.ability.Suppression.active(d, p.level())) return;
        if (d.has(Secondary.POCKET_RIDER) && size(d) == TINY && p.isShiftKeyDown() && e.getTarget() instanceof Player other) {
            p.startRiding(other, true, true);
            e.setCanceled(true);
        } else if (d.has(Secondary.GIANTS_GRIP) && size(d) == BIG && p.getMainHandItem().isEmpty()) {
            if (!p.getPassengers().isEmpty()) {
                var held = p.getPassengers().get(0);
                held.stopRiding();
                Vec3 v = p.getLookAngle().scale(1.6).add(0, 0.4, 0);
                if (held instanceof LivingEntity le) Powers.impulse(le, v, true);
                e.setCanceled(true);
            } else if (e.getTarget() instanceof Mob m && m.getBbHeight() < 1.2f) {
                m.startRiding(p, true, true);
                e.setCanceled(true);
            }
        }
    }
}
