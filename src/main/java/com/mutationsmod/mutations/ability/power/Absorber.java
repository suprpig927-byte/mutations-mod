package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Raycast;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Secondary;
import com.mutationsmod.mutations.mutation.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class Absorber {
    private Absorber() {}

    /** How much charge you can hold safely. Grows with mastery and the Mutation stat. */
    public static float capacity(MutationData d) {
        float cap = 20f + d.level() * 0.5f + d.stat(Stat.MUTATION) * 2f;
        return d.has(Secondary.DEEP_RESERVOIR) ? cap * 1.5f : cap;
    }

    public static void tick(ServerPlayer p, MutationData d) {
        long now = p.level().getGameTime();
        float cap = capacity(d);
        if (d.charge() > cap && now % 20 == 0) {
            Powers.hurtSelf(p, d.has(Secondary.STEADY_VESSEL) ? 0.5f : 1.0f);
            if (now % 60 == 0) p.sendOverlayMessage(Component.literal("Too much charge! Release it!").withStyle(ChatFormatting.RED));
        }
        if (d.charge() > cap * 0.75f && now % 10 == 0) {
            Powers.particles(p, ParticleTypes.ELECTRIC_SPARK, 3, 0.5, 0.05);
        }
    }

    private static boolean unabsorbable(DamageSource source) {
        return source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.DROWN);
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        DamageSource source = event.getSource();
        if (unabsorbable(source) || event.getAmount() <= 0) return;
        boolean elemental = source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LIGHTNING_BOLT);
        float ratio = d.has(Secondary.ELEMENTAL_SPONGE) && elemental ? 1.0f : 0.5f;
        float absorbed = event.getAmount() * ratio;
        event.setAmount(event.getAmount() - absorbed);
        addCharge(p, absorbed);
    }

    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.KINETIC_FEEDBACK)) addCharge(p, event.getAmount() * 0.3f);
    }

    public static void addCharge(ServerPlayer p, float amount) {
        MutationData d = ModAttachments.get(p);
        float cap = capacity(d);
        float charge = d.charge() + amount;
        if (d.has(Secondary.OVERFLOW_SHIELD) && charge > cap) {
            float extra = charge - cap;
            p.setAbsorptionAmount(Math.min(8f, p.getAbsorptionAmount() + extra));
            charge = cap;
        }
        ModAttachments.set(p, d.withCharge(charge));
    }

    // ------------------------------------------------------------------ actives

    public static boolean release(ServerPlayer p, MutationData d) {
        float charge = d.charge();
        if (charge < 2f) {
            p.sendOverlayMessage(Component.literal("Not enough charge.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        float damage = charge * 0.8f * Powers.power(d);
        double knock = d.has(Secondary.SHOCKWAVE) ? 1.6 : 0.5;
        for (LivingEntity e : Powers.around(p, 5)) {
            Powers.hurt(p, e, damage);
            Powers.knockAway(e, p.position(), knock, 0.4);
        }
        Powers.particles(p, ParticleTypes.EXPLOSION, 4, 1.5, 0);
        Powers.particles(p, ParticleTypes.ELECTRIC_SPARK, 60, 2.5, 0.4);
        Powers.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 1.3f);
        afterRelease(p, d);
        return true;
    }

    public static boolean focusedBeam(ServerPlayer p, MutationData d) {
        float charge = d.charge();
        if (charge < 2f) {
            p.sendOverlayMessage(Component.literal("Not enough charge.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        LivingEntity target = Raycast.entity(p, 32);
        var end = target != null ? target.getBoundingBox().getCenter() : Raycast.target(p, 32);
        if (target != null) {
            Powers.hurt(p, target, charge * 1.5f * Powers.power(d));
            Powers.knockAway(target, p.position(), 0.8, 0.2);
        }
        Powers.line((ServerLevel) p.level(), p.getEyePosition().add(p.getLookAngle()), end, ParticleTypes.END_ROD, 0.3);
        Powers.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.8f);
        afterRelease(p, d);
        return true;
    }

    private static void afterRelease(ServerPlayer p, MutationData d) {
        ModAttachments.set(p, ModAttachments.get(p).withCharge(0f));
        if (d.has(Secondary.AFTERGLOW)) Powers.effect(p, MobEffects.REGENERATION, 100, 1);
    }
}
