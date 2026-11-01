package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.entity.ThrownChakram;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Tidecaller: command water. Dries out away from it. */
public class TidecallerPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.TIDECALLER; }

    static boolean wet(ServerPlayer p) {
        return p.isInWaterOrRain();
    }

    static boolean dry(ServerPlayer p, MutationData d) {
        if (Catalysts.holding(p, Mutation.TIDECALLER) || wet(p)) return false;
        boolean hot = Powers.isNether(p.level()) || Powers.temperature(p) >= 1.9f;
        return hot || p.level().getGameTime() - d.timer("last_wet") > 12000;
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        if (wet(p) && now % 20 == 0) ModAttachments.set(p, ModAttachments.get(p).withTimer("last_wet", now));
        if (d.timer("last_wet") == 0) ModAttachments.set(p, ModAttachments.get(p).withTimer("last_wet", now));
        Powers.add(p, Attributes.WATER_MOVEMENT_EFFICIENCY, "tidecaller", d.has(Secondary.HYDRO_JET) ? 1.0 : 0.5);
        Powers.add(p, Attributes.OXYGEN_BONUS, "tidecaller", 100);
        if (now % 20 != 0) return;
        if (p.isInWater()) {
            Powers.effect(p, MobEffects.DOLPHINS_GRACE, 40, 0);
            if (p.isUnderWater()) Powers.effect(p, MobEffects.NIGHT_VISION, 260, 0);
            if (d.has(Secondary.CLEANSING_WATER)) { p.removeEffect(MobEffects.POISON); p.removeEffect(MobEffects.WITHER); }
        }
        if (wet(p) && now % 40 == 0) p.heal(1f);
        if (d.has(Secondary.RAIN_DANCER) && p.level().isRainingAt(p.blockPosition().above())) {
            Powers.effect(p, MobEffects.SPEED, 40, 0);
            Powers.effect(p, MobEffects.STRENGTH, 40, 0);
        }
        if (dry(p, d)) Powers.effect(p, MobEffects.WEAKNESS, 40, 0);
        if (d.has(Secondary.UNDERTOW) && now % 10 == 0) {
            for (LivingEntity e : Kit.foes(p, 8)) if (e.isInWater()) Powers.impulse(e, new Vec3(0, -0.25, 0), false);
        }
    }

    @Override
    public float cooldownFactor(ServerPlayer p, MutationData d, ActiveAbility a) {
        return dry(p, d) ? 2f : 1f;
    }

    @Override
    public void onDealDamage(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.DEEP_PRESSURE) && target.isInWater()) e.setAmount(e.getAmount() * 1.3f);
    }

    /** A wave rolling forward from the player, carrying mobs. */
    static void wave(ServerPlayer p, MutationData d, double length, double width, float dmg, int ticks) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 dir = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z).normalize();
        Vec3 start = p.position().add(dir.scale(1.5));
        double speed = length / ticks;
        Zones.add(level, start, width, ticks, p.getUUID(), z -> {
            ServerPlayer o = z.ownerPlayer();
            z.center = start.add(dir.scale(z.age() * speed));
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            for (double s = -width; s <= width; s += 1) {
                Vec3 at = z.center.add(side.scale(s));
                level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.5, at.z, 4, 0.3, 0.6, 0.3, 0.1);
                level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y + 1.2, at.z, 1, 0.2, 0.4, 0.2, 0.05);
                BlockPos bp = BlockPos.containing(at);
                for (int dy = 0; dy <= 2; dy++) if (level.getBlockState(bp.above(dy)).is(Blocks.FIRE)) level.removeBlock(bp.above(dy), false);
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(width, 2, width),
                    e -> e != o && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player) && !Minions.isMinion(e))) {
                e.clearFire();
                Powers.impulse(e, dir.scale(speed + 0.4).add(0, 0.25, 0), true);
                if (z.touched.add(e.getId()) && o != null) Powers.hurt(o, e, dmg * Powers.power(d));
            }
        }, null);
        Powers.sound(p, SoundEvents.GENERIC_SPLASH, 2f, 0.6f);
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case WATER_WHIP -> {
                LivingEntity t = Kit.look(p, 8);
                if (t == null) return false;
                Powers.hurt(p, t, 5f * Powers.power(d));
                Powers.impulse(t, p.position().subtract(t.position()).normalize().scale(1.0).add(0, 0.3, 0), true);
                Powers.line(level, p.getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.SPLASH, 0.25);
                if (d.has(Secondary.ICE_SHAPER)) {
                    for (BlockPos q : BlockPos.betweenClosed(t.blockPosition().offset(-1, -1, -1), t.blockPosition().offset(1, 0, 1))) {
                        if (level.getBlockState(q).is(Blocks.WATER)) Powers.freezeTemporary(level, q.immutable(), Blocks.ICE.defaultBlockState(), 200);
                    }
                }
                Powers.sound(p, SoundEvents.FISHING_BOBBER_SPLASH, 1.5f, 0.8f);
                return true;
            }
            case TIDAL_WAVE -> { wave(p, d, 12, 2.5, 3f, 20); return true; }
            case BUBBLE_PRISON -> {
                LivingEntity t = Kit.look(p, 16);
                if (t == null) return false;
                t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 0));
                t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 9));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 9));
                Zones.add(level, t.position(), 1, 80, p.getUUID(), z -> {
                    if (t.isAlive() && z.age() % 3 == 0) level.sendParticles(ParticleTypes.BUBBLE_POP, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 8, 0.6, 0.6, 0.6, 0.02);
                }, null);
                Powers.sound(p, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.5f, 1f);
                return true;
            }
            case GEYSER -> {
                LivingEntity t = Kit.look(p, 16);
                LivingEntity who = t != null ? t : p;
                Powers.impulse(who, new Vec3(0, t != null ? 1.6 : 1.8, 0), false);
                for (int y = 0; y < 8; y++) level.sendParticles(ParticleTypes.SPLASH, who.getX(), who.getY() + y * 0.5, who.getZ(), 10, 0.3, 0.2, 0.3, 0.1);
                if (who != p) Powers.hurt(p, who, 4f);
                Powers.sound(p, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 2f, 0.6f);
                return true;
            }
            case RAINMAKER -> {
                if (level.dimension() != Level.OVERWORLD) return false;
                var server = level.getServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "weather rain 1200");
                Powers.particles(p, ParticleTypes.SPLASH, 60, 1.5, 0.2);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        wave(p, d, 30, 3.5, 8f, 30);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (wet(p)) e.setAmount(e.getAmount() * 2f);
    }

    @Override
    public boolean weaponUse(ServerPlayer p, MutationData d, ItemStack stack) {
        ServerLevel level = (ServerLevel) p.level();
        ThrownChakram c = new ThrownChakram(level, p, stack.copyWithCount(1));
        c.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 1.6f, 0);
        level.addFreshEntity(c);
        if (!p.isCreative()) stack.shrink(1);
        Powers.sound(p, SoundEvents.TRIDENT_THROW.value(), 1f, 1f);
        return true;
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.add(p, Attributes.WATER_MOVEMENT_EFFICIENCY, "tidecaller", 0);
        Powers.add(p, Attributes.OXYGEN_BONUS, "tidecaller", 0);
    }
}
