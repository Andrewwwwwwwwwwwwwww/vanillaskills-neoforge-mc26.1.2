package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import java.util.Locale;

/**
 * One bounty-board quest: gather a quantity of an item (turned in at the board), kill a number of a
 * mob type, a FREEBIE (instant free Quest Shards), SKILL (have unlocked N skill-tree nodes — used by
 * the starter board to teach the mod), or STAT (accumulate a vanilla statistic — e.g. distance walked
 * or jumps — counted from when the quest appeared, so it is repeatable each rotation). Rewards Quest Shards.
 *
 * @param id       stable identifier used to key player progress and the translation key. The
 *                 convenience constructors derive it from the title, which is why it matches
 *                 {@link io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang#questKey} exactly.
 *                 Datapack-defined quests will supply it explicitly.
 * @param target   item id (GATHER), entity-type id (KILL) or the literal "any_hostile", or a
 *                 comma-separated list of custom-stat ids (STAT, e.g. "minecraft:walk_one_cm");
 *                 unused for FREEBIE and SKILL.
 * @param weight   relative likelihood of being picked for the board (higher = more common).
 * @param lategame LEGACY (pre-1.2.0 starter filtering) — no longer read; kept as documentation of
 *                 which quests are late-game themed.
 */
public record Quest(String id, Type type, String target, int amount, int reward, String title,
                    int weight, boolean lategame) {
    public enum Type { GATHER, KILL, FREEBIE, SKILL, STAT }

    public static final String ANY_HOSTILE = "any_hostile";

    /** Convenience: a normal-weight (10), non-lategame quest with an id derived from the title. */
    public Quest(Type type, String target, int amount, int reward, String title) {
        this(slug(title), type, target, amount, reward, title, 10, false);
    }

    /** Convenience: explicit weight/lategame, with an id derived from the title. */
    public Quest(Type type, String target, int amount, int reward, String title, int weight, boolean lategame) {
        this(slug(title), type, target, amount, reward, title, weight, lategame);
    }

    /**
     * Turn a quest title into its stable id — "Gather 32 Emeralds" becomes "gather_32_emeralds".
     *
     * <p>⚠ Because the id is derived, <b>renaming a quest changes its id</b> and players lose progress
     * on that one quest. That was already true of its translation key, so the two stay in lockstep;
     * to reword a quest safely, keep the id by using the canonical constructor with the old slug.
     */
    public static String slug(String title) {
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
