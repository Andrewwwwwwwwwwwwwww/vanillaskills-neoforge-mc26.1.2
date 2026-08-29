#!/usr/bin/env node
/*
 * Validate the things that silently break the CLIENT but never fail a build.
 *
 *   node tools/check-assets.js
 *
 * Two real bugs motivated this, both shipped to players unnoticed:
 *
 *   1. PLACEHOLDER TEXTURES IN THE JAR. The 2.0 art drop landed in resourcepack/ only and was never
 *      copied back into src/main/resources/. Nine items (the three shard items and six crates) kept
 *      their 155-byte magenta placeholder inside the mod. Servers looked fine — the pushed pack
 *      covers them — so it only showed in SINGLE PLAYER, where no pack is pushed and the jar is the
 *      only source of art. Every mod texture must be byte-identical to the pack's copy.
 *
 *   2. DUPLICATE LANG KEYS. A second "vanillaskills.points.tasks" was added for a new screen entry
 *      while the old one still existed. Gson parses the language file strictly, so ONE duplicate
 *      throws and the WHOLE file fails to load — every string in every language silently falls back
 *      to its hardcoded English. It logged one line and was otherwise invisible.
 *
 * Neither is a compile error, and neither is caught by check-parity.js (which compares editions
 * against each other — and a bug copied to all four editions is "in parity").
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const MOD_TEX = path.join(ROOT, 'src/main/resources/assets/vanillaskills/textures');
const PACK_TEX = path.join(ROOT, 'resourcepack/assets/vanillaskills/textures');
const LANG = path.join(ROOT, 'src/main/resources/assets/vanillaskills/lang');

let problems = 0;
const fail = (msg) => { console.error('  ✗ ' + msg); problems++; };

function walk(dir, base = dir, out = []) {
  if (!fs.existsSync(dir)) return out;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, base, out);
    else out.push(path.relative(base, p).replace(/\\/g, '/'));
  }
  return out;
}

// --- 1. mod textures must match the pack, byte for byte -------------------------------------
console.log('Textures: mod jar vs pushed pack');
if (!fs.existsSync(PACK_TEX)) {
  fail('resourcepack textures not found at ' + PACK_TEX);
} else {
  for (const rel of walk(MOD_TEX)) {
    const a = path.join(MOD_TEX, rel);
    const b = path.join(PACK_TEX, rel);
    if (!fs.existsSync(b)) { fail(`pack is missing ${rel} (mod has it)`); continue; }
    if (!fs.readFileSync(a).equals(fs.readFileSync(b))) {
      fail(`${rel} differs between the mod and the pack — single player would show the mod's copy`);
    }
  }
  for (const rel of walk(PACK_TEX)) {
    if (!fs.existsSync(path.join(MOD_TEX, rel))) fail(`mod is missing ${rel} (pack has it)`);
  }
}

// --- 2. language files: no duplicate keys, and the same key set everywhere -------------------
console.log('Language files: duplicate keys and key parity');
const langFiles = fs.existsSync(LANG) ? fs.readdirSync(LANG).filter((f) => f.endsWith('.json')) : [];
const keySets = {};
for (const f of langFiles) {
  const raw = fs.readFileSync(path.join(LANG, f), 'utf8');
  const keys = [...raw.matchAll(/^\s*"([^"]+)"\s*:/gm)].map((m) => m[1]);
  const seen = new Set(), dupes = new Set();
  for (const k of keys) (seen.has(k) ? dupes : seen).add(k);
  // Gson throws on the first duplicate and the entire file fails to load, so this is fatal.
  for (const d of dupes) fail(`${f}: duplicate key "${d}" — Gson refuses the whole file`);
  keySets[f] = seen;
}
const [reference, ...others] = Object.keys(keySets);
for (const other of others) {
  for (const k of keySets[reference]) {
    if (!keySets[other].has(k)) fail(`${other} is missing key "${k}" (present in ${reference})`);
  }
  for (const k of keySets[other]) {
    if (!keySets[reference].has(k)) fail(`${reference} is missing key "${k}" (present in ${other})`);
  }
}

console.log(problems === 0 ? '\nAssets OK.' : `\n${problems} problem(s) found.`);
process.exit(problems === 0 ? 0 : 1);
