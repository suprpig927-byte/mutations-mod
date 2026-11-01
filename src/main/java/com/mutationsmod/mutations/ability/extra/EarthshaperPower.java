package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.entity.MutantShot;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Earthshaper: raise pillars and walls, burrow, sense footsteps. Weaker off the ground. */
public class EarthshaperPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.EARTHSHAPER; }

    static float grounded(ServerPlayer p) {
        return Kit.onGroundish(p) ? 1f : 0.5f;
    }

    static boolean geode(ServerPlayer p) { return Catalysts.holding(p, Mutation.EARTHSHAPER); }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        BlockState under = p.level().getBlockState(p.blockPosition().below());
        Powers.add(p, Attributes.ARMOR, "stoneskin", d.has(Secondary.STONESKIN) && under.is(BlockTags.BASE_STONE_OVERWORLD) ? 3 : 0);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "sure_footing", d.has(Secondary.SURE_FOOTING) && p.onGround() ? 1 : 0);
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (!Kit.onGroundish(p)) e.setAmount(e.getAmount() * 1.25f);
    }

    /** A column of temporary stone rising under {@code base}, a moment after the launch. */
    static void pillar(ServerPlayer p, BlockPos base, int height, int ticks) {
        ServerLevel level = (ServerLevel) p.level();
        Powers.later(level, 3, () -> {
            for (int h = 0; h < height; h++) {
                BlockPos q = base.above(h);
                if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(q), LivingEntity::isAlive).isEmpty()) break;
                Powers.placeTemporary(level, q, Blocks.STONE.defaultBlockState(), ticks);
            }
        });
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), base.getX() + 0.5, base.getY() + 0.5, base.getZ() + 0.5, 30, 0.5, 0.5, 0.5, 0.1);
    }

    static void launch(ServerPlayer p, MutationData d, LivingEntity t, float dmg) {
        boolean g = geode(p);
        Powers.impulse(t, new Vec3(0, g ? 1.6 : 1.25, 0), false);
        Powers.hurt(p, t, dmg * Powers.power(d) * grounded(p));
        pillar(p, t.blockPosition(), g ? 4 : 3, g ? 400 : 200);
        if (d.has(Secondary.RUBBLE)) {
            for (LivingEntity e : t.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(3), e -> e != p && e != t && e.isAlive())) Powers.hurt(p, e, 3f);
        }
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case STONE_PILLAR -> {
                LivingEntity t = Kit.look(p, 24);
                if (t == null) return false;
                launch(p, d, t, 6f);
                Powers.sound(p, SoundEvents.STONE_BREAK, 1.5f, 0.6f);
                return true;
            }
            case EARTH_WALL -> {
                boolean g = geode(p);
                VerdantPower.wall(p, Blocks.STONE.defaultBlockState(), g ? 7 : 5, g ? 4 : 3, 2, g ? 800 : 400);
                if (d.has(Secondary.LANDSLIDE)) {
                    for (LivingEntity e : Powers.cone(p, 5, 0.4)) Powers.knockAway(e, p.position(), 1.2, 0.4);
                }
                Powers.sound(p, SoundEvents.STONE_PLACE, 1.5f, 0.6f);
                return true;
            }
            case BURROW -> {
                Vec3 flat = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z).normalize();
                Vec3 from = p.position();
                for (int dist = 12; dist >= 3; dist--) {
                    Vec3 at = from.add(flat.scale(dist));
                    BlockPos top = BlockPos.containing(at.x, at.y + 6, at.z);
                    for (int i = 0; i < 16 && level.getBlockState(top.below()).getCollisionShape(level, top.below()).isEmpty(); i++) top = top.below();
                    Vec3 spot = Powers.safeSpot(p, new Vec3(at.x, top.getY(), at.z));
                    if (spot == null) continue;
                    BlockState dirt = level.getBlockState(p.blockPosition().below());
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, dirt.isAir() ? Blocks.DIRT.defaultBlockState() : dirt), from.x, from.y, from.z, 40, 0.5, 0.3, 0.5, 0.1);
                    if (d.has(Secondary.SANDSTORM)) {
                        for (LivingEntity e : Kit.foes(p, 4)) e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, from.x, from.y + 1, from.z, 20, 1.5, 0.8, 1.5, 0.01);
                    }
                    Powers.teleport(p, spot);
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), spot.x, spot.y, spot.z, 40, 0.5, 0.3, 0.5, 0.1);
                    Powers.sound(p, SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.6f);
                    return true;
                }
                p.sendOverlayMessage(Component.literal("Nowhere to surface.").withStyle(ChatFormatting.GRAY));
                return false;
            }
            case FISSURE -> {
                Vec3 flat = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z).normalize();
                java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
                for (int i = 1; i <= 10; i++) {
                    Vec3 at = p.position().add(flat.scale(i));
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(BlockPos.containing(at).below())), at.x, at.y + 0.1, at.z, 8, 0.3, 0.05, 0.3, 0.1);
                    hit.addAll(level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.2, 1, 1.2), e -> e != p && e.isAlive()));
                }
                for (LivingEntity e : hit) { Kit.root(e, 60); Powers.hurt(p, e, 4f * Powers.power(d) * grounded(p)); }
                Powers.sound(p, SoundEvents.DEEPSLATE_BREAK, 1.5f, 0.5f);
                return true;
            }
            case BOULDER_TOSS -> {
                MutantShot s = new MutantShot(level, p, new ItemStack(Items.COBBLESTONE), 10f * Powers.power(d) * grounded(p), 0, 0.05f,
                        (shot, t) -> Powers.knockAway(t, shot.position(), 1.2, 0.4));
                s.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 1.4f, 0.5f);
                level.addFreshEntity(s);
                Powers.sound(p, SoundEvents.STONE_BREAK, 1.5f, 0.5f);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        for (LivingEntity e : Kit.foes(p, 16)) launch(p, d, e, 12f);
        for (int i = 0; i < 24; i++) {
            double ang = i * Math.PI / 12;
            for (int r = 2; r <= 14; r += 3) {
                Vec3 at = p.position().add(Math.cos(ang) * r, 0.1, Math.sin(ang) * r);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(BlockPos.containing(at).below())), at.x, at.y, at.z, 4, 0.2, 0.05, 0.2, 0.1);
            }
        }
        Powers.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.5f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        for (LivingEntity o : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3), x -> x != p && x != target && x.isAlive())) {
            Powers.hurt(p, o, e.getAmount() * 0.5f);
        }
        ((ServerLevel) p.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), target.getX(), target.getY(), target.getZ(), 20, 1.5, 0.1, 1.5, 0.1);
    }

    /** Mines stone, dirt and sand 50% faster. */
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        if (!(e.getEntity() instanceof net.minecraft.world.entity.player.Player p)) return;
        MutationData d = p.getData(com.mutationsmod.mutations.ModAttachments.DATA);
        if (!d.has(Mutation.EARTHSHAPER) || d.timer("suppressed") > p.level().getGameTime()) return;
        BlockState s = e.getState();
        if (s.is(BlockTags.MINEABLE_WITH_PICKAXE) || s.is(BlockTags.MINEABLE_WITH_SHOVEL)) e.setNewSpeed(e.getNewSpeed() * 1.5f);
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.add(p, Attributes.ARMOR, "stoneskin", 0);
        Powers.add(p, Attributes.KNOCKBACK_RESISTANCE, "sure_footing", 0);
    }
}
