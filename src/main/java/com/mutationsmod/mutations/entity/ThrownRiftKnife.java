package com.mutationsmod.mutations.entity;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.registry.ModWeapons;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Blinker's Rift Knife in flight: wherever it lands, the thrower follows. Then it's back in their hand. */
public class ThrownRiftKnife extends ThrowableItemProjectile {
    public ThrownRiftKnife(EntityType<? extends ThrownRiftKnife> type, Level level) {
        super(type, level);
    }

    public ThrownRiftKnife(Level level, LivingEntity owner, ItemStack stack) {
        super(ModWeapons.RIFT_KNIFE_ENTITY.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModWeapons.RIFT_KNIFE.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (getOwner() instanceof ServerPlayer p && result.getEntity() instanceof LivingEntity target && target != p) {
            Powers.hurt(p, target, 6f * Powers.power(ModAttachments.get(p)));
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel sl) || isRemoved()) return;
        if (getOwner() instanceof ServerPlayer p && p.isAlive() && p.level() == sl) {
            Vec3 back = getDeltaMovement().lengthSqr() > 1e-4 ? getDeltaMovement().normalize().scale(-0.6) : Vec3.ZERO;
            Vec3 spot = Powers.safeSpot(p, result.getLocation().add(back).subtract(0, 0.2, 0));
            if (spot == null) spot = Powers.safeSpot(p, result.getLocation().add(back).add(0, 1, 0));
            Vec3 from = p.position();
            if (spot != null) {
                Powers.teleport(p, spot);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z, 25, 0.3, 0.6, 0.3, 0.1);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, spot.x, spot.y + 1, spot.z, 25, 0.3, 0.6, 0.3, 0.1);
                p.level().playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
            }
            giveBack(p);
        } else if (!getItem().isEmpty()) {
            spawnAtLocation(sl, getItem().copy());
        }
        discard();
    }

    private void giveBack(Player p) {
        ItemStack stack = getItem().copy();
        if (!stack.isEmpty() && !p.getInventory().add(stack)) p.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }
}
