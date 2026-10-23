package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Raycast;
import com.mutationsmod.mutations.ability.Telekinesis;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class KineticMind {
    private KineticMind() {}

    public static void tick(ServerPlayer p, MutationData d) {
        Telekinesis.tick(p, d);
        ServerLevel level = (ServerLevel) p.level();
        long now = level.getGameTime();

        if (d.has(Secondary.ITEM_MAGNET)) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(8))) {
                Vec3 pull = p.position().add(0, 0.5, 0).subtract(item.position());
                if (pull.lengthSqr() > 1) item.setDeltaMovement(pull.normalize().scale(0.35));
            }
        }

        if (Powers.timerActive(d, level, "barrier")) {
            Vec3 c = p.position().add(0, 1, 0);
            for (LivingEntity e : Powers.around(p, 4)) {
                if (Powers.isHostile(e)) Powers.knockAway(e, p.position(), 0.6, 0.15);
            }
            for (Projectile proj : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(4))) {
                if (proj.getOwner() == p) continue;
                Vec3 v = proj.getDeltaMovement();
                Vec3 away = proj.position().subtract(c).normalize();
                if (v.dot(away) < 0) proj.setDeltaMovement(away.scale(Math.max(0.5, v.length() * 0.6)));
            }
            if (now % 3 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = (now * 0.2 + i * Math.PI / 6);
                    level.sendParticles(ParticleTypes.END_ROD, c.x + Math.cos(a) * 3.5, c.y + Math.sin(a * 0.5), c.z + Math.sin(a) * 3.5, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        // Mind Shield: cancelling the hit makes vanilla bounce the arrow off.
        if (d.has(Secondary.MIND_SHIELD) && event.getSource().getDirectEntity() instanceof AbstractArrow) {
            event.setCanceled(true);
            Powers.sound(p, SoundEvents.ILLUSIONER_CAST_SPELL, 0.4f, 2.0f);
        }
        if (Powers.timerActive(d, p.level(), "barrier") && event.getSource().getDirectEntity() instanceof Projectile) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ actives

    public static boolean forcePush(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 look = p.getLookAngle();
        float damage = Math.min(10f, 3f + d.level() * 0.1f) * Powers.power(d);
        for (LivingEntity e : Powers.cone(p, 7.0, 0.5)) {
            Powers.hurt(p, e, damage);
            Powers.impulse(e, look.multiply(1, 0, 1).normalize().scale(1.6).add(0, 0.45, 0), false);
        }
        level.sendParticles(ParticleTypes.CLOUD, p.getX() + look.x * 2, p.getEyeY() + look.y * 2, p.getZ() + look.z * 2, 25, 0.6, 0.6, 0.6, 0.15);
        Powers.sound(p, SoundEvents.ILLUSIONER_CAST_SPELL, 0.8f, 0.7f);
        Telekinesis.addStrain(p);
        return true;
    }

    /** Toggle: pressing again while levitating stops it. */
    public static boolean levitate(ServerPlayer p, MutationData d) {
        Powers.effect(p, MobEffects.LEVITATION, 15 * 20, 0);
        Powers.setTimer(p, "levitate", 15 * 20);
        Powers.sound(p, SoundEvents.ILLUSIONER_CAST_SPELL, 0.5f, 1.8f);
        return true;
    }

    public static boolean isLevitating(ServerPlayer p, MutationData d) {
        return Powers.timerActive(d, p.level(), "levitate") && p.hasEffect(MobEffects.LEVITATION);
    }

    public static void stopLevitating(ServerPlayer p) {
        p.removeEffect(MobEffects.LEVITATION);
        Powers.clearTimer(p, "levitate");
        Powers.effect(p, MobEffects.SLOW_FALLING, 100, 0);
    }

    public static boolean disarm(ServerPlayer p, MutationData d) {
        LivingEntity target = Raycast.entity(p, 16);
        if (target == null || target.getMainHandItem().isEmpty()) {
            p.sendOverlayMessage(Component.literal("Nothing to disarm.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        ItemStack stack = target.getMainHandItem().copy();
        target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (target instanceof Player victim) {
            victim.drop(stack, true, Prediction.SERVER_ONLY);
        } else {
            target.spawnAtLocation((ServerLevel) p.level(), stack, 1.0f);
        }
        Powers.particles(target, ParticleTypes.CLOUD, 8, 0.3, 0.05);
        Powers.sound(target, SoundEvents.ILLUSIONER_CAST_SPELL, 0.6f, 1.2f);
        Telekinesis.addStrain(p);
        return true;
    }

    public static boolean barrier(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "barrier", (long) (6 * 20 * Powers.power(d)));
        Powers.sound(p, SoundEvents.ILLUSIONER_CAST_SPELL, 0.8f, 1.0f);
        return true;
    }
}
