package io.github.andrewwwwwwwwwwwwwww.vanillaskills.config;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTiers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The numeric balance of the five gear tiers, as a {@code "gear"} block inside gameplay.json.
 *
 * <pre>{@code
 * "gear": {
 *   "armor": {
 *     "steel": { "armor": [3,7,5,3], "toughness": 0.0, "knockback": 0.0,
 *                "speed": -0.025, "durability": [330,481,451,390] }
 *   },
 *   "tools": {
 *     "steel": { "durability": 800, "attackDamage": 0.5, "attackSpeed": 0.1, "pickaxeMining": 0.0 }
 *   }
 * }
 * }</pre>
 *
 * <p>Tier ids are {@code hardwood}, {@code rose_gold}, {@code steel}, {@code crystal}, {@code dragon}.
 * Arrays are ordered <b>helmet, chestplate, leggings, boots</b>. {@code speed} is per piece and stacks
 * across the set, as a fraction of base walk speed — Steel's {@code -0.025} is the four-piece {@code -10%}.
 *
 * <p>Anything omitted keeps the shipped value, so a server can tune one number without restating the rest.
 * A tier id that is not one of the five is ignored: tiers are structural (base items, materials, repair
 * rules) and cannot be created from config.
 *
 * <p>⚠ These are stamped onto gear <b>when it is built</b>. Retuning does not rewrite pieces already in
 * chests unless {@code gearRestamp} is on, which brings a player's gear onto current numbers as they log in.
 */
public class GearTuning {

    /** Per-piece armour balance. Null fields keep the shipped value. */
    public static class ArmorSpec {
        public int[] armor;
        public Double toughness;
        public Double knockback;
        public Double speed;
        public int[] durability;
    }

    /** Per-tier tool balance. Null fields keep the shipped value. */
    public static class ToolSpec {
        public Integer durability;
        public Double attackDamage;
        public Double attackSpeed;
        /** Flat {@code mining_efficiency} on this tier's pickaxe only (Dragon's instamine budget). */
        public Double pickaxeMining;
    }

    public Map<String, ArmorSpec> armor = new LinkedHashMap<>();
    public Map<String, ToolSpec> tools = new LinkedHashMap<>();

    /**
     * A tuning block filled in with every shipped value, so the gameplay.json written for a fresh world
     * documents the whole gear table rather than an empty stub.
     */
    public static GearTuning defaults() {
        GearTuning g = new GearTuning();
        for (ArmorTier tier : ArmorTiers.TIERS) {
            ArmorSpec spec = new ArmorSpec();
            spec.armor = tier.defaultArmor.clone();
            spec.toughness = tier.defaultToughness;
            spec.knockback = tier.defaultKnockback;
            spec.speed = tier.defaultSpeed;
            spec.durability = tier.defaultDurability.clone();
            g.armor.put(tier.id, spec);
        }
        for (ToolTier tier : ToolTiers.TIERS) {
            ToolSpec spec = new ToolSpec();
            spec.durability = tier.defaultDurability;
            spec.attackDamage = tier.defaultAttackDamage;
            spec.attackSpeed = tier.defaultAttackSpeed;
            spec.pickaxeMining = tier.defaultPickaxeMining;
            g.tools.put(tier.id, spec);
        }
        return g;
    }

    /**
     * Push this block onto the live tiers.
     *
     * <p>Every tier is reset to its shipped values first, so deleting a key from gameplay.json actually
     * reverts that tier on the next {@code /reload} instead of leaving the last tuned value in place until
     * a restart.
     */
    public void apply() {
        for (ArmorTier tier : ArmorTiers.TIERS) {
            tier.tune(tier.defaultArmor, tier.defaultToughness, tier.defaultKnockback,
                    tier.defaultSpeed, tier.defaultDurability);
            ArmorSpec spec = armor == null ? null : armor.get(tier.id);
            if (spec != null) tier.tune(spec.armor, spec.toughness, spec.knockback, spec.speed, spec.durability);
        }
        for (ToolTier tier : ToolTiers.TIERS) {
            tier.tune(tier.defaultDurability, tier.defaultAttackDamage,
                    tier.defaultAttackSpeed, tier.defaultPickaxeMining);
            ToolSpec spec = tools == null ? null : tools.get(tier.id);
            if (spec != null) tier.tune(spec.durability, spec.attackDamage, spec.attackSpeed, spec.pickaxeMining);
        }
    }
}
