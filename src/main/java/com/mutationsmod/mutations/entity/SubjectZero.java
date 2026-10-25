package com.mutationsmod.mutations.entity;

import com.mutationsmod.mutations.ability.Powers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Subject Zero: the first mutant, sealed in the deepest cell. Cycles through five mutations' powers every
 * eight seconds. While suppressed by Nullite it can't use them (and stops cycling).
 */
public class SubjectZero extends PrisonBoss {
    public enum Form {
        PYRO, CRYO, STORMCALLER, BLINKER, COLOSSUS;
        public static Form byId(int i) { return values()[Math.floorMod(i, values().length)]; }
        public String id() { return name().toLowerCase(); }
    }

    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(SubjectZero.class, EntityDataSerializers.INT);
    private static final Identifier COLOSSUS_ID = Identifier.fromNamespaceAndPath("mutations", "zero_colossus");
    private int formTimer = 160;
    private int powerTimer = 40;

    public SubjectZero(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        this.xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 350.0).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 6.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.SCALE, 1.15);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORM, 0);
    }

    public Form form() { return Form.byId(this.entityData.get(FORM)); }

    private void setForm(Form f) {
        this.entityData.set(FORM, f.ordinal());
        var armor = getAttribute(Attributes.ARMOR);
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (armor != null) armor.removeModifier(COLOSSUS_ID);
        if (speed != null) speed.removeModifier(COLOSSUS_ID);
        if (f == Form.COLOSSUS) {
            if (armor != null) armor.addTransientModifier(new AttributeModifier(COLOSSUS_ID, 14.0, AttributeModifier.Operation.ADD_VALUE));
            if (speed != null) speed.addTransientModifier(new AttributeModifier(COLOSSUS_ID, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!suppressed()) {
            if (form() == Form.PYRO && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return false;
            if (source.is(DamageTypes.LIGHTNING_BOLT)) return false;
        } else {
            amount *= 1.25f;                                     // Nullite leaves it vulnerable
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean fireImmune() {
        return form() == Form.PYRO && !suppressed();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (suppressed()) return;
        if (--formTimer <= 0) {
            formTimer = 160;
            setForm(Form.byId(form().ordinal() + 1 + getRandom().nextInt(Form.values().length - 1)));
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.5f, 0.6f);
            level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 30, 0.5, 1, 0.5, 0.1);
        }
        LivingEntity t = getTarget();
        if (t == null || --powerTimer > 0) return;
        switch (form()) {
            case PYRO -> {
                powerTimer = 30;
                for (int i = 0; i < 3; i++) {
                    Vec3 dir = t.getEyePosition().subtract(getEyePosition()).normalize().add(getRandom().nextGaussian() * 0.05, 0, getRandom().nextGaussian() * 0.05);
                    SmallFireball fb = new SmallFireball(level, this, dir);
                    fb.setPos(getX() + dir.x, getEyeY() - 0.2, getZ() + dir.z);
                    level.addFreshEntity(fb);
                }
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1f, 1f);
            }
            case CRYO -> {
                powerTimer = 40;
                if (hasLineOfSight(t)) {
                    Powers.line(level, getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.SNOWFLAKE, 0.4);
                    t.hurtServer(level, damageSources().freeze(), 6f);
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
                    t.setTicksFrozen(Math.max(t.getTicksFrozen(), 200));
                    level.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1f, 0.6f);
                }
            }
            case STORMCALLER -> {
                powerTimer = 50;
                LightningBolt bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (bolt != null) {
                    bolt.snapTo(t.getX(), t.getY(), t.getZ());
                    level.addFreshEntity(bolt);
                }
            }
            case BLINKER -> {
                powerTimer = 60;
                Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(2));
                if (randomTeleport(behind.x, t.getY(), behind.z, true, net.minecraft.tags.BlockTags.FIRE)) {
                    level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.8f);
                    doHurtTarget(level, t);
                }
            }
            case COLOSSUS -> {
                powerTimer = 80;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6), e -> e != this && e.isAlive())) {
                    e.hurtServer(level, damageSources().mobAttack(this), 8f);
                    Vec3 push = e.position().subtract(position()).normalize().scale(1.4).add(0, 0.6, 0);
                    e.push(push.x, push.y, push.z);
                    e.needsSync = true;
                }
                level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 3, 1, 0, 1, 0);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 0.7f);
            }
        }
    }
}
