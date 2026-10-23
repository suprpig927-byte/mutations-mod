package com.mutationsmod.mutations.ability;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Stat;
import com.mutationsmod.mutations.network.ModPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shared helpers used by every mutation. */
public final class Powers {
    private Powers() {}

    // ------------------------------------------------------------------ data shortcuts

    public static long now(Level level) {
        return level.getGameTime();
    }

    public static boolean timerActive(MutationData d, Level level, String name) {
        return d.timer(name) > level.getGameTime();
    }

    public static void setTimer(ServerPlayer p, String name, long ticksFromNow) {
        ModAttachments.set(p, ModAttachments.get(p).withTimer(name, p.level().getGameTime() + ticksFromNow));
    }

    public static void clearTimer(ServerPlayer p, String name) {
        ModAttachments.set(p, ModAttachments.get(p).withTimer(name, 0));
    }

    /** Ability power multiplier from the Mutation stat (+2% per point). */
    public static float power(MutationData d) {
        return 1f + 0.02f * d.stat(Stat.MUTATION);
    }

    /** Cooldown multiplier from the Mutation stat (-1% per point, at most -50%). */
    public static float cooldownFactor(MutationData d) {
        return Math.max(0.5f, 1f - 0.01f * d.stat(Stat.MUTATION));
    }

    // ------------------------------------------------------------------ attributes & effects

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, path);
    }

    /** Adds, updates or removes (amount 0) a temporary attribute modifier. */
    public static void modifier(LivingEntity e, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = e.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && existing.amount() == amount && existing.operation() == op) return;
        if (existing != null) inst.removeModifier(id);
        if (amount != 0) inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }

    public static void add(LivingEntity e, Holder<Attribute> attribute, String id, double amount) {
        modifier(e, attribute, id(id), amount, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void mult(LivingEntity e, Holder<Attribute> attribute, String id, double amount) {
        modifier(e, attribute, id(id), amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    /** Applies a hidden effect (no particles). */
    public static void effect(LivingEntity e, Holder<MobEffect> effect, int ticks, int amplifier) {
        e.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false, true));
    }

    // ------------------------------------------------------------------ damage

    /** True while the mod itself is dealing damage, so on-hit passives don't trigger on ability damage. */
    public static final ThreadLocal<Boolean> ABILITY_DAMAGE = ThreadLocal.withInitial(() -> false);

    public static boolean hurt(ServerPlayer attacker, LivingEntity target, float amount) {
        if (amount <= 0 || target == attacker) return false;
        ABILITY_DAMAGE.set(true);
        try {
            return target.hurtServer((ServerLevel) attacker.level(), attacker.damageSources().playerAttack(attacker), amount);
        } finally {
            ABILITY_DAMAGE.set(false);
        }
    }

    /** True while a mutation downside is hurting its own owner (never absorbed or dodged). */
    public static final ThreadLocal<Boolean> SELF_DAMAGE = ThreadLocal.withInitial(() -> false);

    public static void hurtSelf(ServerPlayer p, float amount) {
        SELF_DAMAGE.set(true);
        try {
            p.hurtServer((ServerLevel) p.level(), p.damageSources().magic(), amount);
        } finally {
            SELF_DAMAGE.set(false);
        }
    }

    public static boolean isHostile(Entity e) {
        return e.getType().getCategory() == MobCategory.MONSTER
                || (e instanceof Mob m && m.getTarget() instanceof Player);
    }

    /** Living things around a player (not the player). */
    public static List<LivingEntity> around(ServerPlayer p, double radius) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius),
                e -> e != p && e.isAlive() && e.distanceToSqr(p) <= radius * radius);
    }

    /** Living things in a cone in front of the player. */
    public static List<LivingEntity> cone(ServerPlayer p, double range, double minDot) {
        Vec3 look = p.getLookAngle();
        Vec3 eye = p.getEyePosition();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : around(p, range)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > range) continue;
            if (to.normalize().dot(look) >= minDot) out.add(e);
        }
        return out;
    }

    /** Pushes an entity. Players are moved by their own client, so they get a packet instead. */
    public static void impulse(Entity entity, Vec3 velocity, boolean replace) {
        if (entity instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new ModPayloads.Impulse(velocity.x, velocity.y, velocity.z, replace));
        } else {
            entity.setDeltaMovement(replace ? velocity : entity.getDeltaMovement().add(velocity));
        }
    }

    /** Knocks {@code target} away from {@code from}. */
    public static void knockAway(Entity target, Vec3 from, double strength, double lift) {
        Vec3 dir = target.position().subtract(from).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0.01, 0, 0);
        impulse(target, dir.normalize().scale(strength).add(0, lift, 0), false);
    }

    // ------------------------------------------------------------------ fx

    public static void sound(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void particles(Entity at, ParticleOptions type, int count, double spread, double speed) {
        if (at.level() instanceof ServerLevel sl) {
            sl.sendParticles(type, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(), count, spread, spread, spread, speed);
        }
    }

    public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions type, double step) {
        Vec3 d = to.subtract(from);
        int n = (int) Math.max(1, d.length() / step);
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            level.sendParticles(type, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ world

    public static boolean isNight(Level level) {
        long t = level.getOverworldClockTime() % 24000L;
        return t >= 13000 && t <= 23000;
    }

    public static boolean isNether(Level level) {
        return level.dimension() == Level.NETHER;
    }

    public static float temperature(Entity e) {
        return e.level().getBiome(e.blockPosition()).value().getBaseTemperature();
    }

    /** A safe place to stand near {@code target} (feet position), or null. */
    public static Vec3 safeSpot(Entity e, Vec3 target) {
        Level level = e.level();
        for (int dy = 0; dy <= 3; dy++) {
            for (int sign : new int[]{1, -1}) {
                Vec3 pos = target.add(0, dy * sign, 0);
                AABB box = e.getBoundingBox().move(pos.subtract(e.position()));
                if (level.noCollision(e, box)) return pos;
                if (dy == 0) break;
            }
        }
        return null;
    }

    /** Teleports any entity to a feet position. */
    public static void teleport(Entity e, Vec3 pos) {
        e.teleportTo(pos.x, pos.y, pos.z);
        e.resetFallDistance();
    }

    // ------------------------------------------------------------------ temporary blocks

    private record TempBlock(ServerLevel level, BlockPos pos, BlockState placed, long expires) {}

    private static final List<TempBlock> TEMP_BLOCKS = new ArrayList<>();

    /** Places a block that turns back to air after {@code ticks} (if it's still there). */
    public static boolean placeTemporary(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        BlockState current = level.getBlockState(pos);
        if (!current.canBeReplaced()) return false;
        level.setBlock(pos, state, 3);
        TEMP_BLOCKS.add(new TempBlock(level, pos.immutable(), state, level.getGameTime() + ticks));
        return true;
    }

    // ------------------------------------------------------------------ rush windows (Blitz / Charge)

    private static final class Rush {
        final long until;
        final float damage;
        final double knockback;
        final Map<Integer, Long> lastHit = new HashMap<>();

        Rush(long until, float damage, double knockback) {
            this.until = until;
            this.damage = damage;
            this.knockback = knockback;
        }
    }

    private static final Map<UUID, Rush> RUSHES = new HashMap<>();

    /** For {@code ticks}, anything the player runs into takes damage (each target at most once a second). */
    public static void startRush(ServerPlayer p, int ticks, float damage, double knockback) {
        RUSHES.put(p.getUUID(), new Rush(p.level().getGameTime() + ticks, damage, knockback));
    }

    public static boolean rushing(ServerPlayer p) {
        Rush r = RUSHES.get(p.getUUID());
        return r != null && r.until > p.level().getGameTime();
    }

    static void tickRush(ServerPlayer p) {
        Rush r = RUSHES.get(p.getUUID());
        if (r == null) return;
        long now = p.level().getGameTime();
        if (now > r.until) {
            RUSHES.remove(p.getUUID());
            return;
        }
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(0.6), e -> e != p && e.isAlive())) {
            Long last = r.lastHit.get(e.getId());
            if (last != null && now - last < 20) continue;
            r.lastHit.put(e.getId(), now);
            hurt(p, e, r.damage);
            knockAway(e, p.position(), r.knockback, 0.4);
        }
    }

    // ------------------------------------------------------------------ level tick

    public static void tickLevel(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<TempBlock> it = TEMP_BLOCKS.iterator();
        while (it.hasNext()) {
            TempBlock t = it.next();
            if (t.level() != level || now < t.expires()) continue;
            if (level.isLoaded(t.pos()) && level.getBlockState(t.pos()) == t.placed()) {
                level.setBlock(t.pos(), Blocks.AIR.defaultBlockState(), 3);
            }
            it.remove();
        }
    }

    public static void forget(UUID player) {
        RUSHES.remove(player);
    }
}
