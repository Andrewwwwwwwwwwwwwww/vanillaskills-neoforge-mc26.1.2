package io.github.andrewwwwwwwwwwwwwww.vanillaskills.data;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Feat;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quest;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestPool;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestShop;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All datapack-loaded VanillaSkills content, refreshed on every datapack reload (server start and
 * {@code /reload}).
 *
 * <p>The mod ships its own defaults as a datapack inside its jar, so there is exactly one code path:
 * built-in content and pack content load the same way, and a pack can override either.
 *
 * <p>Fields are {@code volatile} and swapped wholesale for a fresh immutable list, so gameplay code
 * reading them on the server thread never sees a half-built collection.
 *
 * <p>⚠ The reload listener runs during the initial datapack load, which happens <b>before</b>
 * {@code SERVER_STARTED} assigns {@link VanillaSkills#server} — so nothing here may touch world state
 * or {@link VanillaSkills#worldDir()}.
 */
public final class VsContent {
    private VsContent() {}

    private static volatile List<Feat> feats = List.of();
    private static volatile List<CrateDef> crates = List.of();
    private static volatile List<SkillCategoryDef> skillCategories = List.of();
    private static volatile List<SkillNodeDef> skillNodes = List.of();
    private static volatile List<Quest> starterQuests = List.of();
    private static volatile List<Quest> rotatingQuests = List.of();
    private static volatile Map<String, Quest> questsById = Map.of();
    private static volatile List<QuestShop.ShopOffer> shopOffers = List.of();

    /** Re-read every content type from the datapacks. */
    public static void reload(ResourceManager manager) {
        feats = VsJsonLoader.load(manager, "feat", FeatDef.class).stream()
                .map(FeatDef::toFeat)
                .toList();
        crates = VsJsonLoader.load(manager, "crate", CrateDef.class);
        skillCategories = VsJsonLoader.load(manager, "skill_category", SkillCategoryDef.class);
        skillNodes = VsJsonLoader.load(manager, "skill_node", SkillNodeDef.class);
        loadQuests(manager);
        shopOffers = VsJsonLoader.load(manager, "shop_offer", ShopOfferDef.class).stream()
                .map(ShopOfferDef::toOffer)
                .toList();

        VanillaSkills.LOGGER.info(
                "Datapack content loaded: {} feat(s), {} crate(s), {} skill lane(s), {} skill node(s), "
                        + "{} starter + {} rotating quest(s), {} shop offer(s)",
                feats.size(), crates.size(), skillCategories.size(), skillNodes.size(),
                starterQuests.size(), rotatingQuests.size(), shopOffers.size());
    }

    /** Split the one {@code quest} content type into the two boards, and index both by id. */
    private static void loadQuests(ResourceManager manager) {
        List<Quest> starters = new ArrayList<>();
        List<Quest> rotating = new ArrayList<>();
        Map<String, Quest> index = new LinkedHashMap<>();
        for (QuestDef def : VsJsonLoader.load(manager, "quest", QuestDef.class)) {
            Quest quest = def.toQuest();
            (Boolean.TRUE.equals(def.starter()) ? starters : rotating).add(quest);
            index.put(quest.id(), quest);
        }
        // Graduation requires claiming every starter quest, and the board can only render so many —
        // so an oversized pool is trimmed rather than left to soft-lock everyone on it forever.
        if (starters.size() > QuestPool.STARTER_CAPACITY) {
            VanillaSkills.LOGGER.warn(
                    "{} starter quests defined but the starter board only holds {} — dropping the extras: {}",
                    starters.size(), QuestPool.STARTER_CAPACITY,
                    starters.subList(QuestPool.STARTER_CAPACITY, starters.size()).stream().map(Quest::id).toList());
            starters = new ArrayList<>(starters.subList(0, QuestPool.STARTER_CAPACITY));
        }
        starterQuests = List.copyOf(starters);
        rotatingQuests = List.copyOf(rotating);
        questsById = Map.copyOf(index);
    }

    /** Every loaded skill-tree lane, in declaration order. */
    public static List<SkillCategoryDef> skillCategories() {
        return skillCategories;
    }

    /** Every loaded skill node, in declaration order. */
    public static List<SkillNodeDef> skillNodes() {
        return skillNodes;
    }

    /** True once the skill tree has been read from the datapacks at least once. */
    public static boolean hasSkillTree() {
        return !skillNodes.isEmpty();
    }

    /** The fixed starter board, in declaration order. */
    public static List<Quest> starterQuests() {
        return starterQuests;
    }

    /** The rotating pool the shared bounty board deals from, in declaration order. */
    public static List<Quest> rotatingQuests() {
        return rotatingQuests;
    }

    /** The quest with this id from either pool, or null. */
    public static Quest quest(String id) {
        return id == null ? null : questsById.get(id);
    }

    /** The whole Quest Shop catalogue, in declaration order. */
    public static List<QuestShop.ShopOffer> shopOffers() {
        return shopOffers;
    }

    /** Every loaded feat, in declaration order. Never null; empty before the first reload. */
    public static List<Feat> feats() {
        return feats;
    }

    /** Every loaded crate, in declaration order. Never null; empty before the first reload. */
    public static List<CrateDef> crates() {
        return crates;
    }

    /** The crate with this id, or null. */
    public static CrateDef crate(String id) {
        for (CrateDef def : crates) {
            if (def.id.equals(id)) return def;
        }
        return null;
    }
}
