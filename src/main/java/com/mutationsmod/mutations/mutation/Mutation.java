package com.mutationsmod.mutations.mutation;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The 12 base mutations. One is rolled for every player on their first spawn. */
public enum Mutation {
    REGENERATOR("Regenerator", 0xFF5BD46A,
            "Rapid regeneration. Immune to poison.",
            "Hunger drains much faster.",
            List.of(ActiveAbility.SURGE, ActiveAbility.IMMUNITY)),
    KINETIC_MIND("Kinetic Mind", 0xFFB57BFF,
            "Telekinetically grab, carry and throw blocks. Range and lift strength grow with mastery.",
            "Heavy use gives you headaches (nausea).",
            List.of(ActiveAbility.TELEKINESIS, ActiveAbility.FORCE_PUSH)),
    STORMCALLER("Stormcaller", 0xFF6FB8FF,
            "Immune to lightning. Faster in the rain.",
            "All of your abilities are weaker in clear weather.",
            List.of(ActiveAbility.LIGHTNING_STRIKE, ActiveAbility.SUMMON_STORM)),
    PHASER("Phaser", 0xFFFFE05A,
            "Faster base speed with adjustable speed tiers. Projectiles partly pass through you. Sonic boom at tier 4+.",
            "You can't wear anything in your chest slot. Top speeds burn hunger.",
            List.of(ActiveAbility.PHASE_DASH, ActiveAbility.SPEED_PUNCH, ActiveAbility.VIBRATE, ActiveAbility.PHASE_THROUGH)),
    CRYO("Cryo", 0xFF9FE8FF,
            "Immune to freezing. Water freezes beneath your feet.",
            "Weakened in hot biomes and the Nether (overworld heat doesn't bother you at night).",
            List.of(ActiveAbility.ICE_BLAST)),
    BLINKER("Blinker", 0xFFD58CFF,
            "Mobs lose track of you easily.",
            "Taking damage sometimes blinks you somewhere random.",
            List.of(ActiveAbility.BLINK, ActiveAbility.DISPLACE)),
    COLOSSUS("Colossus", 0xFFC9A27A,
            "In Steel Form: huge melee damage, knockback resistance, and much faster mining.",
            "In Steel Form: slow, sinks in water, and can't sprint for long.",
            List.of(ActiveAbility.GROUND_POUND, ActiveAbility.TITAN_LEAP, ActiveAbility.STEEL_FORM)),
    SIREN("Siren", 0xFFFF8FB8,
            "You hear mobs through walls (they glow for you).",
            "You're loud: sneaking draws nearby monsters to you.",
            List.of(ActiveAbility.SONIC_SCREAM)),
    ABSORBER("Absorber", 0xFFFFA64D,
            "Half of all damage you take is stored as charge instead.",
            "Charge has no limit, but holding more than your capacity slowly hurts you.",
            List.of(ActiveAbility.RELEASE, ActiveAbility.CHARGED_LEAP)),
    CHAMELEON("Chameleon", 0xFF7FE07F,
            "Invisible while sneaking and standing still. Your first hit from hiding deals huge damage.",
            "Your armor stays visible: mobs still notice you if you wear any.",
            List.of(ActiveAbility.MIMIC, ActiveAbility.COPYCAT)),
    PYRO("Pyro", 0xFFFF6A3D,
            "In Flame On: immune to fire and lava. Sneak + right-click a lava bucket to drink it (heals like a golden apple).",
            "In Flame On: water and rain hurt you.",
            List.of(ActiveAbility.FLAMETHROWER, ActiveAbility.FLAME_FLIGHT, ActiveAbility.FLAME_ON)),
    HOST("Host", 0xFF8A8AA8,
            "A living organism with tendrils: extra reach, wall climbing, stronger after a kill. Bond with it for full power.",
            "It's always hungry: go 10 minutes without a kill and it feeds on you. Takes 75% less fall damage.",
            List.of(ActiveAbility.TENDRIL, ActiveAbility.BOND)),
    BULLET("Bullet", 0xFFE0B04A,
            "Fire bullets made from ores in your inventory. Every material shoots differently.",
            "Firing too fast overheats you: you burn and can't shoot until you cool down.",
            List.of(ActiveAbility.FIRE, ActiveAbility.SWITCH_AMMO, ActiveAbility.SCATTERSHOT)),
    FIERCE_WINGS("Fierce Wings", 0xFFB5652F,
            "Permanent hawk wings: no fall damage. Jump in mid-air to spread them and glide; press jump while gliding to flap upward.",
            "Your wings won't fit under a chestplate, and fire hurts you 50% more.",
            List.of(ActiveAbility.FEATHER_VOLLEY, ActiveAbility.WING_GUST, ActiveAbility.DIVE_STRIKE));

    private final String displayName;
    private final int color;
    private final String upside;
    private final String downside;
    private final List<ActiveAbility> actives;

    Mutation(String displayName, int color, String upside, String downside, List<ActiveAbility> actives) {
        this.displayName = displayName;
        this.color = color;
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

    public String upside() {
        return upside;
    }

    public String downside() {
        return downside;
    }

    public List<ActiveAbility> actives() {
        return actives;
    }

    public List<Secondary> secondaries() {
        return Secondary.forBase(this);
    }

    public static Optional<Mutation> byId(String id) {
        return Arrays.stream(values()).filter(m -> m.id().equals(id)).findFirst();
    }
}
