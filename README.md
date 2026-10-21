# Mutations — v0.1.0 (NeoForge, Minecraft 26.3)

Every player is born with a random, permanent mutation. Use your powers to earn mastery XP,
level up, and (in a later version) unlock secondary mutations and spend stat points.

## What's in this version

- **Random mutation on first join**, saved permanently (kept through death).
- **4 playable mutations:** Regenerator, Kinetic Mind, Stormcaller, Phaser.
  The other 8 are listed in-game as "coming soon".
- **Mastery levels:** XP from killing mobs (tougher mobs give more) and from using active abilities.
  Passives give no XP. No level cap. +3 stat points per level.
- **Mutation screen** (M): your mutation, abilities, cooldowns, mastery level, XP bar,
  stat points, and the secondary-roll slots (levels 10 / 25 / 50).
- **HUD** in the top-left corner showing your abilities and cooldowns.
- **Admin commands** (see below).

## Controls (rebind in Options → Controls → Mutations)

| Key | Action |
|---|---|
| M | Open mutation screen |
| R, V, G, B, N, K | Ability 1–6 (your active abilities, in order) |
| U / J | Phaser speed tier up / down |

## The mutations

**Regenerator**: heals quickly (faster with mastery), immune to poison. Hunger drains much faster.
- Surge: heal 4 hearts, costs 6 hunger (30s cooldown).
- Immunity: totally immune to damage for 8 seconds (3 min cooldown).

**Kinetic Mind**: telekinesis. Strong use gives headaches (nausea).
- Telekinesis: grab the block you're looking at. It floats in front of you. Press again to drop it.
  Range and the hardest block you can lift grow with mastery (obsidian at level ~48).
  Blocks with contents (chests, furnaces...) can't be lifted.
- Force Push: knock back and damage mobs in front of you. While holding a block, it throws the block instead.

**Stormcaller**: immune to lightning, faster in the rain. All abilities are weaker in clear weather.
- Lightning Strike: lightning where you look. Strongest in thunderstorms, weakest in clear skies (6s cooldown).
- Summon Storm: starts a thunderstorm for everyone (20 min cooldown).

**Phaser**: speedster. Faster base speed, adjustable speed tiers (more tiers unlock every 8 mastery levels,
up to 5), takes less projectile damage. Can't wear anything in the chest slot; tiers 2+ burn hunger.
- Phase Dash: dash forward, immune to damage during the dash (6s cooldown).

## Commands (operators only, i.e. cheats on in single player)

`<players>` can be a name, `@s` (yourself), `@a` (everyone), etc.

| Command | What it does |
|---|---|
| `/mutation list` | List every mutation id |
| `/mutation info [player]` | Show a player's mutations, mastery, stat points, cooldowns |
| `/mutation set <players> <mutation>` | Replace their mutations with this one as the base |
| `/mutation add <players> <mutation>` | Add a mutation to their roster (max 4) |
| `/mutation remove <players> <mutation>` | Remove a mutation from their roster |
| `/mutation clear <players>` | Remove all mutations |
| `/mutation reroll <players>` | Give them a new random base mutation |
| `/mutation cooldowns reset <players>` | Make every ability ready right now |
| `/mutation mastery set <players> <level>` | Set mastery level |
| `/mutation mastery add <players> <levels>` | Add (or with a negative number, remove) levels |
| `/mutation mastery xp <players> <amount>` | Give mastery XP (levels up normally) |
| `/mutation mastery reset <players>` | Back to level 1, 0 XP, 0 stat points |
| `/mutation statpoints set <players> <amount>` | Set unspent stat points |

## Building the jar

1. Install **JDK 25** (for example Eclipse Temurin 25 from adoptium.net).
2. Open a terminal in this folder and run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
   The first build downloads Minecraft and NeoForge, so it takes a few minutes.
3. The mod is at `build/libs/mutations-0.1.0.jar`.

## Installing with CurseForge

1. Create a profile with **Minecraft 26.3** and **NeoForge** (pick the newest NeoForge version).
2. On the profile, click the three dots → **Open Folder**, and put the jar in the `mods` folder.
3. Launch and make a new world (or join an existing one). You'll get your mutation right away.

Testing tip: in a creative world with cheats on, use `/mutation set @s phaser` (etc.) to try each one,
and `/mutation cooldowns reset @s` to skip cooldowns.

## Project layout

```
src/main/java/com/mutationsmod/mutations/
  MutationsMod.java          main mod class, wires everything up
  ModAttachments.java        saved per-player data
  data/MutationData.java     what's saved: mutations, mastery, cooldowns...
  mutation/Mutation.java     the 12 base mutations (text, colors, actives)
  mutation/ActiveAbility.java  every active ability + cooldown
  ability/AbilityHandler.java  all gameplay logic (passives, downsides, actives)
  ability/Telekinesis.java   Kinetic Mind's block grabbing
  mastery/Mastery.java       XP and levels
  command/MutationCommand.java  /mutation commands
  network/ModPayloads.java   client <-> server messages
  client/                    keybinds, HUD, mutation screen
```
