package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.entity.FeatherDart;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Fierce Wings: permanent hawk wings. Jump in mid-air to glide (vanilla elytra flight, granted through NeoForge's
 * gliding attribute); press jump while gliding to flap upward. No fall damage. No chestplates, and fire burns
 * 50% hotter.
 */
public final class Wings {
    private Wings() {}

    /** The client sends this ability slot when the player flaps. */
    public static final int FLAP_SLOT = 98;

    public static void tick(ServerPlayer p, MutationData d) {
        Powers.add(p, NeoForgeMod.GLIDING_FLIGHT, "fierce_wings", 1);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            if (!p.getInventory().add(chest)) p.drop(chest, false, Prediction.SERVER_ONLY);
            p.sendOverlayMessage(Component.literal("A chestplate won't fit over your wings.").withStyle(ChatFormatting.YELLOW));
        }
        long now = p.level().getGameTime();
        // Dive Strike lands.
        if (d.timer("dive") > 0 && (p.onGround() || p.isInWater())) {
            land(p, d);
        } else if (d.timer("dive") > 0 && now % 2 == 0) {
            Powers.particles(p, ParticleTypes.CLOUD, 3, 0.3, 0.02);
        }
        // Keen Eye: handled client-side. Wing Shield: slow and sparkly while wrapped.
        if (Powers.timerActive(d, p.level(), "wing_shield")) {
            Powers.effect(p, MobEffects.SLOWNESS, 10, 1);
            for (Projectile proj : p.level().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(2.5), pr -> pr.getOwner() != p)) {
                proj.setDeltaMovement(proj.getDeltaMovement().scale(-0.6));
                proj.needsSync = true;
            }
        }
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        var src = event.getSource();
        if (src.is(DamageTypes.FALL) || src.is(DamageTypes.FLY_INTO_WALL)) {
            event.setCanceled(true);
            return;
        }
        if (src.is(DamageTypeTags.IS_FIRE)) event.setAmount(event.getAmount() * 1.5f);
        if (Powers.timerActive(d, p.level(), "wing_shield")) event.setAmount(event.getAmount() * 0.5f);
    }

    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.PREDATOR) && !p.onGround()) event.setAmount(event.getAmount() * 1.25f);
    }

    // ------------------------------------------------------------------ flapping

    /** A wing beat (sent by the client, which already applied the lift). Costs a little hunger. */
    public static void flap(ServerPlayer p) {
        MutationData d = ModAttachments.get(p);
        if (!d.has(Mutation.FIERCE_WINGS)) return;
        if (!com.mutationsmod.mutations.item.Catalysts.holding(p, Mutation.FIERCE_WINGS)) p.causeFoodExhaustion(0.6f);   // Gale Feather
        Powers.setTimer(p, "flap_anim", 8);
        ServerLevel level = (ServerLevel) p.level();
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.8, p.getZ(), 6, 0.6, 0.1, 0.6, 0.02);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    // ------------------------------------------------------------------ actives

    public static boolean featherVolley(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        float dmg = 4f * Powers.power(d) * (d.has(Secondary.RAZOR_PLUMES) ? 1.3f : 1f);
        for (int i = -2; i <= 2; i++) {
            FeatherDart f = new FeatherDart(level, p, dmg, d.has(Secondary.RAZOR_PLUMES));
            f.shootFromRotation(p, p.getXRot(), p.getYRot() + i * 7f, 0f, 2.2f, 0.5f);
            level.addFreshEntity(f);
        }
        Powers.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 0.6f, 1.8f);
        return true;
    }

    public static boolean wingGust(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        for (LivingEntity e : Powers.cone(p, 8, 0.4)) {
            Powers.hurt(p, e, 3f * Powers.power(d));
            Powers.knockAway(e, p.position(), 1.6, 0.5);
        }
        Vec3 look = p.getLookAngle();
        for (Projectile proj : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(8), pr -> pr.getOwner() != p)) {
            proj.setDeltaMovement(look.scale(1.2));
            proj.needsSync = true;
        }
        for (int i = 1; i <= 6; i++) {
            Vec3 at = p.getEyePosition().add(look.scale(i));
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 5, 0.4 + i * 0.15, 0.4, 0.4 + i * 0.15, 0.05);
        }
        if (!p.onGround()) Powers.impulse(p, look.scale(-0.5).add(0, 0.3, 0), false);
        Powers.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.9f);
        return true;
    }

    public static boolean diveStrike(ServerPlayer p, MutationData d) {
        if (p.onGround()) {
            p.sendOverlayMessage(Component.literal("You need to be in the air to dive.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        Powers.impulse(p, look.normalize().scale(0.5).add(0, -2.2, 0), true);
        ModAttachments.set(p, ModAttachments.get(p).withTimer("dive", (long) Math.max(1, p.getY())));
        Powers.sound(p, SoundEvents.PHANTOM_SWOOP, 1.0f, 1.2f);
        return true;
    }

    private static void land(ServerPlayer p, MutationData d) {
        double fell = Math.max(0, d.timer("dive") - p.getY());
        ModAttachments.set(p, ModAttachments.get(p).withTimer("dive", 0));
        float dmg = (float) Math.min(24, 4 + fell * 0.8) * Powers.power(d) * (d.has(Secondary.TALON_STRIKE) ? 1.5f : 1f);
        ServerLevel level = (ServerLevel) p.level();
        for (LivingEntity e : Powers.around(p, 3.5)) {
            Powers.hurt(p, e, dmg);
            Powers.knockAway(e, p.position(), 0.9, 0.5);
            if (d.has(Secondary.TALON_STRIKE)) e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3));
        }
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                level.getBlockState(p.blockPosition().below())), p.getX(), p.getY(), p.getZ(), 40, 1.5, 0.1, 1.5, 0.2);
        level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.3, p.getZ(), 1, 0, 0, 0, 0);
        Powers.sound(p, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.0f, 1.0f);
    }

    public static boolean wingShield(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "wing_shield", 80);
        Powers.sound(p, SoundEvents.ARMOR_EQUIP_ELYTRA.value(), 1.0f, 0.8f);
        return true;
    }

    public static boolean updraft(ServerPlayer p, MutationData d) {
        Powers.impulse(p, new Vec3(0, 2.4, 0), true);
        ServerLevel level = (ServerLevel) p.level();
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 30, 0.8, 0.2, 0.8, 0.1);
        Powers.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.2f);
        return true;
    }

    /** Skyfall: a few feathers rain down on hostile mobs below. */
    public static void skyfallTick(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        float dmg = 5f * Powers.power(d);
        var targets = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(14, 40, 14),
                e -> e != p && e.isAlive() && e.getY() < p.getY() && Powers.isHostile(e));
        for (int i = 0; i < 3; i++) {
            Vec3 at;
            if (!targets.isEmpty() && p.getRandom().nextFloat() < 0.7f) {
                LivingEntity t = targets.get(p.getRandom().nextInt(targets.size()));
                at = t.position().add(p.getRandom().nextGaussian() * 0.6, 0, p.getRandom().nextGaussian() * 0.6);
            } else {
                at = p.position().add(p.getRandom().nextGaussian() * 8, -30, p.getRandom().nextGaussian() * 8);
            }
            FeatherDart f = new FeatherDart(level, p, dmg, true);
            f.setPos(at.x, Math.min(p.getY() - 1, at.y + 12), at.z);
            f.setDeltaMovement(0, -2.0, 0);
            level.addFreshEntity(f);
        }
    }
}
