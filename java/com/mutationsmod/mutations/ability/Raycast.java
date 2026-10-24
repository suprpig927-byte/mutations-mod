package com.mutationsmod.mutations.ability;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Small helpers for "what is the player looking at". */
public final class Raycast {
    private Raycast() {}

    public static BlockHitResult block(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        return player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    /** The closest living entity the player is looking at (not blocked by walls), or null. */
    public static @Nullable LivingEntity entity(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult wall = block(player, range);
        double maxDist = wall.getType() == HitResult.Type.MISS ? range : wall.getLocation().distanceTo(start);
        Vec3 end = start.add(look.scale(maxDist));

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(1.0);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(0.3).clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = e;
                }
            }
        }
        return best;
    }

    /** The point the player is looking at: an entity, else a block, else max range in the air. */
    public static Vec3 target(Player player, double range) {
        LivingEntity e = entity(player, range);
        if (e != null) return e.position();
        BlockHitResult b = block(player, range);
        if (b.getType() != HitResult.Type.MISS) return b.getLocation();
        return player.getEyePosition().add(player.getLookAngle().scale(range));
    }
}
