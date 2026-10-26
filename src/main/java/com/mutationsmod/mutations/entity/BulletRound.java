package com.mutationsmod.mutations.entity;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.ability.power.Bullet;
import com.mutationsmod.mutations.registry.ModContent;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** A Bullet mutant's round. Looks like the ore it was made from; each kind hits differently. */
public class BulletRound extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Integer> AMMO = SynchedEntityData.defineId(BulletRound.class, EntityDataSerializers.INT);
    private float damage = 6f;
    private boolean ricochet, tracer, scrap, heavy;
    private int bounces, pierced, life;
    private final Set<Integer> hit = new HashSet<>();

    public BulletRound(EntityType<? extends BulletRound> type, Level level) {
        super(type, level);
    }

    public BulletRound(Level level, LivingEntity owner, Bullet.Ammo ammo) {
        super(ModContent.BULLET_ROUND.get(), owner, level, new ItemStack(ammo.item));
        this.entityData.set(AMMO, ammo.ordinal());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AMMO, 0);
    }

    public Bullet.Ammo ammo() {
        return Bullet.Ammo.byId(this.entityData.get(AMMO));
    }

    public void setup(float damage, boolean ricochet, boolean tracer, boolean scrap) {
        this.damage = damage;
        this.ricochet = ricochet || ammo() == Bullet.Ammo.GOLD;
        this.tracer = tracer;
        this.scrap = scrap;
    }

    public void setHeavy(boolean heavy) {
        this.heavy = heavy;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_NUGGET;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.008;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && !isRemoved()) {
            if (++life > 80) discard();
            if (life % 2 == 0) sl.sendParticles(new DustParticleOptions(ammo().color, 0.7f), getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !hit.contains(target.getId());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(level() instanceof ServerLevel sl) || !(result.getEntity() instanceof LivingEntity target)) return;
        hit.add(target.getId());
        com.mutationsmod.mutations.MutationsMod.LOGGER.debug("BULLETHIT {} {} dmg={}", ammo(), target, damage);
        Bullet.Ammo a = ammo();
        ServerPlayer owner = getOwner() instanceof ServerPlayer sp ? sp : null;
        if (a == Bullet.Ammo.NETHERITE) {
            Powers.ABILITY_DAMAGE.set(true);
            try {   // straight through armor
                target.hurtServer(sl, damageSources().indirectMagic(this, owner), damage);
            } finally {
                Powers.ABILITY_DAMAGE.set(false);
            }
        } else if (owner != null) {
            Powers.hurt(owner, target, damage);
        } else {
            target.hurtServer(sl, damageSources().thrown(this, getOwner()), damage);
        }
        if (tracer) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        if (scrap && !target.isAlive()) {
            int n = 1 + random.nextInt(3);
            sl.addFreshEntity(new ItemEntity(sl, target.getX(), target.getY() + 0.5, target.getZ(), new ItemStack(Items.IRON_NUGGET, n)));
        }
        Vec3 push = getDeltaMovement().normalize();
        switch (a) {
            case COAL -> target.igniteForSeconds(5);
            case COPPER -> {
                LivingEntity next = null;
                double best = 25;
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5),
                        e -> e != target && e != owner && e.isAlive())) {
                    double ds = e.distanceToSqr(target);
                    if (ds < best) { best = ds; next = e; }
                }
                if (next != null) {
                    Powers.line(sl, target.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 0.3);
                    if (owner != null) Powers.hurt(owner, next, damage * 0.7f);
                }
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
            }
            case REDSTONE -> blast(sl, owner, target.position().add(0, target.getBbHeight() * 0.5, 0));
            case LAPIS -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                Powers.impulse(target, new Vec3(push.x, 0, push.z).normalize().scale(1.2).add(0, 0.3, 0), false);
            }
            default -> { }
        }
        if (heavy) Powers.impulse(target, new Vec3(push.x, 0, push.z).normalize().scale(2.0).add(0, 0.5, 0), false);
        boolean pierce = a == Bullet.Ammo.DIAMOND && ++pierced < 4;
        if (!pierce) discard();
        else damage *= 0.85f;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel sl)) return;
        com.mutationsmod.mutations.MutationsMod.LOGGER.debug("BULLETBLOCK {} at {} life={}", ammo(), result.getBlockPos(), life);
        if (ammo() == Bullet.Ammo.REDSTONE) {
            blast(sl, getOwner() instanceof ServerPlayer sp ? sp : null, result.getLocation());
            discard();
            return;
        }
        if (ricochet && bounces++ == 0) {
            Vec3 v = getDeltaMovement();
            Direction face = result.getDirection();
            Vec3 bounced = switch (face.getAxis()) {
                case X -> new Vec3(-v.x, v.y, v.z);
                case Y -> new Vec3(v.x, -v.y, v.z);
                case Z -> new Vec3(v.x, v.y, -v.z);
            };
            setDeltaMovement(bounced.scale(0.8));
            setPos(result.getLocation().add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(0.1)));
            hit.clear();
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.2f, 2.0f);
            return;
        }
        sl.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 4, 0.05, 0.05, 0.05, 0.1);
        discard();
    }

    private void blast(ServerLevel sl, ServerPlayer owner, Vec3 at) {
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3), e -> e != owner && e.isAlive())) {
            if (owner != null) Powers.hurt(owner, e, damage * 1.2f);
            Powers.knockAway(e, at, 0.6, 0.3);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        sl.sendParticles(new DustParticleOptions(0xD02020, 1.4f), at.x, at.y, at.z, 20, 0.8, 0.8, 0.8, 0);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.5f);
    }
}
