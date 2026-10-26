package com.mutationsmod.mutations.ability;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.power.Absorber;
import com.mutationsmod.mutations.ability.power.Chameleon;
import com.mutationsmod.mutations.ability.power.Colossus;
import com.mutationsmod.mutations.ability.power.Phaser;
import com.mutationsmod.mutations.ability.power.Stormcaller;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.entity.ThrownChakram;
import com.mutationsmod.mutations.entity.ThrownRiftKnife;
import com.mutationsmod.mutations.item.EchoBowItem;
import com.mutationsmod.mutations.item.MutantWeapon;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.registry.ModWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** The twelve mutation weapons: who may hold them, their right-click powers, and their on-hit effects. */
public final class Weapons {
    private Weapons() {}

    public static boolean canWield(ServerPlayer p, MutationData d, Item item) {
        Mutation m = ModWeapons.owner(item);
        return m == null || p.isCreative() || d.has(m);
    }

    private static boolean powered(ServerPlayer p, MutationData d) {
        return !Suppression.active(d, p.level());
    }

    // ------------------------------------------------------------------ holding

    /** Every tick: anyone without the matching mutation can't keep the weapon in hand; held weapons adjust reach/speed. */
    public static void tick(ServerPlayer p, MutationData d) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = p.getItemInHand(hand);
            Mutation m = ModWeapons.owner(stack.getItem());
            if (m == null || canWield(p, d, stack.getItem())) continue;
            p.setItemInHand(hand, ItemStack.EMPTY);
            if (!stowAway(p, stack)) p.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
            p.sendOverlayMessage(Component.literal("Only a " + m.displayName() + " can wield the " + stack.getHoverName().getString() + ".")
                    .withStyle(ChatFormatting.RED));
        }
        Item main = p.getMainHandItem().getItem();
        boolean on = powered(p, d);
        MutantWeapon.Kind kind = main instanceof MutantWeapon w && d.has(w.mutation()) ? w.kind() : null;
        double reach = !on ? 0 : kind == MutantWeapon.Kind.HALBERD ? 1.5 : kind == MutantWeapon.Kind.WHIP ? 2.5 : 0;
        Powers.add(p, Attributes.ENTITY_INTERACTION_RANGE, "weapon_reach", reach);
        double speed = on && kind == MutantWeapon.Kind.DAGGERS ? 0.12 * Phaser.tier(d) : 0;
        Powers.mult(p, Attributes.ATTACK_SPEED, "blur_daggers", speed);
    }

    /** Moves a stack into the main inventory (not the hotbar). False if there's no room. */
    private static boolean stowAway(ServerPlayer p, ItemStack stack) {
        var inv = p.getInventory();
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ right-click

    public static boolean use(ServerPlayer p, ItemStack stack, InteractionHand hand, MutantWeapon w) {
        MutationData d = ModAttachments.get(p);
        if (!powered(p, d)) {
            p.sendOverlayMessage(Component.literal("Your powers are suppressed!").withStyle(ChatFormatting.GREEN));
            return false;
        }
        ServerLevel level = (ServerLevel) p.level();
        float power = Powers.power(d);
        switch (w.kind()) {
            case CHAKRAM -> {
                ThrownChakram c = new ThrownChakram(level, p, stack.copyWithCount(1));
                c.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 1.4f, 0);
                level.addFreshEntity(c);
                if (!p.isCreative()) stack.shrink(1);
                Powers.sound(p, SoundEvents.TRIDENT_THROW.value(), 1.0f, 1.3f);
                return true;
            }
            case RIFT_KNIFE -> {
                ThrownRiftKnife k = new ThrownRiftKnife(level, p, stack.copyWithCount(1));
                k.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 2.0f, 0.2f);
                level.addFreshEntity(k);
                if (!p.isCreative()) stack.shrink(1);
                p.getCooldowns().addCooldown(stack, 40);
                Powers.sound(p, SoundEvents.ENDER_PEARL_THROW, 1.0f, 1.2f);
                return true;
            }
            case HALBERD -> {
                Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
                for (int i = 2; i <= 10; i++) {
                    Vec3 at = p.position().add(dir.scale(i));
                    BlockPos pos = BlockPos.containing(at);
                    for (int dy = 1; dy >= -2; dy--) {                     // follow the ground
                        BlockPos q = pos.offset(0, dy, 0);
                        if (level.getBlockState(q).canBeReplaced() && !level.getBlockState(q.below()).canBeReplaced()) {
                            Powers.placeTemporary(level, q, Blocks.PACKED_ICE.defaultBlockState(), 60);
                            if (i % 2 == 0) Powers.placeTemporary(level, q.above(), Blocks.ICE.defaultBlockState(), 60);
                            level.sendParticles(ParticleTypes.SNOWFLAKE, q.getX() + 0.5, q.getY() + 1, q.getZ() + 0.5, 6, 0.3, 0.5, 0.3, 0.05);
                            break;
                        }
                    }
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(0.8, 1.5, 0.8),
                            e -> e != p && e.isAlive())) {
                        Powers.hurt(p, e, 6f * power);
                        e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
                        e.setTicksFrozen(Math.max(e.getTicksFrozen(), 160));
                        Powers.impulse(e, new Vec3(0, 0.5, 0), false);
                    }
                }
                p.getCooldowns().addCooldown(stack, 80);
                Powers.sound(p, SoundEvents.GLASS_PLACE, 1.2f, 0.6f);
                return true;
            }
            case CAPACITOR -> {
                if (!Absorber.spend(p, 10f)) return false;
                Vec3 eye = p.getEyePosition();
                Vec3 end = Raycast.target(p, 24);
                Powers.line(level, eye.add(p.getLookAngle()), end, ParticleTypes.END_ROD, 0.4);
                Vec3 step = end.subtract(eye).normalize();
                double len = end.distanceTo(eye);
                java.util.Set<LivingEntity> hit = new java.util.HashSet<>();
                for (double t = 1; t <= len; t += 0.5) {
                    Vec3 at = eye.add(step.scale(t));
                    hit.addAll(level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(0.6), e -> e != p && e.isAlive()));
                }
                for (LivingEntity e : hit) Powers.hurt(p, e, 10f * power);
                p.getCooldowns().addCooldown(stack, 20);
                Powers.sound(p, SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.8f);
                return true;
            }
            case SCYTHE -> {
                for (LivingEntity e : Powers.cone(p, 6, 0.4)) {
                    Powers.hurt(p, e, 5f * power);
                    e.igniteForSeconds(6);
                }
                Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
                for (int i = -6; i <= 6; i++) {
                    double a = Math.atan2(look.z, look.x) + i * 0.13;
                    for (double r = 1.5; r <= 5.5; r += 1) {
                        level.sendParticles(ParticleTypes.FLAME, p.getX() + Math.cos(a) * r, p.getY() + 1.0, p.getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0.01);
                    }
                }
                p.getCooldowns().addCooldown(stack, 60);
                Powers.sound(p, SoundEvents.BLAZE_SHOOT, 1.0f, 0.9f);
                return true;
            }
            case GUNBLADE -> {
                if (!com.mutationsmod.mutations.ability.power.Bullet.gunbladeShot(p, d)) return false;
                p.getCooldowns().addCooldown(stack, 20);
                return true;
            }
            case GLAIVE -> {
                Vec3 look = p.getLookAngle();
                Powers.impulse(p, look.scale(1.6).add(0, 0.25, 0), false);
                level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 12, 0.4, 0.2, 0.4, 0.05);
                p.getCooldowns().addCooldown(stack, 40);
                Powers.sound(p, SoundEvents.ENDER_DRAGON_FLAP, 0.7f, 1.4f);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------ on hit

    /** A mutant hitting something in melee with their weapon. */
    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        ItemStack stack = event.getSource().getWeaponItem();
        if (stack == null || !(stack.getItem() instanceof MutantWeapon w) || !d.has(w.mutation())) return;
        ServerLevel level = (ServerLevel) p.level();
        float power = Powers.power(d);
        switch (w.kind()) {
            case BLOODTHORN -> {
                float ratio = p.getHealth() < p.getMaxHealth() * 0.4f ? 0.35f : 0.15f;
                p.heal(event.getAmount() * ratio);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1, target.getZ(), 4, 0.2, 0.3, 0.2, 0.1);
            }
            case MAUL -> {
                if (Stormcaller.inRain(p)) event.setAmount(event.getAmount() * 1.25f);
                if (!Powers.timerActive(d, level, "maul_bolt")) {
                    Powers.setTimer(p, "maul_bolt", 40);
                    LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                    if (bolt != null) {
                        bolt.snapTo(target.getX(), target.getY(), target.getZ());
                        bolt.setCause(p);
                        level.addFreshEntity(bolt);
                    }
                }
            }
            case HALBERD -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), 120));
            }
            case EARTHBREAKER -> {
                if (!Colossus.steel(d)) break;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3), e -> e != p && e != target && e.isAlive())) {
                    Powers.hurt(p, e, event.getAmount() * 0.5f);
                    Powers.knockAway(e, target.position(), 0.8, 0.4);
                }
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                        level.getBlockState(target.blockPosition().below())), target.getX(), target.getY(), target.getZ(), 30, 1.5, 0.1, 1.5, 0.2);
                Powers.sound(p, SoundEvents.ANVIL_LAND, 0.5f, 0.6f);
            }
            case CAPACITOR -> Absorber.addCharge(p, 1f);
            case SHADE_FANG -> {
                Vec3 toAttacker = p.position().subtract(target.position()).multiply(1, 0, 1).normalize();
                boolean behind = target.getLookAngle().multiply(1, 0, 1).normalize().dot(toAttacker) < -0.3;
                boolean hidden = p.isInvisible() || Chameleon.camouflaged(p, d) || Powers.timerActive(d, level, "smoke");
                if (behind || hidden) {
                    event.setAmount(event.getAmount() * 2.5f);
                    level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
                }
            }
            case SCYTHE -> target.igniteForSeconds(5);
            case GLAIVE -> {
                if (!p.onGround()) event.setAmount(event.getAmount() * 1.5f);
            }
            case WHIP -> {
                Vec3 pull = p.position().subtract(target.position());
                if (pull.length() > 2) Powers.impulse(target, pull.normalize().scale(0.9).add(0, 0.25, 0), false);
                long fed = d.timer("host_fed");
                if (fed > 0) Powers.setTimer(p, "host_fed", Math.min(0, fed + 600 - level.getGameTime()));
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ Echo Bow

    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (!(proj.level() instanceof ServerLevel level) || !proj.entityTags().contains(EchoBowItem.TAG)) return;
        if (!(proj.getOwner() instanceof ServerPlayer p)) return;
        MutationData d = ModAttachments.get(p);
        if (!d.has(Mutation.SIREN) || !powered(p, d)) return;
        proj.removeTag(EchoBowItem.TAG);
        HitResult hit = event.getRayTraceResult();
        Vec3 at = hit.getLocation();
        LivingEntity direct = hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity le ? le : null;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(3.5),
                e -> e != p && e != direct && e.isAlive())) {
            Powers.hurt(p, e, 5f * Powers.power(d));
            e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
            Powers.knockAway(e, at, 0.5, 0.2);
        }
        if (direct != null) direct.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
        level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_BOOM, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.5f);
    }
}
