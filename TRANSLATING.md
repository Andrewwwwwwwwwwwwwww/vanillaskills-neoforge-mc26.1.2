# Translating VanillaSkills

## Thanks to our translators

| Language | Contributor |
| --- | --- |
| Traditional Chinese (`zh_tw`) | **caprese502** (Discord) |

Contribute a language and you'll be credited here, in the README, and in the release notes.


Everything a player can see is translatable. There is **one file to translate** — `en_us.json` —
and it covers both of the mod's text systems (see "How it works" at the end if you're curious).

## For translators

1. Start from the English template:
   [`src/main/resources/assets/vanillaskills/lang/en_us.json`](src/main/resources/assets/vanillaskills/lang/en_us.json)
   (573 entries).
2. Translate only the **values** (the text after each `:`). **Do not change the keys** (the text
   before `:`).
3. **Keep the placeholders** exactly as they are:
   - `%s` and `%d` get replaced with names/numbers. Keep them; you may move them if your language
     needs a different word order, but keep the same number of them in the same order of meaning.
   - `\n` is a line break — keep them where they make sense for your language.
   - `{GRAD}`, `{CONVERT}`, `{MENDING}` (guide pages only) are filled in from server settings.
   - Symbols like `→ ◀ ▶ ✦ ✔ ★ ⏳ 🔒 ×` can stay as they are.
4. Save as `<locale>.json` using your Minecraft language code — e.g. `es_es.json`, `de_de.json`,
   `fr_fr.json`, `ru_ru.json`, `pt_br.json`, `zh_cn.json`. Save as **UTF-8**.
5. Anything you leave untranslated falls back to English automatically, so **partial translations are
   fine** — send in what you have.

### A quick map of the keys

| Prefix | What it is |
| --- | --- |
| `vanillaskills.menu.*` | Menu chrome — buttons, titles, headings |
| `vanillaskills.lane.*` | Skill-tree lane names + their header blurbs |
| `vanillaskills.node.*.desc` | Skill node descriptions (the bonus each tier gives) |
| `vanillaskills.gear.*` | **Armour & tool names** (e.g. `Dragon Chestplate`, `Steel Pickaxe`) |
| `vanillaskills.item.*` | Ingots, scales, upgrade templates |
| `vanillaskills.set.*` | Armour set-bonus tooltips |
| `vanillaskills.quest.*` | Bounty names |
| `vanillaskills.shop.*` | Quest Shop offer names |
| `vanillaskills.feat.*` | Feats and their descriptions |
| `vanillaskills.points.*` | "Earning Skill Shards" screen |
| `vanillaskills.stats.*` | "Your Stats" screen (incl. attribute names) |
| `vanillaskills.board.*` | The physical bounty-board hologram |
| `vanillaskills.recipe.*` | Recipe book entries |
| `vanillaskills.guide.page.*` | Guide book pages |
| `vanillaskills.msg.*` | Chat messages |
| `vanillaskills.help.*` | `/help` command descriptions |
| `vanillaskills.config.*` | Mod Menu config screen (client-side) |

Notes:
- Skill **node titles** are generated as "<Lane name> <numeral>" — translating the lane name in
  `vanillaskills.lane.*` translates every tier of that lane automatically.
- Command names themselves (`/skill`, `/quests`) are never translated — only their descriptions.
- Op / tree-editor admin messages are intentionally left in English.

## For the server owner (installing a translation)

There are two places translations are read from, because the mod has two kinds of text:

**1. Menus, chat, and other server-drawn text — instant, no client action needed**
- Drop the file at `<world>/vanillaskills/lang/<locale>.json` and run `/skill reload`.
- Each player automatically sees it in their own Minecraft language.

**2. Item names and tooltips — these need the resource pack**
- Item names/lore are stored *inside the item*, so the **client** renders them. Put the same
  `<locale>.json` into the resource pack at `assets/vanillaskills/lang/<locale>.json`, re-zip, and
  re-host it (then update `resourcePackSha1` in the config to the new file's SHA-1).
- Players without the pack simply see English names — never raw keys.

**Or send it to the mod author** to be bundled into the jar and the official pack, which covers both
paths for everyone.

## How it works (background)

VanillaSkills is a server-side mod that also works for vanilla clients, so it uses two mechanisms:

- **Server-side translation** for anything the server draws fresh each time it's shown (menus, chat,
  the guide book). The server looks up each player's reported language and renders their copy.
- **Client-side translation keys with English fallbacks** for anything baked into an item
  (gear names, lore) or into a display entity (the bounty board), because those are stored once and
  seen by everyone — the server physically can't render a different version per player. The client
  resolves the key from the resource pack; if it can't, the English fallback shows.

One caveat worth knowing: gear that was **already crafted before this version** keeps the English
name it was stamped with. Newly crafted gear picks up the translatable name.
