package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.entity.MutantShot;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Venom: poison spit, acid and immunity to poison. */
public class VenomPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.VENOM; }

    static int dur(ServerPlayer p, int ticks) {
        return Catalysts.holding(p, Mutation.VENOM) ? ticks * 2 : ticks;
    }

    static void poison(ServerPlayer p, LivingEntity e, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(MobEffects.POISON, dur(p, ticks), amp));
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        if (now % 20 != 0) return;
        boolean cold = Powers.temperature(p) < 0.15f || p.level().getBlockState(p.blockPosition().below()).is(BlockTags.SNOW)
                || p.level().getBlockState(p.blockPosition()).is(Blocks.SNOW);
        if (cold && !d.has(Secondary.WARM_SCALES)) {
            Powers.effect(p, MobEffects.SLOWNESS, 40, 0);
            Powers.effect(p, MobEffects.WEAKNESS, 40, 0);
        }
        if (d.has(Secondary.LETHAL_DOSE) && now % 40 == 0) {
            for (LivingEntity e : Kit.foes(p, 16)) if (e.hasEffect(MobEffects.POISON) && e.getHealth() <= 1.5f) Powers.hurt(p, e, 2f);
        }
    }

    @Override
    public boolean blocksEffect(ServerPlayer p, MutationData d, Holder<MobEffect> effect) {
        return effect.equals(MobEffects.POISON);
    }

    @Override
    public void onMelee(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (!target.hasEffect(MobEffects.POISON)) poison(p, target, 80, 0);
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (!(e.getSource().getEntity() instanceof LivingEntity att) || att == p) return;
        if (d.has(Secondary.NEUROTOXIN) && att.hasEffect(MobEffects.POISON)) e.setAmount(e.getAmount() * 0.7f);
        if (d.has(Secondary.TOXIC_BLOOD) && e.getSource().getDirectEntity() == att) poison(p, att, 60, 0);
    }

    @Override
    public void onKill(ServerPlayer p, MutationData d, LivingEntity victim) {
        if (d.has(Secondary.VENOM_FEAST) && victim.hasEffect(MobEffects.POISON)) {
            p.getFoodData().setFoodLevel(Math.min(20, p.getFoodData().getFoodLevel() + 2));
        }
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case VENOM_SPIT -> {
                MutantShot s = new MutantShot(level, p, new ItemStack(Items.SLIME_BALL), 3f * Powers.power(d), 0, 0.03f, (shot, t) -> {
                    poison(p, t, 120, 1);
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 0));
                });
                s.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 1.6f, 0.5f);
                level.addFreshEntity(s);
                Powers.sound(p, SoundEvents.LLAMA_SPIT, 1f, 0.8f);
                return true;
            }
            case ACID_POOL -> {
                BlockHitResult hit = Kit.lookBlock(p, 16);
                if (hit.getType() != HitResult.Type.BLOCK) return false;
                BlockPos ground = hit.getBlockPos();
                Vec3 c = Vec3.atBottomCenterOf(ground.above());
                if (d.has(Secondary.CORROSIVE)) {
                    for (BlockPos q : BlockPos.betweenClosed(ground.offset(-1, 0, -1), ground.offset(1, 0, 1))) {
                        var s = level.getBlockState(q);
                        if (s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)) Powers.freezeTemporary(level, q.immutable(), Blocks.AIR.defaultBlockState(), 160);
                    }
                }
                Zones.add(level, c, 1.6, 160, p.getUUID(), z -> {
                    if (z.age() % 4 == 0) level.sendParticles(ParticleTypes.ITEM_SLIME, z.center.x, z.center.y + 0.1, z.center.z, 4, 1, 0.05, 1, 0);
                    if (z.age() % 20 != 0) return;
                    ServerPlayer o = z.ownerPlayer();
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(1.6, 1.2, 1.6), e -> e != o && e.isAlive())) {
                        if (o != null) Powers.hurt(o, e, 2f);
                        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                            ItemStack armor = e.getItemBySlot(slot);
                            if (armor.isDamageableItem()) armor.hurtAndBreak(4, e, slot);
                        }
                    }
                }, null);
                Powers.sound(p, SoundEvents.BUCKET_EMPTY, 1f, 0.6f);
                return true;
            }
            case SHED_SKIN -> {
                for (MobEffectInstance inst : new ArrayList<>(p.getActiveEffects())) {
                    if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(inst.getEffect());
                }
                p.heal(6f);
                Powers.particles(p, ParticleTypes.ITEM_SLIME, 30, 0.5, 0.05);
                Powers.sound(p, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1f, 0.6f);
                return true;
            }
            case PARALYZING_BITE -> {
                LivingEntity t = Kit.look(p, 8);
                if (t == null) return false;
                Kit.root(t, 60);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 4));
                poison(p, t, 60, 0);
                return true;
            }
            case SERPENT_LUNGE -> {
                Powers.impulse(p, p.getLookAngle().scale(1.6).add(0, 0.2, 0), true);
                Powers.startRush(p, 12, 6f * Powers.power(d), 0.5);
                Powers.later(level, 6, () -> {
                    for (LivingEntity e : Kit.foes(p, 2.5)) poison(p, e, 120, 1);
                });
                Powers.sound(p, SoundEvents.FOX_BITE, 1f, 0.8f);
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        Zones.add(level, p.position(), 10, 200, p.getUUID(), z -> {
            ServerPlayer o = z.ownerPlayer();
            if (o == null) return;
            z.center = o.position();
            if (z.age() % 4 == 0) level.sendParticles(ParticleTypes.SNEEZE, z.center.x, z.center.y + 1, z.center.z, 30, 6, 1.5, 6, 0.01);
            if (z.age() % 20 != 0) return;
            for (LivingEntity e : Kit.foes(o, 10)) {
                poison(o, e, 60, 2);
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            }
        }, null);
        Powers.sound(p, SoundEvents.WITCH_THROW, 1.5f, 0.5f);
        return true;
    }

    @Override
    public void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        MobEffectInstance cur = target.getEffect(MobEffects.POISON);
        int amp = cur == null ? 0 : Math.min(3, cur.getAmplifier() + 1);
        poison(p, target, 100, amp);
    }
}
