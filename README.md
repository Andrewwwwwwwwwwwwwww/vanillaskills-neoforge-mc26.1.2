# VanillaSkills

A **server-side** progression overhaul for Minecraft **26.1.2** (NeoForge).

Experience is removed and **Skill Shards** take its place — earned from advancements and
found in the world, spent in a fifteen-lane skill tree, at the anvil, and at the Infusing
Table that replaces enchanting. On top of that sit five tiers of craftable gear past
netherite, a bounty board with a rotating shop, and crates you fish out of the water.

Install it on the **server**. Players connect with an unmodified client: the server pushes
a resource pack on join, so vanilla clients see the custom gear and item names with nothing
installed by hand. A client-side install is optional.

## What it adds

- **Skill Shards as a real item.** Withdraw them from the tree, trade them, bank them again.
  Nine compress into an Unstable Skill Shard Block — reinforced deepslate, taken over
  outright — which also generates as ore in all three dimensions.
- **Stable Skill Shard Blocks** damage nearby hostiles, merge to widen that aura, work as a
  beacon base, and are immune to explosions.
- **Five gear tiers**: Hardwood, Rose Gold, Steel, Crystalline and Dragon, each with its own
  tools, armour and set bonus.
- **The Infusing Table** reads enchanted books out of nearby chiseled bookshelves and offers
  exactly those, paid in shards. Books are kept, not consumed.
- **Crates** fished out of the water — a rarity ladder plus biome variants, with a
  slot-machine reel when you open one.
- **Bounty board and Quest Shop**, paid in Quest Shards, which also buy the gear-unlock lanes.
- **Satchel** — a chest surrounded by leather, for early-game portable storage.
- A **wandering trader** who buys raw materials from you for Skill Shards.

## Requirements

- Minecraft **26.1.2**
- **NeoForge** 26.2.0.7-beta or newer
- Installed on the **server**

## Configuration

Per-world, in `<world>/vanillaskills/`:

| File | What it holds |
| --- | --- |
| `gameplay.json` | Every gameplay toggle and number — gear balance, shard rates, board and shop sizes, the pushed pack, and more. |
| `points.json` | What each advancement is worth, and which namespaces count. |

Both reload live with `/skill reload`. A file written by an older version gains any options
added since, so new settings never stay hidden.

Everything that is *content* — the skill tree, quests, shop stock, crates and feats — lives
in datapack files under `data/<namespace>/vanillaskills/` using the vanilla tag format, so a
pack can add, reprice, replace or remove any of it without touching code. The mod ships its
own content exactly that way.

## Commands

| Command | |
| --- | --- |
| `/skill` | Open your skill tree |
| `/quests` (or `/bounty`) | Open the bounty board |
| `/skill toggle nightvision\|stepup` | Per-player toggles |

Operators also get `/skill skillshards`, `/skill questshards`, `/skill reset`,
`/skill recalc`, `/skill reload`, `/skill mending`, `/skill give`, and
`/quests board\|reroll\|graduate\|starter`. `/help` lists them in game.

## Translations

English and Traditional Chinese, both complete. Every quest, crate, feat, shop offer, skill
lane and node description is translatable. See [TRANSLATING.md](TRANSLATING.md).

## Building

Run `./gradlew build`. The jar lands in `build/libs/`.

The pushed texture pack is built from the **26.2 Fabric** repo
(`tools/build-pack.sh <tag>`), which patches its SHA-1 into all four editions at once —
they have to be produced together or the client rejects the download.

## Documentation

Full documentation lives at
<https://andrewwwwwwwwwwwwwww.github.io/modhub/mods/vanillaskills/>.
Release notes are in [CHANGELOG.md](CHANGELOG.md).

## License

All Rights Reserved. See [LICENSE](LICENSE) — this mod is proprietary.
