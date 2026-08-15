# VanillaSkills Resource Pack — Texture Progress

Working pack (populated copy of `../texturepack-template/`). Item icons **16×16**, worn-armor layers
**64×32**, spear in-hand textures **32×32**.

- Item icons: `textures/item/<name>.png`
- Worn armor: `textures/entity/equipment/humanoid/<tier>.png` (helmet/chest/boots) +
  `.../humanoid_leggings/<tier>.png` (leggings)
- Spears (vanilla-style 2-part): GUI icon (16×16) = `<tier>_spear.png`; 3D in-hand (32×32) =
  `<tier>_spear_in_hand.png`. Item override on the base spear selects by `display_context`.
- Tiers: `hardwood`, `rose_gold`, `steel`, `crystal`, `dragon`

## STATUS: COMPLETE (58/58 icons · 10/10 worn)

### Worn armor — 10/10 ✅
- [x] hardwood · [x] rose_gold · [x] steel · [x] crystal · [x] dragon  (humanoid + leggings each)
- Pack ships `equipment/<tier>.json` for all 5 tiers (override the mod jar's base-material fallback).

### Item icons — 58/58 ✅ COMPLETE
All armor (4×5), all tools incl. spears (6×5), all ingots/materials, dragon scale+template, steel_shield.
- [x] Dragon (13) · [x] Hardwood (10) · [x] Rose Gold (11) · [x] Steel (12) · [x] Crystalline (11)
- [x] **fortune_template** — supplied 2026-06-23 (from Downloads `fortune_upgrade.png`, 16×16);
  wiring restored (`minecraft/items/echo_shard.json` + `models/item/fortune_template.json`).

## Naming notes (for future batches)
- `rosegold` → tier id `rose_gold`; `crystalline` → tier id `crystal`; `crystalline_ingot` →
  `crystallized_diamond.png`; `dragon_upgrade` → `dragon_template.png`.
- Spear files: `<tier>_spear_icon.png` (16×16) → `<tier>_spear.png`; `<tier>_spear.png` (32×32) →
  `<tier>_spear_in_hand.png`.
- Spear base items: hardwood→stone, rose_gold→golden, steel→iron, crystal→diamond, dragon→netherite.

## steel_shield — custom BANNER PATTERN (FINAL & CONFIRMED WORKING, 0.18.2, 2026-06-23)
- A resource pack CANNOT give a shield a per-item custom texture via custom_model_data: the shield is
  drawn by the hardcoded `minecraft:shield` special renderer, which ignores custom_model_data and only
  reads `BANNER_PATTERNS` + `BASE_COLOR`. (No SHIELD equipment-layer either, so the armor-asset trick
  doesn't apply.) A custom 3D model "fakes" it but never matches vanilla's renderer — dead end.
- **SOLUTION:** make the steel shield a *bannered* shield with a custom banner pattern, so the REAL
  vanilla renderer draws it (perfect 3D in inventory/held/blocking). Plain (un-bannered) shields stay
  wooden — both look right. Pieces:
  - Mod (`SteelShield.create`): set `DataComponents.BASE_COLOR` (LIGHT_GRAY) + `BANNER_PATTERNS`
    (`BannerPatternLayers.Builder().addIfRegistered(patterns, vanillaskills:steel, WHITE)`), using
    `VanillaSkills.server.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)`. Removed the old
    custom_model_data.
  - Datapack (mod jar): `data/vanillaskills/banner_pattern/steel.json` = `{asset_id, translation_key}`.
  - Resource pack: `assets/vanillaskills/textures/entity/shield/steel.png` (the 64×64 shield-UV steel
    texture — the shield atlas `shield_patterns.json` sources `entity/shield/` across ALL namespaces) +
    lang `block.vanillaskills.banner.steel`=Steel.
  - Removed the pack's `minecraft/items/shield.json` override + `steel_shield` model + icon.
- **Server/vanilla-client OK** (same model as the armor): server stamps the component; banner_pattern is
  a synced registry (vanilla clients get it); client renders from the resource-pack texture. Needs the
  pack (already required). White tint preserves the texture's own colors.
