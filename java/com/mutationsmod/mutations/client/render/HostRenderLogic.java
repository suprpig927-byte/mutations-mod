package com.mutationsmod.mutations.client.render;

import com.mutationsmod.mutations.data.MutationData;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Works out where each Host tendril should reach this frame: grabbing a target, or gripping a wall. */
public final class HostRenderLogic {
    private HostRenderLogic() {}

    /** A Tendril ability strike in progress, as told by the server. */
    public record Strike(int targetEntity, Vec3 targetPos, long start, int duration) {}

    /** player entity id -> current strike */
    public static final Map<Integer, Strike> STRIKES = new ConcurrentHashMap<>();

    /** Tendril anchors on the back, in model pixels (must match TendrilModel). */
    private static final float[][] ANCHORS = {{-2.5f, 2.5f}, {2.5f, 2.5f}, {-2.0f, 7.0f}, {2.0f, 7.0f}};

    public static HostRenderData compute(Player player, MutationData d, float bodyRotDeg, float partialTick) {
        Level level = player.level();
        long now = level.getGameTime();
        boolean bonded = d.timer("bonded") > 0;
        float lash = Math.max(0, d.timer("lash_anim") - now) / 12f;
        float[] yaw = new float[4], pitch = new float[4], len = new float[4];

        Vec3 pos = player.getPosition(partialTick);

        // Wall grip: bonded, in the air, with a wall right next to you.
        Direction wall = bonded && !player.onGround() ? wallSide(player) : null;
        if (wall != null) {
            Vec3 normal = new Vec3(wall.getStepX(), 0, wall.getStepZ());
            Vec3 side = new Vec3(-normal.z, 0, normal.x);
            double toFace = 0.3 + 0.05;
            Vec3[] targets = {
                    pos.add(normal.scale(toFace)).add(side.scale(-0.7)).add(0, 1.8, 0),
                    pos.add(normal.scale(toFace)).add(side.scale(0.7)).add(0, 1.8, 0),
                    pos.add(normal.scale(toFace)).add(side.scale(-0.6)).add(0, 0.3, 0),
                    pos.add(normal.scale(toFace)).add(side.scale(0.6)).add(0, 0.3, 0)};
            for (int t = 0; t < 4; t++) aim(t, pos, bodyRotDeg, targets[t], 1f, yaw, pitch, len);
        }

        // Tendril ability: one tendril shoots out, grabs, and stays attached while pulling.
        Strike s = STRIKES.get(player.getId());
        if (s != null) {
            float progress = (now + partialTick - s.start()) / (float) s.duration();
            if (progress >= 1f || progress < 0f) {
                STRIKES.remove(player.getId());
            } else {
                Vec3 target = s.targetPos();
                if (s.targetEntity() >= 0) {
                    Entity e = level.getEntity(s.targetEntity());
                    if (e != null) target = e.getPosition(partialTick).add(0, e.getBbHeight() * 0.5, 0);
                }
                float ext = progress < 0.2f ? progress / 0.2f : progress > 0.85f ? (1f - progress) / 0.15f : 1f;
                aim(0, pos, bodyRotDeg, target, ext, yaw, pitch, len);
            }
        }
        return new HostRenderData(bonded, lash, yaw, pitch, len);
    }

    /** Which horizontal side has a solid block at chest height, if any. */
    public static Direction wallSide(Player player) {
        Level level = player.level();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (double dy : new double[]{0.5, 1.4}) {
                BlockPos bp = BlockPos.containing(player.getX() + dir.getStepX() * 0.5, player.getY() + dy, player.getZ() + dir.getStepZ() * 0.5);
                if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) return dir;
            }
        }
        return null;
    }

    /**
     * Points tendril {@code t} at a world position. Converts world space into the player model's
     * space (the renderer turns the model to face the body's yaw and flips it upside down).
     */
    private static void aim(int t, Vec3 playerPos, float bodyRotDeg, Vec3 target, float ext,
                            float[] yaw, float[] pitch, float[] len) {
        double a = Math.toRadians(180.0 - bodyRotDeg);
        double cos = Math.cos(a), sin = Math.sin(a);
        // Anchor: model point (ax, ay, 2) pixels -> world.
        double mx = -ANCHORS[t][0] / 16.0, my = 1.501 - ANCHORS[t][1] / 16.0, mz = 2.0 / 16.0;
        Vec3 anchor = playerPos.add(mx * cos + mz * sin, my, -mx * sin + mz * cos);
        Vec3 w = target.subtract(anchor);
        // World direction -> model direction: rotate back by -a, then flip x and y.
        double lx = w.x * Math.cos(-a) + w.z * Math.sin(-a);
        double lz = -w.x * Math.sin(-a) + w.z * Math.cos(-a);
        Vec3 m = new Vec3(-lx, -w.y, lz);
        double dist = m.length();
        if (dist < 1.0E-3) return;
        Vec3 dir = m.scale(1.0 / dist);
        pitch[t] = (float) Math.asin(-dir.y);
        yaw[t] = (float) Math.atan2(dir.x, dir.z);
        len[t] = (float) (dist * 16.0 * ext);
    }
}
