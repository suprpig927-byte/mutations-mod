package com.mutationsmod.mutations.mutation;

/** Omega Awakening: each base mutation's ultimate, unlocked automatically at mastery level 75. */
public enum Ultimate {
    UNDYING(Mutation.REGENERATOR, "Undying", "For 30 seconds you can't drop below half a heart. When it ends you heal to full.", 600),
    GRAVITY_WELL(Mutation.KINETIC_MIND, "Gravity Well", "Rip up the ground around you, spin it in orbit while dragging mobs in, then hurl it all where you look.", 80),
    EYE_OF_THE_STORM(Mutation.STORMCALLER, "Eye of the Storm", "For 20 seconds: fly at full power in any weather while lightning strikes enemies around you every second.", 400),
    TIME_BLUR(Mutation.PHASER, "Time Blur", "For 15 seconds you move and strike far faster than your top speed while everything around you crawls.", 300),
    ABSOLUTE_ZERO(Mutation.CRYO, "Absolute Zero", "Freeze everything within 20 blocks: mobs are encased in ice, the ground turns to snow and ice for a minute, water to ice, lava to obsidian.", 200),
    FRACTURE(Mutation.BLINKER, "Fracture", "Chain-blink between up to 8 enemies in about a second, striking each one.", 40),
    TITAN(Mutation.COLOSSUS, "Titan", "Grow to double size in Steel Form for 30 seconds. Your footsteps shake the ground and hurt mobs.", 600),
    BANSHEE_WAIL(Mutation.SIREN, "Banshee Wail", "A scream in every direction: heavy damage, mobs are stunned for 5 seconds, and glass shatters.", 60),
    OVERLOAD(Mutation.ABSORBER, "Overload", "Dump all your stored charge in one blast that grows with the charge, then absorb 100% of damage for 10 seconds.", 200),
    DOPPELGANGER(Mutation.CHAMELEON, "Doppelganger", "Three decoy copies of you draw the mobs while you go fully invisible, armor and all, for 20 seconds.", 400),
    SUPERNOVA(Mutation.PYRO, "Supernova", "Explode in a ring of fire where you stand, then launch up and fly as living flame for 15 seconds.", 320),
    BULLET_STORM(Mutation.BULLET, "Bullet Storm", "6 seconds of full-auto fire that uses no ammo and builds no heat.", 120),
    SKYFALL(Mutation.FIERCE_WINGS, "Skyfall", "Soar high into the sky and rain a storm of razor feathers on everything below for 8 seconds.", 180),
    CONSUME(Mutation.HOST, "Consume", "The organism takes over: tendrils seize up to 5 nearby mobs and devour them, healing you for each.", 40);

    public static final int LEVEL = 75;
    public static final int COOLDOWN_TICKS = 10 * 60 * 20;

    private final Mutation mutation;
    private final String displayName;
    private final String description;
    private final int glowTicks;

    Ultimate(Mutation mutation, String displayName, String description, int glowTicks) {
        this.mutation = mutation;
        this.displayName = displayName;
        this.description = description;
        this.glowTicks = glowTicks;
    }

    public Mutation mutation() { return mutation; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    /** How long the Omega glow shows after you use it. */
    public int glowTicks() { return glowTicks; }

    public static Ultimate of(Mutation m) {
        if (m == null) return null;
        for (Ultimate u : values()) if (u.mutation == m) return u;
        return null;
    }
}
