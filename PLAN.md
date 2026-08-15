# VanillaSkills 2.0 — Working Plan

Execution plan for the rework specified in [REWORK.md](REWORK.md). That document is the **what and why**;
this one is the **how and in what order**. Where they disagree, REWORK.md wins on intent and this file gets
corrected.

Work items are `P<phase><stream>.<n>`. Each has acceptance criteria — a stream is not done until they pass.

**Conventions for every item:**
- **Make it configurable** (user directive, 2026-07-30: *"most things should be configurable"*). Any tuning
  number — a rate, a radius, a damage figure, a cost, a chance, a cap — goes in `gameplay.json` following the
  existing pattern: a persisted field, a `volatile static` published by `apply()`, and a clamp that makes a
  mis-set value harmless rather than dangerous. Reach for a hardcoded constant only when the value is
  structural (slot indices, save-format versions) rather than balance.
- Shared files must stay byte-identical across all four editions (26.2 Fabric/NeoForge, 26.1.2 Fabric/NeoForge).
  Files known to differ per edition are called out explicitly.
- Build with `gradlew build` (never `clean build`), archive the jar, prune `build/libs/`.
- No dev-server boot tests — build + static/`javap` verification, then hand the jar over for in-game testing.
- Bump `mod_version` and add a dated CHANGELOG entry per shippable change.

---

## Phase 1 — Foundations ✅ COMPLETE (2026-07-30)

All four streams done and propagated to all four editions; every edition builds clean and is
structurally verified. ⚠ **Not runtime-tested** — an in-game pass is still required before `2.0.0-alpha`.

Four independent streams. None blocks another, so they can run in any order or in parallel.
**Phase 1 does not ship on its own** — XP removal is drastic and player-visible while none of the compensating
systems exist yet. Phase 1 + Phase 2 release together as `2.0.0-alpha`.

### Stream 1A — Datapack loader

| # | Work | Notes |
|---|---|---|
| P1A.1 | `data/VsJsonLoader.java` (shared) — generic scanner/parser | `listResourceStacks("vanillaskills/<type>", …)` so **all** packs are visible, processed low→high priority honouring `"replace": true`. Accept a tag-style `{replace, values:[…]}` file, a bare array, or a single object. Per-file try/catch: log the offending file id, skip the bad entry, **never throw** — one broken pack file must not take down the server. |
| P1A.2 | `data/VsContent.java` (shared) — loaded-content registry | Static holders per type + `reload(ResourceManager)` + `isLoaded()`. Returns immutable views. |
| P1A.3 | Fabric registration | `ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(...)` with a `SimpleSynchronousResourceReloadListener`, `getFabricId()` = `vanillaskills:content`. In `VanillaSkills.java` (already edition-specific). |
| P1A.4 | NeoForge registration | **`AddServerReloadListenersEvent`** — ⚠ `AddReloadListenerEvent` does **not** exist in NeoForge 26.x; verified against both the 26.2.0.7-beta and 26.1.2.84 jars, which carry the same class. It extends `Event`, so it goes on the **game bus** (`NeoForge.EVENT_BUS`), not the mod bus. Use `event.addListener(Identifier, PreparableReloadListener)` with a plain vanilla `ResourceManagerReloadListener`. ✅ **Did NOT become an 8th divergent file** — the registration fits inside the already-divergent `VanillaSkills.java`, so divergence stayed at 7. |
| P1A.5 | Feats become data-driven — the pipeline proof | `Feats.ALL` → `VsContent.feats()`; ship the 11 built-ins as `data/vanillaskills/vanillaskills/feat/builtin.json`. Chosen because `featsDone` is already a `Set<String>`, so **no save migration is involved**. `FeatsMenu` reads from content. |
| P1A.6 | Re-apply economy on reload | P computation and `applyEconomy` stay at `SERVER_STARTED` and must also re-run when `/reload` fires. |

**Traps:**
- The reload listener runs during the initial datapack load, **before `SERVER_STARTED` assigns
  `VanillaSkills.server`** — `worldDir()` is null during load. The loader must not touch world state.
- Advancements load in the same reload cycle, so P cannot be computed inside the listener. Keep it at
  `SERVER_STARTED` / post-reload.
- `SkillTree.index()` silently invents a lane for a node with an unknown category id. The loader must **report**
  unknown references rather than let a typo produce a junk lane.

**Acceptance:** a test pack adding a feat is picked up by `/reload`; `"replace": true` removes built-ins; a
deliberately malformed JSON file logs a clear error naming the file and the server stays up.

### Stream 1B — Component migration

| # | Work | Notes |
|---|---|---|
| P1B.1 | Add a single stamping helper | One `Markers` helper setting `ITEM_MODEL` + `ITEM_NAME` + marker together, so the pattern lives in one place instead of ten. |
| P1B.2 | Migrate the **9** `CUSTOM_MODEL_DATA` call sites | `Alloys`, `ArmorTier`, `DragonIngot`, `DragonScale`, `DragonUpgradeTemplate`, `FortuneTemplate`, `ToolTier`, plus `DragonTemplateLoot` and `FortuneTemplateLoot` — ⚠ **those last two are edition-specific and must be edited separately per edition.** |
| P1B.3 | Migrate the **10** gear `CUSTOM_NAME` sites to `ITEM_NAME` | The 9 GUI files (`FeatsMenu`, `InfoMenu`, `PointsScreen`, `QuestMenu`, `RecipeBook`, `RecipeBookMenu`, `ShopMenu`, `SkillTreeMenu`, `StatsScreen`) **keep `CUSTOM_NAME`** — they are display-only container buttons, never held or anvilled. Only real gear and materials move. |
| P1B.4 | Add ~58 `assets/vanillaskills/items/<id>.json` definitions | Shape: `{"model":{"type":"minecraft:model","model":"vanillaskills:item/<id>"}}`. Generate, don't hand-write. |
| P1B.5 | **Delete all 57 `assets/minecraft/items/*.json` overrides** | The payoff: removes the root cause of the 0.19.15 leather-tint and 1.0.2 armour-trim bugs, and the add-on pack collision hazard. |
| P1B.6 | Update **10 of 14** advancement JSONs | `completionist`, `dragon_ingot`, `dragon_template`, `fortune_template`, `metallurgist`, `set_crystal`, `set_dragon`, `set_hardwood`, `set_rose_gold`, `set_steel` carry `custom_model_data` icons → `minecraft:item_model`. Validate with PowerShell `ConvertFrom-Json` (no python on this box). |
| P1B.7 | Legacy re-stamp pass | Pre-migration gear carries the old components and would render as its plain vanilla base once P1B.5 lands. Re-stamp marked stacks in place when seen (player join sweep + throttled inventory tick). Recognition is by `vs_*` marker, which is unchanged. |
| P1B.8 | Rebuild + re-host the pack | Rebuild the standalone pack with `jar.exe` (**never `Compress-Archive`** — it writes backslash entries MC silently cannot read), re-host, re-bake `DEFAULT_RP_SHA1`, add the previous URL to `SUPERSEDED_RP_URLS`. |

**Acceptance:** a freshly crafted Steel Ingot renders from `vanillaskills:steel_ingot` with no `assets/minecraft/`
override present; undyed leather armour renders correctly; a custom armour piece shows its trim in the inventory;
gear from a pre-migration world re-stamps and renders correctly on join; all 14 advancement JSONs parse.

### Stream 1C — XP removal + anvil costs

| # | Work | Notes |
|---|---|---|
| P1C.1 | `ExperienceOrb` mixin | Cancel `award(ServerLevel, Vec3, int)` and `awardWithDirection(ServerLevel, Vec3, Vec3, int)`. **Check whether `awardWithDirection` delegates to `award` first** — if it does, one inject covers both. |
| P1C.2 | `Player` mixin | Cancel `giveExperiencePoints(int)` and `giveExperienceLevels(int)`. |
| P1C.3 | Villager trade XP | `MerchantOffer.shouldRewardExp()` → false. |
| P1C.4 | Residual-source sweep | Furnace pickup, breeding, fishing, bottle o' enchanting, spawners, grindstone. Most funnel through P1C.1; confirm each rather than assume. |
| P1C.5 | Anvil on Skill Shards | In `AnvilMenuMixin`: add a Skill-Shard affordability gate on `mayPickup` + charge on take. ⚠ **Two corrections to the original plan:** the `@ModifyConstant` lifting the 40-level cap **stays** — with Skill Shards a cost of 50 is genuinely payable, so uncapping still does useful work. And the steel-forge path **stays until Phase 3 actually ships the smelting recipe**, or steel becomes uncraftable in between. |
| P1C.6 | Shop catalog | Remove the two `minecraft:experience_bottle` offers. |
| P1C.6a | **Loot chests** | Strip `minecraft:experience_bottle` from vanilla loot tables — it is a chest-loot entry in several of them. Reuses the existing loot-table hook (⚠ edition-specific files). |
| P1C.6b | **Villager trades** | Two separate things: the **XP reward** for trading (P1C.3), and clerics **selling bottles o' enchanting** as a traded item — remove the offer as well as the reward. |
| P1C.7 | Config | `anvilTooExpensiveCap` **stays** as a working knob (see P1C.5). `dragonRepairCost` is re-read as a Skill-Shard price and **stays at 20** — user decision 2026-07-30: the shard economy gains far more faucets in Phase 2 and 5 (three-dimension USSB ore, crates, loot, barter, feats) than the current balance workbook accounts for, so the old level figure carries over unchanged. |
| P1C.8 | Docs | `GuideBook`, `en_us.json`, `zh_tw.json` — the guide describes XP-based mechanics that no longer exist. |

**Acceptance:** mob kills, ore mining, smelting, trading, breeding, fishing and bottles all produce zero XP **and
no orb entities**; the XP bar never moves; an anvil charges Skill Shards, refuses when short, and never shows a
blocked result.

### Stream 1D — Quest index → string id migration

| # | Work | Notes |
|---|---|---|
| P1D.1 | Add `id` to the `Quest` record | Populate from the existing `Lang.questKey(title)` slug so ids and lang keys line up for free. ⚠ **First verify no two titles collide across `STARTER` (15) and `ALL` (57).** |
| P1D.2 | `PlayerSkillData`: **six** fields to string keys | `questKills`, `questClaimed`, `questStatBase`, `questStatNotified`, `starterDone`, `starterKills`. Add a `dataVersion` field. |
| P1D.3 | `QuestBoard.State` | `int[] activeIndices` → `String[] activeIds`. |
| P1D.4 | `Quests.java` | Every index-keyed lookup → id-keyed: `claimedSet`, `killMap`, `progress`, `claim`, `onKill`, `sync`. |
| P1D.5 | `QuestMenu.java` | Slot → id mapping, including the stale-board guard. |
| P1D.6 | Migration on load | If `dataVersion` is below the new version, map old ints through the **current** `QuestPool` ordering to ids, then stamp the version. |

**Hard constraint:** the mapping is only valid against `QuestPool`'s ordering as it exists at migration time.
**Ship this before reordering or datapack-ifying quests** — doing both in one release corrupts the mapping.

**Acceptance:** a real 1.7.6 save loads with starter progress, kill counts, claims and STAT baselines intact;
board state survives a restart; `/quests reroll`, `/quests graduate` and `/quests starter` still work.

---

## Phase 2 — Skill Shard economy ✅ COMPLETE (2026-07-30)

All items P2.1–P2.14 built and wired on **all four editions**; every edition builds clean and is
structurally verified. ⚠ **Not runtime-tested.** Ships with Phase 1 as `2.0.0-alpha`.

Two decisions worth carrying forward:
- **Custom blocks** = real vanilla block + per-world position record + `Display.ItemDisplay` overlay
  (REWORK §6.1). The Stable block sits on `diamond_block` so it is a beacon base natively.
- **Worldgen ore** needs none of that — `reinforced_deepslate` is uncraftable, drops nothing, and vanilla
  places it only on ancient-city floors at ~Y −52, so block type + dimension + height identifies it outright.

| # | Work |
|---|---|
| P2.1 | **USS item** — component-stamped, `vs_uss` marker, placeholder art |
| P2.2 | **Withdraw button** on the skill-tree home screen at slot **36** (directly above the `POINTS_SLOT = 45` counter), with **two-click confirmation** |
| P2.3 | **Deposit** — right-click a held USS to return it to the bank |
| P2.4 | **USSB** — 9 USS → block item; craft + uncraft |
| P2.5 | **SSSB** — 1 USSB + 2 tinted glass + 4 redstone (*assumed layout: redstone corners, tinted glass top/bottom middle, USSB centre*) |
| P2.6 | **Placement + tracking** — chosen vanilla base block, positions/merge-counts in world data (see REWORK §6.1 for the unresolved art route) |
| P2.7 | **7×7×7 hostile-damage aura**, ticked server-side |
| P2.8 | **Creeper immunity** — ⚠ test against spawnmanager, whose `ServerExplosionMixin` cancels *all* explosions inside spawn protection and has caused a withdrawn release before |
| P2.9 | **Merge on right-click**, stacking to 4, expanding the area |
| P2.10 | **Beacon base** — extend `BlockTags.BEACON_BASE_BLOCKS` via our datapack; mixin the beacon block entity for triple range (50 → 150) and double effect amplification |
| P2.11 | **Ore generation in all three dimensions**, all very rare — Overworld Y −10 to 10, Nether Y 15 and below, End anywhere outside the spawn island (gate on biome: exclude `minecraft:the_end`, allow the outer `end_*` biomes). Fabric `BiomeModifications` vs NeoForge biome modifiers = an edition delta |
| P2.12 | **Mining gate** — Crystalline or better, following the existing `DeepslateGate` pattern |
| P2.13 | **Loot + piglin barter injection** — reuse the `FortuneTemplateLoot`/`DragonTemplateLoot` hook (edition-specific files) |
| P2.14 | **Spawners drop USSB** instead of XP |

**Acceptance:** withdraw/deposit round-trips with no shard duplication under rapid clicking; a placed SSSB damages
hostiles in range and survives a creeper; merging to 4 expands the radius and is persisted across restart; a beacon
on an SSSB base reaches 150 blocks at double amplifier.

---

## Phase 3 — Gear pass ✅ COMPLETE (2026-07-30)

All items built on **all four editions**; every edition builds clean. ⚠ **Not runtime-tested.**
Only P3.9 (`Recipe.display()` vanilla-recipe-book entries) is outstanding, and it is cosmetic.

Depends on Phase 2: Crystallized Diamond and Fortune V consume USS and SSSB.

| # | Work |
|---|---|
| P3.1 | **Hardwood** — verify poison-on-hit actually fires; raise per-piece speed (*assumed 0.025 → 0.04*) |
| P3.2 | **Copper mines gold** — datapack override removing gold ores from `#minecraft:incorrect_for_copper_tool`. ⚠ Applies to all copper tools globally |
| P3.3 | **Rose Gold** — mines gold + iron via the per-stack `TOOL` component; **add fire resistance** (harmful-effect immunity and piglin neutrality already work) |
| P3.4 | **Steel** — armour speed penalty increased (*assumed −0.01 → −0.02 per piece*) |
| P3.5 | **Steel Shield** — held movement penalty + substantially more durability |
| P3.6 | **Crystalline** — full set grants Strength + Resistance I (*assumed additive with the existing 25% reflect*) |
| P3.7 | **Dragon scales** — ⚠ **gate the drop on a player kill** (currently fires on *any* dragon death, which THP can trigger on startup); 8 normally, **32 on the world's first player kill only**, via a persistent world flag |
| P3.8 | **Five recipe reworks** — see REWORK §3.7. Each needs matching `RecipeBook`, `GuideBook` and lang updates; these three drift and have caused stale-doc bugs before |
| P3.9 | **`Recipe.display()`** — publish real vanilla-recipe-book entries for custom recipes (display works; autofill does not) |

---

## Phase 4 — Infusing Table

| # | Work |
|---|---|
| P4.1 | Intercept right-click on the **vanilla enchanting table block** (no new block needed) |
| P4.2 | Scan nearby **chiseled bookshelves** in the vanilla enchanting-table layout; read their stored enchanted books |
| P4.3 | Offer exactly those enchantments; **multi-select** |
| P4.4 | Server-side chest-menu GUI modelled on *Enchanting Infuser*'s presentation |
| P4.5 | Fire the `minecraft:enchanted_item` trigger so "Enchanter" stays earnable |
| P4.6 | **Cost model — unresolved, REWORK §6.2** |

---

## Phase 5 — Crates ✅ done (art still placeholder)

Depends on Phase 2 for USS loot. ⚠ **Art is still the magenta/black placeholder** — the licensing question in
REWORK §3.8 was never put to TheGreenSlop. Mechanics did not need it.

| # | Work | Status |
|---|---|---|
| P5.1 | Crate items — right-click-opened, so **no custom block needed**. Base `minecraft:brick`: inert, not placeable, not edible, so opening cannot collide with eating or placing | ✅ done |
| P5.2 | Fishing injection — a pool of our own on `gameplay/fishing`, weight vs empty (`crateFishingWeight` / `crateFishingEmptyWeight`, ≈2.4% per catch by default) | ✅ done |
| P5.3 | Tiers: Wooden / Copper / Iron / Diamond | ✅ done |
| P5.4 | Biome-exclusive variants (Frozen, Lush) via `minecraft:location_check` against our own biome tags — no code | ✅ done |
| P5.5 | Crate loot tables, **including USS** (and a USSB chance in the Diamond crate) | ✅ done |
| P5.6 | **Unboxing** fishing-rod enchantment (3 levels) as a datapack enchantment, read via `table_bonus` | ✅ done |
| P5.7 | **Opening reveal — a slot machine.** One lane per item the crate can pay out, three rows each, reels scrolling and stopping left to right on a payline. Lane count is the crate MAXIMUM so a short roll shows a barrier rather than leaking the result early; Skill Shards are excluded from the lanes. Lane count and reel contents are sampled by rolling the table 40x, since a loot table cannot be asked about itself | ✅ done |

**Design note.** The mod injects exactly one entry into vanilla fishing: a reference to
`vanillaskills:crate_fishing`. Every decision about *which* crate, *how rare*, *which biome* and *how much
Unboxing helps* lives in that datapack table. That is not only for pack authors — loot tables finish loading
**before** the mod's own datapack content does, so a pool built in code from the loaded crate list would be
empty on the first load of every server. Naming a table sidesteps the ordering entirely.

`CrateDef` therefore carries only a crate's identity (id, name, colour, loot table). Drop rates deliberately
do **not** live on it; two sources of truth for rarity would drift.

---

## Phase 6 — Smaller items

| # | Work | Status |
|---|---|---|
| P6.1 | **Feats on by default** — adds ~188 Quest Shards of previously dead content | ✅ done |
| P6.2 | **Nether roof** — `NetherRoof` deals 4/sec of `DamageTypes.OUTSIDE_BORDER` at Y ≥ 128 in the Nether; creative and spectator exempt; 3 config knobs. Breaking the roof bedrock remains possible, per spec | ✅ done |
| P6.3 | **Horse stats** — speed/jump/HP reported in chat when the inventory opens. ⚠ NOT the screen title: 26.2 opens it with `ClientboundMountScreenOpenPacket`, which carries **no title at all**, so there is nothing server-side to write into | ✅ done |
| P6.4 | **Copper helmet light** — **not done.** The user chose a client mixin, and the config plumbing is in place, but the 26.2 renderer rework means `LevelRenderer.getLightColor` / `LightTexture` do not exist under those names. Writing an untestable renderer mixin risks crashing clients, so it was left out rather than guessed at | ⛔ open |

---

## Known gaps as of the 2.0.0-beta.1 test build

1. ⛔ **The skill tree, quests and shop are still not datapack-owned.** The single largest outstanding item.
   `SkillTreeManager` continues to load per-world `skilltree.json` (69 KB, 136 nodes); `VsJsonLoader` serves
   only `feat` and `crate`. The locked decision "datapack fully owns the skill tree" — and with it running
   `applyEconomy` over pack-authored **weights** rather than absolute costs — is unimplemented.

   **Scope, so it is not underestimated:** ~1,130 lines across `SkillTree`, `SkillTreeManager`, `SkillNode`,
   `SkillCategory`, `QuestPool` and `QuestShop`, plus a **migration for live player progress**. Unlocked
   skills are stored per player as node ids, so any change to how nodes are identified has to preserve them
   or people lose their tree. That migration is the risky part, not the loading.

   `/skill editor` and `/skill layout` were already removed; `/skill edit ...` is deliberately **kept** as the
   only remaining way to tune the tree until the datapack path exists.
2. ✅ **Resource pack repointed** (was gap 2). `DEFAULT_RP_URL` names the v2.0.0-beta.1 release,
   `DEFAULT_RP_SHA1` is the real hash of the rebuilt pack, and the pack carries **no**
   `assets/minecraft/items/*` overrides — only lang. The 1.7.6 URL joined `SUPERSEDED_RP_URLS`, so worlds
   pinned to it auto-upgrade instead of being served overrides 2.0 removed.
   ⚠ **RELEASE GATE:** the GitHub release carrying that URL does not exist yet.
3. ✅ **Recipe-book autofill implemented** (was gap 3). `ComponentAutofill` + `RecipeAutofillMixin` on
   `ServerPlaceRecipe.placeRecipe`, filling the grid from the recipe's own `RecipeDisplay` — which holds real
   `ItemStack`s with components, unlike `Ingredient`. Works because placement is server-side.
   ⚠ The book may still render these recipes greyed out: craftability highlighting is computed client-side by
   the same component-blind matcher, and that part is not reachable from a server.
4. ✅ **Temporary diagnostics removed** (was gap 6): `/skill oredebug` and `/skill clearexceptores`.
5. **P6.4 copper helmet light** — still open.
6. **Four assumptions remain unconfirmed**: Hardwood 0.04, Steel −0.02, Crystalline additive, SSSB recipe
   layout.
7. **Nearly nothing is runtime-tested.** Confirmed in play: nether roof, beacon, shard economy, feats, XP
   removal. Everything else is static verification only — it compiles, the JSON parses, and every API shape
   was checked against the 26.2 jar's bytecode.

---

## Cross-cutting checkpoints

Run these before any release tag, not at the end:

1. **Economy re-balance.** Three new Skill Shard faucets land across Phases 2, 5 and 6 — Feats-on, the shard ore,
   and crates. Price them **together** against the balance workbook or the 2126-shard budget drifts unnoticed.
2. **Lang parity.** `en_us` and `zh_tw` must stay key-for-key equal, every literal `"vanillaskills.*"` in code must
   exist in `en_us`, and the pushed pack's lang must match the jar's. ⚠ `perl` without `-CSD` corrupts UTF-8.
3. **Four-edition parity.** Shared files byte-identical; the 26.1.2 delta stays `EntityTypes`→`EntityType` and
   `Items.X.color()`→`Items.COLOR_X`. Divergent files remain the 7 known plus the loader registration.
4. **Casino contract intact.** `SkillMenuExtensions.register/unregister/all/isEmpty`, the five
   `PlayerSkillManager` shard methods, and the extension-slot placement logic. `javap` the built jars to confirm.
5. **Pack ritual.** Any art or lang change → rebuild pack with `jar.exe`, re-host, re-bake SHA, supersede the old
   URL. Verify the hosted SHA matches the baked one after upload.
6. **Release ritual.** Build → archive to `_jar-archive/` → prune `build/libs/` → refresh `curseforge-upload/` and
   `CF Pack Upload/` → commit and push (no AI attribution) → GH release → update ModHub → update memory.

---

## Decisions still open

Tracked in REWORK §6, repeated here so they are not forgotten mid-build:

| Ref | Decision | Needed by |
|---|---|---|
| §6.1 | Custom-block art: global retexture of a sacrificial block vs. blockstate multiplexing | P2.6 |
| §6.2 | Infusing Table cost model | P4.6 |
| §6.3 | Server-side routes for dynamic light and horse stats | P6.3 / P6.4 |
| §6.4 | Whether to rely on `DefaultCustomIngredients.customData` — needs a real vanilla-client test and a NeoForge equivalent | P3.8 |
| §3.8 | svcrates texture permission from the author | P5 art only |

## Assumptions awaiting confirmation

Called out so they can be corrected cheaply rather than discovered late:

- Hardwood per-piece movement speed 0.025 → **0.04** (+10% → +16% for the set)
- Steel per-piece movement penalty −0.01 → **−0.02** (−4% → −8% for the set)
- Crystalline's Strength + Resistance I are **added alongside** the existing 25% melee reflect, not replacing it
- SSSB recipe layout: redstone corners, tinted glass top/bottom middle, USSB centre
