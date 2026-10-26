package com.mutationsmod.mutations.mutation;

import static com.mutationsmod.mutations.mutation.Mutation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Secondary mutations. Each belongs to one base mutation. At mastery levels 10, 25 and 50 you're
 * offered 3 random ones from your base's list and pick one.
 */
public enum Secondary {
    // ---------------- Regenerator ----------------
    ADAMANT_BONES(REGENERATOR, "Adamant Bones", "Take 20% less damage from everything."),
    BLOOD_RUSH(REGENERATOR, "Blood Rush", "Gain Speed II while below 30% health."),
    ADAPTIVE_IMMUNITY(REGENERATOR, "Adaptive Immunity", "Bad effects flip into good ones (poison heals, slowness speeds you up...). Others wear off twice as fast."),
    IRON_STOMACH(REGENERATOR, "Iron Stomach", "Rotten flesh and raw food never give you Hunger."),
    SECOND_WIND(REGENERATOR, "Second Wind", "Survive one killing blow every 10 minutes."),
    THICK_HIDE(REGENERATOR, "Thick Hide", "+4 armor."),
    VITAL_AURA(REGENERATOR, "Vital Aura", "Nearby players slowly regenerate."),
    TRANSFUSION(REGENERATOR, "Transfusion", "Drain health from the mob you're looking at.", ActiveAbility.TRANSFUSION),

    // ---------------- Kinetic Mind ----------------
    LONG_REACH(KINETIC_MIND, "Long Reach", "+10 blocks of telekinesis range."),
    MIND_SHIELD(KINETIC_MIND, "Mind Shield", "Arrows bounce off you."),
    ITEM_MAGNET(KINETIC_MIND, "Item Magnet", "Toggle on to make dropped items fly to you.", ActiveAbility.ITEM_MAGNET),
    LEVITATE(KINETIC_MIND, "Levitate", "Float upward for 15 seconds (press again to stop).", ActiveAbility.LEVITATE),
    HEAVY_LIFTER(KINETIC_MIND, "Heavy Lifter", "Hold up to 3 blocks at once and throw them as a volley."),
    DISARM(KINETIC_MIND, "Disarm", "Rip the held item out of a mob's hands.", ActiveAbility.DISARM),
    IRON_WILL(KINETIC_MIND, "Iron Will", "Immune to the headache downside."),
    PSYCHIC_BARRIER(KINETIC_MIND, "Psychic Barrier", "A dome that repels mobs and projectiles.", ActiveAbility.PSYCHIC_BARRIER),

    // ---------------- Stormcaller ----------------
    STATIC_CHARGE(STORMCALLER, "Static Charge", "Your hits sometimes zap the target and a nearby mob."),
    CONDUCTOR(STORMCALLER, "Conductor", "Lightning Strike hits up to 3 targets."),
    RAINWALKER(STORMCALLER, "Rainwalker", "Fly in the rain (double-tap jump, hold forward to soar where you look)."),
    CHARGED_UP(STORMCALLER, "Charged Up", "Being struck by lightning heals you and speeds you up."),
    GALE_STEP(STORMCALLER, "Gale Step", "Jump much higher in rain and storms."),
    THUNDERCLAP(STORMCALLER, "Thunderclap", "Knock back everything around you.", ActiveAbility.THUNDERCLAP),
    EYE_OF_THE_STORM(STORMCALLER, "Eye of the Storm", "Night vision while it's raining."),
    SUPERCELL(STORMCALLER, "Supercell", "Summon Storm cooldown halved."),

    // ---------------- Phaser ----------------
    AFTERIMAGE(PHASER, "Afterimage", "While sprinting, attacks sometimes pass straight through you."),
    QUICK_HANDS(PHASER, "Quick Hands", "+20% attack speed."),
    HYDROPLANE(PHASER, "Hydroplane", "Sprint across water at speed tier 3+."),
    MOMENTUM(PHASER, "Momentum", "Sprinting hits deal +10% damage per speed tier."),
    EFFICIENT_METABOLISM(PHASER, "Efficient Metabolism", "High speed burns half as much hunger."),
    BLITZ(PHASER, "Blitz", "For 10 seconds, running into mobs damages them.", ActiveAbility.BLITZ),
    STATIC_BUILD_UP(PHASER, "Static Build-Up", "Running fast stores a charge; your next hit releases it."),
    REFLEXES(PHASER, "Reflexes", "A chance to dodge any hit completely. Higher at higher speed tiers."),

    // ---------------- Cryo ----------------
    FROSTBITE(CRYO, "Frostbite", "Your hits slow enemies."),
    COLD_BLOODED(CRYO, "Cold Blooded", "The hot-biome weakness is halved."),
    ICE_BODY(CRYO, "Ice Body", "Standing on snow or ice slowly heals you."),
    GLACIER_SKIN(CRYO, "Glacier Skin", "+4 armor in cold biomes and at night."),
    ICE_WALL(CRYO, "Ice Wall", "Raise a packed-ice wall in front of you.", ActiveAbility.ICE_WALL),
    SNOW_STRIDE(CRYO, "Snow Stride", "Powder snow never traps you, and snow speeds you up."),
    PERMAFROST(CRYO, "Permafrost", "Ice Blast freezes enemies for longer."),
    SHATTER(CRYO, "Shatter", "Frozen or slowed enemies take 50% more damage from your hits."),

    // ---------------- Blinker ----------------
    SHORT_FUSE(BLINKER, "Short Fuse", "Blink cooldown halved."),
    SPATIAL_SENSE(BLINKER, "Spatial Sense", "See nearby players and mobs through walls."),
    RECALL(BLINKER, "Recall", "Blink back to where you were 5 seconds ago.", ActiveAbility.RECALL),
    CONTROLLED_CHAOS(BLINKER, "Controlled Chaos", "Random blinks always carry you away from danger."),
    POCKET_SPACE(BLINKER, "Pocket Space", "Open your ender chest from anywhere (button on the mutation screen)."),
    SWAP(BLINKER, "Swap", "Trade places with a mob or player.", ActiveAbility.SWAP),
    VOID_STEP(BLINKER, "Void Step", "Blinking cancels fall damage."),
    ECHO_BLINK(BLINKER, "Echo Blink", "After a Blink, you can Blink once more for free within 2 seconds."),

    // ---------------- Colossus ----------------
    UNSTOPPABLE(COLOSSUS, "Unstoppable", "Completely immune to knockback."),
    EARTHSHAKER(COLOSSUS, "Earthshaker", "Ground Pound reaches 50% farther."),
    IRON_GRIP(COLOSSUS, "Iron Grip", "Mining speed bonus doubled."),
    BULWARK(COLOSSUS, "Bulwark", "+6 armor while sneaking."),
    CHARGE(COLOSSUS, "Charge", "Rush forward, flinging mobs aside.", ActiveAbility.CHARGE),
    HEAVY_HITTER(COLOSSUS, "Heavy Hitter", "33% chance for melee hits to stun."),
    DEEP_LUNGS(COLOSSUS, "Deep Lungs", "Hold your breath far longer."),
    TITANS_REACH(COLOSSUS, "Titan's Reach", "+1 block of melee reach."),

    // ---------------- Siren ----------------
    ECHOLOCATION(SIREN, "Echolocation", "Hear mobs through walls from twice as far."),
    LULLABY(SIREN, "Lullaby", "Nearby mobs fall asleep until hit.", ActiveAbility.LULLABY),
    SHATTERPOINT(SIREN, "Shatterpoint", "Sonic Scream breaks glass and ice."),
    RESONANCE(SIREN, "Resonance", "Each scream hit on the same target hurts more than the last."),
    SOUNDPROOF(SIREN, "Soundproof", "Immune to the Warden's darkness and sonic boom."),
    BANSHEE_WAIL(SIREN, "Banshee Wail", "Sonic Scream makes mobs flee in terror."),
    TUNED_EARS(SIREN, "Tuned Ears", "See where nearby hostile mobs are on your screen."),
    SILENT_STEPS(SIREN, "Silent Steps", "Removes the 'loud' downside."),

    // ---------------- Absorber ----------------
    DEEP_RESERVOIR(ABSORBER, "Deep Reservoir", "+50% charge capacity."),
    KINETIC_FEEDBACK(ABSORBER, "Kinetic Feedback", "Your melee hits add charge too."),
    SHOCKWAVE(ABSORBER, "Shockwave", "Release also hurls everything back."),
    OVERFLOW_SHIELD(ABSORBER, "Overflow Shield", "Charge taken while over capacity also gives absorption hearts (up to 4)."),
    ELEMENTAL_SPONGE(ABSORBER, "Elemental Sponge", "Fire, lava and lightning damage is fully absorbed."),
    FOCUSED_BEAM(ABSORBER, "Focused Beam", "Spend 15 charge on one powerful long-range beam.", ActiveAbility.FOCUSED_BEAM),
    STEADY_VESSEL(ABSORBER, "Steady Vessel", "Overflow damage halved."),
    AFTERGLOW(ABSORBER, "Afterglow", "Regenerate for a few seconds after releasing."),

    // ---------------- Chameleon ----------------
    SHADOW_STEP(CHAMELEON, "Shadow Step", "No slowdown while sneaking invisibly."),
    SILENT_KILL(CHAMELEON, "Silent Kill", "Ambush kills drop double loot."),
    CAMOUFLAGE(CHAMELEON, "Camouflage", "Stay invisible while sneaking around, not just standing still."),
    COPYCAT_MASTERY(CHAMELEON, "Copycat Mastery", "Copycat traits last twice as long."),
    SCENTLESS(CHAMELEON, "Scentless", "Wolves, bees and other animals never attack you."),
    SMOKE_BURST(CHAMELEON, "Smoke Burst", "Blind nearby mobs and vanish.", ActiveAbility.SMOKE_BURST),
    BACKSTAB(CHAMELEON, "Backstab", "+50% damage when hitting from behind."),
    MIRROR_SKIN(CHAMELEON, "Mirror Skin", "Your armor turns invisible with you, and no longer gives you away."),

    // ---------------- Pyro ----------------
    EMBER_STRIKES(PYRO, "Ember Strikes", "Your hits set enemies on fire."),
    KINDLING(PYRO, "Kindling", "Regenerate near fire and lava."),
    MAGMA_SKIN(PYRO, "Magma Skin", "Anything that hits you in melee catches fire."),
    ASHWALKER(PYRO, "Ashwalker", "Soul sand, soul soil and magma speed you up instead."),
    FLARE(PYRO, "Flare", "A blinding flash that dazzles everything nearby.", ActiveAbility.FLARE),
    HEARTHBOUND(PYRO, "Hearthbound", "Right-click coal or charcoal to eat it."),
    WILDFIRE(PYRO, "Wildfire", "Flamethrower range +50%."),
    PHOENIX(PYRO, "Phoenix", "Once every 20 minutes, revive from death in a burst of flame."),

    // ---------------- Host ----------------
    HUNGRY_TENDRILS(HOST, "Hungry Tendrils", "Tendril cooldown halved."),
    SYMBIOTE_ARMOR(HOST, "Symbiote Armor", "While bonded, Resistance II instead of Resistance I."),
    DEVOUR(HOST, "Devour", "Kills restore hunger."),
    WALL_CRAWLER(HOST, "Wall Crawler", "Climb faster, and hold jump to cling to ceilings."),
    SHAPELESS(HOST, "Shapeless", "Sneak into a 1-block gap to squeeze through it."),
    LASH(HOST, "Lash", "Whip every mob around you.", ActiveAbility.LASH),
    WELL_FED(HOST, "Well Fed", "The organism can go 15 minutes between kills instead of 10."),
    PARASITE(HOST, "Parasite", "Your hits heal you for 20% of the damage."),

    // ---------------- Bullet ----------------
    HOLLOW_POINTS(BULLET, "Hollow Points", "Bullets deal 25% more damage."),
    RICOCHET(BULLET, "Ricochet", "Every bullet bounces off a wall once."),
    STEADY_AIM(BULLET, "Steady Aim", "While sneaking, bullets deal 50% more damage and Scattershot spreads half as much."),
    SCRAP_COLLECTOR(BULLET, "Scrap Collector", "Mobs you kill with bullets drop 1-3 iron nuggets."),
    TRACER_ROUNDS(BULLET, "Tracer Rounds", "Anything your bullets hit glows for 10 seconds."),
    RECYCLER(BULLET, "Recycler", "A 1 in 3 chance not to use up ammo."),
    COOLANT(BULLET, "Coolant", "Heat builds up 40% slower."),
    OVERPRESSURE(BULLET, "Overpressure", "A charged cannon round with triple damage.", ActiveAbility.OVERPRESSURE),

    // ---------------- Fierce Wings ----------------
    RAZOR_PLUMES(FIERCE_WINGS, "Razor Plumes", "Feathers pierce through mobs and deal 30% more damage."),
    TAILWIND(FIERCE_WINGS, "Tailwind", "Glide 30% faster."),
    TALON_STRIKE(FIERCE_WINGS, "Talon Strike", "Dive Strike hits 50% harder and leaves mobs reeling."),
    KEEN_EYE(FIERCE_WINGS, "Keen Eye", "While you're in the air, creatures within 40 blocks glow for you."),
    WING_SHIELD(FIERCE_WINGS, "Wing Shield", "Wrap yourself in your wings: projectiles bounce off, half damage.", ActiveAbility.WING_SHIELD),
    UPDRAFT(FIERCE_WINGS, "Updraft", "Shoot straight up into the sky.", ActiveAbility.UPDRAFT),
    PLUME_REGROWTH(FIERCE_WINGS, "Plume Regrowth", "Feather Volley cooldown halved."),
    PREDATOR(FIERCE_WINGS, "Predator", "Deal 25% more damage while you're in the air.");

    private final Mutation base;
    private final String displayName;
    private final String description;
    private final @Nullable ActiveAbility active;

    Secondary(Mutation base, String displayName, String description) {
        this(base, displayName, description, null);
    }

    Secondary(Mutation base, String displayName, String description, @Nullable ActiveAbility active) {
        this.base = base;
        this.displayName = displayName;
        this.description = description;
        this.active = active;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Mutation base() {
        return base;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public @Nullable ActiveAbility active() {
        return active;
    }

    public static List<Secondary> forBase(Mutation base) {
        return Arrays.stream(values()).filter(s -> s.base == base).toList();
    }

    public static Optional<Secondary> byId(String id) {
        return Arrays.stream(values()).filter(s -> s.id().equals(id)).findFirst();
    }
}
