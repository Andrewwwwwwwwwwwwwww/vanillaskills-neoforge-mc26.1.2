package io.github.andrewwwwwwwwwwwwwww.vanillaskills.data;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Feat;

import java.util.Locale;

/**
 * Datapack definition of a {@link Feat}, read from
 * {@code data/&lt;namespace&gt;/vanillaskills/feat/&lt;name&gt;.json}.
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "id": "dragonslayer",
 *   "type": "kill",
 *   "target": "minecraft:ender_dragon",
 *   "reward": 30,
 *   "icon": "minecraft:dragon_head",
 *   "title": "Dragonslayer",
 *   "desc": "Defeat the Ender Dragon"
 * }
 * }</pre>
 *
 * <p>{@code title} and {@code desc} are English fallbacks — the display text is translated per player
 * via {@code vanillaskills.feat.<id>} and {@code vanillaskills.feat.<id>.desc}, so a pack adding feats
 * can ship its own translations without touching the mod.
 */
public class FeatDef implements VsEntry {

    /** Unique id. Also the translation-key suffix and the value stored in the player's feat set. */
    public String id;

    /** {@code discover} (be inside a structure), {@code kill} (slay an entity type), or
     *  {@code dimension} (enter a dimension). */
    public String type = "discover";

    /** Structure id, entity-type id, or dimension id, matching {@link #type}. */
    public String target;

    /** For {@code discover} only: restrict the check to this dimension. Optional — a cheap early-out. */
    public String dimension;

    /** Quest Shards awarded the first time this feat is earned. */
    public int reward = 10;

    /** Item id shown in the Feats GUI. */
    public String icon = "minecraft:stone";

    /** English fallback title. */
    public String title;

    /** English fallback description. */
    public String desc = "";

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean normalize() {
        if (id == null || id.isBlank()) return false;
        if (target == null || target.isBlank()) return false;
        if (featType() == null) return false;
        if (title == null || title.isBlank()) title = id;
        if (icon == null || icon.isBlank()) icon = "minecraft:stone";
        if (desc == null) desc = "";
        if (dimension != null && dimension.isBlank()) dimension = null;
        if (reward < 0) reward = 0;
        return true;
    }

    /** The parsed {@link Feat.Type}, or null if {@link #type} is not one we know. */
    public Feat.Type featType() {
        if (type == null) return null;
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "discover" -> Feat.Type.DISCOVER;
            case "kill" -> Feat.Type.KILL;
            case "dimension" -> Feat.Type.DIMENSION;
            default -> null;
        };
    }

    /** Convert to the runtime record the gameplay code uses. Only valid after {@link #normalize()}. */
    public Feat toFeat() {
        return new Feat(id, featType(), target, dimension, reward, icon, title, desc);
    }
}
