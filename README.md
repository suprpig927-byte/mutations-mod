# Mutations — v0.3.0 (NeoForge, Minecraft 26.3)

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
| `/mutation cooldowns reset <players>` | Make every ability ready now |
| `/mutation mastery set <players> <level>` | Set mastery level |
| `/mutation mastery add <players> <levels>` | Add (or with a negative number, remove) levels |
| `/mutation mastery xp <players> <amount>` | Give mastery XP |
| `/mutation mastery reset <players>` | Back to level 1, no stats or points |
| `/mutation statpoints set <players> <amount>` | Set unspent stat points |
| `/mutation stats set <players> <stat> <amount>` | Set a stat directly |
| `/mutation stats respec <players>` | Free respec (refund all points) |

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
- **Specimen hall:** cracked specimen tanks (some smashed) and two **Failed Experiment** spawners.
- **Research room:** bookshelves, desks, two lecterns with **Research Notes**, a loot chest (Mastery Tomes, Containment Rods, Mutagen, sometimes a Nullite Shard) and sometimes an abandoned Gene Splicer.
- **Vault:** behind an iron door (press the button beside it): the lab's **Catalyst**, a Mastery Tome, Mutagen Crystals, the lab's research note and sometimes a Containment Rod.
- Ops can build one with `/mutation lab <mutation>`.

## Failed Experiments

Mutated test subjects that guard the labs. Zombie-like, but they never burn in daylight. Each carries one power:
**Burning** (sets you on fire, fire-proof), **Frozen** (slows and chills you), **Unstable** (teleports to you and blinks away when hurt), **Hulking** (40% bigger, much tougher and stronger, slower), **Leeching** (heals when it hits you). They drop rotten flesh and sometimes a Mutagen Crystal.
