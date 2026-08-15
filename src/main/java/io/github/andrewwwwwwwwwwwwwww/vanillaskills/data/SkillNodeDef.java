package io.github.andrewwwwwwwwwwwwwww.vanillaskills.data;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillEffect;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Datapack definition of one skill-tree node, read from
 * {@code data/&lt;namespace&gt;/vanillaskills/skill_node/&lt;name&gt;.json}.
 *
 * <pre>{@code
 * {
 *   "id": "health_6",
 *   "title": "Vitality VI",
 *   "description": ["+2 hearts"],
 *   "icon": "minecraft:golden_apple",
 *   "weight": 14,
 *   "requires": ["health_5"],
 *   "category": "health",
 *   "slot": 29,
 *   "effects": [ { "type": "attribute", "attribute": "minecraft:max_health", … } ]
 * }
 * }</pre>
 *
 * <p><b>Packs author {@code weight}, not a cost.</b> The absolute number is meaningless on its own — what
 * matters is a node's weight relative to its siblings. The real Skill Shard cost is derived at load time by
 * scaling every weight so the whole tree sums to exactly the total earnable shards (P), which is what makes
 * "complete every advancement and you can afford the entire tree" hold no matter what a pack does.
 *
 * <p>Keeping the authored weight separate from the derived cost also fixes a latent bug: the old code scaled
 * {@code cost} in place, so a second {@code /reload} scaled the already-scaled value and the economy drifted.
 *
 * <p><b>{@code id} is load-bearing.</b> Player progress stores unlocked skills as a set of these ids, so
 * renaming one silently revokes that skill for everyone who had it. Add and remove freely; rename with care.
 */
public class SkillNodeDef implements VsEntry {

    /** Unique node id. ⚠ Referenced by saved player progress — see the class note. */
    public String id;

    /** English fallback title; translated via {@code vanillaskills.skill.<id>}. */
    public String title;

    /** Tooltip lines. */
    public List<String> description = new ArrayList<>();

    /** Item shown as the node's icon. */
    public String icon = "minecraft:stone";

    /** Relative price. Scaled against the total earnable shards at load; NOT an absolute cost. */
    public int weight = 1;

    /** Lifetime-earned Skill Shards required before this node may be unlocked at all. */
    public int minEarned = 0;

    /** {@code "skill"} (Skill Shards) or {@code "quest"} (Quest Shards). */
    public String currency = "skill";

    /** Node ids that must be unlocked first. */
    public List<String> requires = new ArrayList<>();

    /** Lane this node belongs to; must match a {@link SkillCategoryDef} id. */
    public String category;

    /** Position within its lane's view. */
    public int slot;

    /** What unlocking it does. */
    public List<SkillEffect> effects = new ArrayList<>();

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean normalize() {
        if (id == null || id.isBlank()) return false;
        if (title == null || title.isBlank()) title = id;
        if (icon == null || icon.isBlank()) icon = "minecraft:stone";
        if (currency == null || currency.isBlank()) currency = "skill";
        if (description == null) description = new ArrayList<>();
        if (requires == null) requires = new ArrayList<>();
        if (effects == null) effects = new ArrayList<>();
        if (weight < 1) weight = 1;
        if (minEarned < 0) minEarned = 0;
        if (slot < 0) slot = 0;
        return true;
    }

    /**
     * Convert to the runtime node.
     *
     * <p>{@code cost} is seeded from the weight and then overwritten by the economy pass. It is never left
     * as the raw weight in a running tree.
     */
    public SkillNode toNode() {
        SkillNode node = new SkillNode(id, title, category, slot, weight, icon);
        node.description = new ArrayList<>(description);
        node.minEarned = minEarned;
        node.currency = currency;
        node.requires = new ArrayList<>(requires);
        node.effects = new ArrayList<>(effects);
        node.normalize();
        return node;
    }
}
