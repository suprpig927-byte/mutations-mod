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
