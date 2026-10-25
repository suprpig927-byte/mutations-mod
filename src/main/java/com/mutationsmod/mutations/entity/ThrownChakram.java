package com.mutationsmod.mutations.entity;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.Raycast;
import com.mutationsmod.mutations.registry.ModWeapons;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Kinetic Mind's Orbit Chakram in flight: steered by the thrower's gaze, cuts through mobs, then returns. */
public class ThrownChakram extends ThrowableItemProjectile {
    private static final double SPEED = 1.4;
    private int age;
    private boolean returning;
    private final Set<Integer> hit = new HashSet<>();

    public ThrownChakram(EntityType<? extends ThrownChakram> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public ThrownChakram(Level level, LivingEntity owner, ItemStack stack) {
        super(ModWeapons.CHAKRAM.get(), owner, level, stack);
        setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return ModWeapons.ORBIT_CHAKRAM.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl) || isRemoved()) return;
        age++;
        Entity owner = getOwner();
        if (!(owner instanceof Player p) || !p.isAlive() || p.level() != level()) {
            if (age > 100) dropAndVanish();
            return;
        }
        if (!returning && age > 22) returning = true;
        Vec3 v;
        if (returning) {
            noPhysics = true;
            Vec3 to = p.getEyePosition().subtract(0, 0.4, 0).subtract(position());
            if (to.length() < 1.6) {
                giveBack(p);
                return;
            }
            v = to.normalize().scale(SPEED + 0.3);
        } else {
            Vec3 target = p instanceof ServerPlayer sp ? Raycast.target(sp, 32) : position().add(getDeltaMovement());
            Vec3 want = target.subtract(position()).normalize().scale(SPEED);
            v = getDeltaMovement().scale(0.75).add(want.scale(0.25)).normalize().scale(SPEED);
        }
        setDeltaMovement(v);
        needsSync = true;
        if (age % 2 == 0) sl.sendParticles(ParticleTypes.PORTAL, getX(), getY(), getZ(), 2, 0.1, 0.1, 0.1, 0.05);
        if (age % 6 == 0) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.3f, 1.8f);
        if (age > 200) giveBack(p);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner() && !hit.contains(target.getId()) && target instanceof LivingEntity;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel) || !(result.getEntity() instanceof LivingEntity target)) return;
        hit.add(target.getId());
        if (getOwner() instanceof ServerPlayer p) {
            Powers.hurt(p, target, 7f * Powers.power(ModAttachments.get(p)));
        } else {
            target.hurtServer((ServerLevel) level(), damageSources().thrown(this, getOwner()), 5f);
        }
        playSound(SoundEvents.TRIDENT_HIT, 0.8f, 1.4f);
        if (hit.size() >= 3) returning = true;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (returning) return;
        returning = true;                                  // bounces off walls and heads home
        playSound(SoundEvents.TRIDENT_HIT_GROUND, 0.8f, 1.3f);
    }

    private void giveBack(Player p) {
        ItemStack stack = getItem().copy();
        if (!stack.isEmpty() && !p.getInventory().add(stack)) p.drop(stack, false);
        playSound(SoundEvents.TRIDENT_RETURN, 0.8f, 1.2f);
        discard();
    }

    private void dropAndVanish() {
        if (level() instanceof ServerLevel sl && !getItem().isEmpty()) spawnAtLocation(sl, getItem().copy());
        discard();
    }
}
