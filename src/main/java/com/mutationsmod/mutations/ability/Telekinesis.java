package com.mutationsmod.mutations.ability;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Kinetic Mind's block telekinesis. A grabbed block becomes a floating falling-block entity that
 * follows the player's gaze. Dropping it lets it fall and land; throwing launches it as a projectile.
 */
public final class Telekinesis {
    private Telekinesis() {}

    private record Held(FallingBlockEntity entity, long since) {}

    private static final Map<UUID, Held> HELD = new HashMap<>();
    /** Recent heavy uses, for the headache downside. */
    private static final Map<UUID, Deque<Long>> STRAIN = new HashMap<>();

    /** Max seconds a block can be held before it slips away. */
    private static final int MAX_HOLD_TICKS = 25 * 20;

    public static double range(MutationData d) {
        return Math.min(20.0, 6.0 + d.level() / 4.0);
    }

    /** Hardest block (by mining hardness) you can lift. Stone is 1.5, obsidian is 50. */
    public static float maxHardness(MutationData d) {
        return 3.0f + (d.level() - 1);
    }

    public static boolean isHolding(ServerPlayer player) {
        Held h = HELD.get(player.getUUID());
        return h != null && h.entity().isAlive();
    }

    public static boolean grab(ServerPlayer player, MutationData data) {
        ServerLevel level = (ServerLevel) player.level();
        BlockHitResult hit = Raycast.block(player, range(data));
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendOverlayMessage(Component.literal("No block in reach.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (state.isAir() || hardness < 0 || level.getBlockEntity(pos) != null) {
            player.sendOverlayMessage(Component.literal("That block won't budge.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (hardness > maxHardness(data)) {
            player.sendOverlayMessage(Component.literal("Too heavy for your mind... (master your mutation to lift it)")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
        entity.setNoGravity(true);
        HELD.put(player.getUUID(), new Held(entity, level.getGameTime()));
        level.playSound(null, pos, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.5f);
        addStrain(player);
        return true;
    }

    /** Called every server tick for every player. */
    static void tick(ServerPlayer player, MutationData data) {
        Held held = HELD.get(player.getUUID());
        if (held == null) return;
        FallingBlockEntity e = held.entity();
        ServerLevel level = (ServerLevel) player.level();
        if (!e.isAlive() || e.level() != level || !data.has(Mutation.KINETIC_MIND)) {
            release(player, false);
            return;
        }
        if (level.getGameTime() - held.since() > MAX_HOLD_TICKS) {
            player.sendOverlayMessage(Component.literal("Your focus slips...").withStyle(ChatFormatting.GRAY));
            release(player, false);
            return;
        }
        Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(3.0)).subtract(0, 0.5, 0);
        Vec3 pull = target.subtract(e.position()).scale(0.35);
        e.setDeltaMovement(pull);
        if (level.getGameTime() % 5 == 0) {
            level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 0.5, e.getZ(), 3, 0.3, 0.3, 0.3, 0.2);
        }
    }

    /** Lets go of the held block. If {@code thrown}, it's launched where the player is looking. */
    public static void release(ServerPlayer player, boolean thrown) {
        Held held = HELD.remove(player.getUUID());
        if (held == null) return;
        FallingBlockEntity e = held.entity();
        if (!e.isAlive()) return;
        e.setNoGravity(false);
        if (thrown) {
            float strength = 1.4f + ModAttachments.get(player).level() * 0.02f;
            e.setDeltaMovement(player.getLookAngle().scale(Math.min(3.0f, strength)));
            e.setHurtsEntities(2.0f, 40);
            ServerLevel level = (ServerLevel) player.level();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 0.6f);
        } else {
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** Heavy use of the mind causes headaches: 4 uses within 30 seconds gives nausea. */
    public static void addStrain(ServerPlayer player) {
        long now = player.level().getGameTime();
        Deque<Long> uses = STRAIN.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
        uses.addLast(now);
        while (!uses.isEmpty() && now - uses.peekFirst() > 30 * 20) uses.removeFirst();
        if (uses.size() >= 4) {
            uses.clear();
            player.addEffect(AbilityHandler.nausea(6 * 20));
            player.sendOverlayMessage(Component.literal("Your head is pounding...").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
