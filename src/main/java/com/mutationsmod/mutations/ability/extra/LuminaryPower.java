package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Luminary: store light (Radiance) and shape it into beams, flashes and bridges. */
public class LuminaryPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.LUMINARY; }

    public static final String RADIANCE = "radiance";

    private static int light(ServerPlayer p) { return p.level().getMaxLocalRawBrightness(p.blockPosition()); }

    /** Lights the player's surroundings like a carried torch (an invisible light block that follows them). */
    static void emitLight(ServerPlayer p, int level) {
        ServerLevel sl = (ServerLevel) p.level();
        BlockPos at = BlockPos.containing(p.getEyePosition());
        if (sl.getBlockState(at).isAir()) {
            Powers.placeTemporary(sl, at, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level), 8);
        }
    }

    private static boolean spend(ServerPlayer p, MutationData d, int base) {
        int cost = light(p) <= 6 ? base * 2 : base;
        int have = Kit.meter(d, RADIANCE);
        if (have < cost) {
            p.sendOverlayMessage(Component.literal("Not enough Radiance (" + have + "/" + cost + "). Stand in bright light.").withStyle(ChatFormatting.YELLOW));
            return false;
        }
        Kit.setMeter(p, RADIANCE, have - cost);
        return true;
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        int l = light(p);
        if (now % 10 == 0 && l >= (d.has(Secondary.RADIANT_AFTERGLOW) ? 8 : 12)) Kit.addMeter(p, RADIANCE, 2, 100);
        if (now % 40 == 0 && l <= 6 && !Catalysts.holding(p, Mutation.LUMINARY)) Kit.addMeter(p, RADIANCE, -1, 100);
        if (now % 4 == 0) emitLight(p, 12);
        int r = Kit.meter(d, RADIANCE);
        if (d.has(Secondary.PHOTOSYNTHESIS) && r >= 100 && now % 40 == 0) p.heal(1f);
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "lightfooted", d.has(Secondary.LIGHTFOOTED) && r > 50 ? 0.2 : 0);
        ServerLevel level = (ServerLevel) p.level();
        if (d.has(Secondary.HALO) && now % 40 == 0) {
            for (LivingEntity e : Kit.foes(p, 6)) if (Kit.isUndead(e)) Powers.hurt(p, e, 1f);
        }
        if (Powers.timerActive(d, level, "solar_flare") && now % 20 == 0) {
            for (LivingEntity e : Kit.foes(p, 20)) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                if (Kit.isUndead(e)) { e.igniteForSeconds(3); Powers.hurt(p, e, 2f); }
            }
            for (Player pl : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(8))) pl.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.5, p.getZ(), 60, 3, 2, 3, 0.2);
            emitLight(p, 15);
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (Powers.timerActive(d, p.level(), "mirror_ward") && e.getSource().getDirectEntity() instanceof Projectile proj) {
            e.setCanceled(true);
            proj.setDeltaMovement(proj.getDeltaMovement().scale(-1));
            proj.setOwner(p);
            proj.needsSync = true;
            return;
        }
        if (d.has(Secondary.BLINDING_AURA) && e.getSource().getEntity() instanceof LivingEntity att && att != p) {
            att.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        }
    }

    /** One beam: pierces up to 3 mobs, bounces off a wall once. */
    private static void lance(ServerPlayer p, MutationData d, Vec3 dir) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 from = p.getEyePosition().subtract(0, 0.15, 0);
        Set<LivingEntity> hit = new HashSet<>();
        double left = 32;
        for (int bounce = 0; bounce < 2 && left > 0; bounce++) {
            Vec3 to = from.add(dir.scale(left));
            BlockHitResult bh = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 end = bh.getType() == HitResult.Type.BLOCK ? bh.getLocation() : to;
            for (double t = 0; t <= end.distanceTo(from); t += 0.5) {
                Vec3 at = from.add(dir.scale(t));
                if (((int) (t * 2)) % 2 == 0) level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                if (hit.size() >= 3) continue;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(0.5), e -> e != p && e.isAlive())) {
                    if (hit.size() < 3 && hit.add(e)) Powers.hurt(p, e, 8f * Powers.power(d) * (Kit.isUndead(e) ? 1.5f : 1f));
                }
            }
            if (bh.getType() != HitResult.Type.BLOCK) break;
            left -= end.distanceTo(from);
            Direction face = bh.getDirection();
            dir = switch (face.getAxis()) {
                case X -> new Vec3(-dir.x, dir.y, dir.z);
                case Y -> new Vec3(dir.x, -dir.y, dir.z);
                case Z -> new Vec3(dir.x, dir.y, -dir.z);
            };
            from = end.add(dir.scale(0.05));
        }
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case PRISM_LANCE -> {
                if (!spend(p, d, 15)) return false;
                Vec3 look = p.getLookAngle();
                lance(p, d, look);
                if (d.has(Secondary.REFRACTION)) {
                    lance(p, d, look.yRot(0.14f));
                    lance(p, d, look.yRot(-0.14f));
                }
                Powers.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.6f);
                return true;
            }
            case FLASH -> {
                if (!spend(p, d, 25)) return false;
                for (LivingEntity e : Kit.foes(p, 8)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                    Kit.root(e, 20);
                }
                level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 120, 1, 1, 1, 0.5);
                emitLight(p, 15);
                Powers.sound(p, SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 1.5f);
                return true;
            }
            case LIGHT_BRIDGE -> {
                if (!spend(p, d, 30)) return false;
                bridge(p, 16, Blocks.STAINED_GLASS.white().defaultBlockState(), 300);
                Powers.sound(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.5f);
                return true;
            }
            case BEACON_PULSE -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(32), e -> e != p && e.isAlive())) Kit.glow(e, 200);
                for (int y = 0; y < 30; y++) level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + y, p.getZ(), 2, 0.1, 0.2, 0.1, 0);
                return true;
            }
            case MIRROR_WARD -> {
                Powers.setTimer(p, "mirror_ward", 100);
                Powers.particles(p, ParticleTypes.END_ROD, 30, 0.6, 0.05);
                return true;
            }
            default -> { return false; }
        }
    }

    /** A straight walkway of temporary blocks in the direction the player faces (rising or falling with their pitch). */
    static void bridge(ServerPlayer p, int length, net.minecraft.world.level.block.state.BlockState state, int ticks) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        double rise = Math.max(-0.7, Math.min(0.7, look.y));
        Vec3 start = p.position().add(0, -0.5, 0);
        for (int i = 1; i <= length; i++) {
            Vec3 at = start.add(flat.scale(i)).add(0, rise * i, 0);
            BlockPos pos = BlockPos.containing(at);
            Powers.placeTemporary(level, pos, state, ticks);
            Vec3 side = new Vec3(-flat.z, 0, flat.x);
            Powers.placeTemporary(level, BlockPos.containing(at.add(side.scale(0.9))), state, ticks);
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "solar_flare", 200);
        Powers.sound(p, SoundEvents.BEACON_ACTIVATE, 2f, 1.4f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (Kit.isUndead(target)) {
            e.setAmount(e.getAmount() * 1.5f);
            target.igniteForSeconds(4);
        }
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "lightfooted", 0);
    }
}
