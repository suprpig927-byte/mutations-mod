package com.mutationsmod.mutations.mutation;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every base mutation in the mod. Only the ones marked {@code implemented} can be rolled
 * or given with commands; the rest are planned and shown as "coming soon".
 */
public enum Mutation {
    REGENERATOR("Regenerator", 0xFF5BD46A, true,
            "Rapid regeneration. Immune to poison.",
            "Hunger drains much faster.",
            List.of(ActiveAbility.SURGE, ActiveAbility.IMMUNITY)),
    KINETIC_MIND("Kinetic Mind", 0xFFB57BFF, true,
            "Telekinetically grab, carry and throw blocks. Range and lift strength grow with mastery.",
            "Heavy use gives you headaches (nausea).",
            List.of(ActiveAbility.TELEKINESIS, ActiveAbility.FORCE_PUSH)),
    STORMCALLER("Stormcaller", 0xFF6FB8FF, true,
            "Immune to lightning. Faster in the rain.",
            "All of your abilities are weaker in clear weather.",
            List.of(ActiveAbility.LIGHTNING_STRIKE, ActiveAbility.SUMMON_STORM)),
    PHASER("Phaser", 0xFFFFE05A, true,
            "Faster base speed with adjustable speed tiers. Projectiles partly pass through you.",
            "You can't wear anything in your chest slot. Top speeds burn hunger.",
            List.of(ActiveAbility.PHASE_DASH)),
    CRYO("Cryo", 0xFF9FE8FF, false, "Coming soon.", "", List.of()),
    BLINKER("Blinker", 0xFFD58CFF, false, "Coming soon.", "", List.of()),
    COLOSSUS("Colossus", 0xFFC9A27A, false, "Coming soon.", "", List.of()),
    SIREN("Siren", 0xFFFF8FB8, false, "Coming soon.", "", List.of()),
    ABSORBER("Absorber", 0xFFFFA64D, false, "Coming soon.", "", List.of()),
    CHAMELEON("Chameleon", 0xFF7FE07F, false, "Coming soon.", "", List.of()),
    PYRO("Pyro", 0xFFFF6A3D, false, "Coming soon.", "", List.of()),
    HOST("Host", 0xFF8A8AA8, false, "Coming soon.", "", List.of());

    private final String displayName;
    private final int color;
    private final boolean implemented;
    private final String upside;
    private final String downside;
    private final List<ActiveAbility> actives;

    Mutation(String displayName, int color, boolean implemented, String upside, String downside, List<ActiveAbility> actives) {
        this.displayName = displayName;
        this.color = color;
        this.implemented = implemented;
        this.upside = upside;
        this.downside = downside;
        this.actives = actives;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String displayName() {
        return displayName;
    }

    /** ARGB color used in the UI. */
    public int color() {
        return color;
    }

    public boolean implemented() {
        return implemented;
    }

    public String upside() {
        return upside;
    }

    public String downside() {
        return downside;
    }

    public List<ActiveAbility> actives() {
        return actives;
    }

    public static Optional<Mutation> byId(String id) {
        return Arrays.stream(values()).filter(m -> m.id().equals(id)).findFirst();
    }

    public static List<Mutation> implementedValues() {
        return Arrays.stream(values()).filter(Mutation::implemented).toList();
    }
}
