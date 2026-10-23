package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class Colossus {
    private Colossus() {}

    public static void tick(ServerPlayer p, MutationData d) {
        Powers.add(p, Attributes.ATTACK_DAMAGE, "colossus_damage", 4.0);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "colossus_knockback", d.has(Secondary.UNSTOPPABLE) ? 1.0 : 0.6);
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "colossus_slow", -0.15);
        Powers.mult(p, Attributes.BLOCK_BREAK_SPEED, "colossus_mining", d.has(Secondary.IRON_GRIP) ? 1.6 : 0.8);
        Powers.add(p, Attributes.OXYGEN_BONUS, "deep_lungs", d.has(Secondary.DEEP_LUNGS) ? 3.0 : 0);
        Powers.add(p, Attributes.ENTITY_INTERACTION_RANGE, "titans_reach", d.has(Secondary.TITANS_REACH) ? 1.0 : 0);
        Powers.add(p, Attributes.ARMOR, "bulwark", d.has(Secondary.BULWARK) && p.isShiftKeyDown() ? 6 : 0);
    }

    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.HEAVY_HITTER) && p.getRandom().nextFloat() < 0.33f) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 6));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 2));
            Powers.particles(target, ParticleTypes.CRIT, 15, 0.4, 0.2);
        }
    }

    // ------------------------------------------------------------------ actives

    public static boolean groundPound(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        double radius = d.has(Secondary.EARTHSHAKER) ? 7.5 : 5.0;
        float damage = 6f * Powers.power(d);
        for (LivingEntity e : Powers.around(p, radius)) {
            Powers.hurt(p, e, damage);
            Powers.knockAway(e, p.position(), 1.0, 0.6);
        }
        BlockState under = level.getBlockState(p.blockPosition().below());
        if (!under.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under, p.blockPosition().below()),
                    p.getX(), p.getY() + 0.1, p.getZ(), 80, radius * 0.4, 0.1, radius * 0.4, 0.2);
        }
        Powers.particles(p, ParticleTypes.EXPLOSION, 3, 1.0, 0);
        Powers.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.7f);
        return true;
    }

    /** Leaps about 3 blocks up. */
    public static boolean titanLeap(ServerPlayer p, MutationData d) {
        Powers.impulse(p, new Vec3(0, 0.75, 0), false);
        Powers.particles(p, ParticleTypes.CLOUD, 10, 0.4, 0.05);
        return true;
    }

    public static boolean charge(ServerPlayer p, MutationData d) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        Powers.impulse(p, look.scale(1.8).add(0, 0.2, 0), true);
        Powers.startRush(p, 15, 5f * Powers.power(d), 1.6);
        Powers.particles(p, ParticleTypes.CLOUD, 20, 0.5, 0.1);
        Powers.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
        return true;
    }
}
