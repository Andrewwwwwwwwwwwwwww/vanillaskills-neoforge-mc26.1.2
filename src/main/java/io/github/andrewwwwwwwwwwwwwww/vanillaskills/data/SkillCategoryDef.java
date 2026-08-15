package io.github.andrewwwwwwwwwwwwwww.vanillaskills.data;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillCategory;

/**
 * Datapack definition of a skill-tree lane, read from
 * {@code data/&lt;namespace&gt;/vanillaskills/skill_category/&lt;name&gt;.json}.
 *
 * <pre>{@code
 * { "id": "health", "title": "Vitality", "icon": "minecraft:golden_apple", "slot": 10 }
 * }</pre>
 *
 * <p>{@code title} is an English fallback — the lane name is translated per player via
 * {@code vanillaskills.lane.<id>}, so a pack adding lanes can ship its own translations.
 */
public class SkillCategoryDef implements VsEntry {

    /** Unique lane id. Nodes join a lane by naming this. */
    public String id;

    /** English fallback name. */
    public String title;

    /** Item shown as the lane's icon on the lane-select screen. */
    public String icon = "minecraft:book";

    /** Position on the lane-select screen. */
    public int slot;

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean normalize() {
        if (id == null || id.isBlank()) return false;
        if (title == null || title.isBlank()) title = id;
        if (icon == null || icon.isBlank()) icon = "minecraft:book";
        if (slot < 0) slot = 0;
        return true;
    }

    /** Convert to the runtime type the GUI and tree use. */
    public SkillCategory toCategory() {
        return new SkillCategory(id, title, icon, slot);
    }
}
