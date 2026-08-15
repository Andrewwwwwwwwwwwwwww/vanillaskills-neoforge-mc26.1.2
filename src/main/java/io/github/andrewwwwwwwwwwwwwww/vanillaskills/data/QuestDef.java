package io.github.andrewwwwwwwwwwwwwww.vanillaskills.data;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quest;

import java.util.Locale;

/**
 * Datapack definition of a {@link Quest}, read from
 * {@code data/&lt;namespace&gt;/vanillaskills/quest/&lt;name&gt;.json}.
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "id": "gather_32_iron_ingots",
 *   "pool": "rotating",
 *   "type": "gather",
 *   "target": "minecraft:iron_ingot",
 *   "amount": 32,
 *   "reward": 3,
 *   "title": "Gather 32 Iron Ingots",
 *   "weight": 10
 * }
 * }</pre>
 *
 * <p>⚠ <b>{@code id} is load-bearing.</b> Player quest progress — starter completions, kill tallies,
 * claimed bounties, STAT baselines — is all keyed by it, as is the dealt board in
 * {@code questboard.json}. Changing an id abandons every player's progress on that quest; changing
 * anything else (title, amount, reward) keeps it. Omit {@code id} and it is derived from the title
 * the same way the pre-2.0 code did ({@link Quest#slug}), which is what keeps the shipped defaults
 * compatible with existing saves.
 *
 * <p>{@code title} is an English fallback; the display text is translated per player via
 * {@code vanillaskills.quest.<id>}, so a pack adding quests can ship its own translations.
 */
public class QuestDef implements VsEntry {

    /** Stable id. Optional — derived from {@link #title} when absent. See the class note. */
    public String id;

    /** {@code starter} (fixed beginner board, each completable once) or {@code rotating} (the shared
     *  bounty board that re-rolls on a timer). Defaults to rotating. */
    public String pool = "rotating";

    /** {@code gather}, {@code kill}, {@code freebie}, {@code skill} or {@code stat}. */
    public String type = "gather";

    /** Item id (gather), entity-type id or {@code any_hostile} (kill), or a comma-separated list of
     *  custom-stat ids (stat). Unused by {@code freebie} and {@code skill}. */
    public String target = "";

    /** How many to gather/kill, skills to unlock, or — for {@code stat} distances — BLOCKS. */
    public int amount = 1;

    /** Quest Shards paid out on claim. */
    public int reward = 1;

    /** English fallback title. */
    public String title;

    /** Relative likelihood of being dealt to the rotating board (higher = more common). Ignored by
     *  the starter pool, which is always fully active. */
    public int weight = 10;

    /** LEGACY themed-as-late-game marker; kept so the shipped defaults round-trip, never read. */
    public boolean lategame = false;

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean normalize() {
        if (title == null || title.isBlank()) {
            // A quest with neither a title nor an id has nothing to render or key progress by.
            if (id == null || id.isBlank()) return false;
            title = id;
        }
        if (id == null || id.isBlank()) id = Quest.slug(title);
        if (questType() == null) return false;
        if (starter() == null) return false;
        if (target == null) target = "";
        // gather/kill/stat are meaningless without something to count.
        if (target.isBlank() && questType() != Quest.Type.FREEBIE && questType() != Quest.Type.SKILL) return false;
        if (amount < 1) amount = 1;
        if (reward < 0) reward = 0;
        if (weight < 1) weight = 1;
        return true;
    }

    /** The parsed {@link Quest.Type}, or null if {@link #type} is not one we know. */
    public Quest.Type questType() {
        if (type == null) return null;
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "gather" -> Quest.Type.GATHER;
            case "kill" -> Quest.Type.KILL;
            case "freebie" -> Quest.Type.FREEBIE;
            case "skill" -> Quest.Type.SKILL;
            case "stat" -> Quest.Type.STAT;
            default -> null;
        };
    }

    /** TRUE for the starter board, FALSE for the rotating board, null if {@link #pool} is unknown. */
    public Boolean starter() {
        if (pool == null) return null;
        return switch (pool.toLowerCase(Locale.ROOT)) {
            case "starter" -> Boolean.TRUE;
            case "rotating", "main", "bounty" -> Boolean.FALSE;
            default -> null;
        };
    }

    /** Convert to the runtime record. Only valid after {@link #normalize()}. */
    public Quest toQuest() {
        return new Quest(id, questType(), target, amount, reward, title, weight, lategame);
    }
}
