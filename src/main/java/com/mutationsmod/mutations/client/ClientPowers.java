package com.mutationsmod.mutations.client;

import com.mutationsmod.mutations.ability.power.Chameleon;
import com.mutationsmod.mutations.ability.power.Host;
import com.mutationsmod.mutations.ability.power.Phaser;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Mutation effects that have to run on the player's own client: movement (flight steering,
 * climbing, sinking, water running) and seeing mobs through walls.
 */
public final class ClientPowers {
    private ClientPowers() {}

    private static int sprintTicks = 0;
    private static long lastPhaseRequest = 0;
    private static int sprintLock = 0;
    /** Entity ids that should glow for this player (read by the render-state modifier). */
    public static final Set<Integer> GLOWING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void tick(Minecraft mc, LocalPlayer p, MutationData d) {
        ClientLevel level = mc.level;
        long now = level.getGameTime();
        Vec3 dm = p.getDeltaMovement();
        boolean flying = p.getAbilities().flying;

        // Superman-style flight: hold forward to soar wherever you look.
        boolean flameFlight = d.has(Mutation.PYRO) && d.timer("flame_flight") > now;
        boolean rainFlight = d.has(Secondary.RAINWALKER) && level.isRainingAt(p.blockPosition().above());
        if (flying && !p.isCreative() && (flameFlight || rainFlight) && mc.options.keyUp.isDown()) {
            p.setDeltaMovement(p.getLookAngle().scale(flameFlight ? 1.0 : 0.9));
            dm = p.getDeltaMovement();
        }

        // Phase Through: walking into a wall asks the server to slip you through it.
        if (d.has(Mutation.PHASER) && d.timer("phasing") > now && p.horizontalCollision
                && mc.options.keyUp.isDown() && now - lastPhaseRequest >= 5) {
            lastPhaseRequest = now;
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(
                    new com.mutationsmod.mutations.network.ModPayloads.ScreenAction(
                            com.mutationsmod.mutations.network.ModPayloads.ScreenAction.PHASE, ""));
        }

        // Colossus: sinks in water, can't sprint for long.
        if (d.has(Mutation.COLOSSUS)) {
            if (p.isInWater() && !flying) p.setDeltaMovement(dm.x, dm.y - 0.035, dm.z);
            if (sprintLock > 0) {
                sprintLock--;
                if (p.isSprinting()) p.setSprinting(false);
            } else if (p.isSprinting()) {
                if (++sprintTicks > 100) {
                    p.setSprinting(false);
                    sprintTicks = 0;
                    sprintLock = 60;
                }
            } else {
                sprintTicks = Math.max(0, sprintTicks - 2);
            }
            dm = p.getDeltaMovement();
        }

        // Hydroplane: run across water.
        if (d.has(Secondary.HYDROPLANE) && Phaser.tier(d) >= 3 && p.isSprinting() && !flying) {
            BlockPos feet = p.blockPosition();
            if (p.isInWater()) {
                p.setDeltaMovement(dm.x, Math.max(dm.y, 0.15), dm.z);
            } else if (level.getFluidState(feet.below()).is(FluidTags.WATER) && dm.y < 0) {
                p.setDeltaMovement(dm.x, 0, dm.z);
            }
            dm = p.getDeltaMovement();
        }

        // Wall climbing (Host, or Copycat after killing a spider).
        boolean climber = d.has(Mutation.HOST) || d.timer(Chameleon.Trait.CLIMB.timer()) > now;
        if (climber && p.horizontalCollision && mc.options.keyUp.isDown() && !flying) {
            double up = d.has(Secondary.WALL_CRAWLER) ? 0.3 : 0.2;
            p.setDeltaMovement(dm.x, Math.max(dm.y, up), dm.z);
            p.resetFallDistance();
            dm = p.getDeltaMovement();
        }
        // Wall Crawler: hold jump under a ceiling to cling to it.
        if (d.has(Secondary.WALL_CRAWLER) && mc.options.keyJump.isDown() && !p.onGround() && !flying) {
            BlockPos above = BlockPos.containing(p.getX(), p.getBoundingBox().maxY + 0.2, p.getZ());
            if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                p.setDeltaMovement(dm.x * 0.8, 0.08, dm.z * 0.8);
                p.resetFallDistance();
            }
        }
        if (d.has(Secondary.SHAPELESS) && d.has(Mutation.HOST)) Host.updateCrawl(p);

        // Snow Stride: climb out of powder snow freely.
        if (d.has(Secondary.SNOW_STRIDE) && level.getBlockState(p.blockPosition()).is(Blocks.POWDER_SNOW) && !p.isShiftKeyDown()) {
            dm = p.getDeltaMovement();
            p.setDeltaMovement(dm.x, Math.max(dm.y, 0.1), dm.z);
        }

        updateGlow(level, p, d);
    }

    /** Siren and Spatial Sense: nearby creatures glow for this player only. */
    private static void updateGlow(ClientLevel level, LocalPlayer p, MutationData d) {
        double range = 0;
        boolean players = false;
        if (d.has(Mutation.SIREN)) range = d.has(Secondary.ECHOLOCATION) ? 32 : 16;
        if (d.has(Secondary.SPATIAL_SENSE)) {
            range = Math.max(range, 24);
            players = true;
        }
        Set<Integer> want = new HashSet<>();
        if (range > 0) {
            boolean includePlayers = players;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range),
                    e -> e != p && e.isAlive() && (includePlayers || !(e instanceof Player)))) {
                want.add(e.getId());
            }
        }
        GLOWING.retainAll(want);
        GLOWING.addAll(want);
    }

    /** Tuned Ears: the nearest hostile mobs, with a direction arrow relative to where you face. */
    public static List<String> tunedEars(ClientLevel level, LocalPlayer p) {
        List<LivingEntity> mobs = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24),
                e -> e.isAlive() && e.getType().getCategory() == MobCategory.MONSTER));
        mobs.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        List<String> out = new ArrayList<>();
        for (LivingEntity e : mobs) {
            if (out.size() >= 4) break;
            Vec3 to = e.position().subtract(p.position());
            double angle = Math.toDegrees(Math.atan2(-to.x, to.z)) - p.getYRot();
            angle = ((angle % 360) + 540) % 360 - 180; // -180..180, 0 = straight ahead
            String arrow = Math.abs(angle) <= 45 ? "↑" : Math.abs(angle) >= 135 ? "↓" : angle > 0 ? "→" : "←";
            out.add(arrow + " " + e.getType().getDescription().getString() + " " + (int) Math.sqrt(e.distanceToSqr(p)) + "m");
        }
        return out;
    }
}
