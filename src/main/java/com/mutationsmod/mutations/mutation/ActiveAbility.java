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
    SPEED_PUNCH("Speed Punch", "A blur of punches on the mob in front of you. More hits at higher speed tiers.", 8),
    VIBRATE("Vibrate", "Vibrate so fast nothing can touch you for 2 seconds, knocking nearby mobs away.", 20),
    PHASE_THROUGH("Phase Through", "For 6 seconds, walk through walls (never through the floor).", 25),
    ICE_BLAST("Ice Blast", "Freeze what you hit and encase it in packed ice.", 5),
    BLINK("Blink", "Teleport to where you're looking.", 4),
    DISPLACE("Displace", "Teleport the mob or player you're looking at somewhere nearby.", 15),
    GROUND_POUND("Ground Pound", "Slam the ground, launching everything around you.", 10),
    TITAN_LEAP("Titan Leap", "Leap 3 blocks into the air.", 2),
    STEEL_FORM("Steel Form", "Toggle: armor up in living steel. All your Colossus powers and drawbacks only work while it's on.", 0.5),
    SONIC_SCREAM("Sonic Scream", "A scream that damages and disorients everything in front of you.", 8),
    RELEASE("Release", "Spend 10 charge to blast everything around you.", 1),
    CHARGED_LEAP("Charged Leap", "Spend 5 charge to leap 10 blocks up.", 1),
    MIMIC("Mimic", "Hostile mobs ignore you for 8 seconds.", 45),
    COPYCAT("Copycat", "Borrow a trait from the last mob type you killed.", 90),
    FLAMETHROWER("Flamethrower", "Spray fire in front of you for 4 seconds. Lights and fuels furnaces.", 10),
    FLAME_FLIGHT("Flame Flight", "Fly on jets of fire for 10 seconds.", 45),
    FLAME_ON("Flame On", "Toggle: become living flame. All your Pyro powers and drawbacks (except Phoenix) only work while it's on.", 0.5),
    TENDRIL("Tendril", "Yank a mob to you, or pull yourself to a block.", 4),
    BOND("Bond", "Toggle: let the organism fully bond with you. Full power, but no armor and it hungers twice as fast.", 0.5),

    // ---- secondary mutation actives ----
    TRANSFUSION("Transfusion", "Drain health from the mob you're looking at.", 20),
    ITEM_MAGNET("Item Magnet", "Toggle: pull dropped items to you.", 0.5),
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
    FOCUSED_BEAM("Focused Beam", "Spend 15 charge on one powerful long-range beam.", 1),
    SMOKE_BURST("Smoke Burst", "Blind nearby mobs and vanish.", 30),
    FLARE("Flare", "A blinding flash that dazzles everything nearby.", 20),
    LASH("Lash", "Whip every mob around you and drag them closer.", 10),

    // ---- Bullet & Fierce Wings ----
    FIRE("Fire", "Shoot a round of your selected ammo.", 0.25),
    SWITCH_AMMO("Switch Ammo", "Cycle to the next kind of ammo you're carrying.", 0.25),
    SCATTERSHOT("Scattershot", "A shotgun blast of 5 pellets (uses one ammo, lots of heat).", 8),
    OVERPRESSURE("Overpressure", "A charged cannon round: triple damage and huge knockback, but it kicks you back too.", 12),
    FEATHER_VOLLEY("Feather Volley", "Throw a fan of 5 razor feathers.", 5),
    WING_GUST("Wing Gust", "A beat of your wings blasts mobs away and knocks projectiles out of the air.", 10),
    DIVE_STRIKE("Dive Strike", "In the air: plunge to the ground. The landing hits harder the farther you fell.", 8),
    WING_SHIELD("Wing Shield", "Wrap yourself in your wings for 4 seconds: projectiles bounce off and you take half damage.", 20),
    UPDRAFT("Updraft", "Shoot straight up into the sky and spread your wings.", 12),

    // ---- Shadow ----
    SHADOW_SLIP("Shadow Slip", "Toggle: merge into the shadows (light level 7 or lower) or step back out.", 0.5),
    SHADOW_STEP("Shadow Step", "Teleport to a dark spot you're looking at, up to 24 blocks away.", 10),
    SNUFF("Snuff", "Put out torches, lanterns and other lights within 8 blocks for 20 seconds.", 30),
    SHADOW_CLONE("Shadow Clone", "A shadow spirit fights beside you for 10 seconds.", 30),
    GRASPING_SHADOWS("Grasping Shadows", "Shadow hands hold every mob near the spot you look at in place for 3 seconds.", 20),

    // ---- Radiation ----
    RADIATION_BEAM("Radiation Beam", "A 2-second green beam (24 blocks) that damages and sickens everything in the line.", 12),
    MELTDOWN_PULSE("Meltdown Pulse", "A burst that withers everything within 6 blocks (Wither II, 5s).", 20),
    IRRADIATE("Irradiate", "Turn the spot you look at into a patch that sickens mobs on it for 30 seconds.", 25),
    DECAY_TOUCH("Decay Touch", "Wither the mob you look at for 8 seconds and drain 3 hearts from it.", 20),

    // ---- Luminary ----
    PRISM_LANCE("Prism Lance", "An instant beam of light that pierces up to 3 mobs and bounces off a wall once. Costs 15 Radiance.", 3),
    FLASH("Flash", "Blind everything within 8 blocks for 4 seconds and stun it for 1. Costs 25 Radiance.", 15),
    LIGHT_BRIDGE("Light Bridge", "A 16-block walkway of solid light where you look, for 15 seconds. Costs 30 Radiance.", 20),
    BEACON_PULSE("Beacon Pulse", "Reveal every mob within 32 blocks for 10 seconds.", 30),
    MIRROR_WARD("Mirror Ward", "For 5 seconds, projectiles that hit you are reflected.", 20),

    // ---- Magnetar ----
    POLARITY_PULL("Polarity Pull", "Yank the mob you're looking at toward you (20 blocks). Twice as strong on mobs wearing or holding metal.", 6),
    REPULSE("Repulse", "A magnetic blast that throws mobs back and sends projectiles flying away.", 12),
    MAG_LIFT("Mag Lift", "Toggle: hover over iron blocks, rails and anvils (jump to rise, sneak to sink).", 0.5),
    SHRAPNEL_BURST("Shrapnel Burst", "Fling up to 8 iron nuggets from your inventory in every direction.", 10),
    METAL_SHELL("Metal Shell", "Orbiting iron blocks every melee hit for 6 seconds.", 25),

    // ---- Venom ----
    VENOM_SPIT("Venom Spit", "Spit a glob that gives Poison II and Slowness I for 6 seconds.", 5),
    ACID_POOL("Acid Pool", "A 3x3 pool of acid where you look: for 8 seconds it hurts mobs in it and wears down their armor.", 15),
    SHED_SKIN("Shed Skin", "Clear all bad effects and heal 3 hearts.", 60),
    PARALYZING_BITE("Paralyzing Bite", "Freeze the mob you look at in place for 3 seconds.", 15),
    SERPENT_LUNGE("Serpent Lunge", "Lunge forward and bite (and poison) the first mob you hit.", 8),

    // ---- Verdant ----
    ROOT_SNARE("Root Snare", "Roots hold the mob you're looking at in place for 4 seconds.", 10),
    THORN_BURST("Thorn Burst", "Thorns erupt from the ground within 5 blocks, hurting and slowing mobs.", 12),
    OVERGROWTH("Overgrowth", "Grow a wall of leaves (5 wide, 3 tall) in front of you for 30 seconds.", 20),
    VINE_SWING("Vine Swing", "Shoot a vine to the block you look at and swing toward it.", 4),
    HEALING_BLOOM("Healing Bloom", "A bloom that heals every player within 5 blocks for 10 seconds.", 40),

    // ---- Earthshaper ----
    STONE_PILLAR("Stone Pillar", "A pillar shoots up under the mob you're looking at and launches it.", 8),
    EARTH_WALL("Earth Wall", "Raise a stone wall (5 wide, 3 tall) in front of you for 20 seconds.", 15),
    BURROW("Burrow", "Sink into the ground and resurface up to 12 blocks ahead.", 12),
    FISSURE("Fissure", "Crack the ground in a line: mobs on it are stuck and hurt for 3 seconds.", 15),
    BOULDER_TOSS("Boulder Toss", "Rip a boulder from the ground and throw it.", 8),

    // ---- Beastcaller ----
    CALL_THE_PACK("Call the Pack", "Summon 3 spirit wolves that fight for you for 30 seconds.", 60),
    COMMAND("Command", "Every animal you've tamed within 32 blocks attacks the mob you're looking at.", 5),
    WILD_SENSE("Wild Sense", "Every creature within 32 blocks glows for 10 seconds.", 30),
    HAWK_EYE("Hawk Eye", "Mark every mob within 48 blocks for 10 seconds.", 40),
    BEAR_FORM("Bear Form", "Become a spirit bear for 15 seconds: more health, big claw hits, slower.", 60),

    // ---- Chrono ----
    REWIND("Rewind", "Snap back to where you were 5 seconds ago, with the health you had then.", 30),
    STASIS("Stasis", "Freeze the mob you look at for 4 seconds. Damage it takes lands all at once when it unfreezes.", 15),
    TIME_BUBBLE("Time Bubble", "For 6 seconds, mobs and projectiles within 6 blocks move at quarter speed.", 25),
    HASTE_FIELD("Haste Field", "You and nearby players move and attack faster for 8 seconds.", 40),
    AGE("Age", "Age the mob you look at: its armor and damage drop for 10 seconds.", 20),

    // ---- Swarm ----
    STING_CLOUD("Sting Cloud", "Send the swarm at the mob you look at: it stings and poisons it for 6 seconds.", 8),
    SCATTER("Scatter", "Burst into insects and reform up to 10 blocks ahead.", 20),
    HIVE_MIND("Hive Mind", "Every mob within 24 blocks glows for 10 seconds.", 30),
    HIVE_SUMMON("Hive Summon", "Call 4 angry bees to fight for you for 20 seconds.", 40),
    WASP_BARRAGE("Wasp Barrage", "A spray of stinging insects in a cone in front of you.", 10),

    // ---- Graviton ----
    HEAVY_FIELD("Heavy Field", "A 5-block field where you look: for 6 seconds mobs inside are pinned and flying mobs crash down.", 15),
    LIFT("Lift", "The mob you look at floats helplessly upward for 3 seconds, then drops.", 10),
    GRAVITY_SLAM("Gravity Slam", "In the air: slam straight down and crush mobs around your landing.", 8),
    REVERSE("Reverse", "Mobs within 8 blocks fall upward for 2 seconds.", 20),
    GRAVITY_LASH("Gravity Lash", "Pull yourself toward the block you look at.", 5),

    // ---- Tidecaller ----
    WATER_WHIP("Water Whip", "An 8-block lash of water that damages and pulls the mob you hit.", 5),
    TIDAL_WAVE("Tidal Wave", "A wave rolls out in front of you, sweeping mobs away and putting out fires.", 20),
    BUBBLE_PRISON("Bubble Prison", "Trap the mob you look at in a bubble for 4 seconds: it floats and can't fight.", 25),
    GEYSER("Geyser", "Launch yourself, or the mob you look at, high into the air.", 12),
    RAINMAKER("Rainmaker", "Start a rainstorm.", 300),

    // ---- Crystalline ----
    SHARD_VOLLEY("Shard Volley", "Fire 6 crystal shards in a spread.", 6),
    CRYSTAL_SPIKES("Crystal Spikes", "A ring of crystal spikes bursts from the ground around you, hurting and lifting mobs.", 12),
    FACET_SHIELD("Facet Shield", "Encase yourself in crystal for 4 seconds: you can't move, take no damage and reflect projectiles.", 25),
    CRYSTAL_BRIDGE("Crystal Bridge", "Grow a 12-block crystal walkway where you look for 20 seconds.", 20),
    SHATTER_NOVA("Shatter Nova", "Big damage around you, but you lose your crystal armor for 10 seconds.", 30),

    // ---- Elastic ----
    STRETCH_GRAB("Stretch Grab", "Your arm shoots out 12 blocks: grab a mob and pull it in, or grab a block and pull yourself to it.", 6),
    SLINGSHOT("Slingshot", "Launch yourself where you look, hitting mobs you fly through.", 10),
    COIL_PUNCH("Coil Punch", "A wound-up punch that hits 3x harder and sends the target flying.", 8),
    PARACHUTE("Parachute", "Stretch into a canopy and float down for 10 seconds.", 15),
    RUBBER_WALL("Rubber Wall", "A rubbery wall in front of you bounces mobs and projectiles back for 5 seconds.", 20),

    // ---- Soulbinder ----
    RAISE("Raise", "Raise the last hostile mob you killed as a ghostly ally for 60 seconds (up to 3). Costs 2 souls.", 10),
    SOUL_DRAIN("Soul Drain", "Steal 4 hearts from the mob you look at. Costs 1 soul.", 8),
    SOUL_SHIELD("Soul Shield", "Spirits circle you and block the next 3 hits within 8 seconds. Costs 3 souls.", 30),
    SOUL_SWAP("Soul Swap", "Trade places with your nearest raised ally.", 10),
    SPIRIT_WALK("Spirit Walk", "Fly through walls as a ghost for 10 seconds, then snap back.", 60),

    // ---- Micro ----
    SIZE_SHIFT("Size Shift", "Cycle size: normal, then tiny, then big.", 2),
    SHRINK_RAY("Shrink Ray", "Shrink the mob you look at for 10 seconds: half size, half health, half damage.", 20),
    GROWTH_STOMP("Growth Stomp", "Grow huge for a moment and stomp the ground around you.", 10),
    ENLARGE_ALLY("Enlarge Ally", "Make the player you look at big for 20 seconds.", 60),
    MICROSCOPIC("Microscopic", "Shrink so small you're invisible for 5 seconds.", 30),

    // ---- Tax Man ----
    LEVY("Levy", "Mark the mob you look at for 15 seconds: it takes 20% more damage from you and drops double loot.", 8),
    AUDIT("Audit", "Every mob within 16 blocks glows for 10 seconds, and the HUD shows the health and armor of the one you look at.", 20),
    GARNISH("Garnish", "Drain 2 hearts from every hostile mob within 8 blocks and heal for each. Costs 20 Revenue.", 20),
    BRIBE("Bribe", "The mob you look at stops attacking for 20 seconds. Costs 30 Revenue.", 20),
    REPOSSESS("Repossess", "Rip the armor and held item off the mob you look at.", 30),

    // ---- Landlord ----
    STAKE_CLAIM("Stake Claim", "Claim the chunk you're standing in (overworld). With 3 claimed, the oldest is released.", 60),
    EVICTION("Eviction", "Throw the mob you look at far away. On your Property it's thrown twice as far and banned for 30 seconds.", 10),
    RENT_DUE("Rent Due", "Every hostile mob on your Property takes damage each second and is slowed for 10 seconds.", 30),
    RETURN_HOME("Return Home", "Teleport to the center of your nearest chunk.", 300),
    HIRE_HELP("Hire Help", "Two iron golem groundskeepers guard you for 60 seconds.", 180);

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
