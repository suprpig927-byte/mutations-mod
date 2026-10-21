package com.mutationsmod.mutations.ability;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mastery.Mastery;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.network.ModPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** All server-side mutation gameplay: passives, downsides, actives, and mastery XP from kills. */
public final class AbilityHandler {
    private AbilityHandler() {}

    private static final Identifier PHASER_SPEED = Identifier.fromNamespaceAndPath(MutationsMod.MODID, "phaser_speed");
    private static final Identifier STORM_SPEED = Identifier.fromNamespaceAndPath(MutationsMod.MODID, "storm_rain_speed");

    /** Short-lived, not-saved state: invulnerability windows (game time they end). */
    private static final Map<UUID, Long> IMMUNE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> DASH_UNTIL = new HashMap<>();

    // ------------------------------------------------------------------ first spawn

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MutationData data = ModAttachments.get(player);
        if (data.rolled()) {
            // Re-send so the client definitely has it after joining.
            ModAttachments.set(player, data);
            return;
        }
        List<Mutation> pool = Mutation.implementedValues();
        Mutation rolled = pool.get(player.getRandom().nextInt(pool.size()));
        ModAttachments.set(player, data.withMutations(List.of(rolled.id())).withRolled(true));
        announceNewMutation(player, rolled);
    }

    /** Respawning or changing dimension creates a fresh client-side player, so re-send the data. */
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ModAttachments.set(player, ModAttachments.get(player));
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Telekinesis.release(player, false);
            ModAttachments.set(player, ModAttachments.get(player));
        }
    }

    public static void announceNewMutation(ServerPlayer player, Mutation m) {
        player.sendSystemMessage(Component.literal("Your mutation has awakened: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(m.displayName()).withStyle(s -> s.withColor(m.color() & 0xFFFFFF).withBold(true))));
        player.sendSystemMessage(Component.literal("Press M to open your mutation screen.").withStyle(ChatFormatting.DARK_GRAY));
        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.2f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 60, 0.4, 0.8, 0.4, 0.4);
    }

    // ------------------------------------------------------------------ ticking passives

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MutationData data = ModAttachments.get(player);
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        // Regenerator: fast regen, poison immunity, faster hunger.
        if (data.has(Mutation.REGENERATOR)) {
            int interval = Math.max(10, 40 - data.level() / 2);
            if (now % interval == 0 && player.getHealth() < player.getMaxHealth() && player.getFoodData().getFoodLevel() > 0) {
                player.heal(1.0f);
                player.causeFoodExhaustion(1.5f);
            }
            player.causeFoodExhaustion(0.02f);
            if (player.hasEffect(MobEffects.POISON)) player.removeEffect(MobEffects.POISON);
        }
        Long immune = IMMUNE_UNTIL.get(player.getUUID());
        if (immune != null && now < immune && now % 4 == 0) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 4, 0.4, 0.6, 0.4, 0.05);
        }

        // Kinetic Mind: keep any held block floating in front of the player.
        Telekinesis.tick(player, data);

        // Stormcaller: faster in the rain.
        boolean inRain = data.has(Mutation.STORMCALLER) && level.isRainingAt(player.blockPosition().above());
        setModifier(player, Attributes.MOVEMENT_SPEED, STORM_SPEED, inRain ? 0.2 : 0.0);

        // Phaser: base speed + speed tier, no chest slot, high tiers burn hunger.
        if (data.has(Mutation.PHASER)) {
            int tier = Math.min(data.speedTier(), maxSpeedTier(data));
            setModifier(player, Attributes.MOVEMENT_SPEED, PHASER_SPEED, 0.15 + 0.35 * tier);
            if (tier >= 2) player.causeFoodExhaustion(0.006f * tier);
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!chest.isEmpty()) {
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                if (!player.getInventory().add(chest)) player.drop(chest, false, Prediction.SERVER_ONLY);
                player.sendOverlayMessage(Component.literal("Your phasing body can't hold a chestplate.").withStyle(ChatFormatting.YELLOW));
            }
        } else {
            setModifier(player, Attributes.MOVEMENT_SPEED, PHASER_SPEED, 0.0);
        }
    }

    public static int maxSpeedTier(MutationData data) {
        return Math.min(5, 1 + data.level() / 8);
    }

    /** Adds, updates or removes (amount 0) a temporary "multiply base" attribute modifier. */
    private static void setModifier(Player player, Holder<Attribute> attribute, Identifier id, double amount) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && existing.amount() == amount) return;
        if (existing != null) inst.removeModifier(id);
        if (amount != 0) inst.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    // ------------------------------------------------------------------ damage & effects

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MutationData data = ModAttachments.get(player);
        DamageSource source = event.getSource();
        long now = player.level().getGameTime();

        if (now < IMMUNE_UNTIL.getOrDefault(player.getUUID(), 0L) || now < DASH_UNTIL.getOrDefault(player.getUUID(), 0L)) {
            event.setCanceled(true);
            return;
        }
        if (data.has(Mutation.STORMCALLER) && source.is(DamageTypes.LIGHTNING_BOLT)) {
            event.setCanceled(true);
            player.clearFire();
            return;
        }
        if (data.has(Mutation.PHASER) && source.is(DamageTypeTags.IS_PROJECTILE)) {
            event.setAmount(event.getAmount() * 0.6f);
        }
    }

    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player && event.getEffectInstance().is(MobEffects.POISON)
                && ModAttachments.get(player).has(Mutation.REGENERATOR)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Mastery XP for kills. */
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != victim) {
            Mastery.addXp(killer, Mastery.xpForKill(victim.getMaxHealth(), victim.getType().getCategory() == MobCategory.MONSTER, victim instanceof Player));
        }
        if (victim instanceof ServerPlayer p) Telekinesis.release(p, false);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Telekinesis.release(p, false);
            IMMUNE_UNTIL.remove(p.getUUID());
            DASH_UNTIL.remove(p.getUUID());
        }
    }

    // ------------------------------------------------------------------ actives

    /** Called when the client presses "Ability N". */
    public static void useSlot(ServerPlayer player, int slot) {
        if (!player.isAlive() || player.isSpectator()) return;
        MutationData data = ModAttachments.get(player);
        List<ActiveAbility> actives = data.actives();
        if (slot < 0 || slot >= actives.size()) return;
        ActiveAbility ability = actives.get(slot);

        // Telekinesis is a toggle and has a tiny cooldown; a held block can always be thrown or dropped.
        if (ability == ActiveAbility.FORCE_PUSH && Telekinesis.isHolding(player)) {
            Telekinesis.release(player, true);
            Telekinesis.addStrain(player);
            return;
        }
        if (ability == ActiveAbility.TELEKINESIS && Telekinesis.isHolding(player)) {
            Telekinesis.release(player, false);
            return;
        }

        long now = player.level().getGameTime();
        long readyAt = data.readyAt(ability);
        if (now < readyAt) {
            long secs = (readyAt - now + 19) / 20;
            player.sendOverlayMessage(Component.literal(ability.displayName() + " is recharging (" + secs + "s)").withStyle(ChatFormatting.RED));
            return;
        }

        boolean used = switch (ability) {
            case SURGE -> surge(player, data);
            case IMMUNITY -> immunity(player);
            case TELEKINESIS -> Telekinesis.grab(player, data);
            case FORCE_PUSH -> forcePush(player, data);
            case LIGHTNING_STRIKE -> lightningStrike(player, data);
            case SUMMON_STORM -> summonStorm(player);
            case PHASE_DASH -> phaseDash(player);
        };
        if (!used) return;

        ModAttachments.set(player, ModAttachments.get(player).withCooldown(ability, now + ability.cooldownTicks()));
        Mastery.addXp(player, Mastery.XP_PER_ABILITY_USE);
    }

    private static boolean surge(ServerPlayer player, MutationData data) {
        var food = player.getFoodData();
        if (food.getFoodLevel() < 6) {
            player.sendOverlayMessage(Component.literal("Too hungry to Surge.").withStyle(ChatFormatting.RED));
            return false;
        }
        food.setFoodLevel(food.getFoodLevel() - 6);
        player.heal(8.0f + data.level() * 0.1f);
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1, player.getZ(), 10, 0.5, 0.6, 0.5, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);
        return true;
    }

    private static boolean immunity(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        IMMUNE_UNTIL.put(player.getUUID(), level.getGameTime() + 8 * 20);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
        player.sendOverlayMessage(Component.literal("IMMUNE").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return true;
    }

    private static boolean forcePush(ServerPlayer player, MutationData data) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        double range = 7.0;
        float damage = Math.min(10f, 3f + data.level() * 0.1f);
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> e != player && e.isAlive())) {
            Vec3 to = e.position().subtract(player.position());
            if (to.length() > range) continue;
            if (to.normalize().dot(look) < 0.5) continue; // only in front
            hit.add(e);
        }
        for (LivingEntity e : hit) {
            Vec3 push = look.multiply(1, 0, 1).normalize().scale(1.6).add(0, 0.45, 0);
            e.hurtServer(level, player.damageSources().playerAttack(player), damage);
            impulse(e, push, false);
        }
        level.sendParticles(ParticleTypes.CLOUD, player.getX() + look.x * 2, player.getEyeY() + look.y * 2, player.getZ() + look.z * 2, 25, 0.6, 0.6, 0.6, 0.15);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 0.7f);
        Telekinesis.addStrain(player);
        return true;
    }

    private static boolean lightningStrike(ServerPlayer player, MutationData data) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 target = Raycast.target(player, 48);
        float damage;
        if (level.isThundering()) damage = 10f;
        else if (level.isRaining()) damage = 7f;
        else damage = 3.5f; // weaker in clear weather
        damage += data.level() * 0.1f;

        var bolt = new net.minecraft.world.entity.LightningBolt(net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT, level);
        bolt.setPos(target);
        bolt.setCause(player);
        bolt.setDamage(damage);
        level.addFreshEntity(bolt);
        if (!level.isRaining()) {
            player.sendOverlayMessage(Component.literal("The clear sky weakens your lightning...").withStyle(ChatFormatting.GRAY));
        }
        return true;
    }

    private static boolean summonStorm(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        var server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "weather thunder 6000");
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 80, 1.0, 1.5, 1.0, 0.3);
        server.getPlayerList().broadcastSystemMessage(
                Component.literal(player.getName().getString() + " has summoned a storm!").withStyle(ChatFormatting.AQUA), false);
        return true;
    }

    private static boolean phaseDash(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        Vec3 dash = new Vec3(look.x * 2.2, Mth.clamp(look.y * 1.2, -0.4, 0.6), look.z * 2.2);
        DASH_UNTIL.put(player.getUUID(), level.getGameTime() + 12);
        impulse(player, dash, true);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.8f);
        return true;
    }

    /** Pushes an entity. Players are moved by their own client, so they get a packet instead. */
    public static void impulse(LivingEntity entity, Vec3 velocity, boolean replace) {
        if (entity instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new ModPayloads.Impulse(velocity.x, velocity.y, velocity.z, replace));
        } else {
            entity.setDeltaMovement(replace ? velocity : entity.getDeltaMovement().add(velocity));
        }
    }

    public static void changeSpeedTier(ServerPlayer player, int delta) {
        MutationData data = ModAttachments.get(player);
        if (!data.has(Mutation.PHASER)) return;
        int max = maxSpeedTier(data);
        int tier = Mth.clamp(data.speedTier() + Integer.signum(delta), 0, max);
        if (tier == data.speedTier()) {
            if (delta > 0) player.sendOverlayMessage(Component.literal("Max speed tier (" + max + ") - level up mastery to go faster")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        ModAttachments.set(player, data.withSpeedTier(tier));
        player.sendOverlayMessage(Component.literal("Speed tier " + tier + " / " + max).withStyle(ChatFormatting.YELLOW));
    }

    /** Called by commands so a cleared/changed mutation doesn't leave buffs behind. */
    public static void resetState(ServerPlayer player) {
        Telekinesis.release(player, false);
        setModifier(player, Attributes.MOVEMENT_SPEED, PHASER_SPEED, 0.0);
        setModifier(player, Attributes.MOVEMENT_SPEED, STORM_SPEED, 0.0);
        IMMUNE_UNTIL.remove(player.getUUID());
        DASH_UNTIL.remove(player.getUUID());
    }

    static MobEffectInstance nausea(int ticks) {
        return new MobEffectInstance(MobEffects.NAUSEA, ticks, 0);
    }
}
