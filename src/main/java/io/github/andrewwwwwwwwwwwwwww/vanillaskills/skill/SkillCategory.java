package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

/**
 * A lane/category on the main skill screen. Clicking its icon opens a view of just that lane's
 * nodes. Deserialized from skilltree.json.
 */
public class SkillCategory {
    public String id;
    public String title;
    public String icon = "minecraft:book";
    public int slot;   // position on the main lane-select screen

    public SkillCategory() {}

    public SkillCategory(String id, String title, String icon, int slot) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.slot = slot;
    }

    public void normalize() {
        if (title == null) title = id;
        if (icon == null) icon = "minecraft:book";
    }
}
