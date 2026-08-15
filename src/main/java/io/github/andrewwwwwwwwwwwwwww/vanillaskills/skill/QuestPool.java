package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.data.VsContent;

import java.util.List;

/**
 * Access to the datapack-defined quest pools.
 *
 * <p>Both pools live in {@code data/&lt;namespace&gt;/vanillaskills/quest/}, split by their
 * {@code "pool"} field — see {@link io.github.andrewwwwwwwwwwwwwww.vanillaskills.data.QuestDef}. The
 * mod ships its own as a bundled datapack, so a pack can add, reprice or replace quests without a
 * code change.
 *
 * <p>Player progress is keyed by quest id rather than list position, so quests can be added, removed
 * or reordered freely — the constraint that forced the old APPEND-ONLY rule on the hardcoded lists.
 */
public final class QuestPool {
    private QuestPool() {}

    /**
     * How many starter quests the board can show at once.
     *
     * <p>The starter GUI is a 9x5 chest with a centred 5-wide by 3-row block of quest slots, and
     * graduation requires claiming <i>every</i> starter quest — so a pack shipping more than this
     * would create unclaimable quests and an unreachable graduation. {@link VsContent} truncates the
     * pool to this length (with a warning) rather than letting that soft-lock happen.
     */
    public static final int STARTER_CAPACITY = 15;

    /**
     * The fixed starter board: every entry is active at once for new players, each completable once.
     * Finishing all of them graduates the player to the rotating board. Never longer than
     * {@link #STARTER_CAPACITY}.
     */
    public static List<Quest> starter() {
        return VsContent.starterQuests();
    }

    /** The rotating pool the shared bounty board deals from, weighted by {@link Quest#weight()}. */
    public static List<Quest> all() {
        return VsContent.rotatingQuests();
    }

    /**
     * Look up a quest by its stable id, across both pools.
     *
     * <p>Returns null for an id that no longer exists — a pack removing a quest that someone had
     * banked progress on. Callers treat that as "that quest is gone" rather than an error.
     */
    public static Quest byId(String id) {
        return VsContent.quest(id);
    }

    /**
     * FROZEN pre-2.0 orderings, used only to translate a legacy save's integer keys into quest ids.
     *
     * <p>Before 2.0 the pools were hardcoded lists and progress was stored as positions in them. Those
     * numbers are only meaningful against the ordering that wrote them, so the migration must not read
     * the live datapack pools — a pack that reorders or replaces quests would otherwise silently remap
     * players onto the wrong ones. Never edit these; they describe history, not content.
     */
    public static final List<String> LEGACY_STARTER_IDS = List.of(
            "gather_32_sticks", "gather_64_cobblestone", "gather_16_coal", "bake_16_bread",
            "gather_16_leather", "smelt_32_copper_ingots", "smelt_16_iron_ingots", "smelt_8_gold_ingots",
            "gather_4_diamonds", "slay_10_zombies", "slay_10_skeletons", "slay_5_creepers",
            "gather_16_bones", "gather_8_string", "unlock_10_skills");

    /** @see #LEGACY_STARTER_IDS */
    public static final List<String> LEGACY_ALL_IDS = List.of(
            "gather_32_iron_ingots", "gather_16_gold_ingots", "gather_10_diamonds",
            "gather_64_copper_ingots", "gather_64_wheat", "gather_24_leather", "gather_16_gunpowder",
            "gather_32_redstone", "gather_32_emeralds", "gather_64_coal", "gather_8_ender_pearls",
            "gather_32_bones", "catch_16_cod", "catch_16_salmon", "catch_6_tropical_fish",
            "catch_4_pufferfish", "slay_25_zombies", "slay_25_skeletons", "slay_15_creepers",
            "slay_20_spiders", "slay_10_endermen", "slay_8_blazes", "slay_15_piglins",
            "slay_15_drowned", "slay_8_witches", "slay_50_hostile_mobs",
            "daily_bonus_free_quest_shards", "gather_64_carrots", "gather_4_honey_bottles",
            "gather_24_amethyst_shards", "gather_32_string", "slay_15_slimes", "slay_10_pillagers",
            "slay_8_guardians", "gather_64_pumpkins", "gather_64_melon_slices", "gather_32_sugar_cane",
            "gather_16_sweet_berries", "gather_16_cocoa_beans", "gather_32_nether_wart",
            "gather_8_chorus_fruit", "gather_16_blaze_rods", "gather_32_nether_quartz",
            "gather_4_ghast_tears", "gather_8_magma_cream", "gather_1_ancient_debris",
            "gather_24_prismarine_shards", "gather_3_nautilus_shells", "gather_32_kelp",
            "gather_8_ink_sacs", "gather_32_raw_iron", "gather_48_deepslate", "gather_8_obsidian",
            "gather_24_lapis_lazuli", "travel_5_000_blocks_on_foot", "swim_1_500_blocks",
            "jump_800_times");
}
