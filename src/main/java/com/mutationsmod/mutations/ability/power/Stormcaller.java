package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Raycast;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class Stormcaller {
    private Stormcaller() {}

    /** Weather multiplier for all Stormcaller powers: weaker in clear skies. */
    public static float weather(Level level) {
        if (level.isThundering()) return 1.25f;
        if (level.isRaining()) return 1.0f;
        return 0.5f;
    }

    public static boolean inRain(ServerPlayer p) {
        return p.level().isRainingAt(p.blockPosition().above()) || com.mutationsmod.mutations.item.Catalysts.holding(p, com.mutationsmod.mutations.mutation.Mutation.STORMCALLER); // Storm Rod
    }

    /** Weather multiplier for this player: the Storm Rod counts as rain. */
    public static float weather(ServerPlayer p) {
        float w = weather(p.level());
        if (com.mutationsmod.mutations.ability.Omega.eyeOfTheStorm(com.mutationsmod.mutations.ModAttachments.get(p), p)) return Math.max(1.25f, w);
        return com.mutationsmod.mutations.item.Catalysts.holding(p, com.mutationsmod.mutations.mutation.Mutation.STORMCALLER) ? Math.max(1.0f, w) : w;
    }

    public static void tick(ServerPlayer p, MutationData d) {
        long now = p.level().getGameTime();
        boolean rain = inRain(p);
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "storm_rain_speed", rain ? 0.2 : 0.0);

        if (now % 20 == 0) {
            if (d.has(Secondary.EYE_OF_THE_STORM) && p.level().isRaining()) Powers.effect(p, MobEffects.NIGHT_VISION, 260, 0);
            if (d.has(Secondary.GALE_STEP) && p.level().isRaining()) Powers.effect(p, MobEffects.JUMP_BOOST, 40, 2);
        }

        // Rainwalker: creative-style flight while it rains on you; float down gently when it stops.
        boolean canFly = ((d.has(Secondary.RAINWALKER) && rain) || com.mutationsmod.mutations.ability.Omega.eyeOfTheStorm(d, p)) && !p.isCreative() && !p.isSpectator();
        boolean flameFlight = d.has(Mutation.PYRO) && Powers.timerActive(d, p.level(), "flame_flight");
        if (!flameFlight) setFlight(p, canFly);

        // Tell clients when we're flying in the rain (for the wind rings).
        boolean storming = canFly && p.getAbilities().flying;
        if (storming != (d.timer("storm_fly") > 0)) {
            com.mutationsmod.mutations.ModAttachments.set(p,
                    com.mutationsmod.mutations.ModAttachments.get(p).withTimer("storm_fly", storming ? 1 : 0));
        }
        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) p.level();
        if (storming && now % 2 == 0) {
            // Swirling wind and sparks around your legs.
            double a = now * 0.5;
            for (int k = 0; k < 2; k++) {
                double ang = a + k * Math.PI;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        p.getX() + Math.cos(ang) * 0.6, p.getY() + 0.3, p.getZ() + Math.sin(ang) * 0.6, 0, 0, -0.2, 0, 0.1);
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    p.getX(), p.getY() + 0.4, p.getZ(), 2, 0.3, 0.3, 0.3, 0.05);
        } else if (rain && now % 5 == 0) {
            // Sparks crackle off your hands in the rain.
            double yaw = Math.toRadians(p.yBodyRot);
            double sx = Math.cos(yaw) * 0.38, sz = Math.sin(yaw) * 0.38;
            for (int s = -1; s <= 1; s += 2) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        p.getX() + sx * s, p.getY() + 0.7, p.getZ() + sz * s, 2, 0.08, 0.1, 0.08, 0.05);
            }
        }
    }

    /** Grants or removes mod flight via NeoForge's creative-flight attribute. */
    public static void setFlight(ServerPlayer p, boolean allowed) {
        boolean had = p.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT) > 0;
        Powers.add(p, NeoForgeMod.CREATIVE_FLIGHT, "mutation_flight", allowed ? 1 : 0);
        if (had && !allowed && p.getAbilities().flying && !p.isCreative() && !p.isSpectator()) {
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            Powers.effect(p, MobEffects.SLOW_FALLING, 200, 0);
        }
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        if (event.getSource().is(DamageTypes.LIGHTNING_BOLT)) {
            event.setCanceled(true);
            p.clearFire();
        }
    }

    public static void onStruck(EntityStruckByLightningEvent event, ServerPlayer p, MutationData d) {
        event.setCanceled(true);
        if (d.has(Secondary.CHARGED_UP) && !Powers.timerActive(d, p.level(), "charged_up")) {
            p.heal(6f);
            Powers.effect(p, MobEffects.SPEED, 200, 1);
            Powers.setTimer(p, "charged_up", 20);
            Powers.particles(p, ParticleTypes.ELECTRIC_SPARK, 30, 0.5, 0.3);
        }
    }

    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.STATIC_CHARGE) && p.getRandom().nextFloat() < 0.25f) {
            float zap = 3f * weather(p) * Powers.power(d);
            event.setAmount(event.getAmount() + zap);
            Powers.particles(target, ParticleTypes.ELECTRIC_SPARK, 15, 0.4, 0.2);
            List<LivingEntity> near = target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5),
                    e -> e != target && e != p && e.isAlive() && Powers.isHostile(e));
            near.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(target))).ifPresent(e -> {
                Powers.hurt(p, e, zap * 0.7f);
                Powers.line((ServerLevel) p.level(), target.getBoundingBox().getCenter(), e.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 0.4);
            });
        }
    }

    // ------------------------------------------------------------------ actives

    public static boolean lightningStrike(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 target = Raycast.target(p, 48);
        float damage = (7f + d.level() * 0.1f) * weather(p) * Powers.power(d);
        bolt(level, p, target, damage);
        if (d.has(Secondary.CONDUCTOR)) {
            level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(target, target).inflate(6),
                            e -> e != p && e.isAlive() && Powers.isHostile(e) && e.position().distanceTo(target) > 1.5)
                    .stream().sorted(Comparator.comparingDouble(e -> e.position().distanceToSqr(target))).limit(2)
                    .forEach(e -> bolt(level, p, e.position(), damage * 0.8f));
        }
        if (!level.isRaining()) {
            p.sendOverlayMessage(Component.literal("The clear sky weakens your lightning...").withStyle(ChatFormatting.GRAY));
        }
        return true;
    }

    private static void bolt(ServerLevel level, ServerPlayer p, Vec3 at, float damage) {
        LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
        bolt.setPos(at);
        bolt.setCause(p);
        bolt.setDamage(damage);
        level.addFreshEntity(bolt);
    }

    public static boolean summonStorm(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        var server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "weather thunder 6000");
        Powers.particles(p, ParticleTypes.ELECTRIC_SPARK, 80, 1.0, 0.3);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(p.getName().getString() + " has summoned a storm!").withStyle(ChatFormatting.AQUA), false);
        return true;
    }

    public static boolean thunderclap(ServerPlayer p, MutationData d) {
        float damage = 4f * weather(p) * Powers.power(d);
        for (LivingEntity e : Powers.around(p, 6)) {
            Powers.hurt(p, e, damage);
            Powers.knockAway(e, p.position(), 1.5 * weather(p), 0.5);
        }
        Powers.particles(p, ParticleTypes.ELECTRIC_SPARK, 60, 2.5, 0.3);
        Powers.particles(p, ParticleTypes.CLOUD, 30, 2.0, 0.2);
        Powers.sound(p, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 1.4f);
        return true;
    }

    public static void forget(ServerPlayer p) {
        if (!ModAttachments.get(p).has(Mutation.PYRO)) setFlight(p, false);
    }
}
