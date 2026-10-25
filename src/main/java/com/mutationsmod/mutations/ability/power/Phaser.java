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

    /** Speed Punch flurries in progress. */
    private static final class Flurry {
        final LivingEntity target;
        int hitsLeft;
        final float damage;

        Flurry(LivingEntity target, int hits, float damage) {
            this.target = target;
            this.hitsLeft = hits;
            this.damage = damage;
        }
    }

    private static final Map<UUID, Flurry> FLURRIES = new HashMap<>();

    public static int maxSpeedTier(MutationData d) {
        return Math.min(5, 1 + d.level() / 8);
    }

    /** Kinetic Soles: one extra speed tier (up to 6). */
    public static int maxSpeedTier(MutationData d, net.minecraft.world.entity.player.Player p) {
        return maxSpeedTier(d) + (com.mutationsmod.mutations.item.Catalysts.holding(p, com.mutationsmod.mutations.mutation.Mutation.PHASER) ? 1 : 0);
    }

    public static int tier(MutationData d) {
        return Math.min(d.speedTier(), Math.min(6, maxSpeedTier(d) + 1));
    }

    public static void tick(ServerPlayer p, MutationData d) {
        if (d.speedTier() > maxSpeedTier(d, p)) {          // dropped the Kinetic Soles: come back down
            d = d.withSpeedTier(maxSpeedTier(d, p));
            ModAttachments.set(p, d);
        }
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
            for (LivingEntity e : Powers.around(p, 15)) {
                if (!Powers.isHostile(e)) continue;
                Powers.hurt(p, e, 3f * Powers.power(d));
                Powers.knockAway(e, p.position(), 1.4, 0.4);
            }
            Powers.particles(p, ParticleTypes.SONIC_BOOM, 1, 0, 0);
            Powers.particles(p, ParticleTypes.CLOUD, 60, 4.0, 0.4);
            Powers.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.5f);
            Powers.setTimer(p, "sonic_boom", 30 * 20);
        }

        // Speed Punch: one hit every other tick until the flurry ends.
        Flurry f = FLURRIES.get(p.getUUID());
        if (f != null && now % 2 == 0) {
            if (f.hitsLeft <= 0 || !f.target.isAlive() || f.target.distanceTo(p) > 5) {
                FLURRIES.remove(p.getUUID());
            } else {
                f.hitsLeft--;
                f.target.setInvulnerableTime(0);
                Powers.hurt(p, f.target, f.damage);
                f.target.setDeltaMovement(f.target.getDeltaMovement().multiply(0.3, 1, 0.3));
                Powers.particles(f.target, ParticleTypes.CRIT, 6, 0.4, 0.2);
                Powers.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.4f, 1.8f);
            }
        }
        if (Powers.timerActive(d, p.level(), "vibrate") && now % 2 == 0) {
            Powers.particles(p, ParticleTypes.END_ROD, 3, 0.3, 0.02);
        }
        if (Powers.timerActive(d, p.level(), "phasing") && now % 4 == 0) {
            Powers.particles(p, ParticleTypes.PORTAL, 3, 0.3, 0.1);
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
        if (Powers.timerActive(d, p.level(), "dash") || Powers.timerActive(d, p.level(), "vibrate")) {
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

    public static boolean speedPunch(ServerPlayer p, MutationData d) {
        LivingEntity target = com.mutationsmod.mutations.ability.Raycast.entity(p, 4.5);
        if (target == null) {
            p.sendOverlayMessage(Component.literal("Nothing in punching range.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        int hits = 4 + 2 * tier(d);
        FLURRIES.put(p.getUUID(), new Flurry(target, hits, 1.5f * Powers.power(d)));
        return true;
    }

    public static boolean vibrate(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "vibrate", (long) (40 * Powers.power(d)));
        for (LivingEntity e : Powers.around(p, 4)) Powers.knockAway(e, p.position(), 1.2, 0.3);
        Powers.particles(p, ParticleTypes.CLOUD, 20, 0.6, 0.1);
        Powers.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.3f, 2.0f);
        return true;
    }

    public static boolean phaseThrough(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "phasing", (long) (6 * 20 * Powers.power(d)));
        Powers.sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.4f, 2.0f);
        p.sendOverlayMessage(Component.literal("Phasing... walk into a wall").withStyle(ChatFormatting.YELLOW));
        return true;
    }

    /**
     * Called by the client when a phasing player walks into a wall. Moves them to the first open
     * space behind it (up to 3 blocks), at the same height: walls only, never floors.
     */
    public static void tryPhase(ServerPlayer p) {
        MutationData d = ModAttachments.get(p);
        if (!d.has(com.mutationsmod.mutations.mutation.Mutation.PHASER) || !Powers.timerActive(d, p.level(), "phasing")) return;
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0E-4) return;
        look = look.normalize();
        Vec3 start = p.position();
        for (double dist = 1.0; dist <= 3.5; dist += 0.5) {
            Vec3 to = start.add(look.scale(dist));
            var box = p.getBoundingBox().move(to.subtract(start));
            if (p.level().noCollision(p, box)) {
                if (dist <= 1.0) return; // nothing to phase through
                Powers.particles(p, ParticleTypes.PORTAL, 15, 0.3, 0.2);
                Powers.teleport(p, to);
                Powers.sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.3f, 2.0f);
                return;
            }
        }
    }

    public static boolean blitz(ServerPlayer p, MutationData d) {
        Powers.startRush(p, 10 * 20, (3f + tier(d)) * Powers.power(d), 1.0);
        Powers.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 2.0f);
        p.sendOverlayMessage(Component.literal("BLITZ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        return true;
    }

    public static void changeSpeedTier(ServerPlayer p, int delta) {
        MutationData d = ModAttachments.get(p);
        int max = maxSpeedTier(d, p);
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
        FLURRIES.remove(id);
    }
}
