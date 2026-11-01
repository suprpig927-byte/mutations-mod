package com.mutationsmod.mutations.ability.extra;

import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * One of the v0.19 mutations. Every hook has a no-op default; each mutation overrides what it needs.
 * Hooks only run while the player's powers aren't suppressed.
 */
public interface ExtraPower {
    Mutation mutation();

    /** Every tick. */
    default void tick(ServerPlayer p, MutationData d, long now) {}

    /** The player is about to take damage. */
    default void onHurt(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d) {}

    /** The player hits something in melee (not ability damage). */
    default void onMelee(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {}

    /** Any damage the player deals, melee or ability (runs before onMelee). */
    default void onDealDamage(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {}

    /** The player killed something. */
    default void onKill(ServerPlayer p, MutationData d, LivingEntity victim) {}

    /** A mob died near or because of the player: its drops (for loot effects). */
    default void onDrops(LivingDropsEvent e, ServerPlayer p, MutationData d) {}

    /** The player would die. Return true to cancel it. */
    default boolean cheatDeath(LivingDeathEvent e, ServerPlayer p, MutationData d) { return false; }

    /** The player died for real. */
    default void onDeath(ServerPlayer p, MutationData d) {}

    /** Multiplier for this player's ability cooldowns. */
    default float cooldownFactor(ServerPlayer p, MutationData d, ActiveAbility a) { return 1f; }

    /** If non-null, the player can't use abilities right now and is told why. */
    default String blocked(ServerPlayer p, MutationData d) { return null; }

    /** An active ability. Return true if it was used (starts the cooldown). */
    default boolean use(ServerPlayer p, MutationData d, ActiveAbility a) { return false; }

    /** The Omega ultimate. Return true if it was used. */
    boolean ultimate(ServerPlayer p, MutationData d);

    /** Right-click with this mutation's weapon. */
    default boolean weaponUse(ServerPlayer p, MutationData d, ItemStack stack) { return false; }

    /** A melee hit with this mutation's weapon. */
    default void weaponHit(LivingIncomingDamageEvent e, ServerPlayer p, MutationData d, LivingEntity target) {}

    /** True if this mob should ignore (not target) the player right now. */
    default boolean ignoredBy(ServerPlayer p, MutationData d, net.minecraft.world.entity.Mob mob) { return false; }

    /** True to block an effect from being applied to the player. */
    default boolean blocksEffect(ServerPlayer p, MutationData d, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) { return false; }

    /** After the player used any active ability. */
    default void afterAbility(ServerPlayer p, MutationData d) {}

    /** Cleanup on logout / mutation change. */
    default void reset(ServerPlayer p) {}
}
