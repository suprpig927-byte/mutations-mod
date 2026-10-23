package com.mutationsmod.mutations.ability;

import com.mutationsmod.mutations.ModAttachments;
import com.mutationsmod.mutations.ability.power.Absorber;
import com.mutationsmod.mutations.ability.power.Blinker;
import com.mutationsmod.mutations.ability.power.Chameleon;
import com.mutationsmod.mutations.ability.power.Colossus;
import com.mutationsmod.mutations.ability.power.Cryo;
import com.mutationsmod.mutations.ability.power.Host;
import com.mutationsmod.mutations.ability.power.KineticMind;
import com.mutationsmod.mutations.ability.power.Phaser;
import com.mutationsmod.mutations.ability.power.Pyro;
import com.mutationsmod.mutations.ability.power.Regenerator;
import com.mutationsmod.mutations.ability.power.Siren;
import com.mutationsmod.mutations.ability.power.Stormcaller;
import com.mutationsmod.mutations.data.MutationData;
import com.mutationsmod.mutations.mastery.Mastery;
import com.mutationsmod.mutations.mastery.StatEffects;
import com.mutationsmod.mutations.mutation.ActiveAbility;
import com.mutationsmod.mutations.mutation.Mutation;
import com.mutationsmod.mutations.mutation.Secondary;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The event hub: routes game events to each mutation's code, and handles ability key presses. */
public final class AbilityHandler {
    private AbilityHandler() {}

    // ------------------------------------------------------------------ joining

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MutationData data = ModAttachments.get(player);
        if (data.rolled()) {
            ModAttachments.set(player, data); // re-send so the client definitely has it
            return;
        }
        Mutation[] pool = Mutation.values();
        Mutation rolled = pool[player.getRandom().nextInt(pool.length)];
        ModAttachments.set(player, data.withMutations(List.of(rolled.id())).withRolled(true));
        announceNewMutation(player, rolled);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ModAttachments.set(player, ModAttachments.get(player));
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Telekinesis.release(player, false);
            ModAttachments.set(player, ModAttachments.get(player));
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Telekinesis.release(p, false);
            forget(p);
        }
    }

    public static void announceNewMutation(ServerPlayer player, Mutation m) {
        player.sendSystemMessage(Component.literal("Your mutation has awakened: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(m.displayName()).withStyle(s -> s.withColor(m.color() & 0xFFFFFF).withBold(true))));
        player.sendSystemMessage(Component.literal("Press M to open your mutation screen.").withStyle(ChatFormatting.DARK_GRAY));
        Powers.sound(player, SoundEvents.TOTEM_USE, 0.6f, 1.2f);
        Powers.particles(player, ParticleTypes.TOTEM_OF_UNDYING, 60, 0.6, 0.4);
    }

    // ------------------------------------------------------------------ ticking

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        MutationData d = ModAttachments.get(p);
        if (p.level().getGameTime() % 10 == 0) StatEffects.apply(p, d);
        if (d.mutations().isEmpty()) return;
        Powers.tickRush(p);

        if (d.has(Mutation.REGENERATOR)) Regenerator.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.KINETIC_MIND)) KineticMind.tick(p, ModAttachments.get(p));
        else Telekinesis.tick(p, d);
        if (d.has(Mutation.STORMCALLER)) Stormcaller.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.PHASER)) Phaser.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.CRYO)) Cryo.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.BLINKER)) Blinker.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.COLOSSUS)) Colossus.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.SIREN)) Siren.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.ABSORBER)) Absorber.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.CHAMELEON)) Chameleon.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.PYRO)) Pyro.tick(p, ModAttachments.get(p));
        if (d.has(Mutation.HOST)) Host.tick(p, ModAttachments.get(p));
    }

    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Telekinesis.onLevelTick(event);
        Powers.tickLevel(level);
        Siren.tickLevel(level);
    }

    // ------------------------------------------------------------------ damage

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        Siren.onMobHurt(victim);
        var source = event.getSource();

        // Offense: a mutant hitting something in melee.
        if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker
                && !Powers.ABILITY_DAMAGE.get() && attacker != victim) {
            MutationData a = ModAttachments.get(attacker);
            if (a.has(Mutation.STORMCALLER)) Stormcaller.onMelee(event, attacker, a, victim);
            if (a.has(Mutation.PHASER)) Phaser.onMelee(event, attacker, a, victim);
            if (a.has(Mutation.CRYO)) Cryo.onMelee(event, attacker, a, victim);
            if (a.has(Mutation.COLOSSUS)) Colossus.onMelee(event, attacker, a, victim);
            if (a.has(Mutation.CHAMELEON)) Chameleon.onMelee(event, attacker, a, victim);
            if (a.has(Mutation.PYRO)) Pyro.onMelee(event, attacker, a, victim);
            if (a.has(Mutation.ABSORBER)) Absorber.onMelee(event, attacker, a, victim);
            if (a.has(Mutation.HOST)) Host.onMelee(event, attacker, a, victim);
        }

        // Defense: a mutant getting hurt.
        if (!(victim instanceof ServerPlayer p) || Powers.SELF_DAMAGE.get()) return;
        MutationData d = ModAttachments.get(p);
        if (d.mutations().isEmpty()) return;
        if (d.has(Mutation.REGENERATOR) && Powers.timerActive(d, p.level(), "immune")) {
            event.setCanceled(true);
            return;
        }
        if (d.has(Mutation.STORMCALLER)) Stormcaller.onHurt(event, p, d);
        if (event.isCanceled()) return;
        if (d.has(Mutation.PYRO)) Pyro.onHurt(event, p, d);
        if (event.isCanceled()) return;
        if (d.has(Mutation.CRYO)) Cryo.onHurt(event, p, d);
        if (event.isCanceled()) return;
        if (d.has(Mutation.SIREN)) Siren.onHurt(event, p, d);
        if (event.isCanceled()) return;
        if (d.has(Mutation.KINETIC_MIND)) KineticMind.onHurt(event, p, d);
        if (event.isCanceled()) return;
        if (d.has(Mutation.PHASER)) Phaser.onHurt(event, p, d);
        if (event.isCanceled()) return;
        if (d.has(Mutation.REGENERATOR)) Regenerator.onHurt(event, p, d);
        if (event.isCanceled()) return;
        if (d.has(Mutation.CHAMELEON)) Chameleon.onHurt(event, p, d);
        if (event.isCanceled()) return;
        if (d.has(Mutation.HOST)) Host.onHurt(event, p, d);
        if (d.has(Mutation.ABSORBER)) Absorber.onHurt(event, p, d);
        if (event.isCanceled()) return;
        if (d.has(Mutation.BLINKER)) Blinker.onHurt(event, p, d);
    }

    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide()) return;
        MutationData d = ModAttachments.get(p);
        if (d.has(Mutation.REGENERATOR)) Regenerator.onEffect(event, p, d);
        if (d.has(Mutation.SIREN)) Siren.onEffect(event, d);
    }

    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            MutationData d = ModAttachments.get(p);
            if (d.has(Mutation.STORMCALLER)) Stormcaller.onStruck(event, p, d);
        }
    }

    // ------------------------------------------------------------------ death & kills

    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        if (victim instanceof ServerPlayer p) {
            MutationData d = ModAttachments.get(p);
            if (d.has(Mutation.REGENERATOR) && Regenerator.secondWind(event, p, d)) return;
            if (d.has(Mutation.PYRO) && Pyro.phoenix(event, p, d)) return;
            Telekinesis.release(p, false);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer killer && killer != victim) {
            MutationData k = ModAttachments.get(killer);
            Mastery.addXp(killer, Mastery.xpForKill(victim.getMaxHealth(),
                    victim.getType().getCategory() == MobCategory.MONSTER, victim instanceof Player));
            if (k.has(Mutation.HOST)) Host.onKill(killer, k);
            if (k.has(Mutation.CHAMELEON)) Chameleon.onKill(killer, victim);
        }
    }

    public static void onDrops(LivingDropsEvent event) {
        if (!event.getEntity().level().isClientSide()) Chameleon.onDrops(event);
    }

    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!event.getEntity().level().isClientSide()) Chameleon.onChangeTarget(event);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        MutationData d = ModAttachments.get(p);
        if (d.has(Mutation.PYRO)) Pyro.onRightClickItem(event, p, d);
    }

    // ------------------------------------------------------------------ active abilities

    /** Called when the client presses "Ability N". */
    public static void useSlot(ServerPlayer p, int slot) {
        if (!p.isAlive() || p.isSpectator()) return;
        MutationData d = ModAttachments.get(p);
        List<ActiveAbility> actives = d.actives();
        if (slot < 0 || slot >= actives.size()) return;
        ActiveAbility ability = actives.get(slot);
        long now = p.level().getGameTime();

        // Toggles that ignore the cooldown.
        if (ability == ActiveAbility.FORCE_PUSH && Telekinesis.isHolding(p)) {
            Telekinesis.release(p, true);
            Telekinesis.addStrain(p);
            return;
        }
        if (ability == ActiveAbility.LEVITATE && KineticMind.isLevitating(p, d)) {
            KineticMind.stopLevitating(p);
            return;
        }
        boolean echo = ability == ActiveAbility.BLINK && d.has(Secondary.ECHO_BLINK) && Powers.timerActive(d, p.level(), "echo_blink");

        long readyAt = d.readyAt(ability);
        if (now < readyAt && !echo) {
            long secs = (readyAt - now + 19) / 20;
            p.sendOverlayMessage(Component.literal(ability.displayName() + " is recharging (" + secs + "s)").withStyle(ChatFormatting.RED));
            return;
        }

        boolean used = switch (ability) {
            case SURGE -> Regenerator.surge(p, d);
            case IMMUNITY -> Regenerator.immunity(p, d);
            case TRANSFUSION -> Regenerator.transfusion(p, d);
            case TELEKINESIS -> Telekinesis.toggle(p, d);
            case FORCE_PUSH -> KineticMind.forcePush(p, d);
            case LEVITATE -> KineticMind.levitate(p, d);
            case DISARM -> KineticMind.disarm(p, d);
            case PSYCHIC_BARRIER -> KineticMind.barrier(p, d);
            case LIGHTNING_STRIKE -> Stormcaller.lightningStrike(p, d);
            case SUMMON_STORM -> Stormcaller.summonStorm(p, d);
            case THUNDERCLAP -> Stormcaller.thunderclap(p, d);
            case PHASE_DASH -> Phaser.phaseDash(p, d);
            case SPEED_PUNCH -> Phaser.speedPunch(p, d);
            case VIBRATE -> Phaser.vibrate(p, d);
            case PHASE_THROUGH -> Phaser.phaseThrough(p, d);
            case BLITZ -> Phaser.blitz(p, d);
            case ICE_BLAST -> Cryo.iceBlast(p, d);
            case ICE_WALL -> Cryo.iceWall(p, d);
            case BLINK -> Blinker.blink(p, d);
            case DISPLACE -> Blinker.displace(p, d);
            case RECALL -> Blinker.recall(p, d);
            case SWAP -> Blinker.swap(p, d);
            case GROUND_POUND -> Colossus.groundPound(p, d);
            case TITAN_LEAP -> Colossus.titanLeap(p, d);
            case CHARGED_LEAP -> Absorber.chargedLeap(p, d);
            case ITEM_MAGNET -> KineticMind.toggleMagnet(p, d);
            case CHARGE -> Colossus.charge(p, d);
            case SONIC_SCREAM -> Siren.sonicScream(p, d);
            case LULLABY -> Siren.lullaby(p, d);
            case RELEASE -> Absorber.release(p, d);
            case FOCUSED_BEAM -> Absorber.focusedBeam(p, d);
            case MIMIC -> Chameleon.mimic(p, d);
            case COPYCAT -> Chameleon.copycat(p, d);
            case SMOKE_BURST -> Chameleon.smokeBurst(p, d);
            case FLAMETHROWER -> Pyro.flamethrower(p, d);
            case FLAME_FLIGHT -> Pyro.flameFlight(p, d);
            case FLARE -> Pyro.flare(p, d);
            case TENDRIL -> Host.tendril(p, d);
            case BONDED_ARMOR -> Host.bondedArmor(p, d);
            case LASH -> Host.lash(p, d);
        };
        if (!used) return;

        if (echo) {
            Powers.clearTimer(p, "echo_blink"); // the free echo doesn't trigger a cooldown
        } else {
            ModAttachments.set(p, ModAttachments.get(p).withCooldown(ability, now + cooldown(ability, d)));
            if (ability == ActiveAbility.BLINK && d.has(Secondary.ECHO_BLINK)) Powers.setTimer(p, "echo_blink", 40);
        }
        if (ability == ActiveAbility.TELEKINESIS || ability == ActiveAbility.ITEM_MAGNET
                || ability == ActiveAbility.TITAN_LEAP || ability == ActiveAbility.CHARGED_LEAP) {
            Mastery.addXp(p, 1); // cheap, spammable abilities give less XP
        } else {
            Mastery.addXp(p, Mastery.XP_PER_ABILITY_USE);
        }
    }

    /** Effective cooldown in ticks, after the Mutation stat and secondary mutations. */
    public static long cooldown(ActiveAbility a, MutationData d) {
        float ticks = a.cooldownTicks() * Powers.cooldownFactor(d);
        if (a == ActiveAbility.BLINK && d.has(Secondary.SHORT_FUSE)) ticks *= 0.5f;
        if (a == ActiveAbility.SUMMON_STORM && d.has(Secondary.SUPERCELL)) ticks *= 0.5f;
        if (a == ActiveAbility.TENDRIL && d.has(Secondary.HUNGRY_TENDRILS)) ticks *= 0.5f;
        return Math.max(1, Math.round(ticks));
    }

    public static void changeSpeedTier(ServerPlayer p, int delta) {
        if (ModAttachments.get(p).has(Mutation.PHASER)) Phaser.changeSpeedTier(p, delta);
    }

    // ------------------------------------------------------------------ cleanup

    private static final String[] MODIFIERS = {"storm_rain_speed", "phaser_speed", "quick_hands", "thick_hide", "glacier_skin",
            "colossus_damage", "colossus_knockback", "colossus_slow", "colossus_mining", "deep_lungs", "titans_reach", "bulwark",
            "host_reach", "host_fed"};

    /** Called by commands so a removed or changed mutation doesn't leave buffs behind. */
    public static void resetState(ServerPlayer p) {
        Telekinesis.release(p, false);
        for (String id : MODIFIERS) {
            for (var attr : List.of(Attributes.MOVEMENT_SPEED, Attributes.ATTACK_SPEED, Attributes.ARMOR, Attributes.ATTACK_DAMAGE,
                    Attributes.KNOCKBACK_RESISTANCE, Attributes.BLOCK_BREAK_SPEED, Attributes.OXYGEN_BONUS, Attributes.ENTITY_INTERACTION_RANGE)) {
                Powers.add(p, attr, id, 0);
            }
        }
        Stormcaller.setFlight(p, false);
        p.setForcedPose(null);
        forget(p);
    }

    private static void forget(ServerPlayer p) {
        Powers.forget(p.getUUID());
        Regenerator.forget(p.getUUID());
        Phaser.forget(p.getUUID());
        Blinker.forget(p.getUUID());
        Chameleon.forget(p.getUUID());
        Pyro.forget(p);
    }

    /** Kept for older callers. */
    public static void impulse(LivingEntity entity, net.minecraft.world.phys.Vec3 velocity, boolean replace) {
        Powers.impulse(entity, velocity, replace);
    }

    static boolean flightAllowed(ServerPlayer p) {
        return p.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT) > 0;
    }
}
