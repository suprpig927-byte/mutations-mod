package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Raycast;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class Host {
    private Host() {}

    /** Ticks the organism can go between kills before it starts feeding on you. */
    public static int graceTicks(MutationData d) {
        return (d.has(Secondary.WELL_FED) ? 5 : 3) * 60 * 20;
    }

    public static void tick(ServerPlayer p, MutationData d) {
        long now = p.level().getGameTime();
        long fed = d.timer("host_fed");
        if (fed == 0) {
            // First tick as a Host: start the feeding clock now. (The timer stores the time of the last kill.)
            Powers.setTimer(p, "host_fed", 0);
            fed = now;
        }
        long sinceFed = now - fed;

        Powers.add(p, Attributes.ENTITY_INTERACTION_RANGE, "host_reach", 1.5);
        // Stronger right after a kill.
        Powers.add(p, Attributes.ATTACK_DAMAGE, "host_fed", sinceFed < 60 * 20 ? 3.0 : 0);

        if (sinceFed > graceTicks(d) && now % 100 == 0) {
            Powers.hurtSelf(p, 1f);
            p.sendOverlayMessage(Component.literal("The organism is starving... it feeds on you.").withStyle(ChatFormatting.DARK_RED));
        }

        if (d.has(Secondary.SHAPELESS)) updateCrawl(p);
    }

    /** Shapeless: sneaking into a 1-block-high gap forces a crawl. Mirrored on the client. */
    public static void updateCrawl(Player p) {
        BlockPos ahead = p.blockPosition().relative(p.getDirection());
        boolean gap = p.level().getBlockState(ahead).getCollisionShape(p.level(), ahead).isEmpty()
                && !p.level().getBlockState(ahead.above()).getCollisionShape(p.level(), ahead.above()).isEmpty();
        if (p.isShiftKeyDown() && gap) {
            p.setForcedPose(Pose.SWIMMING);
        } else if (!p.isShiftKeyDown()) {
            p.setForcedPose(null);
        }
    }

    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.PARASITE)) p.heal(event.getAmount() * 0.2f);
    }

    public static void onKill(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "host_fed", 0); // stores "now": the moment it last fed
        if (d.has(Secondary.DEVOUR)) p.getFoodData().setFoodLevel(Math.min(20, p.getFoodData().getFoodLevel() + 3));
        Powers.particles(p, ParticleTypes.SQUID_INK, 6, 0.3, 0.02);
    }

    // ------------------------------------------------------------------ actives

    public static boolean tendril(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        LivingEntity target = Raycast.entity(p, 16);
        if (target != null) {
            Vec3 pull = p.position().subtract(target.position());
            Powers.impulse(target, pull.normalize().scale(Math.min(2.0, pull.length() * 0.25)).add(0, 0.35, 0), false);
            Powers.hurt(p, target, 2f * Powers.power(d));
            Powers.line(level, p.getEyePosition(), target.getBoundingBox().getCenter(), ParticleTypes.SQUID_INK, 0.4);
        } else {
            BlockHitResult hit = Raycast.block(p, 20);
            if (hit.getType() != HitResult.Type.BLOCK) {
                p.sendOverlayMessage(Component.literal("Nothing to grab.").withStyle(ChatFormatting.GRAY));
                return false;
            }
            Vec3 pull = hit.getLocation().subtract(p.position());
            Powers.impulse(p, pull.normalize().scale(Math.min(2.2, pull.length() * 0.25)).add(0, 0.4, 0), true);
            Powers.line(level, p.getEyePosition(), hit.getLocation(), ParticleTypes.SQUID_INK, 0.4);
        }
        Powers.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 0.6f);
        return true;
    }

    public static boolean bondedArmor(ServerPlayer p, MutationData d) {
        int ticks = (int) ((d.has(Secondary.SYMBIOTE_ARMOR) ? 240 : 120) * Powers.power(d));
        Powers.effect(p, MobEffects.RESISTANCE, ticks, 1);
        Powers.effect(p, MobEffects.STRENGTH, ticks, 0);
        Powers.particles(p, ParticleTypes.SQUID_INK, 30, 0.5, 0.05);
        Powers.sound(p, SoundEvents.ANVIL_HIT, 0.5f, 0.6f);
        return true;
    }

    public static boolean lash(ServerPlayer p, MutationData d) {
        for (LivingEntity e : Powers.around(p, 5)) {
            if (!Powers.isHostile(e) && !(e instanceof Player)) continue;
            Powers.hurt(p, e, 5f * Powers.power(d));
            Vec3 pull = p.position().subtract(e.position());
            Powers.impulse(e, pull.normalize().scale(0.6).add(0, 0.2, 0), false);
            Powers.line((ServerLevel) p.level(), p.getEyePosition(), e.getBoundingBox().getCenter(), ParticleTypes.SQUID_INK, 0.5);
        }
        Powers.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
        return true;
    }
}
