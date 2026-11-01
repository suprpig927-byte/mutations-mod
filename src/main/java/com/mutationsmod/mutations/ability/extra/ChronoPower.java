package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Chrono: rewind yourself, freeze things in time. */
public class ChronoPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.CHRONO; }

    private record Snap(Vec3 pos, float health, long time) {}
    private static final Map<UUID, Deque<Snap>> HISTORY = new HashMap<>();

    /** Mobs frozen in time: stored damage lands when they thaw. */
    private static final class Frozen {
        final LivingEntity e; final long until; final UUID owner; float stored;
        Frozen(LivingEntity e, long until, UUID owner) { this.e = e; this.until = until; this.owner = owner; }
    }
    private static final Map<UUID, Frozen> FROZEN = new HashMap<>();

    public static void freeze(ServerPlayer owner, LivingEntity e, int ticks) {
        if (e instanceof Player) return;
        if (e instanceof Mob m) m.setNoAi(true);
        e.setDeltaMovement(Vec3.ZERO);
        FROZEN.put(e.getUUID(), new Frozen(e, e.level().getGameTime() + ticks, owner.getUUID()));
    }

    public static void tickFrozen(ServerLevel level) {
        long now = level.getGameTime();
        for (Frozen f : new ArrayList<>(FROZEN.values())) {
            if (f.e.level() != level) continue;
            if (!f.e.isAlive()) { FROZEN.remove(f.e.getUUID()); continue; }
            if (now < f.until) {
                f.e.setDeltaMovement(Vec3.ZERO);
                if (now % 10 == 0) level.sendParticles(ParticleTypes.END_ROD, f.e.getX(), f.e.getY() + f.e.getBbHeight() + 0.2, f.e.getZ(), 2, 0.2, 0.1, 0.2, 0);
                continue;
            }
            FROZEN.remove(f.e.getUUID());
            if (f.e instanceof Mob m) m.setNoAi(false);
            if (f.stored > 0) {
                ServerPlayer o = level.getServer().getPlayerList().getPlayer(f.owner);
                if (o != null) Powers.hurt(o, f.e, f.stored);
                else f.e.hurtServer(level, level.damageSources().magic(), f.stored);
                level.sendParticles(ParticleTypes.CRIT, f.e.getX(), f.e.getY() + 1, f.e.getZ(), 20, 0.4, 0.5, 0.4, 0.2);
            }
        }
    }

    /** Damage to a frozen mob is stored, not dealt. */
    public static void onAnyDamage(LivingIncomingDamageEvent e) {
        Frozen f = FROZEN.get(e.getEntity().getUUID());
        if (f == null) return;
        f.stored += e.getAmount();
        e.setCanceled(true);
    }

    private static int window(ServerPlayer p) {
        return Catalysts.holding(p, Mutation.CHRONO) ? 200 : 100;
    }

    private static boolean rewind(ServerPlayer p, int ticksBack) {
        Deque<Snap> h = HISTORY.get(p.getUUID());
        if (h == null || h.isEmpty()) return false;
        long target = p.level().getGameTime() - ticksBack;
        Snap pick = h.peekFirst();
        for (Snap s : h) if (s.time <= target) pick = s;
        ServerLevel level = (ServerLevel) p.level();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        Powers.teleport(p, pick.pos);
        p.setHealth(Math.max(p.getHealth(), Math.min(p.getMaxHealth(), pick.health)));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        Powers.sound(p, SoundEvents.BEACON_DEACTIVATE, 1f, 1.8f);
        return true;
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        if (now % 5 == 0) {
            Deque<Snap> h = HISTORY.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
            h.addLast(new Snap(p.position(), p.getHealth(), now));
            while (!h.isEmpty() && now - h.peekFirst().time > 220) h.removeFirst();
        }
        if (now % 4 == 0) {   // bad effects wear off 25% faster
            for (MobEffectInstance inst : new ArrayList<>(p.getActiveEffects())) {
                if (inst.getEffect().value().getCategory() != MobEffectCategory.HARMFUL || inst.getDuration() <= 1 || inst.isInfiniteDuration()) continue;
                p.forceAddEffect(new MobEffectInstance(inst.getEffect(), inst.getDuration() - 1, inst.getAmplifier(), inst.isAmbient(), inst.isVisible(), inst.showIcon()), null);
            }
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        var src = e.getSource();
        if (src.is(DamageTypes.FELL_OUT_OF_WORLD) || src.is(DamageTypes.STARVE) || src.is(DamageTypes.GENERIC_KILL) || Powers.SELF_DAMAGE.get()) return;
        float chance = d.has(Secondary.DEJA_VU) ? 0.2f : 0.1f;
        if (p.getRandom().nextFloat() < chance) {
            e.setCanceled(true);
            Powers.particles(p, ParticleTypes.END_ROD, 10, 0.4, 0.05);
        }
    }

    @Override
    public void onMelee(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.ECHO_STRIKE) && p.getRandom().nextFloat() < 0.25f) {
            float dmg = e.getAmount() * 0.5f;
            Powers.later((ServerLevel) p.level(), 20, () -> { if (target.isAlive()) Powers.hurt(p, target, dmg); });
        }
    }

    @Override
    public void afterAbility(ServerPlayer p, MutationData d) {
        if (!Catalysts.holding(p, Mutation.CHRONO)) p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0));
    }

    @Override
    public boolean cheatDeath(LivingDeathEvent e, ServerPlayer p, MutationData d) {
        if (!d.has(Secondary.BORROWED_TIME) || Powers.timerActive(d, p.level(), "borrowed_time")) return false;
        if (e.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || e.getSource().is(DamageTypes.GENERIC_KILL)) return false;
        e.setCanceled(true);
        p.setHealth(1f);
        rewind(p, 100);
        Powers.setTimer(p, "borrowed_time", 12000);
        return true;
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case REWIND -> {
                if (!rewind(p, window(p))) return false;
                if (d.has(Secondary.ACCELERATE)) {
                    p.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.HASTE, 100, 1));
                }
                return true;
            }
            case STASIS -> {
                LivingEntity t = Kit.look(p, 16);
                if (t == null) return false;
                freeze(p, t, 80);
                Powers.sound(p, SoundEvents.BELL_BLOCK, 1f, 2f);
                return true;
            }
            case TIME_BUBBLE -> {
                int ticks = d.has(Secondary.LONG_BUBBLE) ? 240 : 120;
                Zones.add(level, p.position(), 6, ticks, p.getUUID(), z -> {
                    ServerPlayer o = z.ownerPlayer();
                    if (o == null) return;
                    z.center = o.position();
                    if (z.age() % 10 == 0) for (LivingEntity e : Kit.foes(o, 6)) e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 15, 3, false, false));
                    for (Projectile pr : level.getEntitiesOfClass(Projectile.class, o.getBoundingBox().inflate(6), x -> x.getOwner() != o && !x.entityTags().contains("mutations_slowed"))) {
                        pr.setDeltaMovement(pr.getDeltaMovement().scale(0.25));
                        pr.addTag("mutations_slowed");
                    }
                    if (z.age() % 4 == 0) level.sendParticles(ParticleTypes.END_ROD, z.center.x, z.center.y + 1, z.center.z, 4, 3, 1.5, 3, 0);
                }, null);
                Powers.sound(p, SoundEvents.BEACON_POWER_SELECT, 1f, 0.5f);
                return true;
            }
            case HASTE_FIELD -> {
                for (Player pl : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(6))) {
                    pl.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1));
                    pl.addEffect(new MobEffectInstance(MobEffects.HASTE, 160, 1));
                }
                Powers.particles(p, ParticleTypes.END_ROD, 30, 2, 0.05);
                return true;
            }
            case AGE -> {
                LivingEntity t = Kit.look(p, 16);
                if (t == null) return false;
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                Powers.mult(t, Attributes.ARMOR, "aged", -0.5);
                Powers.later(level, 200, () -> Powers.mult(t, Attributes.ARMOR, "aged", 0));
                level.sendParticles(ParticleTypes.ASH, t.getX(), t.getY() + 1, t.getZ(), 30, 0.4, 0.6, 0.4, 0);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        for (LivingEntity e : Kit.foes(p, 32)) freeze(p, e, 100);
        for (Projectile pr : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(32), x -> x.getOwner() != p)) pr.setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 200, 10, 3, 10, 0);
        Powers.sound(p, SoundEvents.BELL_RESONATE, 2f, 0.5f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
    }

    @Override
    public boolean weaponUse(ServerPlayer p, MutationData d, ItemStack stack) {
        if (!rewind(p, 60)) return false;
        p.getCooldowns().addCooldown(stack, 400);
        return true;
    }

    @Override
    public void reset(ServerPlayer p) {
        HISTORY.remove(p.getUUID());
    }
}
