package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Raycast;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class Cryo {
    private Cryo() {}

    /** Ice made by Cryo powers melts back after this long. */
    private static final int ICE_LIFETIME = 30 * 20;

    public static boolean isHot(ServerPlayer p) {
        if (Powers.isNether(p.level())) return true;
        return Powers.temperature(p) > 1.0f && !Powers.isNight(p.level());
    }

    public static boolean isCold(ServerPlayer p) {
        return Powers.temperature(p) < 0.3f;
    }

    /** Power multiplier: weakened by heat. */
    public static float heat(ServerPlayer p, MutationData d) {
        if (!isHot(p)) return 1f;
        return d.has(Secondary.COLD_BLOODED) ? 0.8f : 0.6f;
    }

    public static void tick(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        long now = level.getGameTime();
        p.setTicksFrozen(0);

        // Freeze water underfoot (like Frost Walker).
        BlockPos feet = p.blockPosition();
        if (!p.isShiftKeyDown()) {
            for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-2, -1, -2), feet.offset(2, -1, 2))) {
                if (pos.distSqr(feet.below()) > 5) continue;
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.WATER) && state.getFluidState().isSource() && level.getBlockState(pos.above()).isAir()) {
                    level.setBlock(pos, Blocks.FROSTED_ICE.defaultBlockState(), 3);
                    level.scheduleTick(pos.immutable(), Blocks.FROSTED_ICE, 60 + p.getRandom().nextInt(60));
                }
            }
        }

        if (now % 20 == 0) {
            if (isHot(p)) {
                Powers.effect(p, MobEffects.WEAKNESS, 40, 0);
                if (!d.has(Secondary.COLD_BLOODED)) Powers.effect(p, MobEffects.SLOWNESS, 40, 0);
            }
            BlockState under = level.getBlockState(feet.below());
            BlockState in = level.getBlockState(feet);
            boolean onSnowOrIce = isFrozen(under) || isFrozen(in);
            if (d.has(Secondary.ICE_BODY) && onSnowOrIce && now % 40 == 0) p.heal(1f);
            if (d.has(Secondary.SNOW_STRIDE) && (onSnowOrIce || in.is(Blocks.POWDER_SNOW))) Powers.effect(p, MobEffects.SPEED, 40, 1);
        }
        Powers.add(p, Attributes.ARMOR, "glacier_skin",
                d.has(Secondary.GLACIER_SKIN) && (isCold(p) || Powers.isNight(level)) ? 4 : 0);
    }

    private static boolean isFrozen(BlockState s) {
        return s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE) || s.is(Blocks.FROSTED_ICE)
                || s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW);
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        if (event.getSource().is(DamageTypes.FREEZE)) event.setCanceled(true);
    }

    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.SHATTER) && (target.hasEffect(MobEffects.SLOWNESS) || target.getTicksFrozen() > 0)) {
            event.setAmount(event.getAmount() * 1.5f);
            Powers.particles(target, ParticleTypes.SNOWFLAKE, 12, 0.4, 0.1);
        }
        if (d.has(Secondary.FROSTBITE)) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
        }
    }

    // ------------------------------------------------------------------ actives

    public static boolean iceBlast(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        boolean perma = d.has(Secondary.PERMAFROST);
        float scale = Powers.power(d) * heat(p, d);
        LivingEntity target = Raycast.entity(p, 24);
        Vec3 end;
        if (target != null) {
            end = target.getBoundingBox().getCenter();
            Powers.hurt(p, target, 5f * scale);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SLOWNESS, perma ? 120 : 60, 2));
            target.setTicksFrozen(perma ? 300 : 200);
            // Cage the target's feet in packed ice.
            BlockPos base = target.blockPosition();
            int life = perma ? 10 * 20 : 6 * 20;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                Powers.placeTemporary(level, base.relative(dir), Blocks.PACKED_ICE.defaultBlockState(), life);
            }
        } else {
            BlockHitResult hit = Raycast.block(p, 24);
            end = hit.getLocation();
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos at = hit.getBlockPos().relative(hit.getDirection());
                Powers.placeTemporary(level, at, Blocks.PACKED_ICE.defaultBlockState(), ICE_LIFETIME);
                for (Direction dir : Direction.values()) {
                    if (p.getRandom().nextFloat() < 0.45f) {
                        Powers.placeTemporary(level, at.relative(dir), Blocks.PACKED_ICE.defaultBlockState(), ICE_LIFETIME);
                    }
                }
            }
        }
        Powers.line(level, p.getEyePosition().add(p.getLookAngle()), end, ParticleTypes.SNOWFLAKE, 0.5);
        Powers.sound(p, SoundEvents.GLASS_PLACE, 1.0f, 0.6f);
        return true;
    }

    public static boolean iceWall(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        Direction facing = p.getDirection();
        BlockPos center = p.blockPosition().relative(facing, 3);
        Direction side = facing.getClockWise();
        int placed = 0;
        for (int w = -1; w <= 1; w++) {
            for (int h = 0; h < 3; h++) {
                if (Powers.placeTemporary(level, center.relative(side, w).above(h), Blocks.PACKED_ICE.defaultBlockState(), 20 * 20)) placed++;
            }
        }
        if (placed == 0) return false;
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 0.5, 40, 1.2, 1.2, 1.2, 0.05);
        Powers.sound(p, SoundEvents.GLASS_PLACE, 1.0f, 0.5f);
        return true;
    }
}
