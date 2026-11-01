package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import com.mutationsmod.mutations.registry.ModWeapons;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Soulbinder: raise the fallen as allies; feed on souls. Half-dead. */
public class SoulbinderPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.SOULBINDER; }

    public static final String SOULS = "souls";
    private static final Map<UUID, EntityType<?>> LAST_KILL = new HashMap<>();
    private static final ThreadLocal<Boolean> DRAINING = ThreadLocal.withInitial(() -> false);

    static int maxSouls(ServerPlayer p) { return Catalysts.holding(p, Mutation.SOULBINDER) ? 20 : 10; }

    static int life(MutationData d) { return d.has(Secondary.LINGERING_DEAD) ? 2400 : 1200; }

    private static boolean spend(ServerPlayer p, MutationData d, int n) {
        int have = Kit.meter(d, SOULS);
        if (have < n) {
            p.sendOverlayMessage(Component.literal("Needs " + n + " souls (you have " + have + ").").withStyle(ChatFormatting.DARK_AQUA));
            return false;
        }
        Kit.setMeter(p, SOULS, have - n);
        return true;
    }

    static Mob raise(ServerPlayer p, MutationData d, EntityType<?> type, Vec3 at, int ticks) {
        @SuppressWarnings("unchecked")
        EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) type;
        Mob m = Minions.spawn(p, mobType, at, ticks, "Risen");
        if (m == null) return null;
        m.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
        if (d.has(Secondary.GRAVE_PACT)) Powers.mult(m, Attributes.ATTACK_DAMAGE, "grave_pact", 0.3);
        if (m.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) m.setItemSlot(EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
        m.setDropChance(EquipmentSlot.HEAD, 0f);
        return m;
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        Level level = p.level();
        if (now % 20 == 0 && !Catalysts.holding(p, Mutation.SOULBINDER) && level.dimension() == Level.OVERWORLD && !Powers.isNight(level)
                && level.canSeeSky(p.blockPosition().above()) && p.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && !p.isInWaterOrRain()) {
            p.igniteForSeconds(4);
        }
        if (d.has(Secondary.SOUL_SPEED) && now % 10 == 0) {
            var under = level.getBlockState(p.blockPosition().below());
            if (under.is(Blocks.SOUL_SAND) || under.is(Blocks.SOUL_SOIL)) Powers.effect(p, MobEffects.SPEED, 20, 2);
        }
        long walk = d.timer("spirit_walk_end");
        if (walk > 0 && now >= walk) endSpiritWalk(p);
    }

    @Override
    public void onKill(ServerPlayer p, MutationData d, LivingEntity victim) {
        if (Minions.isMinion(victim)) return;
        int n = p.getMainHandItem().is(ModWeapons.REAPERS_SICKLE.get()) ? 2 : 1;
        Kit.addMeter(p, SOULS, n, maxSouls(p));
        if (d.has(Secondary.SOUL_SIPHON)) { DRAINING.set(true); try { p.heal(n); } finally { DRAINING.set(false); } }
        if (victim instanceof Monster) LAST_KILL.put(p.getUUID(), victim.getType());
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + 1, victim.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
        if (Powers.timerActive(d, p.level(), "army") && victim instanceof Monster && Minions.count(p, null) < 20) {
            raise(p, d, victim.getType(), victim.position(), 600);
        }
    }

    @Override
    public void onMelee(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.DEATHLY_CHILL)) target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
    }

    @Override
    public void onDealDamage(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (Kit.isUndead(target)) Marks.set(target, "provoked", p.level().getGameTime() + 600, 0);
    }

    @Override
    public boolean ignoredBy(ServerPlayer p, MutationData d, Mob mob) {
        return d.has(Secondary.UNDEAD_KIN) && Kit.isUndead(mob) && !Marks.has(mob, "provoked");
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (Powers.timerActive(d, p.level(), "soul_shield") && d.timer("shield_hits") > 0) {
            e.setCanceled(true);
            ModAttachments.set(p, ModAttachments.get(p).withTimer("shield_hits", d.timer("shield_hits") - 1));
            Powers.particles(p, ParticleTypes.SOUL, 10, 0.6, 0.05);
            Powers.sound(p, SoundEvents.SOUL_ESCAPE.value(), 1f, 1f);
        }
    }

    @Override
    public void onDeath(ServerPlayer p, MutationData d) {
        if (d.timer("spirit_walk_end") > 0) endSpiritWalk(p);
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case RAISE -> {
                if (Minions.count(p, null) >= 3) { p.sendOverlayMessage(Component.literal("You already command 3 risen.").withStyle(ChatFormatting.DARK_AQUA)); return false; }
                if (!spend(p, d, 2)) return false;
                EntityType<?> type = LAST_KILL.getOrDefault(p.getUUID(), EntityTypes.ZOMBIE);
                raise(p, d, type, Kit.groundNear(level, p.position(), 2, p.getRandom()), life(d));
                Powers.sound(p, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1f, 0.6f);
                return true;
            }
            case SOUL_DRAIN -> {
                LivingEntity t = Kit.look(p, 16);
                if (t == null || !spend(p, d, 1)) return false;
                Powers.hurt(p, t, 8f);
                DRAINING.set(true);
                try { p.heal(8f); } finally { DRAINING.set(false); }
                Powers.line(level, t.getBoundingBox().getCenter(), p.getEyePosition(), ParticleTypes.SOUL, 0.4);
                Powers.sound(p, SoundEvents.SOUL_ESCAPE.value(), 1.5f, 0.6f);
                return true;
            }
            case SOUL_SHIELD -> {
                if (!spend(p, d, 3)) return false;
                ModAttachments.set(p, ModAttachments.get(p).withTimer("soul_shield", level.getGameTime() + 160).withTimer("shield_hits", 3));
                Powers.particles(p, ParticleTypes.SOUL, 30, 0.8, 0.05);
                return true;
            }
            case SOUL_SWAP -> {
                List<Mob> mine = Minions.of(p);
                if (mine.isEmpty()) return false;
                Mob near = mine.stream().min(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(p))).get();
                Vec3 a1 = p.position(), b1 = near.position();
                Powers.teleport(p, b1);
                near.teleportTo(a1.x, a1.y, a1.z);
                level.sendParticles(ParticleTypes.SOUL, a1.x, a1.y + 1, a1.z, 15, 0.3, 0.6, 0.3, 0.03);
                level.sendParticles(ParticleTypes.SOUL, b1.x, b1.y + 1, b1.z, 15, 0.3, 0.6, 0.3, 0.03);
                return true;
            }
            case SPIRIT_WALK -> {
                if (p.isSpectator() || p.isCreative()) return false;
                long now = level.getGameTime();
                ModAttachments.set(p, ModAttachments.get(p).withTimer("spirit_walk_end", now + 200)
                        .withTimer("sw_x", Math.round(p.getX() * 100) + 3_000_000_000L).withTimer("sw_y", Math.round(p.getY() * 100) + 3_000_000_000L)
                        .withTimer("sw_z", Math.round(p.getZ() * 100) + 3_000_000_000L));
                p.setGameMode(GameType.SPECTATOR);
                p.sendOverlayMessage(Component.literal("You leave your body for 10 seconds...").withStyle(ChatFormatting.DARK_AQUA));
                return true;
            }
            default -> { return false; }
        }
    }

    /** Back into the body: survival mode, at the spot you left it. */
    public static void endSpiritWalk(ServerPlayer p) {
        MutationData d = ModAttachments.get(p);
        if (d.timer("spirit_walk_end") <= 0) return;
        double x = (d.timer("sw_x") - 3_000_000_000L) / 100.0, y = (d.timer("sw_y") - 3_000_000_000L) / 100.0, z = (d.timer("sw_z") - 3_000_000_000L) / 100.0;
        ModAttachments.set(p, d.withTimer("spirit_walk_end", 0));
        if (p.isSpectator()) p.setGameMode(GameType.SURVIVAL);
        p.teleportTo(x, y, z);
        p.resetFallDistance();
        Powers.particles(p, ParticleTypes.SOUL, 20, 0.4, 0.05);
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        int n = Math.min(12, Math.max(6, Kit.meter(d, SOULS)));
        Kit.setMeter(p, SOULS, 0);
        EntityType<?>[] dead = {EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.HUSK, EntityTypes.STRAY};
        for (int i = 0; i < n; i++) raise(p, d, dead[p.getRandom().nextInt(dead.length)], Kit.groundNear(level, p.position(), 4, p.getRandom()), 600);
        Powers.setTimer(p, "army", 600);
        level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 80, 4, 1, 4, 0.05);
        Powers.sound(p, SoundEvents.WITHER_SPAWN, 0.8f, 1.2f);
        return true;
    }

    @Override
    public void reset(ServerPlayer p) {
        endSpiritWalk(p);
    }

    /** Healing works half as well on the half-dead. */
    public static void onHeal(LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || DRAINING.get()) return;
        MutationData d = ModAttachments.get(p);
        if (d.has(Mutation.SOULBINDER) && !com.mutationsmod.mutations.ability.Suppression.active(d, p.level())) e.setAmount(e.getAmount() * 0.5f);
    }

    /** Never leave anyone stuck as a spirit. */
    public static void onLogin(ServerPlayer p) {
        if (ModAttachments.get(p).timer("spirit_walk_end") > 0) endSpiritWalk(p);
    }
}
