VanillaSkills — Texture Pack Template
=====================================

VanillaSkills is server-side: every custom item is a VANILLA item tagged with a
`custom_model_data` string. This resource pack retextures them by overriding each
base vanilla item's model so that, when it carries our string, it shows your model.

HOW TO USE
----------
1. Put this folder in  .minecraft/resourcepacks/  (or zip its CONTENTS — pack.mcmeta
   must be at the zip root) and enable it. For a modpack, ship it as a bundled pack.
2. For each item below, create a PNG at:
        assets/vanillaskills/textures/item/<name>.png
   The <name> values are exactly the filenames listed in
        assets/vanillaskills/textures/item/_TEXTURES_NEEDED.txt
   (16x16 like vanilla; tools use the "handheld" model, items the flat "generated").
3. That's it — the model JSONs already point at those textures. Edit the model JSONs
   in assets/vanillaskills/models/item/ only if you want non-default models.

NOTE: pack.mcmeta "pack_format" is set to 55 — change it to match your Minecraft
version if the pack shows as incompatible.

IMPORTANT LIMITATION (worn armor)
---------------------------------
This pack only changes the INVENTORY ICONS of the armor. WORN armor still uses the
base material's look (Hardwood=leather, Rose Gold=gold, Steel=iron, Crystalline=
diamond, Dragon=netherite) because the mod reuses the vanilla equipment assets.
Custom worn-armor textures would require a small mod change (a custom equipment
asset id per tier) — ask the mod author if you need that.

ITEM REFERENCE  (base vanilla item  ->  custom_model_data string  ->  texture file)
----------------------------------------------------------------------------------
MATERIALS / MISC
  gold_ingot                          vanillaskills:rose_gold_ingot       rose_gold_ingot.png
  iron_ingot                          vanillaskills:steel_ingot           steel_ingot.png
  diamond                             vanillaskills:crystallized_diamond  crystallized_diamond.png
  netherite_ingot                     vanillaskills:dragon_ingot          dragon_ingot.png
  phantom_membrane                    vanillaskills:dragon_scale          dragon_scale.png
  echo_shard                          vanillaskills:fortune_template      fortune_template.png
  netherite_upgrade_smithing_template vanillaskills:dragon_template       dragon_template.png
  shield                              vanillaskills:steel_shield          steel_shield.png   (shield model is special; icon only)

ARMOR  (base = leather/golden/iron/diamond/netherite per tier; pieces helmet/chestplate/leggings/boots)
  Hardwood   -> leather_*    -> vanillaskills:hardwood_<piece>    -> hardwood_<piece>.png
  Rose Gold  -> golden_*     -> vanillaskills:rose_gold_<piece>   -> rose_gold_<piece>.png
  Steel      -> iron_*       -> vanillaskills:steel_<piece>       -> steel_<piece>.png
  Crystalline-> diamond_*    -> vanillaskills:crystal_<piece>     -> crystal_<piece>.png
  Dragon     -> netherite_*  -> vanillaskills:dragon_<piece>      -> dragon_<piece>.png

TOOLS  (base = stone/golden/iron/diamond/netherite per tier; kinds pickaxe/axe/shovel/hoe/sword/spear)
  Hardwood   -> stone_*      -> vanillaskills:hardwood_<kind>     -> hardwood_<kind>.png
  Rose Gold  -> golden_*     -> vanillaskills:rose_gold_<kind>    -> rose_gold_<kind>.png
  Steel      -> iron_*       -> vanillaskills:steel_<kind>        -> steel_<kind>.png
  Crystalline-> diamond_*    -> vanillaskills:crystal_<kind>      -> crystal_<kind>.png
  Dragon     -> netherite_*  -> vanillaskills:dragon_<kind>       -> dragon_<kind>.png

(The Elytra-fused Dragon chestplate reuses the dragon_chestplate icon.)

FOLDERS
  assets/minecraft/items/        58 base-item overrides (already wired — usually no edits needed)
  assets/vanillaskills/models/item/   58 model stubs (edit only for custom models)
  assets/vanillaskills/textures/item/ <-- PUT YOUR PNGs HERE (see _TEXTURES_NEEDED.txt)

WORN ARMOR (now supported — v0.10.0+)
-------------------------------------
Each armor tier has its own equipment asset (vanillaskills:<tier>), so you can retexture the
ON-BODY armor too, not just the inventory icon.
  - Edit assets/vanillaskills/equipment/<tier>.json if you want (already points at your textures).
  - Provide the worn textures:
        assets/vanillaskills/textures/entity/equipment/humanoid/<tier>.png
        assets/vanillaskills/textures/entity/equipment/humanoid_leggings/<tier>.png
    (tiers: hardwood, rose_gold, steel, crystal, dragon)
WARNING: without this resource pack, custom worn armor has NO texture (invisible) on a vanilla
client. A modpack MUST ship this pack (bundled or server-required) so players see worn armor.
