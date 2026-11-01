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
    ECLIPSE(Mutation.SHADOW, "Eclipse", "For 30 seconds darkness spreads 24 blocks around you: mobs are blinded, you stay merged in any light, and your hits from the shadows deal double damage.", 600),
    CRITICAL_MASS(Mutation.RADIATION, "Critical Mass", "Overload for 10 seconds, hurting everything within 8 blocks every second, then detonate in a 12-block blast that leaves a sickening zone for 20 seconds. No block damage.", 260),
    SOLAR_FLARE(Mutation.LUMINARY, "Solar Flare", "Become a small sun for 10 seconds: undead within 20 blocks burn, everything is blinded, and nearby players get Regeneration II.", 200),
    MAGNETIC_STORM(Mutation.MAGNETAR, "Magnetic Storm", "Drag every mob within 24 blocks into one point in front of you, crush them together, then fling them outward.", 100),
    PLAGUE_CLOUD(Mutation.VENOM, "Plague Cloud", "A toxic cloud fills 10 blocks around you for 10 seconds: Poison III and Slowness II for everything but you.", 200),
    WILDWOOD(Mutation.VERDANT, "Wildwood", "The land 20 blocks around you erupts: roots trap every mob for 6 seconds, grass and flowers spread, and you get Regeneration III.", 200),
    TECTONIC_QUAKE(Mutation.EARTHSHAPER, "Tectonic Quake", "The ground cracks open and pillars erupt under every mob within 16 blocks.", 60),
    STAMPEDE(Mutation.BEASTCALLER, "Stampede", "A herd of spirit horses charges 30 blocks in the direction you face, trampling everything in its path.", 60),
    TIME_STOP(Mutation.CHRONO, "Time Stop", "For 5 seconds everything within 32 blocks except you stops. When time resumes, every hit you landed hits all at once.", 100),
    LOCUST_PLAGUE(Mutation.SWARM, "Locust Plague", "A huge swarm fills 16 blocks around you for 10 seconds, stinging every mob each second and stripping the leaves (they grow back).", 200),
    SINGULARITY(Mutation.GRAVITON, "Singularity", "A black hole opens where you look for 8 seconds, pulling in every mob and item within 20 blocks and crushing mobs at its core, then bursts.", 160),
    TSUNAMI(Mutation.TIDECALLER, "Tsunami", "A wall of water surges 30 blocks forward, carrying every mob with it. It leaves no water behind.", 60),
    PRISM_FORTRESS(Mutation.CRYSTALLINE, "Prism Fortress", "A crystal dome grows around you for 15 seconds: mobs can't get in, and the dome fires shards at mobs outside.", 300),
    RUBBER_RAMPAGE(Mutation.ELASTIC, "Rubber Rampage", "For 12 seconds you're a bouncing ball: huge jumps, a shockwave every landing, and you hit everything you bounce into.", 240),
    ARMY_OF_THE_DEAD(Mutation.SOULBINDER, "Army of the Dead", "Every soul you have bursts out as a ghostly warrior (at least 6) for 30 seconds, and mobs killed meanwhile rise to fight for you.", 600),
    GIANT_SLAYER(Mutation.MICRO, "Giant Slayer", "Grow to 4x size for 15 seconds with huge health, damage and reach. Your steps shake the ground.", 300),
    TAX_SEASON(Mutation.TAX_MAN, "Tax Season", "For 20 seconds every mob within 24 blocks takes damage each second, every one that dies drops emeralds, and your Revenue fills up.", 400),
    FORECLOSURE(Mutation.LANDLORD, "Foreclosure", "For 20 seconds everything within 24 blocks counts as your Property, hostile mobs near you are evicted every second, and you have Strength II and Resistance II.", 400),
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
