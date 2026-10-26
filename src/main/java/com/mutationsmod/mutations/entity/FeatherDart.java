package com.mutationsmod.mutations.entity;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.registry.ModContent;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** A Fierce Wings razor feather. Optionally pierces (Razor Plumes / Skyfall). */
public class FeatherDart extends ThrowableItemProjectile {
    private float damage = 4f;
    private boolean pierce;
    private int life;
    private final Set<Integer> hit = new HashSet<>();

    public FeatherDart(EntityType<? extends FeatherDart> type, Level level) {
        super(type, level);
    }

    public FeatherDart(Level level, LivingEntity owner, float damage, boolean pierce) {
        super(ModContent.FEATHER_DART.get(), owner, level, new ItemStack(ModContent.HAWK_FEATHER.get()));
        this.damage = damage;
        this.pierce = pierce;
    }

    @Override
    protected Item getDefaultItem() {
        return ModContent.HAWK_FEATHER.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel && !isRemoved() && ++life > 60) discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !hit.contains(target.getId());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel sl) || !(result.getEntity() instanceof LivingEntity target)) return;
        hit.add(target.getId());
        if (getOwner() instanceof ServerPlayer p) Powers.hurt(p, target, damage);
        else target.hurtServer(sl, damageSources().thrown(this, getOwner()), damage);
        sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
        if (!pierce || hit.size() >= 3) discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        discard();
    }
}
