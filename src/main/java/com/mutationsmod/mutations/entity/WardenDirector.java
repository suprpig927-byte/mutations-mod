package com.mutationsmod.mutations.entity;

import com.mutationsmod.mutations.ability.Suppression;
import com.mutationsmod.mutations.registry.ModContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Warden-Director: ran the Containment Prison. Fights with a Nullite Spear (every hit suppresses your
 * powers), wears a full Containment Suit, lunges at distant targets and releases Failed Experiments as he weakens.
 */
public class WardenDirector extends PrisonBoss {
    private int lungeCooldown = 100;
    private int releases = 0;

    public WardenDirector(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        equip();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 220.0).add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ARMOR, 8.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    private void equip() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModContent.NULLITE_SPEAR.get()));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModContent.CONTAINMENT_HELMET.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModContent.CONTAINMENT_CHESTPLATE.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModContent.CONTAINMENT_LEGGINGS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(ModContent.CONTAINMENT_BOOTS.get()));
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof ServerPlayer p) Suppression.apply(p, 5 * 20, true);   // Nullite Spear
        return hit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        float hp = getHealth() / getMaxHealth();
        if ((releases == 0 && hp < 0.66f) || (releases == 1 && hp < 0.33f)) {
            releases++;
            release(level);
        }
        LivingEntity target = getTarget();
        if (target != null && --lungeCooldown <= 0 && distanceToSqr(target) > 36) {
            lungeCooldown = 120;
            Vec3 dir = target.position().subtract(position()).normalize();
            setDeltaMovement(dir.scale(1.6).add(0, 0.35, 0));
            needsSync = true;
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPEAR_USE, SoundSource.HOSTILE, 1.0f, 0.7f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1, getZ(), 12, 0.4, 0.6, 0.4, 0.05);
        }
    }

    /** "Containment breach!" Three Failed Experiments burst out around him. */
    private void release(ServerLevel level) {
        for (Player p : level.players()) {
            if (p.distanceToSqr(this) < 48 * 48) {
                p.sendSystemMessage(Component.literal("Warden-Director: Release the subjects!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.HOSTILE, 1.5f, 0.6f);
        for (int i = 0; i < 3; i++) {
            FailedExperiment fe = ModContent.FAILED_EXPERIMENT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (fe == null) continue;
            double a = i * Math.PI * 2 / 3;
            fe.snapTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, 0, 0);
            fe.finalizeSpawn(level, level.getCurrentDifficultyAt(fe.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
            if (getTarget() != null) fe.setTarget(getTarget());
            level.addFreshEntity(fe);
            level.sendParticles(ParticleTypes.POOF, fe.getX(), fe.getY() + 1, fe.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
    }
}
