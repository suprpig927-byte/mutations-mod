package com.mutationsmod.mutations.mutation;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Every active (keybind-triggered) ability. Cooldowns are in ticks (20 ticks = 1 second). */
public enum ActiveAbility {
    SURGE("Surge", "Burst-heal 4 hearts. Costs 6 hunger.", 30 * 20),
    IMMUNITY("Immunity", "Become completely immune to damage for 8 seconds.", 180 * 20),
    TELEKINESIS("Telekinesis", "Grab the block you're looking at. Press again to drop it, or use Force Push to throw it.", 10),
    FORCE_PUSH("Force Push", "Blast mobs in front of you backwards. Throws a held block.", 8 * 20),
    LIGHTNING_STRIKE("Lightning Strike", "Call lightning down where you're looking.", 6 * 20),
    SUMMON_STORM("Summon Storm", "Turn the weather into a thunderstorm.", 20 * 60 * 20),
    PHASE_DASH("Phase Dash", "Dash forward, passing harmlessly through everything.", 6 * 20);

    private final String displayName;
    private final String description;
    private final int cooldownTicks;

    ActiveAbility(String displayName, String description, int cooldownTicks) {
        this.displayName = displayName;
        this.description = description;
        this.cooldownTicks = cooldownTicks;
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
