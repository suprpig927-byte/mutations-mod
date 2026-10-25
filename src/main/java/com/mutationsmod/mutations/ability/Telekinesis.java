package com.mutationsmod.mutations.ability;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.mutationsmod.mutations.network.ModPayloads;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Kinetic Mind's block telekinesis, earthbender style.
 *
 * <p>A grabbed block becomes a falling-block entity, but the mod moves it itself ("noPhysics"),
 * so vanilla never gets the chance to land it or break it into an item. Every tick its exact
 * position is sent to nearby players, and the holder's own client positions it every frame, so
 * it glides smoothly. Thrown or dropped blocks fly with our own physics and, the moment they hit
 * something, set themselves into the world as a real block right where they struck.
 */
public final class Telekinesis {
    private Telekinesis() {}

    /** A block under telekinetic control: either held by a player or flying through the air. */
    private static final class Kinetic {
        final FallingBlockEntity entity;
        final UUID owner;
        Vec3 velocity = Vec3.ZERO;
        float damageScale = 1f;
        long flyingSince;
        final Set<Integer> alreadyHit = new HashSet<>();

        Kinetic(FallingBlockEntity entity, UUID owner) {
            this.entity = entity;
            this.owner = owner;
        }
    }

    private record Held(Kinetic block, long since, int slot) {}

    private static final Map<UUID, List<Held>> HELD = new HashMap<>();
    /** Omega Gravity Well: blocks circling a player before they're all hurled. */
    private record Orbit(List<Kinetic> blocks, long start, int duration, float damageScale) {}
    private static final Map<UUID, Orbit> ORBITS = new HashMap<>();
    private static final List<Kinetic> FLYING = new ArrayList<>();
    /** Recent heavy uses, for the headache downside. */
    private static final Map<UUID, Deque<Long>> STRAIN = new HashMap<>();

    /** Max time a block can be held before focus slips (vanilla breaks falling blocks after 30s total). */
    private static final int MAX_HOLD_TICKS = 20 * 20;
    private static final int MAX_FLIGHT_TICKS = 8 * 20;
    private static final double HOLD_DISTANCE = 3.0;
    private static final double GRAVITY = 0.04;
    private static final double DRAG = 0.98;

    public static double range(MutationData d) {
        return Math.min(20.0, 6.0 + d.level() / 4.0) + (d.has(Secondary.LONG_REACH) ? 10.0 : 0.0);
    }

    /** How many blocks can be held at once (Heavy Lifter: 3). */
    /** Focus Prism: +50% range. */
    public static double range(MutationData d, net.minecraft.world.entity.player.Player p) {
        return range(d) * (com.mutationsmod.mutations.item.Catalysts.holding(p, com.mutationsmod.mutations.mutation.Mutation.KINETIC_MIND) ? 1.5 : 1.0);
    }

    /** Focus Prism: one extra block. */
    public static int capacity(MutationData d, net.minecraft.world.entity.player.Player p) {
        return capacity(d) + (com.mutationsmod.mutations.item.Catalysts.holding(p, com.mutationsmod.mutations.mutation.Mutation.KINETIC_MIND) ? 1 : 0);
    }

    public static int capacity(MutationData d) {
        return d.has(Secondary.HEAVY_LIFTER) ? 3 : 1;
    }

    /** Hardest block (by mining hardness) you can lift. Stone is 1.5, obsidian is 50. */
    public static float maxHardness(MutationData d) {
        return 3.0f + (d.level() - 1);
    }

    /**
     * Where a held block sits: in front of the eyes (slot 0), or to the left (1) / right (2) of it.
     * Shared with the client so both agree.
     */
    public static Vec3 holdPosition(Vec3 eye, Vec3 look, int slot) {
        Vec3 pos = eye.add(look.scale(HOLD_DISTANCE)).subtract(0, 0.5, 0);
        if (slot == 0) return pos;
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
        right = right.normalize().scale(1.3);
        return slot == 1 ? pos.subtract(right) : pos.add(right);
    }

    private static List<Held> held(ServerPlayer player) {
        List<Held> list = HELD.get(player.getUUID());
        if (list == null) return List.of();
        list.removeIf(h -> !h.block().entity.isAlive());
        return list;
    }

    public static boolean isHolding(ServerPlayer player) {
        return !held(player).isEmpty();
    }

    /**
     * The Telekinesis key: grab what you're looking at if you have room,
     * otherwise let go of everything. Returns true if something was grabbed.
     */
    public static boolean toggle(ServerPlayer player, MutationData data) {
        List<Held> list = held(player);
        if (!list.isEmpty()) {
            if (list.size() < capacity(data, player) && grabbable(player, data)) return grab(player, data);
            release(player, false);
            return false;
        }
        return grab(player, data);
    }

    private static boolean grabbable(ServerPlayer player, MutationData data) {
        BlockHitResult hit = Raycast.block(player, range(data, player));
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        ServerLevel level = (ServerLevel) player.level();
        BlockState state = level.getBlockState(hit.getBlockPos());
        float hardness = state.getDestroySpeed(level, hit.getBlockPos());
        return !state.isAir() && hardness >= 0 && hardness <= maxHardness(data) && level.getBlockEntity(hit.getBlockPos()) == null;
    }

    // ------------------------------------------------------------------ grab

    public static boolean grab(ServerPlayer player, MutationData data) {
        ServerLevel level = (ServerLevel) player.level();
        BlockHitResult hit = Raycast.block(player, range(data, player));
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
        entity.noPhysics = true; // we move it ourselves; vanilla must never land it
        entity.setDeltaMovement(Vec3.ZERO);
        List<Held> list = HELD.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        int slot = 0;
        while (slotTaken(list, slot)) slot++;
        list.add(new Held(new Kinetic(entity, player.getUUID()), level.getGameTime(), slot));
        level.playSound(null, pos, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.5f);
        addStrain(player);
        return true;
    }

    // ------------------------------------------------------------------ Gravity Well (Omega)

    /** Lifts every block at {@code positions} into orbit around the player; hurls them after {@code duration} ticks. */
    public static int startOrbit(ServerPlayer player, List<BlockPos> positions, int duration, float damageScale) {
        ServerLevel level = (ServerLevel) player.level();
        List<Kinetic> blocks = new ArrayList<>();
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || level.getBlockEntity(pos) != null) continue;
            FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
            entity.setNoGravity(true);
            entity.noPhysics = true;
            entity.setDeltaMovement(Vec3.ZERO);
            blocks.add(new Kinetic(entity, player.getUUID()));
        }
        if (!blocks.isEmpty()) ORBITS.put(player.getUUID(), new Orbit(blocks, level.getGameTime(), duration, damageScale));
        return blocks.size();
    }

    private static void tickOrbit(ServerLevel level, ServerPlayer owner, Orbit orbit) {
        long t = level.getGameTime() - orbit.start();
        int n = orbit.blocks().size();
        if (t >= orbit.duration()) {
            ORBITS.remove(owner.getUUID());
            Vec3 aim = Raycast.target(owner, 48);
            for (Kinetic k : orbit.blocks()) {
                if (!k.entity.isAlive()) continue;
                Vec3 dir = aim.subtract(k.entity.position().add(0, 0.5, 0)).normalize();
                k.velocity = dir.scale(2.4);
                k.damageScale = orbit.damageScale();
                k.flyingSince = level.getGameTime();
                FLYING.add(k);
                broadcast(k, -1, 0);
            }
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.2f, 0.4f);
            return;
        }
        double spin = t * 0.18;
        double radius = 2.6 + Math.min(1.4, t * 0.05);
        for (int i = 0; i < n; i++) {
            Kinetic k = orbit.blocks().get(i);
            if (!k.entity.isAlive()) continue;
            double a = spin + i * Math.PI * 2 / n;
            double ring = (i % 2 == 0) ? radius : radius + 1.1;
            double y = owner.getY() + 1.0 + (i % 3) * 0.9 + Math.sin(a * 2 + i) * 0.3;
            k.entity.setPos(owner.getX() + Math.cos(a) * ring, y, owner.getZ() + Math.sin(a) * ring);
            k.entity.setDeltaMovement(Vec3.ZERO);
            broadcast(k, -1, 0);
        }
        if (t % 3 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, owner.getX(), owner.getY() + 1.5, owner.getZ(), 12, radius, 1.2, radius, 0.05);
    }

    // ------------------------------------------------------------------ release

    private static boolean slotTaken(List<Held> list, int slot) {
        for (Held h : list) if (h.slot() == slot) return true;
        return false;
    }

    /** Lets go of every held block. If {@code thrown}, they're launched as a volley where the player is looking. */
    public static void release(ServerPlayer player, boolean thrown) {
        List<Held> list = HELD.remove(player.getUUID());
        if (list == null) return;
        MutationData data = ModAttachments.get(player);
        for (Held held : list) {
            Kinetic k = held.block();
            if (!k.entity.isAlive()) continue;
            ServerLevel level = (ServerLevel) k.entity.level();
            if (thrown) {
                double strength = Math.min(3.0, 1.4 + data.level() * 0.02) * Math.min(1.5f, Powers.power(data));
                // Aim each block at the point you're looking at, so a volley converges.
                Vec3 aim = Raycast.target(player, 48);
                Vec3 dir = aim.subtract(k.entity.position().add(0, 0.5, 0)).normalize();
                k.velocity = dir.scale(strength);
                k.damageScale = Powers.power(data);
            } else {
                k.velocity = Vec3.ZERO;
            }
            k.flyingSince = level.getGameTime();
            FLYING.add(k);
            broadcast(k, -1, 0);
        }
        if (thrown) {
            ((ServerLevel) player.level()).playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 0.6f);
        }
    }

    // ------------------------------------------------------------------ ticking

    /** Per-player checks (called from the player tick). */
    public static void tick(ServerPlayer player, MutationData data) {
        List<Held> list = held(player);
        if (list.isEmpty()) return;
        if (!data.has(Mutation.KINETIC_MIND)) {
            release(player, false);
            return;
        }
        for (Held held : list) {
            if (held.block().entity.level() != player.level()
                    || player.level().getGameTime() - held.since() > MAX_HOLD_TICKS) {
                player.sendOverlayMessage(Component.literal("Your focus slips...").withStyle(ChatFormatting.GRAY));
                release(player, false);
                return;
            }
        }
    }

    /** Moves every held and flying block. Runs before entities tick, so vanilla never interferes. */
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        for (List<Held> list : List.copyOf(HELD.values())) {
            for (Held held : List.copyOf(list)) {
                Kinetic k = held.block();
                if (k.entity.level() != level || !k.entity.isAlive()) continue;
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(k.owner);
                if (owner == null) continue;
                moveHeld(level, owner, k, held.slot());
            }
        }

        for (Map.Entry<UUID, Orbit> e : List.copyOf(ORBITS.entrySet())) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (owner == null || !owner.isAlive()) {
                ORBITS.remove(e.getKey());
                for (Kinetic k : e.getValue().blocks()) {          // owner gone: everything just drops
                    if (!k.entity.isAlive()) continue;
                    k.velocity = Vec3.ZERO;
                    k.flyingSince = level.getGameTime();
                    FLYING.add(k);
                }
                continue;
            }
            if (owner.level() == level) tickOrbit(level, owner, e.getValue());
        }

        Iterator<Kinetic> it = FLYING.iterator();
        while (it.hasNext()) {
            Kinetic k = it.next();
            if (!k.entity.isAlive()) {
                it.remove();
                continue;
            }
            if (k.entity.level() != level) continue;
            if (stepFlight(level, k)) it.remove();
        }
    }

    private static void moveHeld(ServerLevel level, ServerPlayer owner, Kinetic k, int slot) {
        FallingBlockEntity e = k.entity;
        Vec3 target = holdPosition(owner.getEyePosition(), owner.getLookAngle(), slot);
        AABB box = e.getBoundingBox().move(target.subtract(e.position()));
        if (level.noCollision(e, box)) {
            e.setPos(target);
        }
        e.setDeltaMovement(Vec3.ZERO);
        broadcast(k, owner.getId(), slot);
        if (level.getGameTime() % 5 == 0) {
            level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 0.5, e.getZ(), 3, 0.3, 0.3, 0.3, 0.2);
        }
    }

    /** Advances a flying block one tick. Returns true when it's done (placed or gone). */
    private static boolean stepFlight(ServerLevel level, Kinetic k) {
        FallingBlockEntity e = k.entity;
        if (level.getGameTime() - k.flyingSince > MAX_FLIGHT_TICKS || e.getY() < level.getMinY()) {
            e.discard();
            return true;
        }

        k.velocity = k.velocity.add(0, -GRAVITY, 0).scale(DRAG);
        Vec3 v = k.velocity;

        // Move in small sub-steps so fast throws can't tunnel through walls or mobs.
        int steps = Math.max(1, (int) Math.ceil(v.length() / 0.4));
        Vec3 step = v.scale(1.0 / steps);
        for (int i = 0; i < steps; i++) {
            AABB next = e.getBoundingBox().move(step);

            if (hitEntities(level, k, next)) {
                broadcast(k, -1, 0);
                return false; // bounced; keep flying next tick
            }
            if (!level.noCollision(e, next)) {
                land(level, k);
                return true;
            }
            e.setPos(e.position().add(step));
        }
        e.setDeltaMovement(Vec3.ZERO);
        broadcast(k, -1, 0);
        return false;
    }

    /** Damages and knocks back mobs the block slams into, then bounces it off them. */
    private static boolean hitEntities(ServerLevel level, Kinetic k, AABB box) {
        FallingBlockEntity e = k.entity;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(k.owner);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box.inflate(0.1),
                t -> t.isAlive() && !t.getUUID().equals(k.owner) && !k.alreadyHit.contains(t.getId()));
        if (victims.isEmpty()) return false;

        double speed = k.velocity.length();
        if (speed < 0.3) return false; // a slow block just settles, it doesn't hurt

        BlockState state = e.getBlockState();
        float hardness = Math.max(0, Math.min(8f, state.getDestroySpeed(level, e.blockPosition())));
        int mastery = owner != null ? ModAttachments.get(owner).level() : 1;
        float damage = (float) (2.0 + speed * 2.5 + hardness * 0.5 + mastery * 0.05) * k.damageScale;

        for (LivingEntity victim : victims) {
            k.alreadyHit.add(victim.getId());
            if (owner != null) Powers.hurt(owner, victim, damage);
            else victim.hurtServer(level, level.damageSources().magic(), damage);
            Vec3 push = k.velocity.multiply(1, 0, 1).normalize().scale(1.2).add(0, 0.35, 0);
            Powers.impulse(victim, push, false);
        }
        level.playSound(null, e.getX(), e.getY(), e.getZ(), state.getSoundType(level, e.blockPosition(), e).getBreakSound(),
                SoundSource.BLOCKS, 1.0f, 0.7f);
        // Bounce back off the target so the block lands beside it instead of on top of it.
        k.velocity = new Vec3(-k.velocity.x * 0.25, Math.max(0.15, k.velocity.y * 0.2), -k.velocity.z * 0.25);
        return true;
    }

    /** The block hit the world: set it into place right where it struck. */
    private static void land(ServerLevel level, Kinetic k) {
        FallingBlockEntity e = k.entity;
        BlockState state = e.getBlockState();
        BlockPos center = BlockPos.containing(e.getBoundingBox().getCenter());

        BlockPos spot = null;
        for (BlockPos candidate : new BlockPos[]{center, center.above(), BlockPos.containing(e.position())}) {
            if (canPlaceAt(level, candidate)) {
                spot = candidate;
                break;
            }
        }

        if (spot != null) {
            level.setBlock(spot, state, 3);
            level.playSound(null, spot, state.getSoundType(level, spot, e).getPlaceSound(), SoundSource.BLOCKS, 1.0f, 0.8f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state, spot),
                    spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5, 24, 0.4, 0.4, 0.4, 0.15);
        } else {
            // Nowhere sensible to put it (e.g. wedged between torches): drop it rather than lose it.
            e.spawnAtLocation(level, new ItemStack(state.getBlock()), 0.0f);
        }
        e.discard();
    }

    private static boolean canPlaceAt(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced()) return false;
        // Never entomb a mob or player inside the landing block.
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(pos), LivingEntity::isAlive).isEmpty();
    }

    /** Sends the block's exact position to everyone nearby (and the holder). */
    private static void broadcast(Kinetic k, int holderId, int slot) {
        FallingBlockEntity e = k.entity;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(e,
                new ModPayloads.KineticBlock(e.getId(), holderId, slot, e.getX(), e.getY(), e.getZ()));
    }

    // ------------------------------------------------------------------ strain

    /** Heavy use of the mind causes headaches: 4 uses within 30 seconds gives nausea. */
    public static void addStrain(ServerPlayer player) {
        if (ModAttachments.get(player).has(Secondary.IRON_WILL)) return;
        long now = player.level().getGameTime();
        Deque<Long> uses = STRAIN.computeIfAbsent(player.getUUID(), x -> new ArrayDeque<>());
        uses.addLast(now);
        while (!uses.isEmpty() && now - uses.peekFirst() > 30 * 20) uses.removeFirst();
        if (uses.size() >= 4) {
            uses.clear();
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 6 * 20, 0));
            player.sendOverlayMessage(Component.literal("Your head is pounding...").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
