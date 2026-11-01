package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Raycast;
import com.mutationsmod.mutations.data.MutationData;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Small helpers shared by the v0.19 mutations. */
public final class Kit {
    private Kit() {}

    // ---------------------------------------------------------------- meters (stored as named timers)

    public static int meter(MutationData d, String name) {
        return (int) d.timer("m_" + name);
    }

    public static void setMeter(ServerPlayer p, String name, int value) {
        ModAttachments.set(p, ModAttachments.get(p).withTimer("m_" + name, Math.max(0, value)));
    }

    public static void addMeter(ServerPlayer p, String name, int delta, int max) {
        int v = meter(ModAttachments.get(p), name);
        setMeter(p, name, Math.min(max, Math.max(0, v + delta)));
    }

    // ---------------------------------------------------------------- looking & beams

    public static LivingEntity look(ServerPlayer p, double range) {
        return Raycast.entity(p, range);
    }

    public static BlockHitResult lookBlock(ServerPlayer p, double range) {
        return Raycast.block(p, range);
    }

    /** Living things along a straight line from the eyes, stopping at the first solid block (or passing up to {@code pierceBlocks}). */
    public static List<LivingEntity> beam(ServerPlayer p, Vec3 from, Vec3 dir, double range, int pierceBlocks, ParticleOptions particle) {
        ServerLevel level = (ServerLevel) p.level();
        Set<LivingEntity> hit = new LinkedHashSet<>();
        Vec3 d = dir.normalize();
        int solids = 0;
        Vec3 end = from;
        for (double t = 0.5; t <= range; t += 0.5) {
            Vec3 at = from.add(d.scale(t));
            BlockPos bp = BlockPos.containing(at);
            if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) {
                if (++solids > pierceBlocks * 2) break;
            }
            end = at;
            hit.addAll(level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(0.5), e -> e != p && e.isAlive()));
            if (particle != null && ((int) (t * 2)) % 2 == 0) level.sendParticles(particle, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
        }
        return new ArrayList<>(hit);
    }

    public static List<LivingEntity> beam(ServerPlayer p, double range, int pierceBlocks, ParticleOptions particle) {
        return beam(p, p.getEyePosition().subtract(0, 0.15, 0), p.getLookAngle(), range, pierceBlocks, particle);
    }

    /** Where the player is looking: the block face hit, or the end of the reach. */
    public static Vec3 lookPoint(ServerPlayer p, double range) {
        BlockHitResult hit = lookBlock(p, range);
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : p.getEyePosition().add(p.getLookAngle().scale(range));
    }

    // ---------------------------------------------------------------- mobs

    public static boolean isUndead(LivingEntity e) {
        return e.getType().is(EntityTypeTags.UNDEAD);
    }

    public static boolean isMetal(ItemStack s) {
        if (s.isEmpty()) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        return id.contains("iron") || id.contains("chainmail") || id.contains("netherite") || id.contains("gold") || id.contains("copper");
    }

    public static boolean wearsMetal(LivingEntity e) {
        for (EquipmentSlot s : EquipmentSlot.values()) if (isMetal(e.getItemBySlot(s))) return true;
        return false;
    }

    public static int metalArmorPieces(LivingEntity e) {
        int n = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getItemBySlot(s).getItem()).getPath();
            if (id.contains("iron") || id.contains("chainmail") || id.contains("netherite")) n++;
        }
        return n;
    }

    /** Holds a mob in place. */
    public static void root(LivingEntity e, int ticks) {
        e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 9, false, true));
        e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
    }

    public static void glow(LivingEntity e, int ticks) {
        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
    }

    /** Hostile mobs around the player. */
    public static List<LivingEntity> hostiles(ServerPlayer p, double radius) {
        return Powers.around(p, radius).stream().filter(Powers::isHostile).filter(e -> !Minions.isMinion(e)).toList();
    }

    /** Everything around the player except players and the player's own minions. */
    public static List<LivingEntity> foes(ServerPlayer p, double radius) {
        return Powers.around(p, radius).stream().filter(e -> !(e instanceof net.minecraft.world.entity.player.Player))
                .filter(e -> !Minions.isMinion(e)).toList();
    }

    public static boolean onGroundish(LivingEntity e) {
        return e.onGround() && !e.isInWater();
    }

    /** A random walkable spot on the ground near {@code center}. */
    public static Vec3 groundNear(ServerLevel level, Vec3 center, double spread, net.minecraft.util.RandomSource r) {
        double x = center.x + (r.nextDouble() - 0.5) * 2 * spread, z = center.z + (r.nextDouble() - 0.5) * 2 * spread;
        BlockPos top = BlockPos.containing(x, center.y + 3, z);
        for (int i = 0; i < 8 && level.getBlockState(top.below()).getCollisionShape(level, top.below()).isEmpty(); i++) top = top.below();
        return new Vec3(x, top.getY(), z);
    }
}
