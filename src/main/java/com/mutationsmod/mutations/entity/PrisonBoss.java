package com.mutationsmod.mutations.entity;

import com.mutationsmod.mutations.registry.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/** Shared boss plumbing: a boss bar, never despawns, stays in its arena, and can be shut down by Nullite. */
public abstract class PrisonBoss extends Monster {
    protected final ServerBossEvent bossBar;

    protected PrisonBoss(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossBar = new ServerBossEvent(getUUID(), getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.xpReward = 120;
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Nullite (spear hits, Inhibitor Collar, Inhibitor Field) shuts the boss's powers off. */
    public boolean suppressed() {
        return hasEffect(ModContent.SUPPRESSED);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        bossBar.setProgress(getHealth() / getMaxHealth());
        bossBar.setName(getDisplayName().copy().append(suppressed() ? Component.literal("  [SUPPRESSED]") : Component.empty()));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        bossBar.setName(getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }
}
