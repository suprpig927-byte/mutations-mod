package com.mutationsmod.mutations.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Failed Experiment: a mutated test subject from the Gene Labs. Each one carries one weak power.
 * Never burns in daylight and never turns into a drowned.
 */
public class FailedExperiment extends Zombie {
    public enum Power {
        BURNING, FROZEN, UNSTABLE, HULKING, LEECHING;
        public static Power byId(int id) { return values()[Math.floorMod(id, values().length)]; }
        public String id() { return name().toLowerCase(); }
    }

    private static final EntityDataAccessor<Integer> POWER = SynchedEntityData.defineId(FailedExperiment.class, EntityDataSerializers.INT);
    private static final Identifier HULK_ID = Identifier.fromNamespaceAndPath("mutations", "hulking");
    private int teleportCooldown = 60;

    public FailedExperiment(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(POWER, 0);
    }

    public Power power() {
        return Power.byId(this.entityData.get(POWER));
    }

    public void setPower(Power p) {
        this.entityData.set(POWER, p.ordinal());
        var scale = getAttribute(Attributes.SCALE);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        var health = getAttribute(Attributes.MAX_HEALTH);
        var damage = getAttribute(Attributes.ATTACK_DAMAGE);
        for (var inst : new net.minecraft.world.entity.ai.attributes.AttributeInstance[]{scale, speed, health, damage}) {
            if (inst != null) inst.removeModifier(HULK_ID);
        }
        if (p == Power.HULKING) {
            if (scale != null) scale.addPermanentModifier(new AttributeModifier(HULK_ID, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if (speed != null) speed.addPermanentModifier(new AttributeModifier(HULK_ID, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if (health != null) health.addPermanentModifier(new AttributeModifier(HULK_ID, 20.0, AttributeModifier.Operation.ADD_VALUE));
            if (damage != null) damage.addPermanentModifier(new AttributeModifier(HULK_ID, 4.0, AttributeModifier.Operation.ADD_VALUE));
            setHealth(getMaxHealth());
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
                                                   @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        setBaby(false);
        setPower(Power.byId(level.getRandom().nextInt(Power.values().length)));
        return out;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            switch (power()) {
                case BURNING -> living.igniteForSeconds(4f);
                case FROZEN -> {
                    living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
                    living.setTicksFrozen(Math.max(living.getTicksFrozen(), 140));
                }
                case LEECHING -> heal((float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75f);
                default -> {}
            }
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // Unstable: sometimes blinks away when hurt.
        if (hurt && power() == Power.UNSTABLE && getRandom().nextFloat() < 0.35f) teleportNear(getX(), getY(), getZ(), 6);
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel sl) {
            Power p = power();
            if (tickCount % 6 == 0) {
                switch (p) {
                    case BURNING -> sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 1, 0.25, 0.5, 0.25, 0.01);
                    case FROZEN -> sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 1, 0.25, 0.5, 0.25, 0.01);
                    case UNSTABLE -> sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 2, 0.25, 0.5, 0.25, 0.2);
                    case LEECHING -> sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, getX(), getY() + 1.2, getZ(), 1, 0.2, 0.3, 0.2, 0.01);
                    default -> {}
                }
            }
            if (p == Power.BURNING && isOnFire()) clearFire();
            // Unstable: blinks next to its target every few seconds.
            LivingEntity target = getTarget();
            if (p == Power.UNSTABLE && target != null && --teleportCooldown <= 0) {
                teleportCooldown = 80 + getRandom().nextInt(60);
                if (distanceToSqr(target) > 9) teleportNear(target.getX(), target.getY(), target.getZ(), 3);
            }
        }
    }

    private void teleportNear(double x, double y, double z, double spread) {
        for (int i = 0; i < 8; i++) {
            double tx = x + (getRandom().nextDouble() - 0.5) * 2 * spread;
            double tz = z + (getRandom().nextDouble() - 0.5) * 2 * spread;
            if (randomTeleport(tx, y, tz, true, BlockTags.FIRE)) return;
        }
    }

    @Override
    public boolean fireImmune() {
        return power() == Power.BURNING || super.fireImmune();
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Power", power().ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(POWER, input.getIntOr("Power", 0));
    }
}
