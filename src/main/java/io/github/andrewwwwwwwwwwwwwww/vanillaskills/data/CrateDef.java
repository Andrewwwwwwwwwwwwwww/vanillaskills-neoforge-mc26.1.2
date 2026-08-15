package io.github.andrewwwwwwwwwwwwwww.vanillaskills.data;

import com.google.gson.annotations.SerializedName;

/**
 * Datapack definition of a crate, read from
 * {@code data/&lt;namespace&gt;/vanillaskills/crate/&lt;name&gt;.json}.
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "id": "iron",
 *   "name": "Iron Crate",
 *   "color": "#D8D8D8",
 *   "loot_table": "vanillaskills:crate/iron"
 * }
 * }</pre>
 *
 * <p>This is only the crate's <i>identity</i>: what it is called, how it looks, and which loot table it
 * pays out. The contents are an ordinary datapack loot table, so a pack can change what a crate holds — or
 * point it at a different table entirely — without touching anything else.
 *
 * <p><b>How crates are obtained</b> is likewise data: the mod injects a single reference to
 * {@code vanillaskills:crate_fishing} into vanilla's fishing table, and that table decides which crate you
 * catch, how often, and where. Rarity, biome exclusivity and the Unboxing bonus all live there. Nothing
 * about drop rates belongs on this record — putting them here as well would mean two sources of truth.
 */
public class CrateDef implements VsEntry {

    /** Unique id. Also the model suffix ({@code vanillaskills:crate_<id>}) and the lang-key suffix. */
    public String id;

    /** English fallback name. Display text translates via {@code vanillaskills.crate.<id>}. */
    public String name;

    /** Name colour as {@code #RRGGBB}. */
    public String color = "#FFFFFF";

    /** Loot table rolled when the crate is opened. */
    @SerializedName("loot_table")
    public String lootTable;

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean normalize() {
        if (id == null || id.isBlank()) return false;
        if (lootTable == null || lootTable.isBlank()) return false;
        if (name == null || name.isBlank()) name = id;
        if (color == null || color.isBlank()) color = "#FFFFFF";
        return true;
    }

    /** The name colour as 0xRRGGBB, falling back to white if the string is malformed. */
    public int rgb() {
        try {
            return Integer.parseInt(color.startsWith("#") ? color.substring(1) : color, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
