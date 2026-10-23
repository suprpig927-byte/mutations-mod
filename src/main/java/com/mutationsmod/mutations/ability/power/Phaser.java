package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class Phaser {
    private Phaser() {}

    /** Ticks spent running fast, for Static Build-Up. */
    private static final Map<UUID, Integer> RUN_TICKS = new HashMap<>();

    public static int maxSpeedTier(MutationData d) {
        return Math.min(5, 1 + d.level() / 8);
    }

    public static int tier(MutationData d) {
        return Math.min(d.speedTier(), maxSpeedTier(d));
    }

    public static void tick(ServerPlayer p, MutationData d) {
        int tier = tier(d);
        long now = p.level().getGameTime();
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "phaser_speed", 0.15 + 0.35 * tier);
        Powers.mult(p, Attributes.ATTACK_SPEED, "quick_hands", d.has(Secondary.QUICK_HANDS) ? 0.2 : 0);
        if (tier >= 2 && p.isSprinting()) {
            float burn = 0.006f * tier;
            if (d.has(Secondary.EFFICIENT_METABOLISM)) burn *= 0.5f;
            p.causeFoodExhaustion(burn);
        }

        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            if (!p.getInventory().add(chest)) p.drop(chest, false, Prediction.SERVER_ONLY);
            p.sendOverlayMessage(Component.literal("Your phasing body can't hold a chestplate.").withStyle(ChatFormatting.YELLOW));
        }

        // Sonic Boom: at tier 4+ while sprinting, a shockwave every 30 seconds.
        if (tier >= 4 && p.isSprinting() && !Powers.timerActive(d, p.level(), "sonic_boom")) {
            for (LivingEntity e : Powers.around(p, 5)) {
                if (!Powers.isHostile(e)) continue;
                Powers.hurt(p, e, 3f * Powers.power(d));
                Powers.knockAway(e, p.position(), 1.4, 0.4);
            }
            Powers.particles(p, ParticleTypes.SONIC_BOOM, 1, 0, 0);
            Powers.particles(p, ParticleTypes.CLOUD, 30, 1.5, 0.3);
            Powers.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.5f);
            Powers.setTimer(p, "sonic_boom", 30 * 20);
        }

        // Static Build-Up: 3 seconds of running at tier 2+ charges your next hit.
        if (d.has(Secondary.STATIC_BUILD_UP)) {
            int run = (tier >= 2 && p.isSprinting()) ? RUN_TICKS.getOrDefault(p.getUUID(), 0) + 1 : 0;
            RUN_TICKS.put(p.getUUID(), run);
            if (run == 60) {
                Powers.setTimer(p, "static_ready", 20 * 60);
                p.sendOverlayMessage(Component.literal("Static charged!").withStyle(ChatFormatting.YELLOW));
            }
            if (Powers.timerActive(d, p.level(), "static_ready") && now % 5 == 0) {
                Powers.particles(p, ParticleTypes.ELECTRIC_SPARK, 2, 0.4, 0.05);
            }
        }
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        var source = event.getSource();
        if (Powers.timerActive(d, p.level(), "dash")) {
            event.setCanceled(true);
            return;
        }
        boolean attack = source.getEntity() != null;
        if (attack && d.has(Secondary.REFLEXES) && p.getRandom().nextFloat() < 0.05f + 0.05f * tier(d)) {
            dodge(event, p);
            return;
        }
        if (attack && d.has(Secondary.AFTERIMAGE) && p.isSprinting() && p.getRandom().nextFloat() < 0.25f) {
            dodge(event, p);
            return;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) event.setAmount(event.getAmount() * 0.6f);
    }

    private static void dodge(LivingIncomingDamageEvent event, ServerPlayer p) {
        event.setCanceled(true);
        Powers.particles(p, ParticleTypes.END_ROD, 6, 0.4, 0.02);
        p.sendOverlayMessage(Component.literal("Dodged!").withStyle(ChatFormatting.YELLOW));
    }

    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.MOMENTUM) && p.isSprinting()) {
            event.setAmount(event.getAmount() * (1f + 0.1f * tier(d)));
        }
        if (d.has(Secondary.STATIC_BUILD_UP) && Powers.timerActive(d, p.level(), "static_ready")) {
            event.setAmount(event.getAmount() + 4f * Powers.power(d));
            Powers.particles(target, ParticleTypes.ELECTRIC_SPARK, 20, 0.4, 0.3);
            Powers.clearTimer(p, "static_ready");
            RUN_TICKS.put(p.getUUID(), 0);
        }
    }

    // ------------------------------------------------------------------ actives

    public static boolean phaseDash(ServerPlayer p, MutationData d) {
        Vec3 look = p.getLookAngle();
        Vec3 dash = new Vec3(look.x * 2.2, Mth.clamp(look.y * 1.2, -0.4, 0.6), look.z * 2.2);
        Powers.setTimer(p, "dash", 12);
        Powers.impulse(p, dash, true);
        Powers.particles(p, ParticleTypes.END_ROD, 20, 0.3, 0.05);
        Powers.sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.8f);
        return true;
    }

    public static boolean blitz(ServerPlayer p, MutationData d) {
        Powers.startRush(p, 10 * 20, (3f + tier(d)) * Powers.power(d), 1.0);
        Powers.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 2.0f);
        p.sendOverlayMessage(Component.literal("BLITZ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        return true;
    }

    public static void changeSpeedTier(ServerPlayer p, int delta) {
        MutationData d = ModAttachments.get(p);
        int max = maxSpeedTier(d);
        int tier = Mth.clamp(d.speedTier() + Integer.signum(delta), 0, max);
        if (tier == d.speedTier()) {
            if (delta > 0) p.sendOverlayMessage(Component.literal("Max speed tier (" + max + ") - level up mastery to go faster")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        ModAttachments.set(p, d.withSpeedTier(tier));
        p.sendOverlayMessage(Component.literal("Speed tier " + tier + " / " + max).withStyle(ChatFormatting.YELLOW));
    }

    public static void forget(UUID id) {
        RUN_TICKS.remove(id);
    }
}
