package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Raycast;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class Blinker {
    private Blinker() {}

    private record Snapshot(Vec3 pos, long time) {}

    /** Where each Blinker was over the last few seconds, for Recall. */
    private static final Map<UUID, Deque<Snapshot>> HISTORY = new HashMap<>();

    public static void tick(ServerPlayer p, MutationData d) {
        long now = p.level().getGameTime();
        if (now % 5 == 0) {
            Deque<Snapshot> h = HISTORY.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
            h.addLast(new Snapshot(p.position(), now));
            while (h.size() > 30) h.removeFirst();
        }
        // Mobs lose track of you easily.
        if (now % 20 == 0) {
            for (Mob mob : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p)) {
                if (mob.distanceTo(p) > 8 && p.getRandom().nextFloat() < 0.35f) mob.setTarget(null);
            }
        }
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        if (event.getAmount() <= 0 || p.getRandom().nextFloat() >= 0.3f) return;
        // Downside: an uncontrolled blink when hit.
        Vec3 dir;
        Entity attacker = event.getSource().getEntity();
        if (d.has(Secondary.CONTROLLED_CHAOS) && attacker != null) {
            dir = p.position().subtract(attacker.position()).multiply(1, 0, 1);
            if (dir.lengthSqr() < 1.0E-4) dir = p.getLookAngle().reverse();
            dir = dir.normalize().yRot((p.getRandom().nextFloat() - 0.5f) * 0.8f);
        } else {
            double angle = p.getRandom().nextDouble() * Math.PI * 2;
            dir = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }
        double dist = 3 + p.getRandom().nextDouble() * 3;
        Vec3 spot = Powers.safeSpot(p, p.position().add(dir.scale(dist)));
        if (spot != null) {
            blinkFx(p);
            Powers.teleport(p, spot);
            blinkFx(p);
        }
    }

    private static void blinkFx(Entity e) {
        Powers.particles(e, ParticleTypes.PORTAL, 25, 0.4, 0.4);
        Powers.sound(e, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.4f);
    }

    // ------------------------------------------------------------------ actives

    public static boolean blink(ServerPlayer p, MutationData d) {
        double range = 16 * Math.min(1.5f, Powers.power(d));
        BlockHitResult hit = Raycast.block(p, range);
        Vec3 target;
        if (hit.getType() == HitResult.Type.BLOCK) {
            // Land on top of / in front of what you looked at.
            target = hit.getLocation().subtract(p.getLookAngle().scale(0.6));
            target = new Vec3(target.x, Math.floor(target.y + 0.01), target.z);
        } else {
            target = p.getEyePosition().add(p.getLookAngle().scale(range)).subtract(0, p.getEyeHeight(), 0);
        }
        Vec3 spot = Powers.safeSpot(p, target);
        if (spot == null) {
            p.sendOverlayMessage(Component.literal("No room to blink there.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        blinkFx(p);
        Powers.teleport(p, spot);
        if (d.has(Secondary.VOID_STEP)) p.resetFallDistance();
        blinkFx(p);
        return true;
    }

    public static boolean displace(ServerPlayer p, MutationData d) {
        LivingEntity target = Raycast.entity(p, 16);
        if (target == null) {
            p.sendOverlayMessage(Component.literal("No target.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        for (int i = 0; i < 16; i++) {
            double angle = p.getRandom().nextDouble() * Math.PI * 2;
            double dist = 5 + p.getRandom().nextDouble() * 7;
            Vec3 spot = Powers.safeSpot(target, target.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist));
            if (spot != null) {
                blinkFx(target);
                Powers.teleport(target, spot);
                blinkFx(target);
                return true;
            }
        }
        return false;
    }

    public static boolean recall(ServerPlayer p, MutationData d) {
        Deque<Snapshot> h = HISTORY.get(p.getUUID());
        long now = p.level().getGameTime();
        if (h == null || h.isEmpty()) return false;
        Snapshot best = h.peekFirst();
        for (Snapshot s : h) {
            if (now - s.time() >= 100) best = s;
        }
        blinkFx(p);
        Powers.teleport(p, best.pos());
        blinkFx(p);
        return true;
    }

    public static boolean swap(ServerPlayer p, MutationData d) {
        LivingEntity target = Raycast.entity(p, 24);
        if (target == null) {
            p.sendOverlayMessage(Component.literal("No target.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        Vec3 mine = p.position();
        Vec3 theirs = target.position();
        blinkFx(p);
        blinkFx(target);
        Powers.teleport(p, theirs);
        Powers.teleport(target, mine);
        return true;
    }

    /** Pocket Space: opens your ender chest anywhere. */
    public static void openPocket(ServerPlayer p) {
        MutationData d = ModAttachments.get(p);
        if (!d.has(Mutation.BLINKER) || !d.has(Secondary.POCKET_SPACE)) return;
        p.openMenu(new SimpleMenuProvider((id, inv, player) -> ChestMenu.threeRows(id, inv, player.getEnderChestInventory()),
                Component.literal("Pocket Space")));
        Powers.sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.4f, 2.0f);
    }

    public static void forget(UUID id) {
        HISTORY.remove(id);
    }

    /** Reused for the dimension check in commands etc. */
    static ServerLevel level(ServerPlayer p) {
        return (ServerLevel) p.level();
    }
}
