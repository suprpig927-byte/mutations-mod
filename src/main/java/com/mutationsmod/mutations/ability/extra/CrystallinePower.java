package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.entity.MutantShot;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Crystalline: crystal skin, shards and reflection. Shatters under heavy blows. */
public class CrystallinePower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.CRYSTALLINE; }

    static boolean gem(ServerPlayer p) { return Catalysts.holding(p, Mutation.CRYSTALLINE); }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        boolean shattered = Powers.timerActive(d, p.level(), "shattered");
        Powers.add(p, Attributes.ARMOR, "crystal_skin", shattered ? 0 : (d.has(Secondary.DIAMOND_SKIN) ? 10 : 6));
        if (Powers.timerActive(d, p.level(), "facet_shield") && now % 4 == 0) {
            Powers.effect(p, MobEffects.SLOWNESS, 10, 9);
            Powers.particles(p, ParticleTypes.END_ROD, 4, 0.6, 0);
        }
        if (d.has(Secondary.REGROWTH) && now % 40 == 0 && now - d.timer("last_hurt") > 100) p.heal(1f);
        if (d.has(Secondary.GLIMMER) && now % 4 == 0) LuminaryPower.emitLight(p, 11);
        if (d.has(Secondary.SHARP_EDGES) && now % 20 == 0) {
            for (LivingEntity e : Kit.foes(p, 1.3)) Powers.hurt(p, e, 1f);
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        var src = e.getSource();
        if (Powers.timerActive(d, p.level(), "facet_shield")) {
            if (src.getDirectEntity() instanceof Projectile pr) { pr.setDeltaMovement(pr.getDeltaMovement().scale(-1)); pr.needsSync = true; }
            e.setCanceled(true);
            return;
        }
        if (d.has(Secondary.REFRACTING_BODY) && src.getDirectEntity() instanceof Projectile pr && p.getRandom().nextFloat() < 0.25f) {
            pr.setDeltaMovement(pr.getDeltaMovement().scale(-0.8));
            pr.needsSync = true;
            e.setCanceled(true);
            return;
        }
        com.mutationsmod.mutations.ModAttachments.set(p, com.mutationsmod.mutations.ModAttachments.get(p).withTimer("last_hurt", p.level().getGameTime()));
        boolean mace = src.getWeaponItem() != null && src.getWeaponItem().is(Items.MACE);
        if (!gem(p) && (src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypes.FALL) || mace)) e.setAmount(e.getAmount() * 1.5f);
        if (src.getEntity() instanceof LivingEntity att && att != p && src.getDirectEntity() == att) {
            Powers.hurt(p, att, e.getAmount() * (gem(p) ? 0.35f : 0.2f));
        }
    }

    static MutantShot shard(ServerPlayer p, MutationData d, float dmg) {
        float mult = d.has(Secondary.RESONANCE_SHARDS) ? 1.5f : 1f;
        return new MutantShot(p.level(), p, new ItemStack(Items.AMETHYST_SHARD), dmg * mult * Powers.power(d), 1, 0.01f, null);
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case SHARD_VOLLEY -> {
                for (int i = 0; i < 6; i++) {
                    MutantShot s = shard(p, d, 4f);
                    s.shootFromRotation(p, p.getXRot() + (p.getRandom().nextFloat() - 0.5f) * 6, p.getYRot() + (i - 2.5f) * 6f, 0, 2.0f, 0.5f);
                    level.addFreshEntity(s);
                }
                Powers.sound(p, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5f, 1.2f);
                return true;
            }
            case CRYSTAL_SPIKES -> {
                for (LivingEntity e : Kit.foes(p, 4)) {
                    Powers.hurt(p, e, 6f * Powers.power(d));
                    Powers.impulse(e, new Vec3(0, 0.7, 0), false);
                }
                for (int i = 0; i < 16; i++) {
                    double ang = i * Math.PI / 8;
                    BlockPos q = BlockPos.containing(p.getX() + Math.cos(ang) * 3, p.getY(), p.getZ() + Math.sin(ang) * 3);
                    if (level.getBlockState(q).isAir() && level.getBlockState(q.below()).isFaceSturdy(level, q.below(), Direction.UP)) {
                        Powers.placeTemporary(level, q, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP), 60);
                    }
                }
                Powers.sound(p, SoundEvents.AMETHYST_BLOCK_BREAK, 1.5f, 0.8f);
                return true;
            }
            case FACET_SHIELD -> {
                Powers.setTimer(p, "facet_shield", 80);
                Powers.sound(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.8f);
                return true;
            }
            case CRYSTAL_BRIDGE -> {
                LuminaryPower.bridge(p, 12, Blocks.STAINED_GLASS.purple().defaultBlockState(), 400);
                Powers.sound(p, SoundEvents.AMETHYST_BLOCK_PLACE, 1.5f, 1f);
                return true;
            }
            case SHATTER_NOVA -> {
                for (LivingEntity e : Kit.foes(p, 5)) {
                    Powers.hurt(p, e, 14f * Powers.power(d));
                    Powers.knockAway(e, p.position(), 1.0, 0.4);
                }
                Powers.setTimer(p, "shattered", 200);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()), p.getX(), p.getY() + 1, p.getZ(), 120, 1.5, 1, 1.5, 0.3);
                Powers.sound(p, SoundEvents.GLASS_BREAK, 2f, 0.6f);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 c = p.position();
        for (LivingEntity e : Kit.foes(p, 6)) Powers.knockAway(e, c, 1.6, 0.4);
        var glass = Blocks.STAINED_GLASS.magenta().defaultBlockState();
        for (BlockPos q : BlockPos.betweenClosed(BlockPos.containing(c).offset(-6, -1, -6), BlockPos.containing(c).offset(6, 6, 6))) {
            double dist = Math.sqrt(q.distToCenterSqr(c.x, c.y, c.z));
            if (dist >= 5.5 && dist < 6.5 && q.getY() >= (int) Math.floor(c.y)) Powers.placeTemporary(level, q.immutable(), glass, 300);
        }
        Zones.add(level, c, 6, 300, p.getUUID(), z -> {
            ServerPlayer o = z.ownerPlayer();
            if (o == null || z.age() % 40 != 0) return;
            int n = 0;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(16),
                    e -> e.isAlive() && e.distanceToSqr(c) > 49 && Powers.isHostile(e) && !Minions.isMinion(e))) {
                if (n++ >= 4) break;
                Vec3 from = c.add(0, 3, 0);
                Powers.line(level, from, e.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 0.4);
                Powers.hurt(o, e, 5f * Powers.power(d));
            }
        }, null);
        Powers.sound(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.5f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (p.getRandom().nextFloat() >= 0.25f) return;
        for (LivingEntity o : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3), x -> x != p && x != target && x.isAlive())) Powers.hurt(p, o, 3f);
        ((ServerLevel) p.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()), target.getX(), target.getY() + 1, target.getZ(), 30, 0.6, 0.6, 0.6, 0.2);
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.add(p, Attributes.ARMOR, "crystal_skin", 0);
    }
}
