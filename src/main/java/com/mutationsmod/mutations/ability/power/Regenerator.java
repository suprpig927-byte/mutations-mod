package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Raycast;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

public final class Regenerator {
    private Regenerator() {}

    private static final Map<UUID, List<MobEffectInstance>> PENDING = new HashMap<>();
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    public static void tick(ServerPlayer p, MutationData d) {
        long now = p.level().getGameTime();
        int interval = Math.max(8, (int) ((40 - d.level() / 2) / Powers.power(d)));
        if (now % interval == 0 && p.getHealth() < p.getMaxHealth() && p.getFoodData().getFoodLevel() > 0) {
            p.heal(1.0f);
            p.causeFoodExhaustion(1.5f);
        }
        p.causeFoodExhaustion(0.02f);
        if (!d.has(Secondary.ADAPTIVE_IMMUNITY) && p.hasEffect(MobEffects.POISON)) p.removeEffect(MobEffects.POISON);

        if (Powers.timerActive(d, p.level(), "immune") && now % 4 == 0) {
            Powers.particles(p, ParticleTypes.TOTEM_OF_UNDYING, 4, 0.4, 0.05);
        }
        if (d.has(Secondary.BLOOD_RUSH) && p.getHealth() < p.getMaxHealth() * 0.3f && now % 10 == 0) {
            Powers.effect(p, MobEffects.SPEED, 30, 1);
        }
        Powers.add(p, Attributes.ARMOR, "thick_hide",
                d.has(Secondary.THICK_HIDE) ? 4 : 0);
        if (d.has(Secondary.VITAL_AURA) && now % 40 == 0) {
            for (LivingEntity e : Powers.around(p, 8)) {
                if (e instanceof Player ally) Powers.effect(ally, MobEffects.REGENERATION, 60, 0);
            }
        }

        // Adaptive Immunity: apply the flipped/shortened effects queued from the effect event.
        List<MobEffectInstance> pending = PENDING.remove(p.getUUID());
        if (pending != null) {
            APPLYING.set(true);
            try {
                for (MobEffectInstance inst : pending) p.addEffect(inst);
            } finally {
                APPLYING.set(false);
            }
        }
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        if (d.has(Secondary.ADAPTIVE_IMMUNITY)
                && (event.getSource().is(DamageTypes.MAGIC) || event.getSource().is(DamageTypes.INDIRECT_MAGIC))) {
            // Harming magic heals instead.
            p.heal(event.getAmount());
            event.setCanceled(true);
            return;
        }
        if (d.has(Secondary.ADAMANT_BONES)) event.setAmount(event.getAmount() * 0.8f);
    }

    public static void onEffect(MobEffectEvent.Applicable event, Player p, MutationData d) {
        if (APPLYING.get()) return;
        MobEffectInstance inst = event.getEffectInstance();
        if (d.has(Secondary.IRON_STOMACH) && inst.is(MobEffects.HUNGER) && !d.has(Secondary.ADAPTIVE_IMMUNITY)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (!d.has(Secondary.ADAPTIVE_IMMUNITY)) {
            if (inst.is(MobEffects.POISON)) event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (inst.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) return;

        Holder<MobEffect> flipped = flip(inst);
        int duration = inst.getDuration();
        MobEffectInstance replacement;
        if (flipped != null) {
            int dur = flipped == MobEffects.SATURATION ? Math.min(duration, 20) : duration;
            replacement = new MobEffectInstance(flipped, dur, inst.getAmplifier(), false, true, true);
        } else {
            int dur = duration < 0 ? duration : Math.max(1, duration / 2);
            replacement = new MobEffectInstance(inst.getEffect(), dur, inst.getAmplifier(), inst.isAmbient(), inst.isVisible(), inst.showIcon());
        }
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        PENDING.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(replacement);
    }

    private static Holder<MobEffect> flip(MobEffectInstance inst) {
        if (inst.is(MobEffects.POISON) || inst.is(MobEffects.WITHER)) return MobEffects.REGENERATION;
        if (inst.is(MobEffects.INSTANT_DAMAGE)) return MobEffects.INSTANT_HEALTH;
        if (inst.is(MobEffects.SLOWNESS)) return MobEffects.SPEED;
        if (inst.is(MobEffects.WEAKNESS)) return MobEffects.STRENGTH;
        if (inst.is(MobEffects.MINING_FATIGUE)) return MobEffects.HASTE;
        if (inst.is(MobEffects.HUNGER)) return MobEffects.SATURATION;
        if (inst.is(MobEffects.BLINDNESS) || inst.is(MobEffects.DARKNESS)) return MobEffects.NIGHT_VISION;
        return null;
    }

    /** Second Wind: returns true if the death was prevented. */
    public static boolean secondWind(LivingDeathEvent event, ServerPlayer p, MutationData d) {
        if (!d.has(Secondary.SECOND_WIND) || Powers.timerActive(d, p.level(), "second_wind")) return false;
        event.setCanceled(true);
        p.setHealth(6f);
        p.setAbsorptionAmount(4f);
        Powers.effect(p, MobEffects.REGENERATION, 100, 1);
        Powers.setTimer(p, "second_wind", 10 * 60 * 20);
        Powers.particles(p, ParticleTypes.TOTEM_OF_UNDYING, 40, 0.5, 0.4);
        Powers.sound(p, SoundEvents.TOTEM_USE, 0.7f, 1.3f);
        p.sendSystemMessage(Component.literal("Second Wind! Your body refuses to die.").withStyle(ChatFormatting.GREEN));
        return true;
    }

    // ------------------------------------------------------------------ actives

    public static boolean surge(ServerPlayer p, MutationData d) {
        var food = p.getFoodData();
        if (food.getFoodLevel() < 6) {
            p.sendOverlayMessage(Component.literal("Too hungry to Surge.").withStyle(ChatFormatting.RED));
            return false;
        }
        food.setFoodLevel(food.getFoodLevel() - 6);
        p.heal((8.0f + d.level() * 0.1f) * Powers.power(d));
        Powers.particles(p, ParticleTypes.HEART, 10, 0.5, 0.1);
        Powers.sound(p, SoundEvents.PLAYER_LEVELUP, 0.5f, 1.6f);
        return true;
    }

    public static boolean immunity(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "immune", (long) (8 * 20 * Powers.power(d)));
        Powers.sound(p, SoundEvents.TOTEM_USE, 0.5f, 1.4f);
        p.sendOverlayMessage(Component.literal("IMMUNE").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return true;
    }

    public static boolean transfusion(ServerPlayer p, MutationData d) {
        LivingEntity target = Raycast.entity(p, 16);
        if (target == null) {
            p.sendOverlayMessage(Component.literal("No target.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        float amount = Math.min(target.getHealth(), 4f * Powers.power(d));
        Powers.hurt(p, target, amount);
        p.heal(amount);
        Powers.line((ServerLevel) p.level(), target.getEyePosition(), p.getEyePosition(), ParticleTypes.HEART, 1.0);
        return true;
    }

    public static void forget(UUID id) {
        PENDING.remove(id);
    }
}
