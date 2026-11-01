package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Beastcaller: animals answer to you. */
public class BeastcallerPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.BEASTCALLER; }

    static boolean ownedBy(LivingEntity e, ServerPlayer p) {
        return e instanceof TamableAnimal t && t.isTame() && p.getUUID().equals(t.getOwnerReference() == null ? null : t.getOwnerReference().getUUID());
    }

    static List<TamableAnimal> pets(ServerPlayer p, double r) {
        return p.level().getEntitiesOfClass(TamableAnimal.class, p.getBoundingBox().inflate(r), t -> ownedBy(t, p) && t.isAlive());
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        ServerLevel level = (ServerLevel) p.level();
        if (now % 20 == 0) {
            for (Animal a : level.getEntitiesOfClass(Animal.class, p.getBoundingBox().inflate(10), a -> !(a instanceof TamableAnimal t && t.isTame()) && !Minions.isMinion(a))) {
                if (a.distanceToSqr(p) > 9 && a.getNavigation().isDone()) a.getNavigation().moveTo(p, 1.0);
            }
            if (p.getVehicle() instanceof LivingEntity mount && d.has(Secondary.SWIFT_MOUNT)) mount.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1));
        }
        if (now % 40 == 0) {
            for (TamableAnimal t : pets(p, 12)) {
                if (d.has(Secondary.HERD_HEALER)) t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0));
                if (d.has(Secondary.ANIMAL_TONGUE)) {
                    t.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0));
                }
            }
        }
        boolean bear = Powers.timerActive(d, level, "bear_form");
        Powers.add(p, Attributes.MAX_HEALTH, "bear_form", bear ? 20 : 0);
        Powers.add(p, Attributes.ATTACK_DAMAGE, "bear_form", bear ? 6 : 0);
        Powers.mult(p, Attributes.SCALE, "bear_form", bear ? 0.3 : 0);
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "bear_form", bear ? -0.15 : 0);
        if (!bear && p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    @Override
    public boolean ignoredBy(ServerPlayer p, MutationData d, Mob mob) {
        return d.has(Secondary.BEAST_HIDE) && (p.getVehicle() instanceof Animal) && mob instanceof Enemy;
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case CALL_THE_PACK -> {
                boolean alpha = Catalysts.holding(p, Mutation.BEASTCALLER);
                int n = d.has(Secondary.BIGGER_PACK) ? 5 : 3;
                for (int i = 0; i < n; i++) {
                    Vec3 at = Kit.groundNear(level, p.position(), 2.5, p.getRandom());
                    var wolf = Minions.spawn(p, EntityTypes.WOLF, at, alpha ? 1200 : 600, "Spirit Wolf");
                    if (wolf == null) continue;
                    wolf.tame(p);
                    if (alpha) Powers.mult(wolf, Attributes.ATTACK_DAMAGE, "alpha_fang", 0.5);
                }
                Powers.sound(p, SoundEvents.WOLF_SHAKE, 1.5f, 1f);
                return true;
            }
            case COMMAND -> {
                LivingEntity t = Kit.look(p, 32);
                if (t == null) return false;
                for (TamableAnimal pet : pets(p, 32)) pet.setTarget(t);
                Minions.attack(p, t);
                Kit.glow(t, 100);
                Powers.sound(p, SoundEvents.WOLF_SHAKE, 1f, 1f);
                return true;
            }
            case WILD_SENSE, HAWK_EYE -> {
                double r = a == ActiveAbility.HAWK_EYE ? 48 : 32;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e != p && e.isAlive())) Kit.glow(e, 200);
                if (a == ActiveAbility.HAWK_EYE) level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 12, p.getZ(), 30, 3, 0.5, 3, 0.05);
                Powers.sound(p, SoundEvents.PARROT_AMBIENT, 1f, 0.8f);
                return true;
            }
            case BEAR_FORM -> {
                Powers.setTimer(p, "bear_form", 300);
                Powers.particles(p, ParticleTypes.CLOUD, 30, 0.6, 0.05);
                Powers.sound(p, SoundEvents.POLAR_BEAR_WARNING, 1.5f, 0.8f);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 dir = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z).normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        List<Mob> herd = new ArrayList<>();
        for (int i = -2; i <= 2; i++) {
            var horse = EntityTypes.HORSE.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (horse == null) continue;
            Vec3 at = p.position().subtract(dir.scale(2)).add(side.scale(i * 1.6));
            horse.snapTo(at.x, at.y, at.z, p.getYRot(), 0);
            horse.setNoAi(true);
            horse.setPermanentlyInvulnerable(true);
            horse.addTag(Minions.TAG);
            level.addFreshEntity(horse);
            herd.add(horse);
        }
        Zones.add(level, p.position(), 3, 40, p.getUUID(), z -> {
            ServerPlayer o = z.ownerPlayer();
            for (Mob h : herd) {
                if (!h.isAlive()) continue;
                Vec3 next = h.position().add(dir.scale(0.8));
                h.setPos(next.x, next.y, next.z);
                h.setDeltaMovement(dir.scale(0.8));
                level.sendParticles(ParticleTypes.CLOUD, h.getX(), h.getY() + 0.2, h.getZ(), 2, 0.4, 0.1, 0.4, 0.01);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, h.getBoundingBox().inflate(1.2), e -> !herd.contains(e) && e != o && e.isAlive()
                        && !(e instanceof Player) && !Minions.isMinion(e))) {
                    if (z.touched.add(e.getId()) && o != null) Powers.hurt(o, e, 10f * Powers.power(d));
                    Powers.impulse(e, dir.scale(1.0).add(0, 0.4, 0), false);
                }
            }
        }, z -> {
            for (Mob h : herd) {
                level.sendParticles(ParticleTypes.POOF, h.getX(), h.getY() + 1, h.getZ(), 10, 0.4, 0.5, 0.4, 0.02);
                h.discard();
            }
        });
        Powers.sound(p, SoundEvents.HORSE_GALLOP, 2f, 0.8f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        Marks.set(target, "pack_mark", p.level().getGameTime() + 200, 0);
        Kit.glow(target, 200);
        for (TamableAnimal pet : pets(p, 32)) pet.setTarget(target);
        Minions.attack(p, target);
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.add(p, Attributes.MAX_HEALTH, "bear_form", 0);
        Powers.add(p, Attributes.ATTACK_DAMAGE, "bear_form", 0);
        Powers.mult(p, Attributes.SCALE, "bear_form", 0);
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "bear_form", 0);
    }

    // ---------------------------------------------------------------- events

    /** Tamed animals of a Beastcaller take 30% less damage. */
    public static void onAnyDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof TamableAnimal t) || !t.isTame() || t.getOwnerReference() == null) return;
        if (!(t.level() instanceof ServerLevel sl)) return;
        ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(t.getOwnerReference().getUUID());
        if (owner != null && ModAttachments.get(owner).has(Mutation.BEASTCALLER)) e.setAmount(e.getAmount() * 0.7f);
    }

    /** A passive animal died: nearby Beastcallers feel it. */
    public static void onAnyDeath(LivingEntity victim) {
        if (!(victim instanceof Animal) || Minions.isMinion(victim) || !(victim.level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(victim) > 16 * 16) continue;
            MutationData d = ModAttachments.get(p);
            if (!d.has(Mutation.BEASTCALLER) || com.mutationsmod.mutations.ability.Suppression.active(d, p.level())) continue;
            Powers.hurtSelf(p, d.has(Secondary.THICK_FUR) ? 1f : 2f);
        }
    }

    /** One feeding tames any tameable animal. */
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        MutationData d = ModAttachments.get(p);
        if (!d.has(Mutation.BEASTCALLER) || com.mutationsmod.mutations.ability.Suppression.active(d, p.level())) return;
        ItemStack held = p.getItemInHand(e.getHand());
        if (e.getTarget() instanceof TamableAnimal t && !t.isTame() && t.isFood(held)) {
            t.tame(p);
            t.setOrderedToSit(false);
            if (!p.isCreative()) held.shrink(1);
            ((ServerLevel) p.level()).sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + 1, t.getZ(), 6, 0.3, 0.3, 0.3, 0);
            e.setCanceled(true);
        } else if (e.getTarget() instanceof AbstractHorse h && !h.isTamed() && !held.isEmpty() && h.isFood(held)) {
            h.tameWithName(p);
            if (!p.isCreative()) held.shrink(1);
            ((ServerLevel) p.level()).sendParticles(ParticleTypes.HEART, h.getX(), h.getY() + 1.5, h.getZ(), 6, 0.3, 0.3, 0.3, 0);
            e.setCanceled(true);
        }
    }
}
