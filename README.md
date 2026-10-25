# Mutations — v0.17.0 (NeoForge, Minecraft 26.3)

Every player is born with a random, permanent mutation. Use your powers to earn mastery XP,
level up, spend stat points, and evolve secondary mutations.
See **DESIGN.md** for every mutation, ability and secondary.

## What's in this version

- **All 12 base mutations:** Regenerator, Kinetic Mind, Stormcaller, Phaser, Cryo, Blinker,
  Colossus, Siren, Absorber, Chameleon, Pyro, Host.
- **96 secondary mutations** (8 per base). At mastery levels **10, 25 and 50** you're offered
  3 random ones from your base's list and pick one.
- **Stats:** Strength, Vitality, Agility, Endurance, Luck, Mutation. +3 points per mastery level,
  no caps. **Respec** refunds everything for 30 Minecraft XP levels.
- **Mutation screen (M)** with two tabs: *Mutation* (abilities, secondaries, rolls) and *Stats*.
- **HUD** (top-left): abilities and cooldowns, plus meters like Absorber charge and the Host's hunger.
- Kinetic Mind: smooth telekinesis; thrown blocks set into the world where they land.

## Controls (rebind in Options → Controls → Mutations)

| Key | Action |
|---|---|
| M | Open mutation screen |
| R, V, G, B, N, K | Ability 1–6 (base abilities first, then secondary ones, in order) |
| U / J | Phaser speed tier up / down |
| Z | Omega ultimate (mastery level 75+) |

Other controls:
- **Rainwalker / Flame Flight:** double-tap jump to take off (Flame Flight launches you automatically), hold forward to fly where you look.
- **Pyro:** sneak + right-click a lava bucket to drink it. With Hearthbound, right-click coal to eat it.
- **Host:** walk into a wall while holding forward to climb. Shapeless: sneak into a 1-block gap to crawl.
- **Chameleon:** sneak and stand still to turn invisible.
- **Pocket Space (Blinker):** button on the mutation screen.

## Commands (operators only, i.e. cheats on in single player)

`<players>` can be a name, `@s` (yourself), `@a` (everyone), etc. Tab-complete shows every id.

| Command | What it does |
|---|---|
| `/mutation list` | List every base mutation |
| `/mutation list <mutation>` | List that base's secondary mutations |
| `/mutation info [player]` | Mutations, secondaries, mastery, stats, cooldowns |
| `/mutation set <players> <mutation>` | Set the base mutation (clears secondaries) |
| `/mutation add <players> <mutation>` | Add an extra base mutation (max 4) |
| `/mutation remove <players> <mutation>` | Remove a base mutation |
| `/mutation clear <players>` | Remove all mutations |
| `/mutation reroll <players>` | New random base mutation |
| `/mutation secondary add <players> <secondary>` | Give a secondary mutation |
| `/mutation secondary remove <players> <secondary>` | Remove a secondary mutation |
| `/mutation secondary clear <players>` | Remove all secondaries |
| `/mutation roll <players>` | Offer a secondary roll right now (ignores level) |
| `/mutation cooldowns reset <players>` | Make every ability (and the Omega ultimate) ready now |
| `/mutation ultimate <players>` | Fire their Omega ultimate now (ignores level and cooldown) |
| `/mutation mastery set <players> <level>` | Set mastery level |
| `/mutation mastery add <players> <levels>` | Add (or with a negative number, remove) levels |
| `/mutation mastery xp <players> <amount>` | Give mastery XP |
| `/mutation mastery reset <players>` | Back to level 1, no stats or points |
| `/mutation statpoints set <players> <amount>` | Set unspent stat points |
| `/mutation stats set <players> <stat> <amount>` | Set a stat directly |
| `/mutation stats respec <players>` | Free respec (refund all points) |
| `/mutation lab <mutation>` / `crater` / `haven` / `prison` | Build that structure where you stand |

Testing tip: `/mutation set @s cryo`, `/mutation mastery set @s 50`, then open the screen (M) to roll secondaries.

## Building

Push to GitHub and download the jar from the **Actions** tab (see `.github/workflows/build.yml`),
or build locally with JDK 25: `gradlew.bat build` → `build/libs/mutations-0.3.0.jar`.

## Mutagen, the Gene Splicer and Mastery Tomes

- **Mutagen Ore** (stone and deepslate) generates about as often as gold, from the bottom of the world up to Y 32. It glows faintly. Mine it with an iron pickaxe or better; the ore drops itself. Smelt or blast it into **Mutagen Ingots**.
- **Mutagen Geodes** are rare, small geodes deep underground (Y -56 to 0): a smooth basalt and calcite shell lined with **Blocks of Mutagen Crystal**, with **Mutagen Clusters** growing inside. Breaking a cluster drops 2-4 **Mutagen Crystals**. Four crystals craft a Block of Mutagen Crystal.
- **Gene Splicer**: crafted from ingots, crystals, a diamond and a brewing stand:
  ```
  I C I
  C D C     I = Mutagen Ingot, C = Mutagen Crystal
  I B I     D = Diamond, B = Brewing Stand
  ```
  Right-click it to:
  - **Respec stats**: 2 Mutagen Crystals + 10 XP levels (instead of 30 levels from the mutation screen).
  - **Splice out a secondary**: 4 Mutagen Crystals + 20 XP levels. Removes that secondary and offers 3 new ones from your base mutation's list (never the one you removed). Pick one in the Splicer or the mutation screen.
- **Mastery Tome**: right-click to read it for enough mastery XP to gain about two levels. Found (with Mutagen Crystals) in dungeon, mineshaft, desert and jungle temple, stronghold library, ancient city and woodland mansion chests.

## Nullite and the Nullite Spear

Nullite is a green crystal that shuts mutant powers off, like kryptonite.

- **Meteor Craters** are very rare (about one per 1,200 chunks), ~30-block-wide bowls blasted into the overworld surface with a raised rim. A half-buried **Meteorite** core sits in the middle, covered in **Nullite Clusters**; Mutagen Crystal is scattered across the bowl. Ops can call one down with `/mutation crater`.
- **Nullite Clusters**: mine with an iron pickaxe for 1-2 **Nullite Shards**. Like kryptonite, standing within 8 blocks of Nullite suppresses a mutant's powers.
- **Containment Rods** are found in Gene Lab chests.
- **Nullite Spear**: Nullite Shard, Nullite Shard, Containment Rod stacked vertically (shards on top). A real spear (jab and charge attacks), between iron and diamond. Repaired with Nullite Shards.
  - A **jab** suppresses the mutant you hit for **5 seconds**; a **charged hit** for **15 seconds**.
  - **Suppressed**: no active abilities, no passives, no toggled forms, no mutation looks. The HUD shows a countdown.
  - **Holding** the spear (either hand) suppresses your own powers while you hold it.

## Catalysts

Each base mutation has a Catalyst, found in the vault of that mutation's Gene Lab (see below). Hold it in either hand for its boost:

| Mutation | Catalyst | Boost |
|---|---|---|
| Regenerator | Troll Heart | Regenerate twice as fast |
| Kinetic Mind | Focus Prism | +50% telekinesis range, hold 1 extra block |
| Stormcaller | Storm Rod | Counts as rain for you, even in clear weather |
| Phaser | Kinetic Soles | Unlocks one extra speed tier |
| Cryo | Frost Core | Heat no longer weakens you |
| Blinker | Ender Lens | +50% Blink and Displace range |
| Colossus | Titan Gauntlet | Ground Pound hits 50% harder and farther |
| Siren | Resonance Crystal | Sonic Scream reaches 50% farther and stuns |
| Absorber | Capacitor Cell | Absorb charge 50% faster |
| Chameleon | Prism Cloak | Mimic lasts twice as long |
| Pyro | Ember Heart | Rain no longer hurts you (water still does) |
| Host | Symbiote Jar | The organism starves half as fast |

## Abandoned Gene Labs

Rare ruined research labs on the overworld surface (about one per 400 chunks), each built to study one mutation and decorated for it.

- **Surface:** a crumbling white lab building with broken windows and debris. A stairwell leads down.
- **Specimen hall:** rows of two-block-tall **Specimen Chambers** (glass tubes of glowing fluid in the lab's mutation color, a subject floating inside; about a third are smashed open and empty) and two **Failed Experiment** spawners. Two sealed chambers flank the vault chest.
- **Research room:** bookshelves, desks, two lecterns with **Research Notes**, a loot chest (Mastery Tomes, Containment Rods, Mutagen, sometimes a Nullite Shard) and sometimes an abandoned Gene Splicer.
- **Vault:** behind an iron door (press the button beside it): the lab's **Catalyst**, a Mastery Tome, Mutagen Crystals, the lab's research note and sometimes a Containment Rod.
- Ops can build one with `/mutation lab <mutation>`.

## Failed Experiments

Mutated test subjects that guard the labs. Zombie-like, but they never burn in daylight. Each carries one power:
**Burning** (sets you on fire, fire-proof), **Frozen** (slows and chills you), **Unstable** (teleports to you and blinks away when hurt), **Hulking** (40% bigger, much tougher and stronger, slower), **Leeching** (heals when it hits you). They drop rotten flesh and sometimes a Mutagen Crystal.

## Specimen Chamber

A two-block-tall glass tube of glowing Mutagen fluid with a subject floating inside, built in the Gene Splicer's style. Craft: Mutagen Ingot / Glass / Mutagen Ingot, Glass / Mutagen Crystal / Glass, Mutagen Ingot / Glass / Mutagen Ingot. Gene Labs have them in their mutation's color.

## Containment gear

- **Inhibitor Field** (block). Craft: Iron / Nullite / Iron, Nullite / Redstone Block / Nullite, Iron / Nullite / Iron. Every second it suppresses all mutant players within 16 blocks, and puts the Suppressed effect on mobs.
- **Inhibitor Collar.** Craft: Leather, Nullite Shard, Leather. Right-click a player or mob to suppress them for 60 seconds. Single use.
- **Containment Suit** (helmet, suit, leggings, boots). Mutagen Ingots plus a Nullite Shard. Each piece cuts damage from mutation abilities by 15%, so a full set blocks 60%.
- **Psi Beacon.** Craft: Eye of Ender on top, Mutagen Crystal / Glass / Mutagen Crystal, then 3 Mutagen Ingots. Right-click it to list every mutant within 64 blocks and how far away they are.

## Mutant Haven

A hidden cave village. Look for a small mossy rock outcrop on the surface. A spiral stair leads down into a big glowing cavern with a campfire plaza, a Gene Splicer, a pond, a mushroom patch and five huts. Each hut belongs to a Haven Mutant trader. Every trader has their own look (skin tone and hair), dressed for their job:

| Trader | Sells |
|---|---|
| Quartermaster | Mutagen Ore, Ingots and Crystals, Nullite Shards |
| Archivist | Mastery Tomes |
| Keeper | Its own mutation's Catalyst (64 emeralds + 32 crystals), Specimen Chambers |
| Cartographer | Gene Lab, Meteor Crater and Containment Prison maps |
| Cook | Haven food: Glowcap Stew, Cave Loaf, Crystal Candy, Smoked Blindfish |

Haven food is just food. It has no powers. Ops can build a Haven with `/mutation haven`.

## Containment Prison

A rare prison built to hold mutants, with two separate boss fights.

1. **Guard tower** on the surface. A spiral stair leads down.
2. **Cell block.** Two rows of barred cells, some broken open, with Failed Experiment spawners and loot. Two **Inhibitor Fields** in the ceiling switch your powers off here.
3. **Director's office.** This is where you fight the **Warden-Director** (220 HP). He wears a full Containment Suit and swings a **Shock Baton** about every 2 seconds; each hit jolts you with a moment of Slowness. Every 12 seconds or so he lunges at you from range, and he releases 2 Failed Experiments at 66% and again at 33% health.
   - **Drops:** the **Director's Keycard**, his **Shock Baton** (an iron-tier club that slows what it hits), the **Overseer's Spear** (a stronger spear whose suppression lasts twice as long), Nullite and a Mastery Tome.
4. **The deep cell.** A stair leads down to a sealed **Containment Gate**. Right-click it with the Keycard to open it. Inside is **Subject Zero** (300 HP), who switches power every 8 seconds and uses it every 3–6 seconds:
   - **Pyro:** shoots two fireballs.
   - **Cryo:** fires a freezing line that slows you.
   - **Stormcaller:** calls lightning.
   - **Blinker:** teleports behind you.
   - **Colossus:** gains armor and ground-pounds.
   
   Hit it with Nullite (a Nullite or Overseer's Spear, or an Inhibitor Collar) to lock it in place. While suppressed it can't switch forms and takes 25% more damage.
   - **Drops:** the **Heart of Zero** (use it to permanently gain **one extra secondary mutation slot**; works once per player), 3 Mastery Tomes, Nullite and Mutagen.

Chests in the office and the deep cell hold plenty of Nullite, Mastery Tomes, Inhibitor Collars and Containment Suit pieces. Ops can build a prison with `/mutation prison`. Containment Gates can't be broken.

## Omega Awakening

At **mastery level 75** your base mutation reaches its full potential and unlocks an **ultimate** on its own key (**Z**, rebindable). Each ultimate has a **10-minute cooldown**, which the Mutation stat shortens like any other. While one is running, your eyes blaze and an aura swirls around you in your mutation's color.

| Mutation | Ultimate | What it does |
|---|---|---|
| Regenerator | **Undying** | For 30s you can't drop below half a heart. When it ends you heal to full. |
| Kinetic Mind | **Gravity Well** | Rips up to 24 blocks out of the ground around you, spins them in orbit for 3s while dragging nearby mobs in, then hurls them all where you look. |
| Stormcaller | **Eye of the Storm** | For 20s you fly with full storm power in any weather, and lightning strikes up to 2 enemies near you every second. |
| Phaser | **Time Blur** | For 15s you move and attack far faster than your top speed; mobs within 24 blocks crawl and incoming projectiles slow down. |
| Cryo | **Absolute Zero** | Hits and freezes everything within 20 blocks and cages mobs in ice. The ground (and trees) turn to snow and ice for a minute before thawing back, water turns to ice and lava to obsidian. A blizzard keeps them frozen for 10s. |
| Blinker | **Fracture** | Chain-blinks to up to 8 enemies in sight, striking each one, in about a second. |
| Colossus | **Titan** | For 30s you grow to double size in Steel Form, with more damage and reach; your footsteps shake the ground and hurt mobs. |
| Siren | **Banshee Wail** | A scream in every direction: heavy damage within 14 blocks, mobs are stunned for 5s, and glass shatters. |
| Absorber | **Overload** | Releases all your stored charge in one blast (bigger and stronger the more charge), then for 10s you absorb 100% of damage as charge. |
| Chameleon | **Doppelganger** | Three decoys wearing your skin wander off and draw the mobs, while you go fully invisible (armor too) for 20s. |
| Pyro | **Supernova** | You explode in a ring of fire on the ground where you stand, then launch up and fly as living flame for 15s. |
| Host | **Consume** | Tendrils seize up to 5 nearby mobs, drag them in and devour them; you heal 2 hearts and get food for each, and the organism is fed. |

Bosses can't be stunned by Banshee Wail or seized by Consume.

## Mutation Weapons

Every mutation has its own weapon, a bit stronger than iron and weaker than diamond. **Only that mutation can hold it.** If anyone else tries, it's pushed out of their hand into their inventory (or dropped if it's full). The special powers switch off while you're suppressed. Repair them with Mutagen Ingots.

Every recipe uses Mutagen Ingots (M), Mutagen Crystals (C), sticks (S) and one themed ingredient (X):

| Mutation | Weapon | Power | Themed ingredient |
|---|---|---|---|
| Regenerator | **Bloodthorn Blade** | Hits heal you (15% of the damage, 35% when you're below 40% health). | Golden Apple |
| Kinetic Mind | **Orbit Chakram** | Right-click to throw it. It steers toward where you're looking, cuts through up to 3 mobs, then flies back to your hand. | Ender Pearl |
| Stormcaller | **Thunderhead Maul** | Hits call lightning on the target (once every 2s) and do 25% more damage in the rain. | Lightning Rod |
| Phaser | **Blur Daggers** | Attack 12% faster for every speed tier. | Sugar |
| Cryo | **Glacier Halberd** | +1.5 reach. Hits slow and frost mobs. Right-click raises a line of ice spikes that hurts, freezes and pops up mobs (4s cooldown). | Blue Ice |
| Blinker | **Rift Knife** | Right-click to throw it. You teleport to wherever it lands and it returns to your hand (2s cooldown). | Eye of Ender |
| Colossus | **Earthbreaker** | In Steel Form, each hit sends out a shockwave that hits mobs around the target for half damage. | Block of Iron |
| Siren | **Echo Bow** | Arrows burst in a sonic boom where they land, hurting and dazing everything within 3.5 blocks. | Amethyst Shard |
| Absorber | **Capacitor Blade** | Hits store 1 charge. Right-click spends 10 charge on a 24-block energy slash. | Block of Redstone |
| Chameleon | **Shade Fang** | Deals 2.5x damage from behind, or while you're hidden or invisible. | Ink Sac |
| Pyro | **Ember Scythe** | Hits set mobs on fire. Right-click sweeps an arc of flame in front of you (3s cooldown). | Blaze Rod |
| Host | **Symbiote Whip** | +2.5 reach. Hits yank mobs toward you and push the organism's hunger back by 30s. | Fermented Spider Eye |
