package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Shadow: merge into the shadows where it's dark; bright light pulls you back out. */
public class ShadowPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.SHADOW; }

    public static boolean merged(MutationData d) { return d.timer("merged") > 0; }

    private static int light(ServerPlayer p) {
        return p.level().getMaxLocalRawBrightness(p.blockPosition());
    }

    private static int limit(ServerPlayer p) {
        return Catalysts.holding(p, Mutation.SHADOW) ? 13 : 10;
    }

    private static boolean eclipse(MutationData d, ServerPlayer p) {
        return Powers.timerActive(d, p.level(), "eclipse");
    }

    private static void merge(ServerPlayer p) {
        ModAttachments.set(p, ModAttachments.get(p).withTimer("merged", 1).withTimer("pull_start", 0));
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p)) m.setTarget(null);
        Powers.particles(p, ParticleTypes.SQUID_INK, 30, 0.4, 0.02);
        Powers.sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.4f, 0.5f);
    }

    private static void unmerge(ServerPlayer p, boolean forced) {
        MutationData d = ModAttachments.get(p);
        ModAttachments.set(p, d.withTimer("merged", 0).withTimer("shadow_exit", p.level().getGameTime()));
        p.removeEffect(MobEffects.INVISIBILITY);
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "shadow_speed", 0);
        Powers.particles(p, ParticleTypes.SQUID_INK, 20, 0.4, 0.02);
        if (forced) {
            if (!d.has(Secondary.DEEP_DARK)) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            p.sendOverlayMessage(Component.literal("The light drags you out of the shadows!").withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        Level level = p.level();
        boolean day = level.dimension() == Level.OVERWORLD && !Powers.isNight(level) && level.getBrightness(LightLayer.SKY, p.blockPosition()) >= 15;
        if (day && !merged(d) && now % 20 == 0) Powers.effect(p, MobEffects.WEAKNESS, 40, 0);
        if (d.has(Secondary.NIGHT_EYES) && now % 100 == 0) Powers.effect(p, MobEffects.NIGHT_VISION, 400, 0);
        if (!merged(d)) {
            Powers.mult(p, Attributes.MOVEMENT_SPEED, "shadow_speed", 0);
            return;
        }
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "shadow_speed", 0.3);
        if (now % 20 == 0) p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
        if (d.has(Secondary.SHADE_CLOAK) && now % 40 == 0) p.heal(1f);
        if (now % 4 == 0 && level instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.1, p.getZ(), 1, 0.2, 0, 0.2, 0);
        if (eclipse(d, p) || now % 5 != 0) return;
        if (light(p) > limit(p)) {
            if (d.has(Secondary.LIGHTLESS)) {
                long start = d.timer("pull_start");
                if (start == 0) { ModAttachments.set(p, d.withTimer("pull_start", now)); return; }
                if (now - start < 60) return;
            }
            unmerge(p, true);
        } else if (d.timer("pull_start") != 0) {
            ModAttachments.set(p, d.withTimer("pull_start", 0));
        }
    }

    @Override
    public boolean ignoredBy(ServerPlayer p, MutationData d, Mob mob) {
        return merged(d);
    }

    @Override
    public void onDealDamage(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        long now = p.level().getGameTime();
        if (merged(d)) {
            if (!eclipse(d, p)) {
                e.setCanceled(true);         // can't attack from the shadows (except during Eclipse)
                return;
            }
            e.setAmount(e.getAmount() * 2f);
        }
        if (d.has(Secondary.UMBRAL_STRIKE) && d.timer("shadow_exit") > 0 && now - d.timer("shadow_exit") < 100) {
            e.setAmount(e.getAmount() * 3f);
            ModAttachments.set(p, ModAttachments.get(p).withTimer("shadow_exit", 0));
            Powers.particles(target, ParticleTypes.SQUID_INK, 20, 0.3, 0.05);
        }
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case SHADOW_SLIP -> {
                if (merged(d)) { unmerge(p, false); return true; }
                if (light(p) > 7 && !eclipse(d, p)) {
                    p.sendOverlayMessage(Component.literal("It's too bright here to slip into the shadows (light " + light(p) + ", needs 7 or less).").withStyle(ChatFormatting.GRAY));
                    return false;
                }
                merge(p);
                return true;
            }
            case SHADOW_STEP -> {
                BlockHitResult hit = Kit.lookBlock(p, 24);
                Vec3 target = hit.getType() == HitResult.Type.BLOCK ? Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection())) : p.getEyePosition().add(p.getLookAngle().scale(24));
                Vec3 spot = Powers.safeSpot(p, target);
                if (spot == null) { p.sendOverlayMessage(Component.literal("No room there.").withStyle(ChatFormatting.GRAY)); return false; }
                if (level.getMaxLocalRawBrightness(BlockPos.containing(spot)) > 7 && !eclipse(d, p)) {
                    p.sendOverlayMessage(Component.literal("That spot is too bright.").withStyle(ChatFormatting.GRAY));
                    return false;
                }
                level.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 25, 0.3, 0.6, 0.3, 0.02);
                Powers.teleport(p, spot);
                level.sendParticles(ParticleTypes.SQUID_INK, spot.x, spot.y + 1, spot.z, 25, 0.3, 0.6, 0.3, 0.02);
                Powers.sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 0.5f);
                return true;
            }
            case SNUFF -> {
                int n = 0;
                BlockPos c = p.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(c.offset(-8, -4, -8), c.offset(8, 6, 8))) {
                    BlockState s = level.getBlockState(pos);
                    if (s.getLightEmission(level, pos) <= 0 || !s.getFluidState().isEmpty() || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)
                            || s.is(Blocks.NETHER_PORTAL) || s.is(Blocks.LIGHT)) continue;
                    if (Powers.freezeTemporary(level, pos.immutable(), Blocks.AIR.defaultBlockState(), 400)) {
                        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.01);
                        n++;
                    }
                }
                Powers.sound(p, SoundEvents.FIRE_EXTINGUISH, 1f, 0.6f);
                p.sendOverlayMessage(Component.literal("Snuffed out " + n + " lights.").withStyle(ChatFormatting.DARK_GRAY));
                return true;
            }
            case SHADOW_CLONE -> {
                var vex = Minions.spawn(p, EntityTypes.VEX, p.position().add(0, 1, 0), 200, "Shadow");
                return vex != null;
            }
            case GRASPING_SHADOWS -> {
                Vec3 at = Kit.lookPoint(p, 24);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(6), e -> e != p && e.isAlive())) {
                    Kit.root(e, 60);
                    level.sendParticles(ParticleTypes.SQUID_INK, e.getX(), e.getY() + 0.3, e.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
                }
                Powers.sound(p, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.5f, 0.6f);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        Powers.setTimer(p, "eclipse", 600);
        if (!merged(d)) merge(p);
        ServerLevel level = (ServerLevel) p.level();
        Zones.add(level, p.position(), 24, 600, p.getUUID(), z -> {
            ServerPlayer o = z.ownerPlayer();
            if (o == null) return;
            z.center = o.position();
            if (z.age() % 20 == 0) {
                for (LivingEntity e : Kit.foes(o, 24)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
                }
            }
            if (z.age() % 3 == 0) level.sendParticles(ParticleTypes.SQUID_INK, o.getX(), o.getY() + 2, o.getZ(), 20, 10, 2, 10, 0);
        }, null);
        Powers.sound(p, SoundEvents.WARDEN_EMERGE, 1f, 0.8f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        if (light(p) <= 7) e.setAmount(e.getAmount() * 1.4f);
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "shadow_speed", 0);
    }

    /** Merged shadows can't break blocks. */
    public static void onBreak(net.neoforged.neoforge.event.level.block.BreakBlockEvent e) {
        if (e.getPlayer() instanceof ServerPlayer p && merged(ModAttachments.get(p)) && ModAttachments.get(p).has(Mutation.SHADOW)) e.setCanceled(true);
    }
}
