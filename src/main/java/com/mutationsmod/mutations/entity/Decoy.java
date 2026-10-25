package com.mutationsmod.mutations.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Chameleon's Omega decoy: a living copy of the player (a vanilla mannequin wearing their skin) that wanders
 * around drawing mobs, then vanishes. Tagged so any copy left behind in an unloaded chunk is removed on load.
 */
public class Decoy extends Mannequin {
    public static final String TAG = "mutations_decoy";
    private int life = 400;
    private float heading;

    public Decoy(Level level) {
        super(level);
        addTag(TAG);
    }

    public static Decoy of(ServerPlayer owner, Vec3 at, int life) {
        Decoy d = new Decoy(owner.level());
        d.setComponent(DataComponents.PROFILE, ResolvableProfile.createResolved(owner.getGameProfile()));
        d.life = life;
        d.heading = owner.getRandom().nextFloat() * 360f;
        d.snapTo(at.x, at.y, at.z, d.heading, 0);
        d.setCustomName(owner.getName());
        d.setCustomNameVisible(false);
        return d;
    }

    @Override
    protected Component getDescription() {
        return null;                                         // no "Mannequin" label
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (--life <= 0) {
            poof(sl);
            return;
        }
        // Wander: pick a new heading now and then and walk that way.
        if (tickCount % 30 == 0) heading += (getRandom().nextFloat() - 0.5f) * 140f;
        double rad = Math.toRadians(heading);
        Vec3 v = getDeltaMovement();
        setDeltaMovement(-Math.sin(rad) * 0.14, v.y, Math.cos(rad) * 0.14);
        setYRot(heading);
        yBodyRot = heading;
        yHeadRot = heading;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hit = super.hurtServer(level, source, amount);
        if (getHealth() <= 0 || amount >= 6) poof(level);
        return hit;
    }

    private void poof(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.03);
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 1, getZ(), 8, 0.3, 0.6, 0.3, 0.03);
        discard();
    }
}
