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
            List.of(ActiveAbility.FEATHER_VOLLEY, ActiveAbility.WING_GUST, ActiveAbility.DIVE_STRIKE)),
    SHADOW("Shadow", 0xFF5A4A7A,
            "Slip into the shadows where the light level is 7 or lower: invisible, 30% faster, and mobs lose you. You can't attack while merged, and light above 10 pulls you out.",
            "Full daylight weakens you (Weakness I).",
            List.of(ActiveAbility.SHADOW_SLIP, ActiveAbility.SHADOW_STEP, ActiveAbility.SNUFF)),
    RADIATION("Radiation", 0xFF7CFF4A,
            "Immune to poison and wither. Your hits give mobs a stack of Radiation Sickness (1 damage per stack every 2 seconds for 10 seconds, up to 5 stacks).",
            "You glow, so hostile mobs spot you from twice as far, and animals near you won't breed.",
            List.of(ActiveAbility.RADIATION_BEAM, ActiveAbility.MELTDOWN_PULSE)),
    LUMINARY("Luminary", 0xFFFFF4B0,
            "Standing in light level 12 or higher charges Radiance (up to 100), which your abilities spend. You light up the ground around you as you walk.",
            "At light level 6 or lower Radiance slowly drains and your abilities cost double.",
            List.of(ActiveAbility.PRISM_LANCE, ActiveAbility.FLASH, ActiveAbility.LIGHT_BRIDGE)),
    MAGNETAR("Magnetar", 0xFFB04050,
            "Projectiles curve away from you (half of them miss). Dropped items and XP fly to you from 10 blocks away.",
            "Iron, chainmail and netherite armor weighs you down (-5% speed per piece), and lightning seeks you out in storms.",
            List.of(ActiveAbility.POLARITY_PULL, ActiveAbility.REPULSE, ActiveAbility.MAG_LIFT)),
    VENOM("Venom", 0xFF6FBF3A,
            "Immune to poison. Your melee hits poison the target (Poison I, 4s).",
            "Cold-blooded: in cold biomes and on snow you have Slowness I and Weakness I.",
            List.of(ActiveAbility.VENOM_SPIT, ActiveAbility.ACID_POOL, ActiveAbility.SHED_SKIN)),
    VERDANT("Verdant", 0xFF3FAF5F,
            "Crops and saplings within 8 blocks grow 3x faster. Right-click plants with an empty hand to bonemeal them. You heal while standing on grass, moss or leaves.",
            "You're a plant: meat and fish give you no food.",
            List.of(ActiveAbility.ROOT_SNARE, ActiveAbility.THORN_BURST, ActiveAbility.OVERGROWTH)),
    EARTHSHAPER("Earthshaper", 0xFF9A7A50,
            "You mine stone, dirt and sand 50% faster. Tremorsense: mobs moving on the ground within 16 blocks glow for you.",
            "Away from the ground (in the air or in water) your abilities are half as strong and you take 25% more damage.",
            List.of(ActiveAbility.STONE_PILLAR, ActiveAbility.EARTH_WALL, ActiveAbility.BURROW)),
    BEASTCALLER("Beastcaller", 0xFFC08040,
            "Tame animals with a single feeding. Animals within 10 blocks follow you, and your tamed animals take 30% less damage.",
            "You feel their pain: when a passive animal dies within 16 blocks, you take 2 damage.",
            List.of(ActiveAbility.CALL_THE_PACK, ActiveAbility.COMMAND, ActiveAbility.WILD_SENSE)),
    CHRONO("Chrono", 0xFF60C8D8,
            "You sense danger early: 10% of hits miss you entirely, and bad effects wear off 25% faster.",
            "Time pulls back: each ability you use gives you Hunger I for 5 seconds.",
            List.of(ActiveAbility.REWIND, ActiveAbility.STASIS, ActiveAbility.TIME_BUBBLE)),
    SWARM("Swarm", 0xFFE8C030,
            "A swarm of insects buzzes around you, stinging hostile mobs within 3 blocks. Bees never attack you.",
            "Smoke and fire scatter the swarm: after fire damage or standing near a campfire, your swarm and abilities are gone for 15 seconds.",
            List.of(ActiveAbility.STING_CLOUD, ActiveAbility.SCATTER, ActiveAbility.HIVE_MIND)),
    GRAVITON("Graviton", 0xFF8060E0,
            "Low gravity for you: you jump higher, fall slowly with no fall damage, and can hold sneak in mid-air to hover for 3 seconds.",
            "Your field is unstable: every 30 to 60 seconds gravity flips on you for a moment and lifts you into the air.",
            List.of(ActiveAbility.HEAVY_FIELD, ActiveAbility.LIFT, ActiveAbility.GRAVITY_SLAM)),
    TIDECALLER("Tidecaller", 0xFF3080D0,
            "Breathe underwater, swim fast and see clearly in water. You heal while in water or rain.",
            "You dry out: in deserts, badlands and the Nether, or 10 minutes away from water, you're weakened and your cooldowns double. Water or rain resets it.",
            List.of(ActiveAbility.WATER_WHIP, ActiveAbility.TIDAL_WAVE, ActiveAbility.BUBBLE_PRISON)),
    CRYSTALLINE("Crystalline", 0xFFD080F0,
            "Crystal skin: +6 armor, and 20% of melee damage you take is reflected at the attacker.",
            "Crystal shatters: explosions, falls and maces deal 50% more damage to you.",
            List.of(ActiveAbility.SHARD_VOLLEY, ActiveAbility.CRYSTAL_SPIKES, ActiveAbility.FACET_SHIELD)),
    ELASTIC("Elastic", 0xFFF07090,
            "+2 blocks of reach. You bounce instead of taking fall damage, and knockback against you is halved.",
            "You're soft: swords, axes and tridents deal 25% more damage to you.",
            List.of(ActiveAbility.STRETCH_GRAB, ActiveAbility.SLINGSHOT, ActiveAbility.COIL_PUNCH)),
    SOULBINDER("Soulbinder", 0xFF40D0C0,
            "Each mob you kill leaves you a soul (up to 10). Souls power your abilities.",
            "You're half-dead: healing works half as well on you, and you burn in daylight without a helmet.",
            List.of(ActiveAbility.RAISE, ActiveAbility.SOUL_DRAIN, ActiveAbility.SOUL_SHIELD)),
    MICRO("Micro", 0xFF50B0FF,
            "Size Shift between normal, tiny (fast, hard to hit, fits through gaps, no fall damage) and big (more health, reach and damage).",
            "Tiny has half health and weak hits. Big is slower and burns hunger 3x faster.",
            List.of(ActiveAbility.SIZE_SHIFT, ActiveAbility.SHRINK_RAY, ActiveAbility.GROWTH_STOMP)),
    TAX_MAN("Tax Man", 0xFF40A040,
            "Every mob you kill pays tax: Revenue equal to 10% of its max health (up to 100). 1 in 5 kills also drops an emerald or gold nuggets.",
            "Nobody likes the tax man: villagers raise their prices, iron golems attack you, and you lose all your Revenue when you die.",
            List.of(ActiveAbility.LEVY, ActiveAbility.AUDIT, ActiveAbility.GARNISH)),
    LANDLORD("Landlord", 0xFF8A6A4A,
            "Own up to 3 chunks as your Property. There you have Strength I, Resistance I and regeneration, trespassers glow, and every 10 minutes you collect rent from the villagers and animals living there.",
            "You're homesick: more than 64 blocks from all your chunks, you're weakened and your cooldowns are 25% longer.",
            List.of(ActiveAbility.STAKE_CLAIM, ActiveAbility.EVICTION, ActiveAbility.RENT_DUE));

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
