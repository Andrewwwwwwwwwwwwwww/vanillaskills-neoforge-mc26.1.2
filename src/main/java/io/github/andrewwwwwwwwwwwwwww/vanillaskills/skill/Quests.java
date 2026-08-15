package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-player bounty progress. New players work the FIXED starter board — every quest in
 * {@link QuestPool#starter()} is always active and completable once, in any order; progress never
 * rotation-resets. Completing ALL of them graduates the player to the shared universal board
 * (3 rotating quests from {@link QuestPool#all()} on the 5-hour timer).
 */
public final class Quests {
    private Quests() {}

    /** LEGACY knob (pre-1.2.0 graduation count). Graduation is now "finish every starter quest";
     *  kept so old gameplay.json files still parse. */
    public static int GRADUATE_AT = 15;

    /** Data-format version for the fixed-starter system (1.2.0). */
    private static final int STARTER_VERSION = 2;

    /**
     * Migrates old saves and resets per-rotation progress when the shared board rolls over.
     * Starter progress is rotation-independent, so pre-graduation players only get the migration.
     */
    public static void sync(ServerPlayer player) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        boolean changed = false;

        // 2.0 migration: quest progress moved from integer keys to stable quest ids. Runs before the
        // rest of sync so everything below already sees ids.
        if (data.questDataVersion < PlayerSkillData.QUEST_ID_VERSION) {
            migrateQuestIds(data);
            data.questDataVersion = PlayerSkillData.QUEST_ID_VERSION;
            changed = true;
        }

        // 1.2.0 migration: the starter board changed from 3 random rotating quests to the fixed
        // 15 — players mid-starter are fully reset (user decision); graduated players are untouched.
        if (data.starterVersion < STARTER_VERSION) {
            data.starterVersion = STARTER_VERSION;
            if (!data.graduated) {
                data.questsCompleted = 0;
                data.questKills.clear();
                data.questClaimed.clear();
                data.starterDone.clear();
                data.starterKills.clear();
            }
            data.starterSlots = new int[0]; // legacy field, no longer read
            changed = true;
        }

        long rotation = VanillaSkills.QUESTS.rotationId();
        if (data.graduated && data.questRotation != rotation) {
            data.questRotation = rotation;
            data.questKills.clear();
            data.questClaimed.clear();
            data.questStatBase.clear();
            data.questStatNotified.clear();
            changed = true;
        }
        // For each active STAT quest: snapshot its baseline once (so progress counts only from now, not
        // lifetime), then ping the player the first time it completes. STAT quests tally silently in the
        // background, so without this ping there's no feedback while walking/swimming/jumping.
        if (data.graduated) {
            List<Quest> active = VanillaSkills.QUESTS.active();
            for (int i = 0; i < active.size(); i++) {
                Quest q = active.get(i);
                if (q.type() != Quest.Type.STAT) continue;
                if (!data.questStatBase.containsKey(q.id())) {
                    data.questStatBase.put(q.id(), readStat(player, q.target()));
                    changed = true;
                }
                if (!data.questStatNotified.contains(q.id()) && !data.questClaimed.contains(q.id())
                        && progress(player, i) >= q.amount()) {
                    data.questStatNotified.add(q.id());
                    player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.ready","Bounty ready to claim: %s — /quests", q.title())).withStyle(ChatFormatting.GREEN));
                    changed = true;
                }
            }
        }
        if (changed) VanillaSkills.PLAYERS.save(player.getUUID());
    }

    /** The player's current quest list: all fixed starters pre-graduation, else the shared 3. */
    public static List<Quest> activeFor(ServerPlayer player) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        return data.graduated ? VanillaSkills.QUESTS.active() : QuestPool.starter();
    }

    public static Quest questFor(ServerPlayer player, int index) {
        List<Quest> a = activeFor(player);
        return (index >= 0 && index < a.size()) ? a.get(index) : null;
    }

    public static boolean isGraduated(ServerPlayer player) {
        return VanillaSkills.PLAYERS.get(player.getUUID()).graduated;
    }

    /** Starter quests completed so far (pre-graduation UI). */
    public static int graduationProgress(ServerPlayer player) {
        return VanillaSkills.PLAYERS.get(player.getUUID()).starterDone.size();
    }

    /** The claimed-set for the player's current board (starter claims are permanent). */
    private static Set<String> claimedSet(PlayerSkillData data) {
        return data.graduated ? data.questClaimed : data.starterDone;
    }

    /** The kill-progress map for the player's current board. */
    private static Map<String, Integer> killMap(PlayerSkillData data) {
        return data.graduated ? data.questKills : data.starterKills;
    }

    /** The stable id of the quest in a board slot, or null if that slot is empty. */
    private static String idAt(ServerPlayer player, int index) {
        Quest q = questFor(player, index);
        return q == null ? null : q.id();
    }

    // ---- 2.0 migration: quest progress moved from integer keys to stable quest ids ----

    /**
     * Rewrites a pre-2.0 player's quest progress onto quest ids.
     *
     * <p>The two families of key meant different things and must be remapped against different lists:
     * {@code starterDone}/{@code starterKills} were positions in the old hardcoded starter list and are
     * permanent, whereas the four rotating collections were <b>board slot numbers</b> for whatever
     * rotation was live when they were written — so those resolve against the live board, which is
     * itself only meaningful for the rotation that is currently dealt.
     *
     * <p>The starter side uses {@link QuestPool#LEGACY_STARTER_IDS}, the frozen pre-2.0 ordering,
     * rather than the datapack pool: the old numbers only mean anything against the ordering that
     * wrote them, so a pack that reorders the starter board must not be able to shuffle anyone's
     * completed quests.
     *
     * <p>Keys that are already ids pass through untouched, so running this twice is harmless.
     */
    private static void migrateQuestIds(PlayerSkillData data) {
        List<String> legacy = QuestPool.LEGACY_STARTER_IDS;
        data.starterDone = remapSet(data.starterDone, i -> legacyId(legacy, i));
        data.starterKills = remapMap(data.starterKills, i -> legacyId(legacy, i));

        List<Quest> board = VanillaSkills.QUESTS.active();
        data.questClaimed = remapSet(data.questClaimed, i -> idIn(board, i));
        data.questStatNotified = remapSet(data.questStatNotified, i -> idIn(board, i));
        data.questKills = remapMap(data.questKills, i -> idIn(board, i));
        data.questStatBase = remapMap(data.questStatBase, i -> idIn(board, i));
    }

    private static String idIn(List<Quest> list, int index) {
        return (index >= 0 && index < list.size()) ? list.get(index).id() : null;
    }

    /** Same lookup against a frozen list of raw ids (the pre-2.0 orderings). */
    private static String legacyId(List<String> ids, int index) {
        return (index >= 0 && index < ids.size()) ? ids.get(index) : null;
    }

    /** Old keys were integers; anything non-numeric is already an id and is kept as-is. */
    private static String migrateKey(String key, java.util.function.IntFunction<String> lookup) {
        try {
            return lookup.apply(Integer.parseInt(key.trim()));
        } catch (NumberFormatException e) {
            return key;
        }
    }

    private static Set<String> remapSet(Set<String> old, java.util.function.IntFunction<String> lookup) {
        Set<String> out = new java.util.LinkedHashSet<>();
        for (String key : old) {
            String id = migrateKey(key, lookup);
            if (id != null) out.add(id);
        }
        return out;
    }

    private static <V> Map<String, V> remapMap(Map<String, V> old, java.util.function.IntFunction<String> lookup) {
        Map<String, V> out = new java.util.HashMap<>();
        for (Map.Entry<String, V> entry : old.entrySet()) {
            String id = migrateKey(entry.getKey(), lookup);
            if (id != null) out.put(id, entry.getValue());
        }
        return out;
    }

    /** Called when a player kills something — advances any matching active KILL quests on their board. */
    public static void onKill(ServerPlayer killer, Entity dead) {
        sync(killer);
        PlayerSkillData data = VanillaSkills.PLAYERS.get(killer.getUUID());
        List<Quest> active = activeFor(killer);
        Set<String> claimed = claimedSet(data);
        Map<String, Integer> kills = killMap(data);
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
        boolean hostile = dead instanceof Enemy;
        boolean changed = false;
        for (int i = 0; i < active.size(); i++) {
            Quest q = active.get(i);
            if (q.type() != Quest.Type.KILL || claimed.contains(q.id())) continue;
            boolean match = q.target().equals(Quest.ANY_HOSTILE) ? hostile : q.target().equals(id);
            if (!match) continue;
            int cur = kills.getOrDefault(q.id(), 0);
            if (cur >= q.amount()) continue;
            kills.put(q.id(), cur + 1);
            changed = true;
            if (cur + 1 == q.amount()) {
                killer.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(killer,"vanillaskills.msg.ready","Bounty ready to claim: %s — /quests", q.title()))
                        .withStyle(ChatFormatting.GREEN));
            }
        }
        if (changed) VanillaSkills.PLAYERS.save(killer.getUUID());
    }

    public static boolean isClaimed(ServerPlayer player, int index) {
        String id = idAt(player, index);
        return id != null && claimedSet(VanillaSkills.PLAYERS.get(player.getUUID())).contains(id);
    }

    /** Progress toward the quest (kills done, items held, or skills unlocked), capped at the amount. */
    public static int progress(ServerPlayer player, int index) {
        Quest q = questFor(player, index);
        if (q == null) return 0;
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        return switch (q.type()) {
            case FREEBIE -> q.amount(); // always ready
            case SKILL -> Math.min(q.amount(), data.unlocked.size());
            case KILL -> Math.min(q.amount(), killMap(data).getOrDefault(q.id(), 0));
            case GATHER -> Math.min(q.amount(), countItem(player, q.target()));
            case STAT -> {
                long base = data.questStatBase.getOrDefault(q.id(), readStat(player, q.target()));
                long delta = Math.max(0, readStat(player, q.target()) - base);
                long done = q.target().contains("_one_cm") ? delta / 100 : delta; // cm -> blocks
                yield (int) Math.min(q.amount(), done);
            }
        };
    }

    /** Attempt to claim a quest's reward; messages the player with the result. */
    public static void claim(ServerPlayer player, int index) {
        sync(player);
        Quest q = questFor(player, index);
        if (q == null) return;
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        String questId = q.id();
        Set<String> claimed = claimedSet(data);
        if (claimed.contains(questId)) {
            player.sendSystemMessage(Component.literal(data.graduated
                    ? "You've already claimed this bounty."
                    : "You've already completed this starter quest.").withStyle(ChatFormatting.RED));
            return;
        }
        switch (q.type()) {
            case KILL -> {
                int cur = killMap(data).getOrDefault(questId, 0);
                if (cur < q.amount()) {
                    player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.not_done","Not done yet: %d/%d", cur, q.amount())).withStyle(ChatFormatting.RED));
                    return;
                }
            }
            case GATHER -> {
                int have = countItem(player, q.target());
                if (have < q.amount()) {
                    player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.need_more","You need %d more.", q.amount() - have)).withStyle(ChatFormatting.RED));
                    return;
                }
                removeItem(player, q.target(), q.amount());
            }
            case SKILL -> {
                int have = data.unlocked.size();
                if (have < q.amount()) {
                    int need = q.amount() - have;
                    player.sendSystemMessage(Component.literal(need == 1
                            ? io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.need_skills_one","Unlock 1 more skill in the skill tree (/skill).")
                            : io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.need_skills_many","Unlock %d more skills in the skill tree (/skill).", need)).withStyle(ChatFormatting.RED));
                    return;
                }
            }
            case STAT -> {
                if (progress(player, index) < q.amount()) {
                    player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.not_done_yet","Not done yet: %d/%d", progress(player, index), q.amount())).withStyle(ChatFormatting.RED));
                    return;
                }
            }
            case FREEBIE -> { /* nothing to verify or consume */ }
        }
        claimed.add(questId);
        VanillaSkills.PLAYERS.addQuestShards(player, q.reward());
        player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.complete","Bounty complete: %s  +%d Quest Shards", q.title(), q.reward())).withStyle(ChatFormatting.GOLD));

        if (!data.graduated) {
            data.questsCompleted++;
            int total = QuestPool.starter().size();
            int done = data.starterDone.size();
            if (done >= total) {
                graduate(player, data);
            } else {
                player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.starter_progress","Starter quests: %d/%d complete.", done, total)).withStyle(ChatFormatting.GRAY));
            }
        }
        VanillaSkills.PLAYERS.save(player.getUUID());
    }

    private static void graduate(ServerPlayer player, PlayerSkillData data) {
        data.graduated = true;
        data.questKills.clear();
        data.questClaimed.clear();
        data.questRotation = VanillaSkills.QUESTS.rotationId(); // start the universal board fresh
        player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.graduated","★ You've graduated to the main Bounty Board! Every quest is now available.")).withStyle(ChatFormatting.GOLD));
    }

    /** Op: force a player onto the universal board. */
    public static void forceGraduate(ServerPlayer player) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        data.starterVersion = STARTER_VERSION;
        graduate(player, data);
        VanillaSkills.PLAYERS.save(player.getUUID());
    }

    /** Op: send a player back to the starter board (fresh). */
    public static void resetToStarter(ServerPlayer player) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        data.graduated = false;
        data.questsCompleted = 0;
        data.questKills.clear();
        data.questClaimed.clear();
        data.starterDone.clear();
        data.starterKills.clear();
        data.starterVersion = STARTER_VERSION;
        data.questRotation = VanillaSkills.QUESTS.rotationId();
        VanillaSkills.PLAYERS.save(player.getUUID());
    }

    public static Item item(String id) {
        Identifier loc = Identifier.tryParse(id);
        if (loc == null) return Items.PAPER;
        return BuiltInRegistries.ITEM.get(loc).map(Holder::value).orElse(Items.PAPER);
    }

    /** Sum of the given custom stats (comma-separated ids, e.g. "minecraft:walk_one_cm") for the player. */
    private static long readStat(ServerPlayer player, String targets) {
        long sum = 0;
        for (String id : targets.split(",")) {
            Identifier loc = Identifier.tryParse(id.trim());
            if (loc == null) continue;
            // StatType keys its stats in an IdentityHashMap, so getValue only matches the EXACT registered
            // Identifier object — a freshly parsed (equal but different) one silently returns 0. Resolve
            // the id to the canonical registered object via the custom-stat registry first.
            Identifier canonical = BuiltInRegistries.CUSTOM_STAT.get(loc).map(Holder::value).orElse(null);
            if (canonical != null) sum += player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM, canonical);
        }
        return sum;
    }

    private static int countItem(ServerPlayer player, String id) {
        Item item = item(id);
        int n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && !Markers.isOurs(s)) n += s.getCount(); // don't count our marked variants
        }
        return n;
    }

    private static void removeItem(ServerPlayer player, String id, int amount) {
        Item item = item(id);
        int remaining = amount;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && !Markers.isOurs(s)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }
}
