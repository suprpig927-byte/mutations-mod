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
    PREDATOR(FIERCE_WINGS, "Predator", "Deal 25% more damage while you're in the air."),

    // ---------------- Shadow ----------------
    UMBRAL_STRIKE(SHADOW, "Umbral Strike", "Your first hit within 5 seconds of leaving the shadows deals 3x damage."),
    NIGHT_EYES(SHADOW, "Night Eyes", "Permanent Night Vision."),
    DEEP_DARK(SHADOW, "Deep Dark", "Being pulled out of the shadows doesn't blind you."),
    CREEPING_DARK(SHADOW, "Creeping Dark", "While merged, walk into a wall to climb it."),
    SHADE_CLOAK(SHADOW, "Shade Cloak", "While merged you heal half a heart every 2 seconds."),
    LIGHTLESS(SHADOW, "Lightless", "Bright light takes 3 seconds to pull you out instead of pulling you out at once."),
    SHADOW_CLONE(SHADOW, "Shadow Clone", "A shadow spirit fights beside you for 10 seconds.", ActiveAbility.SHADOW_CLONE),
    GRASPING_SHADOWS(SHADOW, "Grasping Shadows", "Shadow hands hold every mob within 6 blocks of the spot you look at in place for 3 seconds.", ActiveAbility.GRASPING_SHADOWS),

    // ---------------- Radiation ----------------
    HALF_LIFE(RADIATION, "Half-Life", "Radiation Sickness lasts twice as long."),
    FALLOUT(RADIATION, "Fallout", "Mobs that die while sick spread their sickness to mobs within 3 blocks."),
    RAD_SHIELD(RADIATION, "Rad Shield", "Anything that shoots you gets sickness."),
    ISOTOPE_FEEDING(RADIATION, "Isotope Feeding", "Standing within 6 blocks of sick mobs slowly heals you."),
    CONTAINED(RADIATION, "Contained", "You stop glowing, and animals and mobs treat you normally again."),
    GAMMA_BURST(RADIATION, "Gamma Burst", "Radiation Beam passes through up to 2 blocks."),
    IRRADIATE(RADIATION, "Irradiate", "Turn the spot you look at into a glowing patch that sickens mobs on it for 30 seconds.", ActiveAbility.IRRADIATE),
    DECAY_TOUCH(RADIATION, "Decay Touch", "Wither the mob you look at for 8 seconds and drain 3 hearts from it.", ActiveAbility.DECAY_TOUCH),

    // ---------------- Luminary ----------------
    PHOTOSYNTHESIS(LUMINARY, "Photosynthesis", "At full Radiance you slowly heal."),
    REFRACTION(LUMINARY, "Refraction", "Prism Lance splits into 3 beams."),
    HALO(LUMINARY, "Halo", "Undead within 6 blocks take 1 damage every 2 seconds."),
    RADIANT_AFTERGLOW(LUMINARY, "Afterglow", "Radiance charges at light level 8 and up."),
    BLINDING_AURA(LUMINARY, "Blinding Aura", "Mobs that hit you are blinded for 2 seconds."),
    LIGHTFOOTED(LUMINARY, "Lightfooted", "Speed I while you have more than 50 Radiance."),
    BEACON_PULSE(LUMINARY, "Beacon Pulse", "A column of light reveals every mob within 32 blocks for 10 seconds.", ActiveAbility.BEACON_PULSE),
    MIRROR_WARD(LUMINARY, "Mirror Ward", "For 5 seconds, projectiles that hit you are reflected.", ActiveAbility.MIRROR_WARD),

    // ---------------- Magnetar ----------------
    IRON_SKIN(MAGNETAR, "Iron Skin", "+4 armor while you wear no armor."),
    DISARMING_FIELD(MAGNETAR, "Disarming Field", "Mobs that hit you sometimes drop their weapon."),
    RAILRIDER(MAGNETAR, "Railrider", "Minecarts you ride go much faster."),
    BULLET_CATCHER(MAGNETAR, "Bullet Catcher", "Projectiles you deflect stop dead and drop."),
    FERROUS_FIST(MAGNETAR, "Ferrous Fist", "Hits on mobs wearing or holding metal deal 50% more damage."),
    COMPASS_SENSE(MAGNETAR, "Compass Sense", "The HUD points to the nearest ore within 16 blocks."),
    SHRAPNEL_BURST(MAGNETAR, "Shrapnel Burst", "Fling up to 8 iron nuggets from your inventory in every direction.", ActiveAbility.SHRAPNEL_BURST),
    METAL_SHELL(MAGNETAR, "Metal Shell", "Orbiting iron blocks every melee hit for 6 seconds.", ActiveAbility.METAL_SHELL),

    // ---------------- Venom ----------------
    NEUROTOXIN(VENOM, "Neurotoxin", "Poisoned mobs deal 30% less damage."),
    TOXIC_BLOOD(VENOM, "Toxic Blood", "Mobs that hit you get poisoned."),
    VENOM_FEAST(VENOM, "Venom Feast", "Killing a poisoned mob restores 2 hunger."),
    CORROSIVE(VENOM, "Corrosive", "Acid Pool also dissolves dirt, sand and gravel (they come back afterwards)."),
    LETHAL_DOSE(VENOM, "Lethal Dose", "Your poison can kill: poisoned mobs at half a heart keep taking damage."),
    WARM_SCALES(VENOM, "Warm Scales", "The cold weakness is gone."),
    PARALYZING_BITE(VENOM, "Paralyzing Bite", "Freeze the mob you look at in place for 3 seconds.", ActiveAbility.PARALYZING_BITE),
    SERPENT_LUNGE(VENOM, "Serpent Lunge", "Lunge forward and bite the first mob you hit.", ActiveAbility.SERPENT_LUNGE),

    // ---------------- Verdant ----------------
    BARKSKIN(VERDANT, "Barkskin", "+4 armor while you stand still."),
    POLLEN_CLOUD(VERDANT, "Pollen Cloud", "Mobs that hit you sometimes get Nausea."),
    DEEP_ROOTS(VERDANT, "Deep Roots", "Knockback can't move you while you're sneaking."),
    GREEN_THUMB(VERDANT, "Green Thumb", "Grown crops you harvest drop 50% more."),
    THORNMAIL(VERDANT, "Thornmail", "Melee attackers take 2 damage back."),
    SUN_FED(VERDANT, "Sun-Fed", "Under the open sky in daytime your hunger doesn't drain."),
    VINE_SWING(VERDANT, "Vine Swing", "Shoot a vine to the block you look at and swing toward it.", ActiveAbility.VINE_SWING),
    HEALING_BLOOM(VERDANT, "Healing Bloom", "A bloom that heals every player within 5 blocks for 10 seconds.", ActiveAbility.HEALING_BLOOM),

    // ---------------- Earthshaper ----------------
    STONESKIN(EARTHSHAPER, "Stoneskin", "+3 armor while standing on stone."),
    ORE_SENSE(EARTHSHAPER, "Ore Sense", "The HUD lists the ores within 8 blocks of you."),
    LANDSLIDE(EARTHSHAPER, "Landslide", "Earth Wall shoves mobs back as it rises."),
    SURE_FOOTING(EARTHSHAPER, "Sure Footing", "Immune to knockback while on the ground."),
    SANDSTORM(EARTHSHAPER, "Sandstorm", "Burrow leaves a dust cloud that blinds nearby mobs."),
    RUBBLE(EARTHSHAPER, "Rubble", "Stone Pillar showers nearby mobs with rubble."),
    FISSURE(EARTHSHAPER, "Fissure", "Crack the ground in a line: mobs on it are stuck and hurt for 3 seconds.", ActiveAbility.FISSURE),
    BOULDER_TOSS(EARTHSHAPER, "Boulder Toss", "Rip a boulder from the ground and throw it.", ActiveAbility.BOULDER_TOSS),

    // ---------------- Beastcaller ----------------
    BIGGER_PACK(BEASTCALLER, "Bigger Pack", "Call the Pack summons 5 wolves."),
    BEAST_HIDE(BEASTCALLER, "Beast Hide", "Mobs ignore you while you're riding an animal."),
    SWIFT_MOUNT(BEASTCALLER, "Swift Mount", "Animals you ride get Speed II."),
    HERD_HEALER(BEASTCALLER, "Herd Healer", "Your tamed animals regenerate near you."),
    THICK_FUR(BEASTCALLER, "Thick Fur", "The animal-death downside is halved."),
    ANIMAL_TONGUE(BEASTCALLER, "Animal Tongue", "Your tamed animals near you get Speed I and Strength I."),
    HAWK_EYE(BEASTCALLER, "Hawk Eye", "A spirit hawk circles overhead and marks every mob within 48 blocks for 10 seconds.", ActiveAbility.HAWK_EYE),
    BEAR_FORM(BEASTCALLER, "Bear Form", "Become a spirit bear for 15 seconds: more health, big claw hits, slower.", ActiveAbility.BEAR_FORM),

    // ---------------- Chrono ----------------
    DEJA_VU(CHRONO, "Deja Vu", "The dodge chance rises to 20%."),
    LONG_BUBBLE(CHRONO, "Long Bubble", "Time Bubble lasts twice as long."),
    ACCELERATE(CHRONO, "Accelerate", "After Rewind you get Speed II and Haste II for 5 seconds."),
    FAST_FORWARD(CHRONO, "Fast Forward", "Crops near you grow faster."),
    ECHO_STRIKE(CHRONO, "Echo Strike", "Your hits sometimes land a second time a moment later."),
    BORROWED_TIME(CHRONO, "Borrowed Time", "Once every 10 minutes, a killing blow rewinds you 5 seconds instead."),
    HASTE_FIELD(CHRONO, "Haste Field", "You and nearby players move and attack faster for 8 seconds.", ActiveAbility.HASTE_FIELD),
    AGE(CHRONO, "Age", "Age the mob you look at: its armor and damage drop for 10 seconds.", ActiveAbility.AGE),

    // ---------------- Swarm ----------------
    KILLER_BEES(SWARM, "Killer Bees", "Swarm stings deal 2 damage."),
    HONEY_BODY(SWARM, "Honey Body", "Honey, cookies, cake and pie heal you 2 extra hearts."),
    POLLINATOR(SWARM, "Pollinator", "Crops near you grow faster."),
    BUZZING_SHIELD(SWARM, "Buzzing Shield", "The swarm knocks 30% of projectiles out of the air."),
    LARVAL_REBIRTH(SWARM, "Larval Rebirth", "Once every 10 minutes, a killing blow scatters you instead, leaving you at 3 hearts."),
    CARRIER_SWARM(SWARM, "Carrier Swarm", "Fall damage is halved."),
    HIVE_SUMMON(SWARM, "Hive Summon", "Call 4 angry bees to fight for you for 20 seconds.", ActiveAbility.HIVE_SUMMON),
    WASP_BARRAGE(SWARM, "Wasp Barrage", "A spray of stinging insects in a cone in front of you.", ActiveAbility.WASP_BARRAGE),

    // ---------------- Graviton ----------------
    FEATHER_STEP(GRAVITON, "Feather Step", "Walk on water and lava while sprinting."),
    CRUSHING_WEIGHT(GRAVITON, "Crushing Weight", "Mobs in Heavy Field take 1 damage every second."),
    ORBIT(GRAVITON, "Orbit", "Dropped items within 6 blocks drift to you."),
    MASS_SHIFT(GRAVITON, "Mass Shift", "Immune to knockback, and your hits knock mobs twice as far."),
    MOONWALK(GRAVITON, "Moonwalk", "Jump even higher."),
    REPULSOR_FIELD(GRAVITON, "Repulsor Field", "Arrows fired at you slow to a stop and drop."),
    REVERSE(GRAVITON, "Reverse", "Gravity flips for mobs within 8 blocks: they fall upward for 2 seconds.", ActiveAbility.REVERSE),
    GRAVITY_LASH(GRAVITON, "Gravity Lash", "Pull yourself toward the block you look at.", ActiveAbility.GRAVITY_LASH),

    // ---------------- Tidecaller ----------------
    RAIN_DANCER(TIDECALLER, "Rain Dancer", "Rain gives you Speed I and Strength I."),
    DEEP_PRESSURE(TIDECALLER, "Deep Pressure", "Your hits deal 30% more to mobs in water."),
    HYDRO_JET(TIDECALLER, "Hydro Jet", "Swimming is twice as fast."),
    CLEANSING_WATER(TIDECALLER, "Cleansing Water", "Standing in water clears poison and wither."),
    ICE_SHAPER(TIDECALLER, "Ice Shaper", "Water Whip freezes the water under the mob it hits."),
    UNDERTOW(TIDECALLER, "Undertow", "Mobs in water near you are dragged down."),
    GEYSER(TIDECALLER, "Geyser", "A column of water launches you, or the mob you look at, high into the air.", ActiveAbility.GEYSER),
    RAINMAKER(TIDECALLER, "Rainmaker", "Start a rainstorm for 60 seconds.", ActiveAbility.RAINMAKER),

    // ---------------- Crystalline ----------------
    DIAMOND_SKIN(CRYSTALLINE, "Diamond Skin", "+4 more armor."),
    REFRACTING_BODY(CRYSTALLINE, "Refracting Body", "25% of projectiles bounce off you."),
    RESONANCE_SHARDS(CRYSTALLINE, "Resonance", "Your shards deal 50% more damage."),
    REGROWTH(CRYSTALLINE, "Regrowth", "Out of combat for 5 seconds, you regenerate."),
    GLIMMER(CRYSTALLINE, "Glimmer", "You shine in the dark like a torch."),
    SHARP_EDGES(CRYSTALLINE, "Sharp Edges", "Mobs touching you take 1 damage every second."),
    CRYSTAL_BRIDGE(CRYSTALLINE, "Crystal Bridge", "Grow a 12-block crystal walkway where you look for 20 seconds.", ActiveAbility.CRYSTAL_BRIDGE),
    SHATTER_NOVA(CRYSTALLINE, "Shatter Nova", "Break your skin outward: big damage around you, but you lose its armor for 10 seconds.", ActiveAbility.SHATTER_NOVA),

    // ---------------- Elastic ----------------
    SUPER_BOUNCE(ELASTIC, "Super Bounce", "You bounce back almost as high as you fell."),
    LONG_ARMS(ELASTIC, "Long Arms", "+2 more blocks of reach."),
    ABSORBENT(ELASTIC, "Absorbent", "Explosion damage is halved."),
    SQUEEZE(ELASTIC, "Squeeze", "Sneak into a 1-block gap to squeeze through it."),
    REBOUND(ELASTIC, "Rebound", "30% of arrows bounce back at the shooter."),
    TANGLE(ELASTIC, "Tangle", "Your hits slow the target for 2 seconds."),
    PARACHUTE(ELASTIC, "Parachute", "Stretch into a canopy and float down for 10 seconds.", ActiveAbility.PARACHUTE),
    RUBBER_WALL(ELASTIC, "Rubber Wall", "A rubbery wall in front of you bounces mobs and projectiles back for 5 seconds.", ActiveAbility.RUBBER_WALL),

    // ---------------- Soulbinder ----------------
    GRAVE_PACT(SOULBINDER, "Grave Pact", "Your raised allies deal 30% more damage."),
    SOUL_SIPHON(SOULBINDER, "Soul Siphon", "Each soul you collect heals half a heart."),
    UNDEAD_KIN(SOULBINDER, "Undead Kin", "Zombies and skeletons leave you alone unless you attack them."),
    LINGERING_DEAD(SOULBINDER, "Lingering Dead", "Raised allies last 120 seconds."),
    SOUL_SPEED(SOULBINDER, "Soul Speed", "Move fast on soul sand and soul soil."),
    DEATHLY_CHILL(SOULBINDER, "Deathly Chill", "Your hits slow the target for 2 seconds."),
    SOUL_SWAP(SOULBINDER, "Soul Swap", "Trade places with your nearest raised ally.", ActiveAbility.SOUL_SWAP),
    SPIRIT_WALK(SOULBINDER, "Spirit Walk", "Leave your body as a ghost for 10 seconds and fly through walls, then snap back.", ActiveAbility.SPIRIT_WALK),

    // ---------------- Micro ----------------
    TINY_TERROR(MICRO, "Tiny Terror", "Tiny hits deal normal damage."),
    UNSEEN(MICRO, "Unseen", "While tiny, mobs can't spot you beyond 6 blocks."),
    HEAVYWEIGHT(MICRO, "Heavyweight", "While big, you're immune to knockback."),
    POCKET_RIDER(MICRO, "Pocket Rider", "While tiny, sneak-right-click a player to ride on their shoulder."),
    QUICK_SHIFT(MICRO, "Quick Shift", "Size Shift cooldown halved."),
    GIANTS_GRIP(MICRO, "Giant's Grip", "While big, right-click a small mob to pick it up; right-click again to throw it."),
    ENLARGE_ALLY(MICRO, "Enlarge Ally", "Make the player you look at big for 20 seconds.", ActiveAbility.ENLARGE_ALLY),
    MICROSCOPIC(MICRO, "Microscopic", "Shrink so small you're invisible for 5 seconds.", ActiveAbility.MICROSCOPIC),

    // ---------------- Tax Man ----------------
    DEDUCTIONS(TAX_MAN, "Deductions", "Abilities cost 30% less Revenue."),
    COMPOUND_INTEREST(TAX_MAN, "Compound Interest", "While you hold 50+ Revenue, you gain 1 more every 10 seconds."),
    LOOPHOLE(TAX_MAN, "Loophole", "Once every 10 minutes, 50 Revenue pays off a killing blow and you survive at 2 hearts."),
    COLLECTIONS_AGENCY(TAX_MAN, "Collections Agency", "Mobs marked by Levy have Slowness II."),
    TAX_HAVEN(TAX_MAN, "Tax Haven", "Near villagers you regenerate, and villagers stop overcharging you."),
    GOLDEN_PARACHUTE(TAX_MAN, "Golden Parachute", "Fall damage is paid with Revenue (1 per heart) instead of health."),
    BRIBE(TAX_MAN, "Bribe", "The mob you look at stops attacking for 20 seconds. Costs 30 Revenue.", ActiveAbility.BRIBE),
    REPOSSESS(TAX_MAN, "Repossess", "Rip the armor and held item off the mob you look at.", ActiveAbility.REPOSSESS),

    // ---------------- Landlord ----------------
    GATED_COMMUNITY(LANDLORD, "Gated Community", "Hostile mobs can't spawn on your Property."),
    SECURITY_DEPOSIT(LANDLORD, "Security Deposit", "Mobs that hurt you on your Property drop double loot."),
    HOME_FIELD(LANDLORD, "Home Field", "You take 25% less damage on your Property."),
    PRIME_REAL_ESTATE(LANDLORD, "Prime Real Estate", "Crops, saplings and baby animals on your Property grow faster."),
    PROPERTY_VALUE(LANDLORD, "Property Value", "+1 max heart for each chunk you own."),
    REMOTE_LANDLORD(LANDLORD, "Remote Landlord", "The homesick downside is gone."),
    RETURN_HOME(LANDLORD, "Return Home", "Teleport to the center of your nearest chunk.", ActiveAbility.RETURN_HOME),
    HIRE_HELP(LANDLORD, "Hire Help", "Two iron golem groundskeepers guard you for 60 seconds.", ActiveAbility.HIRE_HELP);

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
