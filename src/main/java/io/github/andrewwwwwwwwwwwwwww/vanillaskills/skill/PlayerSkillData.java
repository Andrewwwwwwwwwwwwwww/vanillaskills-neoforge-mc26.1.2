package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player progression, stored at world/vanillaskills/players/&lt;uuid&gt;.json.
 */
public class PlayerSkillData {
    public int version = 1;
    public Set<String> unlocked = new LinkedHashSet<>();
    public int pointsAvailable = 0; // Skill Shards available to spend
    public int pointsEarned = 0;    // Skill Shards earned lifetime
    public int questShardsAvailable = 0; // Quest Shards available to spend in the shop
    public int questShardsEarned = 0;    // Quest Shards earned lifetime
    public float lastHealth = -1f; // health at last logout, restored after max-health modifiers reapply
    public boolean completionRewarded = false; // got the full-tree Dragon Ingot reward
    public boolean nightVisionDisabled = false; // /skill toggle nightvision (only relevant once unlocked)
    public boolean stepUpDisabled = false;      // /skill toggle stepup — also auto-suppressed while sneaking
    public Set<String> creditedAdvancements = new LinkedHashSet<>();
    public boolean initialized = false;

    // Bounty board progress for the current rotation, keyed by QUEST ID (was the board slot number
    // before 2.0 — see QUEST_ID_VERSION). Cleared on every rotation.
    public long questRotation = -1;
    public Map<String, Integer> questKills = new HashMap<>();
    public Set<String> questClaimed = new LinkedHashSet<>();
    // Repeatable STAT quests: baseline stat value snapshotted when the quest appears, so progress only
    // counts what you do DURING the current rotation. Reset each roll.
    public Map<String, Long> questStatBase = new HashMap<>();
    public Set<String> questStatNotified = new LinkedHashSet<>(); // STAT quests pinged as "ready"; reset per rotation

    // One-time Feats (structure discoveries, boss kills, entering the End). Permanent; never rotation-reset.
    public Set<String> featsDone = new LinkedHashSet<>();

    // Starter board: new players complete ALL fixed starter quests (QuestPool.starter()) to graduate
    // to the universal rotating board. Starter progress never rotation-resets.
    public int questsCompleted = 0;
    public boolean graduated = false;
    public int[] starterSlots = new int[0]; // LEGACY (pre-1.2.0 random starter board); unused, kept for old saves
    public Set<String> starterDone = new LinkedHashSet<>();      // claimed starter quest ids (one-time)
    public Map<String, Integer> starterKills = new HashMap<>();  // kill progress per starter quest id
    public int starterVersion = 0; // 2 = fixed-starter system (1.2.0 migration marker)

    /**
     * Save-format version for quest progress.
     *
     * <p>0/absent = pre-2.0, where the six quest collections above were keyed by an integer. Gson
     * writes map keys as strings and reads JSON numbers into strings, so an old file still parses —
     * the values just arrive as numeric strings like {@code "3"}, which {@link Quests} converts to
     * real quest ids on first sync.
     *
     * <p>⚠ The two families of index meant different things, which the migration has to respect:
     * {@code starterDone}/{@code starterKills} were indices into the pre-2.0 hardcoded starter list and are
     * permanent, while the four rotating collections were <b>board slot numbers</b> (0-5) that only
     * made sense for the current rotation.
     */
    public int questDataVersion = 0;
    public static final int QUEST_ID_VERSION = 1;

    public void normalize() {
        if (unlocked == null) unlocked = new LinkedHashSet<>();
        if (creditedAdvancements == null) creditedAdvancements = new LinkedHashSet<>();
        if (questKills == null) questKills = new HashMap<>();
        if (questClaimed == null) questClaimed = new LinkedHashSet<>();
        if (questStatBase == null) questStatBase = new HashMap<>();
        if (questStatNotified == null) questStatNotified = new LinkedHashSet<>();
        if (featsDone == null) featsDone = new LinkedHashSet<>();
        if (starterSlots == null) starterSlots = new int[0];
        if (starterDone == null) starterDone = new LinkedHashSet<>();
        if (starterKills == null) starterKills = new HashMap<>();
    }

    public boolean hasUnlocked(String id) {
        return unlocked.contains(id);
    }

    public void grantPoints(int amount) {
        pointsAvailable += amount;
        pointsEarned += amount;
    }

    public void grantQuestShards(int amount) {
        questShardsAvailable += amount;
        questShardsEarned += amount;
    }
}
