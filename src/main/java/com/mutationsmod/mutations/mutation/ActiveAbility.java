package com.mutationsmod.mutations.mutation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Every active (keybind-triggered) ability, base and secondary. Cooldowns are in seconds. */
public enum ActiveAbility {
    // ---- base mutation actives ----
    SURGE("Surge", "Burst-heal 4 hearts. Costs 6 hunger.", 30),
    IMMUNITY("Immunity", "Completely immune to damage for 8 seconds.", 180),
    TELEKINESIS("Telekinesis", "Grab the block you're looking at. Press again to drop it.", 0.5),
    FORCE_PUSH("Force Push", "Blast mobs in front of you back. Throws held blocks.", 8),
    LIGHTNING_STRIKE("Lightning Strike", "Call lightning down where you're looking.", 6),
    SUMMON_STORM("Summon Storm", "Turn the weather into a thunderstorm.", 1200),
    PHASE_DASH("Phase Dash", "Dash forward, passing harmlessly through everything.", 6),
    ICE_BLAST("Ice Blast", "Freeze what you hit and encase it in packed ice.", 5),
    BLINK("Blink", "Teleport to where you're looking.", 4),
    DISPLACE("Displace", "Teleport the mob or player you're looking at somewhere nearby.", 15),
    GROUND_POUND("Ground Pound", "Slam the ground, launching everything around you.", 10),
    SONIC_SCREAM("Sonic Scream", "A scream that damages and disorients everything in front of you.", 8),
    RELEASE("Release", "Blast out all your stored charge around you.", 3),
    MIMIC("Mimic", "Hostile mobs ignore you for 8 seconds.", 45),
    COPYCAT("Copycat", "Borrow a trait from the last mob type you killed.", 90),
    FLAMETHROWER("Flamethrower", "Spray fire in front of you for 2 seconds.", 10),
    FLAME_FLIGHT("Flame Flight", "Fly on jets of fire for 10 seconds.", 45),
    TENDRIL("Tendril", "Yank a mob to you, or pull yourself to a block.", 4),
    BONDED_ARMOR("Bonded Armor", "The organism hardens over you: big damage resistance and strength.", 40),

    // ---- secondary mutation actives ----
    TRANSFUSION("Transfusion", "Drain health from the mob you're looking at.", 20),
    LEVITATE("Levitate", "Float upward for 15 seconds. Press again to stop.", 30),
    DISARM("Disarm", "Rip the held item out of a mob's or player's hands.", 15),
    PSYCHIC_BARRIER("Psychic Barrier", "A dome that repels mobs and projectiles for 6 seconds.", 40),
    THUNDERCLAP("Thunderclap", "Knock back everything around you with a thunderclap.", 15),
    BLITZ("Blitz", "For 10 seconds, running into mobs or players damages them.", 45),
    ICE_WALL("Ice Wall", "Raise a wall of packed ice in front of you.", 15),
    RECALL("Recall", "Blink back to where you were 5 seconds ago.", 20),
    SWAP("Swap", "Trade places with the mob or player you're looking at.", 15),
    CHARGE("Charge", "Rush forward, flinging mobs aside.", 12),
    LULLABY("Lullaby", "Nearby mobs fall asleep until hit (5 seconds).", 40),
    FOCUSED_BEAM("Focused Beam", "Fire all your charge as one long-range beam.", 3),
    SMOKE_BURST("Smoke Burst", "Blind nearby mobs and vanish.", 30),
    FLARE("Flare", "A blinding flash that dazzles everything nearby.", 20),
    LASH("Lash", "Whip every mob around you and drag them closer.", 10);

    private final String displayName;
    private final String description;
    private final int cooldownTicks;

    ActiveAbility(String displayName, String description, double cooldownSeconds) {
        this.displayName = displayName;
        this.description = description;
        this.cooldownTicks = (int) Math.round(cooldownSeconds * 20);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public static Optional<ActiveAbility> byId(String id) {
        return Arrays.stream(values()).filter(a -> a.id().equals(id)).findFirst();
    }
}
