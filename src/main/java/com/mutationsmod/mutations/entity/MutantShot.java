package com.mutationsmod.mutations.entity;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.registry.ModContent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** A generic mutant projectile (venom glob, crystal shard, boulder, shrapnel...): looks like an item, hurts what it hits. */
public class MutantShot extends ThrowableItemProjectile {
    private float damage = 4f;
    private int pierce;
    private float gravity = 0.03f;
    private int life;
    private BiConsumer<MutantShot, LivingEntity> onHit;
    private final Set<Integer> hit = new HashSet<>();

    public MutantShot(EntityType<? extends MutantShot> type, Level level) {
        super(type, level);
    }

    public MutantShot(Level level, LivingEntity owner, ItemStack look, float damage, int pierce, float gravity, BiConsumer<MutantShot, LivingEntity> onHit) {
        super(ModContent.MUTANT_SHOT.get(), owner, level, look);
        this.damage = damage;
        this.pierce = pierce;
        this.gravity = gravity;
        this.onHit = onHit;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    @Override
    protected double getDefaultGravity() {
        return gravity;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel && !isRemoved() && ++life > 100) discard();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !hit.contains(target.getId()) && !com.mutationsmod.mutations.ability.extra.Minions.isMinion(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel sl) || !(result.getEntity() instanceof LivingEntity target)) return;
        hit.add(target.getId());
        if (getOwner() instanceof ServerPlayer p) Powers.hurt(p, target, damage);
        else target.hurtServer(sl, damageSources().thrown(this, getOwner()), damage);
        if (onHit != null) onHit.accept(this, target);
        if (hit.size() > pierce) burst(sl);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel sl) burst(sl);
    }

    private void burst(ServerLevel sl) {
        ItemStack s = getItem();
        if (!s.isEmpty()) sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(s)), getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.1);
        discard();
    }
}
