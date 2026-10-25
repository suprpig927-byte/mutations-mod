package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

public final class Siren {
    private Siren() {}

    private record Fear(UUID from, long until, Vec3 source) {}

    /** Mobs put to sleep by Lullaby: entity id -> wake time. */
    private static final Map<Mob, Long> ASLEEP = new HashMap<>();
    /** Mobs fleeing from Banshee Wail. */
    private static final Map<Mob, Fear> AFRAID = new HashMap<>();
    /** Resonance stacks: "screamer|target" -> (stacks, last hit time). */
    private static final Map<String, long[]> RESONANCE = new HashMap<>();

    public static void tick(ServerPlayer p, MutationData d) {
        // Downside: you're loud. Sneaking draws monsters to you.
        if (!d.has(Secondary.SILENT_STEPS) && p.isShiftKeyDown() && p.level().getGameTime() % 40 == 0) {
            for (Mob mob : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16),
                    m -> Powers.isHostile(m) && m.getTarget() == null && !ASLEEP.containsKey(m))) {
                mob.setTarget(p);
            }
        }
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        if (d.has(Secondary.SOUNDPROOF) && event.getSource().is(DamageTypes.SONIC_BOOM)) event.setCanceled(true);
    }

    public static void onEffect(MobEffectEvent.Applicable event, MutationData d) {
        if (d.has(Secondary.SOUNDPROOF) && event.getEffectInstance().is(MobEffects.DARKNESS)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Any mob that takes damage wakes up. */
    public static void onMobHurt(LivingEntity victim) {
        if (victim instanceof Mob mob && ASLEEP.remove(mob) != null) mob.setNoAi(false);
    }

    // ------------------------------------------------------------------ actives

    public static boolean sonicScream(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        long now = level.getGameTime();
        float base = 6f * Powers.power(d);
        boolean crystal = com.mutationsmod.mutations.item.Catalysts.holding(p, com.mutationsmod.mutations.mutation.Mutation.SIREN); // Resonance Crystal
        for (LivingEntity e : Powers.cone(p, crystal ? 15 : 10, 0.6)) {
            float dmg = base;
            if (crystal) {
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 9));   // stunned
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 4));
            }
            if (d.has(Secondary.RESONANCE)) {
                String key = p.getUUID() + "|" + e.getUUID();
                long[] r = RESONANCE.get(key);
                int stacks = (r != null && now - r[1] < 200) ? (int) Math.min(4, r[0] + 1) : 0;
                RESONANCE.put(key, new long[]{stacks, now});
                dmg *= 1f + 0.25f * stacks;
            }
            Powers.hurt(p, e, dmg);
            e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
            Powers.knockAway(e, p.position(), 0.6, 0.2);
            if (d.has(Secondary.BANSHEE_WAIL) && e instanceof Mob mob) {
                AFRAID.put(mob, new Fear(p.getUUID(), now + 100, p.position()));
                mob.setTarget(null);
            }
        }
        if (d.has(Secondary.SHATTERPOINT)) shatter(p, level);
        Vec3 look = p.getLookAngle();
        for (int i = 1; i <= 4; i++) {
            Vec3 at = p.getEyePosition().add(look.scale(i * 2.2));
            level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        Powers.sound(p, SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.3f);
        return true;
    }

    /** Breaks glass and ice in a cone in front of the player. */
    private static void shatter(ServerPlayer p, ServerLevel level) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        BlockPos center = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -4, -8), center.offset(8, 5, 8))) {
            Vec3 to = Vec3.atCenterOf(pos).subtract(eye);
            if (to.length() > 9 || to.normalize().dot(look) < 0.6) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            if (path.contains("glass") || path.contains("ice")) level.destroyBlock(pos, false);
        }
    }

    public static boolean lullaby(ServerPlayer p, MutationData d) {
        long until = p.level().getGameTime() + (long) (100 * Powers.power(d));
        int count = 0;
        for (LivingEntity e : Powers.around(p, 10)) {
            if (e instanceof Mob mob && !mob.isNoAi() && e.getMaxHealth() <= 150) { // bosses are immune
                mob.setNoAi(true);
                mob.setTarget(null);
                ASLEEP.put(mob, until);
                count++;
            }
        }
        Powers.particles(p, ParticleTypes.NOTE, 20, 3.0, 0.5);
        Powers.sound(p, SoundEvents.ILLUSIONER_CAST_SPELL, 0.8f, 1.8f);
        return count > 0;
    }

    /** Wakes sleepers and steers frightened mobs away. */
    public static void tickLevel(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Map.Entry<Mob, Long>> it = ASLEEP.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Mob mob = e.getKey();
            if (mob.level() != level) continue;
            if (!mob.isAlive() || now >= e.getValue()) {
                if (mob.isAlive()) mob.setNoAi(false);
                it.remove();
            } else if (now % 10 == 0) {
                level.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
        }
        Iterator<Map.Entry<Mob, Fear>> fit = AFRAID.entrySet().iterator();
        while (fit.hasNext()) {
            var e = fit.next();
            Mob mob = e.getKey();
            if (mob.level() != level) continue;
            if (!mob.isAlive() || now >= e.getValue().until()) {
                fit.remove();
            } else if (now % 10 == 0) {
                Vec3 away = mob.position().subtract(e.getValue().source()).multiply(1, 0, 1);
                if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
                Vec3 dest = mob.position().add(away.normalize().scale(8));
                mob.setTarget(null);
                mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.4);
            }
        }
        if (now % 600 == 0) RESONANCE.entrySet().removeIf(en -> now - en.getValue()[1] > 600);
    }
}
