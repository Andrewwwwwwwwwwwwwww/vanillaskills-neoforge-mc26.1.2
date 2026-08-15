# VanillaSkills

A **server-side** skill-tree and progression overhaul for Fabric (Minecraft 26.1.2).

Players connect with a vanilla client — no client mod required. A bundled resource
pack (added later) supplies textures for the new items.

## Current features

### Mending removal
Mending is completely removed from the game:

- **Hard guarantee (mixin):** `ItemEnchantmentsMutableMixin` hooks the single
  chokepoint every enchantment application passes through
  (`ItemEnchantments.Mutable#set` / `#upgrade`). Mending can never be written onto
  any item — covers loot tables, the enchanting table, the `/enchant` command,
  villager `createBook`, and anything else.
- **Clean rolls (datapack tags):** bundled overrides of `#minecraft:tradeable` and
  `#minecraft:on_random_loot` remove Mending from the selection pools so librarian
  trades and chest loot never *roll* it (avoiding blank enchanted books).

### Skill tree
Per-player skill tree with a chest-based GUI (no client mod needed):

- **Points** are earned from advancements (configurable per-advancement in
  `config/vanillaskills/points.json`; recipe advancements ignored; existing players
  are credited retroactively on first join).
- **Tree** is defined in `config/vanillaskills/skilltree.json` (a default 15-lane tree —
  Vitality, Fleet Foot, Prospector, Fortune Finder, Warrior, Guardian (armor), Reach
  (block+entity interaction range), Mountaineer (step height), Aquatic (oxygen + underwater
  speed/mining), **Armorsmith**/**Toolsmith** (5 nodes each — one per tier — gating crafting of
  that tier's gear), **Brewmaster** (5 nodes — beneficial potions up to +100% duration),
  **Evasion** (chance to dodge arrows), **Cultivator** (bonus crop yield), and a **Night Vision**
  capstone (one node, cost 150, grants permanent night vision) — is written on first run; all use
  vanilla attributes/flags, no client mod). Prospector now runs 5 nodes
  to +12 mining efficiency (instamine stone with an Efficiency V diamond pick). Effects support
  `attribute`, `status_effect`, and `flag` types. Attribute bonuses apply as transient
  modifiers on join/respawn, so they never duplicate.
- **Commands:**
  - `/skill` (or `/skills`) — open the tree GUI
  - `/skill points` — show your points
  - `/skill points <player> add|set <n>` — op
  - `/skill reset <player>` / `/skill recalc <player>` — op
  - `/skill reload` — op, reloads tree + points config from disk
  - `/skill regen` — op, overwrites `skilltree.json` with the latest built-in default tree
    (backs up the old file first); use this to pick up new lanes after updating the mod
  - `/skill editor` — op, opens the tree GUI in **edit mode**: left-click a node to pick it
    up, click an empty slot to move it or another node to swap, right-click to delete
  - `/skill edit ...` — op, live-edit the server's tree (add/remove nodes, set
    cost/slot/icon/title/desc, requirements, attribute effects) writing back to JSON

### Bounty board (quests) & Quest Shop
A shared quest system with its own currency and shop:

- **Two currencies:** the skill tree spends **Skill Shards** (earned from advancements); bounties
  award **Quest Shards** (spent in the shop). Conversion is one-way: **3 Quest Shards → 1 Skill Shard**.
- **`/quests`** (alias **`/bounty`**) — open the quest GUI. Three bounties are active at a time and
  **re-roll every 5 hours**. Each is a *gather-items* (turn in at the board) or *kill-mobs* bounty
  worth a couple of Quest Shards. Anyone can do each once per rotation; progress is tracked per
  player and resets each rotation. Kill progress counts automatically; gather quests consume the
  items on claim (your own custom marked items don't count toward gather quests).
- **Quest Shop:** a Shop button in the quest GUI opens a **daily-rotating catalog** (~105 boost items,
  weighted so cheap commons show most). Pay with **Quest Shards (left-click)** or **Skill Shards
  (right-click, auto-converted 3:1)**. A permanent converter slot turns Quest Shards into Skill
  Shards. Stock rotates at UTC midnight.
- **Floating board:** **`/quests board`** (op) spawns a holograms-style floating "✦ Bounty Board ✦"
  text with an invisible interaction entity — anyone can right-click it to open the quest GUI, no
  command needed. **`/quests board remove`** (op) removes the nearest board. Board positions and quest
  state persist across restarts.

### Advancements
Custom mod advancements reward progression milestones — crafting each full armor set, discovering the
upgrade templates, forging a Dragon Ingot, unlocking a full skill-tree path, and a **Skill Master**
advancement for completing the *entire* skill tree (grants 5 Dragon Ingots). Only vanilla
(`minecraft:`) and mod (`vanillaskills:`) advancements grant points — datapacks like VanillaTweaks
are ignored.

### Craftable Fortune IV & V
Fortune past the vanilla cap of III, obtainable only by crafting:

1. **Fortune Upgrade Smithing Template** — a vanilla netherite upgrade template with a custom
   name + hidden marker tag (kept as a vanilla item so the mod stays server-side; a recolored
   texture can come via a resource pack). It is **its own thing**: a mixin blocks the marked
   template from working in the smithing table, so it can't be used as a netherite upgrade.
   - **Found** in Ancient City and minecart (abandoned mineshaft) chests at ~8.6% per chest —
     the same rarity as an Ancient City enchanted golden apple (injected via the loot API).
   - **Duplicated** in a crafting table (output 2):
     ```
     glow berries | Fortune Upgrade template | glow berries
     sculk        | diamond block            | sculk
     glow berries | emerald block            | glow berries
     ```
2. **Upgrade a Fortune book** — consumes two Fortune N books + the template:
   ```
   lapis block | diamond block | lapis block
   Fortune N book | Fortune Upgrade template | Fortune N book
   lapis block | diamond block | lapis block
   ```
   Output: one **Fortune (N+1)** book (N = 3 → IV, N = 4 → V). The template is consumed.
3. Apply the book to a tool in an **anvil**. Fortune's `max_level` stays 3, so the enchanting
   table, villagers, loot and anvil-*combining* never exceed III — IV/V are exclusive to this
   recipe. An anvil mixin un-clamps the over-level book/tool so the IV/V actually sticks.

### New armor tiers
Reskinned vanilla armor stamped with custom stats/name/marker (server-side; textures via an
optional resource pack keyed on the `custom_model_data` hook). Crafted in normal armor shapes
via one custom recipe; repaired at an anvil with the tier's material.

- **Hardwood** (leather base) — crafted from **Wood blocks** (`oak_wood` etc., stripped + hyphae
  too). Light & fast: +8% movement with a full set.
- **Rose Gold** (golden base) — crafted from **Rose Gold Ingots** (4 gold + 4 copper → 4).
  Immune to **all** negative status effects with a full set; piglins stay neutral (gold base).
- **Steel** (iron base) — crafted from **Steel Ingots** (1 iron + 1 coal → 1). High armor +
  high toughness for reliable defense; slightly slower (−4% with a full set).
- **Crystalline** / Diamond II (diamond base) — crafted from **Crystallized Diamonds**
  (4 diamonds + 1 amethyst block → 4). Stats sit between diamond and netherite (toughness 2.5,
  small built-in knockback resistance). Full-set bonus: **reflects 25% of melee damage** back at
  attackers (thorns-style, repaired with Crystallized Diamonds).
- **Dragon** / Netherite II (netherite base) — the top tier. **Upgraded from netherite armor in a
  smithing table:** *Dragon Upgrade Template* (found in End City treasure, ~4%) + netherite armor +
  *Dragon Ingot* → Dragon armor (enchants preserved). A **Dragon Ingot** is forged by surrounding a
  netherite ingot with **8 Dragon Scales** (dropped by the Ender Dragon, 8 per kill). Toughness 3,
  highest durability. Full-set bonuses: **immunity to fire / lava / dragon's breath**, and an active
  **dive-dash** (hold sneak while airborne, ~3s cooldown). Dragon **tools** are crafted from Dragon Ingots.
  - **Elytra combine:** **drop** a Dragon chestplate + an Elytra together **on top of an anvil block**
    to fuse them into one gliding chestplate; **drop** the combined chestplate **on a grindstone block**
    to split them back. Each keeps its own enchantments, so they always work standalone. (Item-drop
    mechanic, like the Vanilla Tweaks Armored Elytra — anvil-GUI elytra combining isn't natively supported.)

Each tier also has the full **tool set** (pickaxe, axe, shovel, hoe, sword), crafted in the
normal tool shapes from the tier's material + sticks, and repaired with the tier material.
Hardwood tools are wooden-tier; Rose Gold and Steel are iron-tier with more durability;
**Crystalline** tools are diamond-tier (between diamond and netherite); **Dragon** tools are
netherite-tier with the highest durability and hardest hits.

A creative tab ("VanillaSkills") collects all custom items, and `/skill guide` opens an
in-game info book. (Custom *blocks* aren't possible in a server-side / vanilla-client mod, so
there are none.)

Both metals are vanilla copper ingots with a name + hidden marker (no new items registered).
Anvil repair is locked to each tier's own material — only Rose Gold Ingots repair Rose Gold
armor, only Steel Ingots repair Steel armor (plain copper / the wrong alloy won't work).

## Planned

- Bundled resource pack supplying textures for all custom items and worn armor (worn custom armor is
  currently invisible on vanilla clients until the pack ships)
- Additional tier-specific combat effects (bleed, poison)
- Stronger tipped arrows

## Building

Open in IntelliJ IDEA with the Fabric/Loom Gradle import, then run the `build`
task. Output jar lands in `build/libs/`.

## License

All Rights Reserved. See the [LICENSE](LICENSE) file — these mods are proprietary.
