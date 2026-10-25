package com.mutationsmod.mutations.ability;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.power.Absorber;
import com.mutationsmod.mutations.ability.power.Colossus;
import com.mutationsmod.mutations.ability.power.Cryo;
import com.mutationsmod.mutations.ability.power.Host;
import com.mutationsmod.mutations.ability.power.Pyro;
import com.mutationsmod.mutations.ability.power.Stormcaller;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.entity.Decoy;
import com.mutationsmod.mutations.entity.PrisonBoss;
import com.mutationsmod.mutations.mastery.Mastery;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Ultimate;
import com.mutationsmod.mutations.network.ModPayloads;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Omega Awakening: at mastery level 75 your base mutation unlocks one huge ultimate on its own key, with a
 * 10-minute cooldown (shortened by the Mutation stat). While an ultimate is running you glow in your mutation's color.
 */
public final class Omega {
    private Omega() {}

    /** Ability-key slot the client sends for the Omega key. */
    public static final int SLOT = 99;

    public static boolean unlocked(MutationData d) {
        return d.base() != null && d.level() >= Ultimate.LEVEL;
    }

    public static long cooldown(MutationData d) {
        return Math.max(20, Math.round(Ultimate.COOLDOWN_TICKS * Powers.cooldownFactor(d)));
    }

    // ------------------------------------------------------------------ unlocking

    /** Called on level-up: the moment you cross level 75. */
    public static void onLevelUp(ServerPlayer p, int oldLevel, int level) {
        if (oldLevel >= Ultimate.LEVEL || level < Ultimate.LEVEL) return;
        MutationData d = ModAttachments.get(p);
        Ultimate u = Ultimate.of(d.base());
        if (u == null) return;
        Mutation m = d.base();
        p.sendSystemMessage(Component.literal("OMEGA AWAKENING").withStyle(s -> s.withColor(m.color() & 0xFFFFFF).withBold(true)));
        p.sendSystemMessage(Component.literal("Your " + m.displayName() + " has reached its full potential. New ultimate: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(u.displayName()).withStyle(s -> s.withColor(m.color() & 0xFFFFFF).withBold(true))));
        p.sendSystemMessage(Component.literal(u.description() + " Press Z (rebindable) to use it.").withStyle(ChatFormatting.DARK_GRAY));
        ServerLevel sl = (ServerLevel) p.level();
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.7f);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 0.8f);
        burst(sl, p, m, 80);
        Powers.setTimer(p, "omega_glow", 100);
    }

    // ------------------------------------------------------------------ using

    public static void use(ServerPlayer p) {
        MutationData d = ModAttachments.get(p);
        Ultimate u = Ultimate.of(d.base());
        if (u == null) return;
        if (!unlocked(d)) {
            p.sendOverlayMessage(Component.literal("Your Omega ultimate awakens at mastery level " + Ultimate.LEVEL + ".").withStyle(ChatFormatting.GRAY));
            return;
        }
        long now = p.level().getGameTime();
        if (now < d.omegaReady()) {
            long secs = (d.omegaReady() - now + 19) / 20;
            p.sendOverlayMessage(Component.literal(u.displayName() + " is recharging (" + (secs >= 60 ? secs / 60 + "m " + secs % 60 + "s" : secs + "s") + ")")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!fire(p, d, u)) return;
        ModAttachments.set(p, ModAttachments.get(p).withOmegaReady(now + cooldown(d)));
        Powers.setTimer(p, "omega_glow", u.glowTicks());
        Mastery.addXp(p, Mastery.XP_PER_ABILITY_USE * 5);
        Mutation m = d.base();
        p.sendOverlayMessage(Component.literal("Ω " + u.displayName().toUpperCase())
                .withStyle(s -> s.withColor(m.color() & 0xFFFFFF).withBold(true)));
        ServerLevel level = (ServerLevel) p.level();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.6f);
        burst(level, p, m, 40);
    }

    /** Runs the ultimate. Returns false if it couldn't be used (no cooldown is spent). */
    public static boolean fire(ServerPlayer p, MutationData d, Ultimate u) {
        return switch (u) {
            case UNDYING -> undying(p);
            case GRAVITY_WELL -> gravityWell(p, d);
            case EYE_OF_THE_STORM -> eyeOfTheStorm(p);
            case TIME_BLUR -> timeBlur(p);
            case ABSOLUTE_ZERO -> absoluteZero(p, d);
            case FRACTURE -> fracture(p, d);
            case TITAN -> titan(p, d);
            case BANSHEE_WAIL -> bansheeWail(p, d);
            case OVERLOAD -> overload(p, d);
            case DOPPELGANGER -> doppelganger(p);
            case SUPERNOVA -> supernova(p);
            case CONSUME -> consume(p, d);
        };
    }

    private static boolean active(MutationData d, ServerPlayer p, String timer) {
        return d.timer(timer) > p.level().getGameTime();
    }

    public static boolean active(ServerPlayer p, String timer) {
        return active(ModAttachments.get(p), p, timer);
    }

    // ------------------------------------------------------------------ Regenerator: Undying

    private static boolean undying(ServerPlayer p) {
        Powers.setTimer(p, "undying", 600);
        Powers.particles(p, ParticleTypes.TOTEM_OF_UNDYING, 60, 0.6, 0.4);
        Powers.sound(p, SoundEvents.TOTEM_USE, 0.8f, 1.2f);
        return true;
    }

    // ------------------------------------------------------------------ Kinetic Mind: Gravity Well

    private static boolean gravityWell(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        BlockPos feet = p.blockPosition();
        List<BlockPos> picks = new ArrayList<>();
        for (int r = 2; r <= 6 && picks.size() < 40; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int dy = 0; dy >= -2; dy--) {
                        BlockPos pos = feet.offset(dx, dy - 1, dz);
                        BlockState s = level.getBlockState(pos);
                        if (s.isAir() || !level.getBlockState(pos.above()).isAir()) continue;
                        float h = s.getDestroySpeed(level, pos);
                        if (h >= 0 && h <= 3.5f && level.getBlockEntity(pos) == null && s.getFluidState().isEmpty()) picks.add(pos);
                        break;
                    }
                }
            }
        }
        java.util.Collections.shuffle(picks, new java.util.Random(p.getRandom().nextLong()));
        int count = Math.min(picks.size(), 12 + Math.min(12, d.level() / 10));
        if (count == 0) {
            p.sendOverlayMessage(Component.literal("Nothing here to lift.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        Telekinesis.startOrbit(p, picks.subList(0, count), 60, Powers.power(d) * 1.5f);
        Powers.setTimer(p, "gravity_well", 60);
        Powers.sound(p, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.2f, 0.5f);
        return true;
    }

    // ------------------------------------------------------------------ Stormcaller: Eye of the Storm

    private static boolean eyeOfTheStorm(ServerPlayer p) {
        Powers.setTimer(p, "eye_storm", 400);
        Powers.impulse(p, new Vec3(0, 0.8, 0), false);
        Stormcaller.setFlight(p, true);
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
        Powers.sound(p, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
        return true;
    }

    /** Stormcaller flight and full weather power while Eye of the Storm lasts. */
    public static boolean eyeOfTheStorm(MutationData d, Player p) {
        return d.timer("eye_storm") > p.level().getGameTime();
    }

    // ------------------------------------------------------------------ Phaser: Time Blur

    private static boolean timeBlur(ServerPlayer p) {
        Powers.setTimer(p, "time_blur", 300);
        Powers.sound(p, SoundEvents.BEACON_POWER_SELECT, 1.0f, 2.0f);
        return true;
    }

    // ------------------------------------------------------------------ Cryo: Absolute Zero

    private static boolean absoluteZero(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        float scale = Powers.power(d) * Cryo.heat(p, d);
        for (LivingEntity e : Powers.around(p, 20)) {
            Powers.hurt(p, e, 10f * scale);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 3));
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), 400));
            if (e instanceof Mob) {
                BlockPos base = e.blockPosition();
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    Powers.placeTemporary(level, base.relative(dir), Blocks.PACKED_ICE.defaultBlockState(), 200);
                    Powers.placeTemporary(level, base.relative(dir).above(), Blocks.PACKED_ICE.defaultBlockState(), 200);
                }
            }
        }
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-20, -6, -20), c.offset(20, 6, 20))) {
            double dx = pos.getX() - c.getX(), dz = pos.getZ() - c.getZ();
            if (dx * dx + dz * dz > 400) continue;
            BlockState s = level.getBlockState(pos);
            var fluid = s.getFluidState();
            if (fluid.isEmpty()) continue;
            if (fluid.is(FluidTags.WATER) && s.is(Blocks.WATER)) {
                level.setBlock(pos, (fluid.isSource() ? Blocks.ICE : Blocks.PACKED_ICE).defaultBlockState(), 3);
            } else if (fluid.is(FluidTags.LAVA) && s.is(Blocks.LAVA)) {
                level.setBlock(pos, (fluid.isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE).defaultBlockState(), 3);
            }
        }
        // The ground itself freezes: the surface turns to snow and ice for a minute, then thaws back.
        net.minecraft.util.RandomSource rnd = p.getRandom();
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                if (dx * dx + dz * dz > 400) continue;
                int x = c.getX() + dx, z = c.getZ() + dz;
                int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                if (Math.abs(top - c.getY()) > 10) continue;
                BlockPos surface = new BlockPos(x, top, z);
                if (!level.getBlockState(surface).getFluidState().isEmpty()) continue;   // water/lava handled above
                float f = rnd.nextFloat();
                BlockState frozen = (f < 0.55f ? Blocks.SNOW_BLOCK : f < 0.85f ? Blocks.PACKED_ICE : Blocks.ICE).defaultBlockState();
                if (Powers.freezeTemporary(level, surface, frozen, FROST_TICKS)) {
                    Powers.freezeTemporary(level, surface.below(), (rnd.nextBoolean() ? Blocks.PACKED_ICE : Blocks.SNOW_BLOCK).defaultBlockState(), FROST_TICKS);
                    if (rnd.nextFloat() < 0.5f) Powers.placeTemporary(level, surface.above(), Blocks.SNOW.defaultBlockState(), FROST_TICKS);
                }
            }
        }
        Powers.setTimer(p, "absolute_zero", 200);
        level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 300, 8, 2, 8, 0.15);
        Powers.sound(p, SoundEvents.GLASS_BREAK, 1.5f, 0.5f);
        Powers.sound(p, SoundEvents.POWDER_SNOW_STEP, 1.5f, 0.5f);
        return true;
    }

    // ------------------------------------------------------------------ Blinker: Fracture

    private record Chain(List<Integer> targets, int[] index, float damage) {}
    private static final Map<UUID, Chain> FRACTURES = new HashMap<>();

    private static boolean fracture(ServerPlayer p, MutationData d) {
        List<LivingEntity> targets = new ArrayList<>(Powers.around(p, 16).stream()
                .filter(e -> (e instanceof Mob || e instanceof Player) && p.hasLineOfSight(e)).toList());
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        if (targets.isEmpty()) {
            p.sendOverlayMessage(Component.literal("No one in sight to strike.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        List<Integer> ids = targets.stream().limit(8).map(Entity::getId).toList();
        FRACTURES.put(p.getUUID(), new Chain(ids, new int[]{0}, 9f * Powers.power(d)));
        return true;
    }

    private static void tickFracture(ServerPlayer p, ServerLevel level) {
        Chain c = FRACTURES.get(p.getUUID());
        if (c == null || level.getGameTime() % 3 != 0) return;
        while (c.index()[0] < c.targets().size()) {
            Entity e = level.getEntity(c.targets().get(c.index()[0]++));
            if (!(e instanceof LivingEntity t) || !t.isAlive()) continue;
            Vec3 from = p.position();
            Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
            Vec3 spot = Powers.safeSpot(p, behind);
            if (spot == null) spot = Powers.safeSpot(p, t.position());
            if (spot != null) {
                Powers.teleport(p, spot);
                p.connection.teleport(spot.x, spot.y, spot.z, (float) Math.toDegrees(Math.atan2(-(t.getX() - spot.x), t.getZ() - spot.z)), p.getXRot());
            }
            Powers.line(level, from.add(0, 1, 0), p.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 0.4);
            Powers.hurt(p, t, c.damage());
            level.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.6f);
            return;
        }
        FRACTURES.remove(p.getUUID());
        p.resetFallDistance();
    }

    // ------------------------------------------------------------------ Colossus: Titan

    private static boolean titan(ServerPlayer p, MutationData d) {
        if (!Colossus.steel(d)) Colossus.toggleSteel(p, d);
        Powers.setTimer(p, "titan", 600);
        Powers.sound(p, SoundEvents.IRON_GOLEM_REPAIR, 1.5f, 0.5f);
        Powers.sound(p, SoundEvents.ANVIL_LAND, 1.0f, 0.5f);
        return true;
    }

    private static void tickTitan(ServerPlayer p, MutationData d, ServerLevel level, boolean on) {
        Powers.mult(p, Attributes.SCALE, "omega_titan", on ? 1.0 : 0);
        Powers.add(p, Attributes.ATTACK_DAMAGE, "omega_titan", on ? 6 : 0);
        Powers.add(p, Attributes.ENTITY_INTERACTION_RANGE, "omega_titan", on ? 2 : 0);
        Powers.add(p, Attributes.BLOCK_INTERACTION_RANGE, "omega_titan", on ? 2 : 0);
        Powers.add(p, Attributes.SAFE_FALL_DISTANCE, "omega_titan", on ? 10 : 0);
        if (!on) return;
        if (level.getGameTime() % 10 == 0 && p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.002) {
            BlockState under = level.getBlockState(p.blockPosition().below());
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), p.getX(), p.getY(), p.getZ(), 25, 1.2, 0.1, 1.2, 0.2);
            Powers.sound(p, SoundEvents.ANVIL_LAND, 0.4f, 0.4f);
            for (LivingEntity e : Powers.around(p, 3.5)) {
                if (!e.onGround()) continue;
                Powers.hurt(p, e, 4f * Powers.power(d));
                Powers.knockAway(e, p.position(), 0.6, 0.35);
            }
        }
    }

    // ------------------------------------------------------------------ Siren: Banshee Wail

    private static boolean bansheeWail(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        for (LivingEntity e : Powers.around(p, 14)) {
            Powers.hurt(p, e, 14f * Powers.power(d));
            e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            if (e.getMaxHealth() <= 150 && !(e instanceof PrisonBoss)) {
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 9));    // stunned
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 4));
                if (e instanceof Mob mob) mob.setTarget(null);
            }
            Powers.knockAway(e, p.position(), 0.8, 0.25);
        }
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-8, -4, -8), c.offset(8, 6, 8))) {
            if (pos.distSqr(c) > 64) continue;
            BlockState s = level.getBlockState(pos);
            if (s.isAir()) continue;
            String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
            if (path.contains("glass")) level.destroyBlock(pos, false);
        }
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            for (int r = 2; r <= 10; r += 4) {
                level.sendParticles(ParticleTypes.SONIC_BOOM, p.getX() + Math.cos(a) * r, p.getY() + 1.2, p.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
        Powers.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 0.8f);
        Powers.sound(p, SoundEvents.GHAST_SCREAM, 1.5f, 0.6f);
        return true;
    }

    // ------------------------------------------------------------------ Absorber: Overload

    private static boolean overload(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        float charge = d.charge();
        double radius = 4 + Math.min(12, charge / 10.0);
        float damage = Math.min(90f, 6f + charge * 0.6f) * Powers.power(d);
        for (LivingEntity e : Powers.around(p, radius)) {
            Powers.hurt(p, e, damage);
            Powers.knockAway(e, p.position(), 1.6, 0.6);
        }
        ModAttachments.set(p, ModAttachments.get(p).withCharge(0));
        Powers.setTimer(p, "overload", 200);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 120, radius * 0.4, 1, radius * 0.4, 0.4);
        Powers.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.6f);
        p.sendOverlayMessage(Component.literal(String.format("OVERLOAD: released %.0f charge", charge)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return true;
    }

    /** Absorber: during Overload, all damage is stored as charge. */
    public static boolean overloading(MutationData d, Player p) {
        return d.timer("overload") > p.level().getGameTime();
    }

    // ------------------------------------------------------------------ Chameleon: Doppelganger

    private static final Map<UUID, List<Decoy>> DECOYS = new HashMap<>();

    private static boolean doppelganger(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        List<Decoy> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double a = i * Math.PI * 2 / 3 + p.getRandom().nextDouble();
            Vec3 at = Powers.safeSpot(p, p.position().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5));
            if (at == null) at = p.position();
            Decoy dcy = Decoy.of(p, at, 400);
            level.addFreshEntity(dcy);
            level.sendParticles(ParticleTypes.POOF, at.x, at.y + 1, at.z, 15, 0.3, 0.6, 0.3, 0.02);
            list.add(dcy);
        }
        DECOYS.put(p.getUUID(), list);
        Powers.setTimer(p, "doppel", 400);
        Powers.setTimer(p, "mimic", 400);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 400, 0, false, false, true));
        redirect(p, level, list);
        Powers.sound(p, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.2f, 1.0f);
        return true;
    }

    private static void redirect(ServerPlayer p, ServerLevel level, List<Decoy> decoys) {
        List<Decoy> alive = decoys.stream().filter(Entity::isAlive).toList();
        if (alive.isEmpty()) return;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24), m -> m instanceof Monster && m.isAlive())) {
            if (mob.getTarget() == null || mob.getTarget() == p || !mob.getTarget().isAlive()) {
                mob.setTarget(alive.get(p.getRandom().nextInt(alive.size())));
            }
        }
    }

    // ------------------------------------------------------------------ Pyro: Supernova

    /** How long Absolute Zero's frozen ground lasts before it thaws (ticks). */
    private static final int FROST_TICKS = 60 * 20;

    private static boolean supernova(ServerPlayer p) {
        MutationData d = ModAttachments.get(p);
        if (!Pyro.lit(d)) ModAttachments.set(p, d.withTimer("flame_on", 1));
        Powers.sound(p, SoundEvents.BLAZE_SHOOT, 1.5f, 0.5f);
        detonate(p, ModAttachments.get(p), (ServerLevel) p.level());   // explodes right where you stand
        return true;
    }

    private static void detonate(ServerPlayer p, MutationData d, ServerLevel level) {
        ModAttachments.set(p, ModAttachments.get(p).withTimer("supernova", 0));
        // The blast goes off on the ground under you (even if you're in the air).
        BlockPos ground = p.blockPosition();
        for (int i = 0; i < 12 && level.getBlockState(ground.below()).getCollisionShape(level, ground.below()).isEmpty(); i++) ground = ground.below();
        double gy = ground.getY() + 0.1;
        for (LivingEntity e : Powers.around(p, 9)) {
            Powers.hurt(p, e, 16f * Powers.power(d));
            e.igniteForSeconds(8);
            Powers.knockAway(e, p.position(), 1.4, 0.5);
        }
        for (int ring = 2; ring <= 9; ring += 1) {
            for (int i = 0; i < ring * 6; i++) {
                double a = i * Math.PI * 2 / (ring * 6);
                level.sendParticles(ParticleTypes.FLAME, p.getX() + Math.cos(a) * ring, gy + 0.2, p.getZ() + Math.sin(a) * ring, 1, 0, 0.1, 0, 0.02);
                if (i % 3 == 0) level.sendParticles(ParticleTypes.LAVA, p.getX() + Math.cos(a) * ring, gy, p.getZ() + Math.sin(a) * ring, 1, 0, 0, 0, 0);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), gy + 0.5, p.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LAVA, p.getX(), gy + 0.5, p.getZ(), 40, 2, 0.5, 2, 0.5);
        level.sendParticles(ParticleTypes.FLAME, p.getX(), gy + 0.5, p.getZ(), 120, 1.5, 0.3, 1.5, 0.35);
        Powers.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.8f);
        // Then fly as living flame.
        Powers.setTimer(p, "flame_flight", 300);
        Powers.add(p, NeoForgeMod.CREATIVE_FLIGHT, "mutation_flight", 1);
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
        Powers.impulse(p, new Vec3(0, 0.9, 0), false);                  // launched up out of the blast
    }

    // ------------------------------------------------------------------ Host: Consume

    private record Feast(List<Integer> targets, long devourAt) {}
    private static final Map<UUID, Feast> FEASTS = new HashMap<>();

    private static boolean consume(ServerPlayer p, MutationData d) {
        List<LivingEntity> prey = new ArrayList<>(Powers.around(p, 10).stream()
                .filter(e -> e instanceof Mob && !(e instanceof PrisonBoss) && p.hasLineOfSight(e)).toList());
        prey.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        if (prey.isEmpty()) {
            p.sendOverlayMessage(Component.literal("Nothing to feed on.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        List<Integer> ids = new ArrayList<>();
        for (LivingEntity e : prey.subList(0, Math.min(5, prey.size()))) {
            ids.add(e.getId());
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,
                    new ModPayloads.TendrilStrike(p.getId(), e.getId(), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 20));
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 9));
        }
        FEASTS.put(p.getUUID(), new Feast(ids, p.level().getGameTime() + 16));
        Powers.sound(p, SoundEvents.WARDEN_TENDRIL_CLICKS, 1.5f, 0.6f);
        return true;
    }

    private static void tickFeast(ServerPlayer p, MutationData d, ServerLevel level) {
        Feast f = FEASTS.get(p.getUUID());
        if (f == null) return;
        long now = level.getGameTime();
        for (int id : f.targets()) {
            if (!(level.getEntity(id) instanceof LivingEntity e) || !e.isAlive()) continue;
            if (now < f.devourAt()) {
                Vec3 pull = p.position().subtract(e.position());
                if (pull.length() > 1.8) Powers.impulse(e, pull.normalize().scale(0.45).add(0, 0.08, 0), true);
            }
        }
        if (now < f.devourAt()) return;
        FEASTS.remove(p.getUUID());
        int fed = 0;
        for (int id : f.targets()) {
            if (!(level.getEntity(id) instanceof LivingEntity e) || !e.isAlive()) continue;
            Powers.hurt(p, e, 20f * Powers.power(d));
            level.sendParticles(ParticleTypes.SQUID_INK, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 15, 0.3, 0.4, 0.3, 0.05);
            fed++;
        }
        if (fed > 0) {
            p.heal(4f * fed);
            p.getFoodData().setFoodLevel(Math.min(20, p.getFoodData().getFoodLevel() + 2 * fed));
            Host.onKill(p, d);                                         // the organism is fed
            Powers.sound(p, SoundEvents.GENERIC_EAT.value(), 1.5f, 0.5f);
        }
    }

    // ------------------------------------------------------------------ ticking

    public static void tick(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        long now = level.getGameTime();

        // Undying ends: heal to full.
        long undying = d.timer("undying");
        if (undying > 0 && now >= undying) {
            ModAttachments.set(p, ModAttachments.get(p).withTimer("undying", 0));
            p.setHealth(p.getMaxHealth());
            Powers.particles(p, ParticleTypes.HEART, 12, 0.6, 0.1);
            Powers.sound(p, SoundEvents.PLAYER_LEVELUP, 0.7f, 1.6f);
            p.sendOverlayMessage(Component.literal("Undying fades... you're fully healed.").withStyle(ChatFormatting.GREEN));
        }

        // Gravity Well: drag nearby mobs toward you while the blocks spin.
        if (active(d, p, "gravity_well") && now % 4 == 0) {
            for (LivingEntity e : Powers.around(p, 10)) {
                if (e.distanceTo(p) < 2.5) continue;
                Powers.impulse(e, p.position().subtract(e.position()).normalize().scale(0.25), false);
            }
        }

        // Eye of the Storm: lightning picks off enemies around you every second.
        if (active(d, p, "eye_storm")) {
            if (now % 20 == 0) {
                List<LivingEntity> foes = new ArrayList<>(Powers.around(p, 16).stream().filter(Powers::isHostile).toList());
                foes.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
                for (LivingEntity e : foes.subList(0, Math.min(2, foes.size()))) {
                    LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                    if (bolt == null) continue;
                    bolt.snapTo(e.getX(), e.getY(), e.getZ());
                    bolt.setCause(p);
                    level.addFreshEntity(bolt);
                }
            }
            if (now % 2 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 4, 0.6, 0.8, 0.6, 0.1);
                double a = now * 0.4;
                level.sendParticles(ParticleTypes.CLOUD, p.getX() + Math.cos(a) * 1.2, p.getY() + 0.2, p.getZ() + Math.sin(a) * 1.2, 0, 0, 0.1, 0, 0.1);
            }
        }

        // Time Blur: you speed up, the world crawls.
        boolean blur = active(d, p, "time_blur");
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "omega_blur", blur ? 0.9 : 0);
        Powers.mult(p, Attributes.ATTACK_SPEED, "omega_blur", blur ? 1.5 : 0);
        if (blur && now % 10 == 0) {
            for (LivingEntity e : Powers.around(p, 24)) {
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 4, false, false));
                e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 25, 2, false, false));
            }
            for (Entity proj : level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, p.getBoundingBox().inflate(10),
                    pr -> pr.getOwner() != p && !pr.entityTags().contains("mutations_blurred"))) {
                proj.setDeltaMovement(proj.getDeltaMovement().scale(0.25));
                proj.addTag("mutations_blurred");
            }
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 3, 0.4, 0.6, 0.4, 0.01);
        }

        // Absolute Zero: the blizzard keeps everything frozen.
        if (active(d, p, "absolute_zero") && now % 4 == 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 3, p.getZ(), 40, 12, 3, 12, 0.05);
            if (now % 20 == 0) for (LivingEntity e : Powers.around(p, 20)) e.setTicksFrozen(Math.max(e.getTicksFrozen(), 200));
        }

        tickFracture(p, level);
        tickTitan(p, d, level, active(d, p, "titan"));

        // Doppelganger: keep the mobs on your decoys.
        List<Decoy> decoys = DECOYS.get(p.getUUID());
        if (decoys != null) {
            if (!active(d, p, "doppel")) {
                for (Decoy dc : decoys) if (dc.isAlive()) dc.discard();
                DECOYS.remove(p.getUUID());
            } else if (now % 20 == 0) {
                redirect(p, level, decoys);
            }
        }

        long nova = d.timer("supernova");
        if (nova > 0 && now >= nova) detonate(p, ModAttachments.get(p), level);
        else if (nova > 0) level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.5, p.getZ(), 10, 0.3, 0.3, 0.3, 0.05);

        tickFeast(p, d, level);

        // The Omega aura (also drawn as a glow on the player model by the client).
        if (active(d, p, "omega_glow") && now % 3 == 0 && d.base() != null) {
            int rgb = d.base().color() & 0xFFFFFF;
            level.sendParticles(new DustParticleOptions(rgb, 1.2f), p.getX(), p.getY() + p.getBbHeight() * 0.5, p.getZ(), 4,
                    0.4, p.getBbHeight() * 0.35, 0.4, 0);
        }
    }

    // ------------------------------------------------------------------ damage hooks

    /** Regenerator Undying: damage can't take you below half a heart. Runs before other defenses. */
    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        if (d.has(Mutation.REGENERATOR) && active(d, p, "undying")) {
            float max = p.getHealth() - 1f;
            if (max <= 0) event.setCanceled(true);
            else if (event.getAmount() > max) event.setAmount(max);
        }
    }

    /** Undying also stops death from anything but the void or /kill. */
    public static boolean preventDeath(LivingDeathEvent event, ServerPlayer p, MutationData d) {
        if (!d.has(Mutation.REGENERATOR) || !active(d, p, "undying")) return false;
        var src = event.getSource();
        if (src.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD) || src.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) return false;
        event.setCanceled(true);
        p.setHealth(1f);
        return true;
    }

    // ------------------------------------------------------------------ fx & cleanup

    private static void burst(ServerLevel level, ServerPlayer p, Mutation m, int count) {
        int rgb = m.color() & 0xFFFFFF;
        level.sendParticles(new DustParticleOptions(rgb, 2.0f), p.getX(), p.getY() + 1, p.getZ(), count, 0.8, 1.0, 0.8, 0);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), count / 2, 0.5, 0.8, 0.5, 0.15);
    }

    /** Removes decoys and running sequences (logout, mutation change). */
    public static void forget(ServerPlayer p) {
        List<Decoy> decoys = DECOYS.remove(p.getUUID());
        if (decoys != null) for (Decoy dc : decoys) if (dc.isAlive()) dc.discard();
        FRACTURES.remove(p.getUUID());
        FEASTS.remove(p.getUUID());
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "omega_blur", 0);
        Powers.mult(p, Attributes.ATTACK_SPEED, "omega_blur", 0);
        tickTitan(p, MutationData.EMPTY, (ServerLevel) p.level(), false);
    }
}
