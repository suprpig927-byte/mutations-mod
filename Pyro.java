package com.mutationsmod.mutations.ability.power;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.Powers;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class Pyro {
    private Pyro() {}

    /** Flame On toggle: every Pyro power and drawback (except Phoenix) only works while it's on. */
    public static boolean lit(MutationData d) {
        return d.timer("flame_on") > 0;
    }

    public static boolean toggleFlame(ServerPlayer p, MutationData d) {
        boolean on = !lit(d);
        ModAttachments.set(p, ModAttachments.get(p).withTimer("flame_on", on ? 1 : 0));
        if (on) {
            Powers.particles(p, ParticleTypes.FLAME, 40, 0.5, 0.08);
            Powers.sound(p, SoundEvents.BLAZE_SHOOT, 1.0f, 0.7f);
            p.sendOverlayMessage(Component.literal("FLAME ON").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else {
            Powers.clearTimer(p, "flamethrower");
            endFlight(p);
            Powers.particles(p, ParticleTypes.SMOKE, 25, 0.4, 0.03);
            Powers.sound(p, SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.0f);
            p.sendOverlayMessage(Component.literal("Your flames die down.").withStyle(ChatFormatting.GRAY));
        }
        return true;
    }

    private static void endFlight(ServerPlayer p) {
        MutationData d = ModAttachments.get(p);
        if (d.timer("flame_flight") == 0) return;
        Powers.clearTimer(p, "flame_flight");
        if (!(d.has(Mutation.STORMCALLER) && d.has(Secondary.RAINWALKER) && Stormcaller.inRain(p))) Stormcaller.setFlight(p, false);
    }

    /** Flamethrower, Flame Flight and Flare need Flame On. */
    private static boolean needFlame(ServerPlayer p, MutationData d) {
        if (lit(d)) return true;
        p.sendOverlayMessage(Component.literal("You need Flame On.").withStyle(ChatFormatting.GRAY));
        return false;
    }

    private static final java.util.Map<java.util.UUID, BlockPos> LAST_STEP = new java.util.HashMap<>();

    public static void tick(ServerPlayer p, MutationData d) {
        ServerLevel level = (ServerLevel) p.level();
        long now = level.getGameTime();
        if (!lit(d)) {
            endFlight(p);
            return;
        }
        if (p.isOnFire()) p.clearFire();

        boolean wet = p.isInWaterOrRain();
        if (wet) {
            // Steam hisses off you.
            if (now % 4 == 0) level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.2, p.getZ(), 2, 0.3, 0.5, 0.3, 0.01);
            if (now % 20 == 0) {
                Powers.hurtSelf(p, 1f);
                level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 8, 0.3, 0.5, 0.3, 0.03);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRE_EXTINGUISH,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.2f + p.getRandom().nextFloat() * 0.4f);
            }
        } else {
            // Embers drift off you, and you scorch the grass where you step.
            if (now % 3 == 0) {
                level.sendParticles(ParticleTypes.SMALL_FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 1, 0.3, 0.6, 0.3, 0.01);
            }
            if (now % 25 == 0) level.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 1.2, p.getZ(), 1, 0.2, 0.3, 0.2, 0);
            BlockPos step = p.blockPosition();
            if (p.onGround() && !step.equals(LAST_STEP.put(p.getUUID(), step))) {
                BlockPos below = step.below();
                if (level.getBlockState(below).is(Blocks.GRASS_BLOCK)) {
                    Powers.freezeTemporary(level, below, Blocks.COARSE_DIRT.defaultBlockState(), 8 * 20);
                }
                level.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.05, p.getZ(), 3, 0.2, 0.02, 0.2, 0.01);
            }
        }

        // Flamethrower stream.
        if (Powers.timerActive(d, level, "flamethrower") && now % 2 == 0) {
            double range = d.has(Secondary.WILDFIRE) ? 9 : 6;
            for (LivingEntity e : Powers.cone(p, range, 0.75)) {
                e.igniteForSeconds(5f);
                Powers.hurt(p, e, 1.5f * Powers.power(d));
            }
            heatFurnace(level, p, range);
            Vec3 look = p.getLookAngle();
            Vec3 eye = p.getEyePosition().subtract(0, 0.3, 0);
            for (int i = 0; i < 6; i++) {
                double dist = 1 + p.getRandom().nextDouble() * range;
                Vec3 at = eye.add(look.scale(dist));
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, dist * 0.06, dist * 0.06, dist * 0.06, 0.02);
            }
        }

        // Flame Flight.
        boolean flying = Powers.timerActive(d, level, "flame_flight");
        if (flying) {
            Powers.add(p, net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT, "mutation_flight", 1);
            // Jets of fire blasting down from your fists and feet.
            double yaw = Math.toRadians(p.yBodyRot);
            double sideX = Math.cos(yaw), sideZ = Math.sin(yaw);
            for (int s = -1; s <= 1; s += 2) {
                double hx = p.getX() + sideX * 0.38 * s, hz = p.getZ() + sideZ * 0.38 * s;
                double fx = p.getX() + sideX * 0.12 * s, fz = p.getZ() + sideZ * 0.12 * s;
                level.sendParticles(ParticleTypes.FLAME, hx, p.getY() + 0.6, hz, 0, 0, -1, 0, 0.18);
                level.sendParticles(ParticleTypes.FLAME, fx, p.getY() - 0.05, fz, 0, 0, -1, 0, 0.22);
            }
            if (now % 4 == 0) level.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() - 0.4, p.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
        } else {
            endFlight(p); // just ended
        }

        if (d.has(Secondary.KINDLING) && now % 40 == 0 && nearFire(level, p.blockPosition())) {
            p.heal(2f);
            Powers.particles(p, ParticleTypes.FLAME, 6, 0.4, 0.02);
        }
        if (d.has(Secondary.ASHWALKER) && now % 20 == 0) {
            BlockState under = level.getBlockState(p.blockPosition().below());
            BlockState in = level.getBlockState(p.blockPosition());
            if (under.is(Blocks.SOUL_SAND) || under.is(Blocks.SOUL_SOIL) || under.is(Blocks.MAGMA_BLOCK) || in.is(Blocks.SOUL_SAND)) {
                Powers.effect(p, MobEffects.SPEED, 40, 1);
            }
        }
    }

    /** Ticks of burn time the flamethrower keeps a furnace topped up to (30 seconds). */
    private static final int FURNACE_FUEL = 600;

    /** Flamethrower aimed at a furnace, smoker or blast furnace lights it and cooks fast. */
    private static void heatFurnace(ServerLevel level, ServerPlayer p, double range) {
        var hit = com.mutationsmod.mutations.ability.Raycast.block(p, range);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) return;
        try {
            int lit = ObfuscationReflectionHelper.getPrivateValue(AbstractFurnaceBlockEntity.class, furnace, "litTimeRemaining");
            if (lit < FURNACE_FUEL) {
                ObfuscationReflectionHelper.setPrivateValue(AbstractFurnaceBlockEntity.class, furnace, FURNACE_FUEL, "litTimeRemaining");
                ObfuscationReflectionHelper.setPrivateValue(AbstractFurnaceBlockEntity.class, furnace, FURNACE_FUEL, "litTotalTime");
            }
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
                state = state.setValue(BlockStateProperties.LIT, true);
                level.setBlock(pos, state, 3);
            }
            // Pyro flames burn hot: two extra cooking ticks every time.
            AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
            AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
            furnace.setChanged();
        } catch (RuntimeException ignored) {
            // If the furnace internals ever change, the flamethrower just won't cook.
        }
    }

    private static boolean nearFire(ServerLevel level, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -2, -3), center.offset(3, 2, 3))) {
            BlockState s = level.getBlockState(pos);
            if (s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.LAVA) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.MAGMA_BLOCK)) return true;
        }
        return false;
    }

    public static void onHurt(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d) {
        var source = event.getSource();
        if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.LAVA)) {
            event.setCanceled(true);
            return;
        }
        if (d.has(Secondary.MAGMA_SKIN) && source.getDirectEntity() instanceof LivingEntity attacker && attacker == source.getEntity()) {
            attacker.igniteForSeconds(4f);
        }
    }

    public static void onMelee(LivingIncomingDamageEvent event, ServerPlayer p, MutationData d, LivingEntity target) {
        if (d.has(Secondary.EMBER_STRIKES)) target.igniteForSeconds(4f);
    }

    /** Lava drinking and Hearthbound coal eating. */
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event, ServerPlayer p, MutationData d) {
        ItemStack stack = event.getItemStack();
        if (!lit(d)) return;
        if (stack.is(Items.LAVA_BUCKET) && p.isShiftKeyDown()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (Powers.timerActive(d, p.level(), "lava_drink")) {
                p.sendOverlayMessage(Component.literal("You're still digesting the last bucket.").withStyle(ChatFormatting.GRAY));
                return;
            }
            Powers.setTimer(p, "lava_drink", 15 * 20);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0));
            if (!p.isCreative()) p.setItemInHand(event.getHand(), new ItemStack(Items.BUCKET));
            Powers.particles(p, ParticleTypes.FLAME, 20, 0.4, 0.05);
            Powers.sound(p, SoundEvents.BUCKET_FILL_LAVA, 1.0f, 0.8f);
            return;
        }
        if (d.has(Secondary.HEARTHBOUND) && (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) && p.getFoodData().getFoodLevel() < 20) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            p.getFoodData().setFoodLevel(Math.min(20, p.getFoodData().getFoodLevel() + 3));
            if (!p.isCreative()) stack.shrink(1);
            Powers.particles(p, ParticleTypes.SMOKE, 8, 0.3, 0.02);
            Powers.sound(p, SoundEvents.PLAYER_BURP, 0.6f, 1.0f);
        }
    }

    /** Phoenix: returns true if the death was prevented. */
    public static boolean phoenix(LivingDeathEvent event, ServerPlayer p, MutationData d) {
        if (!d.has(Secondary.PHOENIX) || Powers.timerActive(d, p.level(), "phoenix")) return false;
        event.setCanceled(true);
        p.setHealth(p.getMaxHealth() * 0.5f);
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        for (LivingEntity e : Powers.around(p, 5)) {
            Powers.hurt(p, e, 8f * Powers.power(d));
            e.igniteForSeconds(6f);
            Powers.knockAway(e, p.position(), 1.2, 0.5);
        }
        Powers.setTimer(p, "phoenix", 20 * 60 * 20);
        ModAttachments.set(p, ModAttachments.get(p).withTimer("flame_on", 1)); // reborn in flames
        Powers.particles(p, ParticleTypes.FLAME, 120, 1.5, 0.3);
        Powers.sound(p, SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);
        p.sendSystemMessage(Component.literal("You rise from the ashes!").withStyle(ChatFormatting.GOLD));
        return true;
    }

    // ------------------------------------------------------------------ actives

    public static boolean flamethrower(ServerPlayer p, MutationData d) {
        if (!needFlame(p, d)) return false;
        Powers.setTimer(p, "flamethrower", 80);
        Powers.sound(p, SoundEvents.BLAZE_SHOOT, 1.0f, 0.8f);
        return true;
    }

    public static boolean flameFlight(ServerPlayer p, MutationData d) {
        if (!needFlame(p, d)) return false;
        if (p.isInWaterOrRain()) {
            p.sendOverlayMessage(Component.literal("Your flames sputter in the water.").withStyle(ChatFormatting.GRAY));
            return false;
        }
        Powers.setTimer(p, "flame_flight", (long) (10 * 20 * Powers.power(d)));
        Powers.add(p, net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT, "mutation_flight", 1);
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
        }
        Powers.impulse(p, new Vec3(0, 0.6, 0), false);
        Powers.sound(p, SoundEvents.BLAZE_SHOOT, 1.0f, 1.2f);
        return true;
    }

    public static boolean flare(ServerPlayer p, MutationData d) {
        if (!needFlame(p, d)) return false;
        for (LivingEntity e : Powers.around(p, 10)) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) (100 * Powers.power(d)), 0));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            if (e instanceof Mob mob) mob.setTarget(null);
        }
        Powers.particles(p, ParticleTypes.END_ROD, 60, 2.5, 0.4);
        Powers.particles(p, ParticleTypes.FLAME, 40, 2.0, 0.2);
        Powers.sound(p, SoundEvents.BLAZE_SHOOT, 1.0f, 1.6f);
        return true;
    }

    public static void forget(ServerPlayer p) {
        if (ModAttachments.get(p).timer("flame_flight") != 0) Powers.clearTimer(p, "flame_flight");
    }
}
