package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.entity.MutantShot;
import com.mutationsmod.mutations.item.Catalysts;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Magnetar: pull, push and deflect with magnetism. */
public class MagnetarPower implements ExtraPower {
    @Override public Mutation mutation() { return Mutation.MAGNETAR; }

    static double range(ServerPlayer p, double base) {
        return Catalysts.holding(p, Mutation.MAGNETAR) ? base * 2 : base;
    }

    @Override
    public void tick(ServerPlayer p, MutationData d, long now) {
        ServerLevel level = (ServerLevel) p.level();
        double r = range(p, 10);
        AABB box = p.getBoundingBox().inflate(r);
        for (ItemEntity it : level.getEntitiesOfClass(ItemEntity.class, box, i -> !i.hasPickUpDelay())) {
            it.setDeltaMovement(p.position().add(0, 0.5, 0).subtract(it.position()).normalize().scale(0.4));
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
            orb.setDeltaMovement(p.position().add(0, 0.5, 0).subtract(orb.position()).normalize().scale(0.4));
        }
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "magnetar_heavy", -0.05 * Kit.metalArmorPieces(p));
        if (d.timer("maglift") > 0 && !p.isShiftKeyDown()) {
            int gap = -1;
            for (int i = 1; i <= 4 && gap < 0; i++) {
                var st = level.getBlockState(p.blockPosition().below(i));
                if (st.is(net.minecraft.world.level.block.Blocks.IRON_BLOCK) || st.is(net.minecraft.tags.BlockTags.RAILS) || st.is(net.minecraft.tags.BlockTags.ANVIL)
                        || st.is(net.minecraft.world.level.block.Blocks.IRON_BARS) || st.is(net.minecraft.world.level.block.Blocks.IRON_TRAPDOOR)
                        || st.is(net.minecraft.world.level.block.Blocks.IRON_DOOR) || st.is(net.minecraft.world.level.block.Blocks.HEAVY_CORE)) gap = i;
                else if (!st.isAir()) break;
            }
            if (gap > 0) {
                p.resetFallDistance();
                Powers.effect(p, gap <= 2 ? MobEffects.LEVITATION : MobEffects.SLOW_FALLING, 6, 0);
                if (now % 4 == 0) level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY(), p.getZ(), 2, 0.3, 0.05, 0.3, 0);
            }
        }
        boolean bare = p.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && p.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && p.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && p.getItemBySlot(EquipmentSlot.FEET).isEmpty();
        Powers.add(p, Attributes.ARMOR, "iron_skin", d.has(Secondary.IRON_SKIN) && bare ? 4 : 0);
        if (level.isThundering() && now % 600 == 0 && p.getRandom().nextFloat() < 0.3f && level.canSeeSky(p.blockPosition())) {
            LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) { bolt.snapTo(p.getX(), p.getY(), p.getZ()); level.addFreshEntity(bolt); }
        }
        if (d.has(Secondary.RAILRIDER) && p.getVehicle() instanceof AbstractMinecart cart) {
            Vec3 v = cart.getDeltaMovement();
            if (v.horizontalDistance() > 0.05 && v.horizontalDistance() < 1.2) cart.setDeltaMovement(v.multiply(1.08, 1, 1.08));
        }
        if (Powers.timerActive(d, level, "metal_shell") && now % 3 == 0) {
            double a = now * 0.4;
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState()),
                    p.getX() + Math.cos(a) * 1.2, p.getY() + 1, p.getZ() + Math.sin(a) * 1.2, 2, 0.05, 0.3, 0.05, 0);
        }
    }

    @Override
    public void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {
        if (e.getSource().getDirectEntity() instanceof Projectile proj && p.getRandom().nextFloat() < 0.5f) {
            e.setCanceled(true);
            Vec3 v = proj.getDeltaMovement();
            proj.setDeltaMovement(d.has(Secondary.BULLET_CATCHER) ? new Vec3(0, -0.1, 0) : new Vec3(-v.z, v.y, v.x).scale(0.6));
            proj.needsSync = true;
            Powers.sound(p, SoundEvents.CHAIN_HIT, 0.7f, 1.6f);
            return;
        }
        boolean melee = e.getSource().getDirectEntity() == e.getSource().getEntity() && e.getSource().getEntity() instanceof LivingEntity;
        if (melee && Powers.timerActive(d, p.level(), "metal_shell")) {
            e.setCanceled(true);
            Powers.sound(p, SoundEvents.ANVIL_LAND, 0.4f, 1.6f);
            return;
        }
        if (d.has(Secondary.DISARMING_FIELD) && melee && e.getSource().getEntity() instanceof Mob m && p.getRandom().nextFloat() < 0.2f) {
            ItemStack held = m.getMainHandItem();
            if (!held.isEmpty()) {
                m.spawnAtLocation((ServerLevel) p.level(), held.copy());
                m.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void onMelee(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.FERROUS_FIST) && Kit.wearsMetal(target)) e.setAmount(e.getAmount() * 1.5f);
    }

    @Override
    public boolean use(ServerPlayer p, MutationData d, ActiveAbility a) {
        ServerLevel level = (ServerLevel) p.level();
        switch (a) {
            case POLARITY_PULL -> {
                LivingEntity t = Kit.look(p, range(p, 20));
                if (t == null) return false;
                yank(p, t, Kit.wearsMetal(t) ? 2.0 : 1.2);
                return true;
            }
            case REPULSE -> {
                for (LivingEntity e : Kit.foes(p, 8)) Powers.knockAway(e, p.position(), 1.8, 0.5);
                for (Projectile proj : level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(8), pr -> pr.getOwner() != p)) {
                    proj.setDeltaMovement(proj.position().subtract(p.position()).normalize().scale(1.5));
                    proj.needsSync = true;
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 60, 2, 1, 2, 0.3);
                Powers.sound(p, SoundEvents.BEACON_DEACTIVATE, 1f, 0.6f);
                return true;
            }
            case MAG_LIFT -> {
                boolean on = d.timer("maglift") <= 0;
                Powers.setTimer(p, "maglift", 0);
                com.mutationsmod.mutations.ModAttachments.set(p, com.mutationsmod.mutations.ModAttachments.get(p).withTimer("maglift", on ? 1 : 0));
                p.sendOverlayMessage(Component.literal(on ? "Mag Lift ON: hover over iron, rails and anvils" : "Mag Lift off").withStyle(ChatFormatting.RED));
                return true;
            }
            case SHRAPNEL_BURST -> {
                int have = p.isCreative() ? 8 : Math.min(8, p.getInventory().countItem(Items.IRON_NUGGET));
                if (have <= 0) { p.sendOverlayMessage(Component.literal("You need iron nuggets.").withStyle(ChatFormatting.GRAY)); return false; }
                if (!p.isCreative()) p.getInventory().clearOrCountMatchingItems(s -> s.is(Items.IRON_NUGGET), have, p.inventoryMenu.getCraftSlots());
                for (int i = 0; i < have; i++) {
                    double ang = i * Math.PI * 2 / have;
                    MutantShot s = new MutantShot(level, p, new ItemStack(Items.IRON_NUGGET), 5f * Powers.power(d), 0, 0.02f, null);
                    s.setPos(p.getX(), p.getY() + 1.2, p.getZ());
                    s.shoot(Math.cos(ang), 0.05, Math.sin(ang), 1.6f, 0f);
                    level.addFreshEntity(s);
                }
                Powers.sound(p, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.6f);
                return true;
            }
            case METAL_SHELL -> {
                Powers.setTimer(p, "metal_shell", 120);
                Powers.sound(p, SoundEvents.ARMOR_EQUIP_IRON.value(), 1f, 0.7f);
                return true;
            }
            default -> { return false; }
        }
    }

    static void yank(ServerPlayer p, LivingEntity t, double strength) {
        Vec3 dir = p.position().subtract(t.position());
        Powers.impulse(t, dir.normalize().scale(Math.min(strength, dir.length() * 0.25 + 0.4)).add(0, 0.35, 0), true);
        Powers.line((ServerLevel) p.level(), p.getEyePosition(), t.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 0.4);
        Powers.sound(p, SoundEvents.CHAIN_PLACE, 1f, 0.6f);
    }

    @Override
    public boolean ultimate(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        Vec3 point = p.getEyePosition().add(p.getLookAngle().scale(6));
        Zones.add(level, point, 24, 100, p.getUUID(), z -> {
            ServerPlayer o = z.ownerPlayer();
            if (o == null) return;
            long age = z.age();
            var mobs = level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(24),
                    e -> e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player) && !Minions.isMinion(e));
            if (age < 60) {
                for (LivingEntity e : mobs) {
                    Vec3 to = z.center.subtract(e.position());
                    if (to.length() > 1) Powers.impulse(e, to.normalize().scale(Math.min(0.8, to.length() * 0.2)), true);
                }
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, z.center.x, z.center.y, z.center.z, 10, 1, 1, 1, 0.3);
            } else if (age == 60) {
                for (LivingEntity e : mobs) if (e.distanceToSqr(z.center) < 16) Powers.hurt(o, e, 15f * Powers.power(d));
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, z.center.x, z.center.y, z.center.z, 1, 0, 0, 0, 0);
                Powers.sound(o, SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
            } else if (age == 62) {
                for (LivingEntity e : mobs) if (e.distanceToSqr(z.center) < 36) Powers.knockAway(e, z.center, 2.2, 0.8);
            }
        }, null);
        Powers.sound(p, SoundEvents.BEACON_POWER_SELECT, 1.5f, 0.6f);
        return true;
    }

    @Override
    public boolean weaponUse(ServerPlayer p, MutationData d, ItemStack stack) {
        LivingEntity t = Kit.look(p, 16);
        if (t == null) return false;
        yank(p, t, 1.6);
        p.getCooldowns().addCooldown(stack, 60);
        return true;
    }

    @Override
    public void reset(ServerPlayer p) {
        Powers.mult(p, Attributes.MOVEMENT_SPEED, "magnetar_heavy", 0);
        Powers.add(p, Attributes.ARMOR, "iron_skin", 0);
    }
}
