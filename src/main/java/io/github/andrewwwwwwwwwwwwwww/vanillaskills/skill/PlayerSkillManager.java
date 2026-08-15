package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.PointsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns per-player skill data: loading/saving, point grants, unlocks, and applying effects.
 */
public class PlayerSkillManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<UUID, PlayerSkillData> cache = new ConcurrentHashMap<>();
    private PointsConfig points = new PointsConfig();
    private int totalEarnable = 0; // P = total points a completionist can earn (computed at start)

    public void setPointsConfig(PointsConfig points) {
        this.points = points;
    }

    public PointsConfig pointsConfig() {
        return points;
    }

    public PlayerSkillData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDisk);
    }

    // ---- lifecycle ----

    /** Called when a player joins: load, initialize once, (re)apply all effects. */
    public void onJoin(ServerPlayer player) {
        PlayerSkillData data = get(player.getUUID());
        SkillTree tree = VanillaSkills.TREE.tree();

        if (!data.initialized) {
            data.pointsAvailable += points.startingPoints;
            data.pointsEarned += points.startingPoints;
            if (tree.has(SkillTree.ROOT_ID)) data.unlocked.add(SkillTree.ROOT_ID);
            tallyExistingAdvancements(player, data);
            if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.STARTER_QUESTS) {
                data.graduated = true; // config: new players skip the starter board
            }
            data.initialized = true;
            save(player.getUUID());
        } else if (tree.has(SkillTree.ROOT_ID)) {
            data.unlocked.add(SkillTree.ROOT_ID);
        }

        applyAll(player);
        reevaluateAdvancements(player); // retroactively grant path/completion rewards

        // Restore health AFTER max-health modifiers are reapplied — vanilla clamps it to the base
        // max during load (before our transient modifiers exist), which would shrink the health bar.
        if (data.lastHealth > 0f) {
            player.setHealth(Math.min(player.getMaxHealth(), data.lastHealth));
        }
    }

    /** Record the player's health on logout so it can be restored after modifiers reapply on join. */
    public void onLeave(ServerPlayer player) {
        PlayerSkillData data = get(player.getUUID());
        data.lastHealth = player.getHealth();
        unload(player.getUUID());
    }

    /** Re-check lane-completion and full-tree advancements (e.g. for trees finished before the feature existed). */
    private void reevaluateAdvancements(ServerPlayer player) {
        PlayerSkillData data = get(player.getUUID());
        SkillTree tree = VanillaSkills.TREE.tree();
        for (SkillCategory cat : tree.categories()) {
            boolean any = false, full = true;
            for (SkillNode n : tree.nodesIn(cat.id)) {
                any = true;
                if (!data.hasUnlocked(n.id)) { full = false; break; }
            }
            if (any && full) { awardAdvancement(player, "vanillaskills:specialist", "path"); break; }
        }
        checkCompletionist(player, data);
    }

    private void awardAdvancement(ServerPlayer player, String id, String criterion) {
        MinecraftServer server = VanillaSkills.server;
        if (server == null) return;
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            if (holder.id().toString().equals(id)) {
                player.getAdvancements().award(holder, criterion);
                return;
            }
        }
    }

    /** Reapply all attribute effects (e.g. after respawn). */
    public void applyAll(ServerPlayer player) {
        PlayerSkillData data = get(player.getUUID());
        SkillTree tree = VanillaSkills.TREE.tree();
        for (String id : data.unlocked) {
            SkillNode node = tree.byId(id);
            if (node != null) SkillEffects.applyNode(player, node);
        }
        // The set of unlocked skills decides which of our recipes are visible in the book, so any time the
        // effects are reapplied the book is reconciled too — that covers unlocking, refunding and /skill reset.
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipeUnlocks.sync(player);
    }

    // ---- advancement points ----

    /** Called by the advancement mixin when an advancement completes. */
    public void onAdvancementCompleted(ServerPlayer player, String advancementId) {
        if (!isCounted(advancementId)) return;
        if (points.ignoreRecipeAdvancements && isRecipe(advancementId)) return;
        PlayerSkillData data = get(player.getUUID());
        if (data.creditedAdvancements.contains(advancementId)) return;

        AdvancementHolder holder = findHolder(advancementId);
        int amount = holder != null ? points.pointsFor(holder) : points.perAdvancement;
        data.creditedAdvancements.add(advancementId);
        if (amount > 0) {
            data.grantPoints(amount);
            player.sendSystemMessage(Component.literal(amount == 1 ? io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.gain_shard_one","+1 Skill Shard") : io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.gain_shard_many","+%d Skill Shards", amount))
                    .withStyle(ChatFormatting.GREEN));
        }
        save(player.getUUID());
    }

    private void tallyExistingAdvancements(ServerPlayer player, PlayerSkillData data) {
        MinecraftServer server = VanillaSkills.server;
        if (server == null) return;
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            String id = holder.id().toString();
            if (!isCounted(id)) continue;
            if (points.ignoreRecipeAdvancements && isRecipe(id)) continue;
            if (data.creditedAdvancements.contains(id)) continue;
            if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
                int amount = points.pointsFor(holder);
                data.creditedAdvancements.add(id);
                if (amount > 0) data.grantPoints(amount);
            }
        }
    }

    /** Finds a loaded advancement by id string, or null. */
    private static AdvancementHolder findHolder(String id) {
        MinecraftServer server = VanillaSkills.server;
        if (server == null) return null;
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            if (holder.id().toString().equals(id)) return holder;
        }
        return null;
    }

    /** Total points a player can ever earn: starting bonus + every counted, non-recipe advancement. */
    public int computeTotalEarnable() {
        int total = points.startingPoints;
        MinecraftServer server = VanillaSkills.server;
        if (server != null) {
            for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
                String id = holder.id().toString();
                if (!isCounted(id)) continue;
                if (points.ignoreRecipeAdvancements && isRecipe(id)) continue;
                total += points.pointsFor(holder);
            }
        }
        totalEarnable = total;
        VanillaSkills.LOGGER.info("Total earnable Skill Shards (P) = {}", total);
        return total;
    }

    public int totalEarnable() {
        return totalEarnable;
    }

    /** Total Skill Shards obtainable from our own (vanillaskills:) advancements. */
    public int customAdvancementTotal() {
        int total = 0;
        MinecraftServer server = VanillaSkills.server;
        if (server != null) {
            for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
                String id = holder.id().toString();
                if (!id.startsWith("vanillaskills:")) continue;
                if (points.ignoreRecipeAdvancements && isRecipe(id)) continue;
                total += points.pointsFor(holder);
            }
        }
        return total;
    }

    /** Total Skill Shards a left-click on this node would charge (it buys the node + locked prereqs). */
    public int chainCost(ServerPlayer player, String nodeId) {
        SkillTree tree = VanillaSkills.TREE.tree();
        PlayerSkillData data = get(player.getUUID());
        LinkedHashSet<String> chain = new LinkedHashSet<>();
        if (!resolveChain(tree, data, nodeId, chain, new HashSet<>())) {
            SkillNode n = tree.byId(nodeId);
            return n != null ? n.cost : 0;
        }
        int total = 0;
        for (String id : chain) {
            SkillNode n = tree.byId(id);
            if (n != null) total += n.cost;
        }
        return total;
    }

    /** Op command: wipe credited advancements and re-tally (e.g. after editing points.json). */
    public int recalc(ServerPlayer player) {
        PlayerSkillData data = get(player.getUUID());
        int before = data.pointsEarned;
        int spent = data.pointsEarned - data.pointsAvailable; // points already spent on unlocks
        data.creditedAdvancements.clear();
        // Seed with the starting bonus so a player with no advancements keeps their 5 starting points,
        // then re-tally earned from advancements; spent unlocks are preserved.
        data.pointsEarned = points.startingPoints;
        tallyExistingAdvancements(player, data);
        data.pointsAvailable = Math.max(0, data.pointsEarned - spent);
        save(player.getUUID());
        return data.pointsEarned - before;
    }

    /** Only vanilla Minecraft and our own VanillaSkills advancements award points (not datapacks like VanillaTweaks). */
    /** True if this advancement's namespace is one the points config pays out for. */
    private boolean isCounted(String advancementId) {
        int colon = advancementId.indexOf(':');
        String namespace = colon >= 0 ? advancementId.substring(0, colon) : "minecraft";
        java.util.List<String> allowed = points == null ? null : points.countedNamespaces;
        // A config written before this setting existed deserializes it as null; fall back to the
        // pre-config behaviour rather than silently paying out nothing.
        if (allowed == null || allowed.isEmpty()) {
            return namespace.equals("minecraft") || namespace.equals("vanillaskills");
        }
        return allowed.contains(namespace);
    }

    private static boolean isRecipe(String advancementId) {
        int colon = advancementId.indexOf(':');
        String path = colon >= 0 ? advancementId.substring(colon + 1) : advancementId;
        return path.startsWith("recipes/");
    }

    // ---- unlocking ----

    /** Attempt to unlock a node for the player; messages the player with the result. */
    public boolean unlock(ServerPlayer player, String nodeId) {
        SkillTree tree = VanillaSkills.TREE.tree();
        SkillNode node = tree.byId(nodeId);
        if (node == null) {
            player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.node_gone","That skill no longer exists.")).withStyle(ChatFormatting.RED));
            return false;
        }
        PlayerSkillData data = get(player.getUUID());
        if (data.hasUnlocked(nodeId)) return false;

        for (String req : node.requires) {
            if (!data.hasUnlocked(req)) {
                SkillNode reqNode = tree.byId(req);
                String reqName = reqNode != null ? reqNode.title : req;
                player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                        "vanillaskills.msg.requires", "Requires: %s", reqName)).withStyle(ChatFormatting.RED));
                return false;
            }
        }
        if (node.minEarned > 0 && data.pointsEarned < node.minEarned) {
            player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                    "vanillaskills.msg.gate_locked", "Unlocks after earning %d Skill Shards (you've earned %d).",
                    node.minEarned, data.pointsEarned)).withStyle(ChatFormatting.RED));
            return false;
        }
        String cur = currencyName(player, node);
        if (available(data, node) < node.cost) {
            player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                    "vanillaskills.msg.need_currency", "Not enough %s (need %d).", cur, node.cost))
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        deduct(data, node, node.cost);
        data.unlocked.add(nodeId);
        SkillEffects.applyNode(player, node);
        checkPathAdvancement(player, node, data);
        checkCompletionist(player, data);
        save(player.getUUID());
        player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.unlocked","Unlocked %s!", node.title)).withStyle(ChatFormatting.GREEN));
        return true;
    }

    /** Buy a node AND every locked prerequisite below it in one go, if the player can afford the whole chain. */
    public boolean unlockChain(ServerPlayer player, String nodeId) {
        SkillTree tree = VanillaSkills.TREE.tree();
        SkillNode target = tree.byId(nodeId);
        if (target == null) {
            player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.msg.node_gone","That skill no longer exists.")).withStyle(ChatFormatting.RED));
            return false;
        }
        PlayerSkillData data = get(player.getUUID());
        if (data.hasUnlocked(nodeId)) return false;

        LinkedHashSet<String> chain = new LinkedHashSet<>();
        if (!resolveChain(tree, data, nodeId, chain, new HashSet<>())) {
            player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                    "vanillaskills.msg.chain_unresolved", "This skill's requirements can't be resolved."))
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        int total = 0, maxGate = 0;
        for (String id : chain) {
            SkillNode n = tree.byId(id);
            if (n == null) continue;
            total += n.cost;
            maxGate = Math.max(maxGate, n.minEarned);
        }
        String cur = currencyName(player, target);
        if (maxGate > 0 && data.pointsEarned < maxGate) {
            player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                    "vanillaskills.msg.gate_locked", "Unlocks after earning %d Skill Shards (you've earned %d).",
                    maxGate, data.pointsEarned)).withStyle(ChatFormatting.RED));
            return false;
        }
        if (available(data, target) < total) {
            player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                    "vanillaskills.msg.chain_cost", "Not enough %s (need %d for the chain).", cur, total))
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        for (String id : chain) {
            SkillNode n = tree.byId(id);
            if (n == null || data.hasUnlocked(id)) continue;
            deduct(data, n, n.cost);
            data.unlocked.add(id);
            SkillEffects.applyNode(player, n);
            checkPathAdvancement(player, n, data);
        }
        checkCompletionist(player, data);
        save(player.getUUID());
        int count = chain.size();
        String msg = count == 1
                ? io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                        // Same three args as the plural form, so a translation can use %1$/%2$/%3$
                        // interchangeably between the two and reorder for its own grammar.
                        "vanillaskills.msg.unlocked_one", "Unlocked %1$d skill for %2$d %3$s.", count, total, cur)
                : io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                        "vanillaskills.msg.unlocked_many", "Unlocked %d skills for %d %s.", count, total, cur);
        player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GREEN));
        return true;
    }

    /** The display name of a node's currency, in the player's language. */
    private static String currencyName(net.minecraft.server.level.ServerPlayer player, SkillNode node) {
        return node.isQuestCurrency()
                ? io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, "vanillaskills.menu.quest_shards", "Quest Shards")
                : io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, "vanillaskills.menu.skill_shards", "Skill Shards");
    }

    /** The player's available balance in the node's currency. */
    private static int available(PlayerSkillData data, SkillNode node) {
        return node.isQuestCurrency() ? data.questShardsAvailable : data.pointsAvailable;
    }

    /** Deduct an amount in the node's currency. */
    private static void deduct(PlayerSkillData data, SkillNode node, int amount) {
        if (node.isQuestCurrency()) data.questShardsAvailable -= amount;
        else data.pointsAvailable -= amount;
    }

    /** Refund an amount in the node's currency. */
    private static void refund(PlayerSkillData data, SkillNode node, int amount) {
        if (node.isQuestCurrency()) data.questShardsAvailable += amount;
        else data.pointsAvailable += amount;
    }

    /** Refund a node and every unlocked node that depends on it (cascade), returning all their Shards. */
    public boolean refundChain(ServerPlayer player, String nodeId) {
        SkillTree tree = VanillaSkills.TREE.tree();
        SkillNode target = tree.byId(nodeId);
        if (target == null) return false;
        if (SkillTree.ROOT_ID.equals(nodeId)) {
            player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                    "vanillaskills.msg.base_no_refund", "The base node can't be refunded.")).withStyle(ChatFormatting.RED));
            return false;
        }
        PlayerSkillData data = get(player.getUUID());
        if (!data.hasUnlocked(nodeId)) return false;

        Set<String> remove = new LinkedHashSet<>();
        for (String uid : data.unlocked) {
            if (uid.equals(nodeId) || dependsOn(tree, uid, nodeId, new HashSet<>())) remove.add(uid);
        }
        int refunded = 0;
        for (String id : remove) {
            SkillNode n = tree.byId(id);
            if (n == null) continue;
            SkillEffects.removeNode(player, n);
            data.unlocked.remove(id);
            refund(data, n, n.cost);
            refunded += n.cost;
        }
        save(player.getUUID());
        int count = remove.size();
        String refundCur = currencyName(player, target);
        String refundMsg = count == 1
                ? io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                        "vanillaskills.msg.refunded_one", "Refunded 1 skill for %d %s.", refunded, refundCur)
                : io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                        "vanillaskills.msg.refunded_many", "Refunded %d skills for %d %s.", count, refunded, refundCur);
        player.sendSystemMessage(Component.literal(refundMsg).withStyle(ChatFormatting.YELLOW));
        return true;
    }

    /** DFS that adds {@code nodeId}'s locked prerequisites (prereqs first) then the node itself to {@code chain}. */
    private static boolean resolveChain(SkillTree tree, PlayerSkillData data, String nodeId,
                                        LinkedHashSet<String> chain, Set<String> visiting) {
        if (data.hasUnlocked(nodeId) || chain.contains(nodeId)) return true;
        if (!visiting.add(nodeId)) return false; // cycle
        SkillNode n = tree.byId(nodeId);
        if (n == null) return false;
        for (String req : n.requires) {
            if (!resolveChain(tree, data, req, chain, visiting)) return false;
        }
        chain.add(nodeId);
        visiting.remove(nodeId);
        return true;
    }

    /** True if {@code nodeId} (transitively) requires {@code targetId}. */
    private static boolean dependsOn(SkillTree tree, String nodeId, String targetId, Set<String> seen) {
        SkillNode n = tree.byId(nodeId);
        if (n == null || !seen.add(nodeId)) return false;
        for (String req : n.requires) {
            if (req.equals(targetId) || dependsOn(tree, req, targetId, seen)) return true;
        }
        return false;
    }

    /** Grants the "Specialist" advancement when the player has fully unlocked a lane. */
    private void checkPathAdvancement(ServerPlayer player, SkillNode node, PlayerSkillData data) {
        if (node.category == null) return;
        SkillTree tree = VanillaSkills.TREE.tree();
        for (SkillNode n : tree.nodesIn(node.category)) {
            if (!data.hasUnlocked(n.id)) return; // lane not complete yet
        }
        MinecraftServer server = VanillaSkills.server;
        if (server == null) return;
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            if (holder.id().toString().equals("vanillaskills:specialist")) {
                player.getAdvancements().award(holder, "path");
                break;
            }
        }
    }

    /** When every node in the tree is unlocked, grant the completion advancement + 4 Dragon Ingots (once). */
    private void checkCompletionist(ServerPlayer player, PlayerSkillData data) {
        if (data.completionRewarded) return; // tracked in our own data, not the advancement state
        SkillTree tree = VanillaSkills.TREE.tree();
        if (tree.nodes.isEmpty()) return;
        for (SkillNode n : tree.nodes) {
            if (CraftingGate.laneDisabled(n.category)) continue; // hidden lanes don't count
            if (!data.hasUnlocked(n.id)) return; // tree not fully unlocked
        }

        data.completionRewarded = true;
        awardAdvancement(player, "vanillaskills:completionist", "complete");
        // (note: the loop above skips config-disabled lanes, so completion stays reachable)
        final int rewardIngots = 4;
        net.minecraft.world.item.ItemStack reward =
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonIngot.create();
        reward.setCount(rewardIngots);
        if (!player.getInventory().add(reward)) player.drop(reward, false);
        // Pass the count so translations can place the number where their language needs it.
        player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                "vanillaskills.msg.mastered", "Skill tree mastered! +%d Dragon Ingots", rewardIngots))
                .withStyle(ChatFormatting.GOLD));
        save(player.getUUID());
    }

    /** Op command: clear all unlocks and refund earned points. */
    public void reset(ServerPlayer player) {
        SkillTree tree = VanillaSkills.TREE.tree();
        PlayerSkillData data = get(player.getUUID());
        for (String id : data.unlocked) {
            SkillNode node = tree.byId(id);
            if (node != null) SkillEffects.removeNode(player, node);
        }
        data.unlocked.clear();
        data.pointsAvailable = data.pointsEarned;
        if (tree.has(SkillTree.ROOT_ID)) data.unlocked.add(SkillTree.ROOT_ID);
        save(player.getUUID());
    }

    public void addPoints(ServerPlayer player, int amount) {
        PlayerSkillData data = get(player.getUUID());
        data.grantPoints(amount);
        save(player.getUUID());
    }

    public void addQuestShards(ServerPlayer player, int amount) {
        PlayerSkillData data = get(player.getUUID());
        data.grantQuestShards(amount);
        save(player.getUUID());
    }

    public void setQuestShards(ServerPlayer player, int amount) {
        PlayerSkillData data = get(player.getUUID());
        int spent = data.questShardsEarned - data.questShardsAvailable;
        data.questShardsAvailable = Math.max(0, amount);
        data.questShardsEarned = data.questShardsAvailable + Math.max(0, spent);
        save(player.getUUID());
    }

    public int skillShards(ServerPlayer player) {
        return get(player.getUUID()).pointsAvailable;
    }

    public int questShards(ServerPlayer player) {
        return get(player.getUUID()).questShardsAvailable;
    }

    /**
     * Return Skill Shards to the player's spendable balance <b>without</b> crediting lifetime earned.
     *
     * <p>The counterpart to {@link #spendSkillShards}, used when banking a physical Unstable Skill Shard.
     * It deliberately does not call {@code grantPoints}: withdrawing only decrements {@code pointsAvailable},
     * so crediting {@code pointsEarned} on the way back would let a player inflate their lifetime total by
     * withdrawing and re-banking the same shard forever — and lifetime earned is what gates Night Vision at
     * P/3. Keeping it out also means shards dug out of the ground can be spent but cannot be farmed to skip
     * that gate.
     */
    public void depositSkillShards(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        PlayerSkillData data = get(player.getUUID());
        data.pointsAvailable += amount;
        save(player.getUUID());
    }

    /** Spend available Skill Shards (does not reduce lifetime earned). Returns false if too few. */
    public boolean spendSkillShards(ServerPlayer player, int amount) {
        PlayerSkillData data = get(player.getUUID());
        if (data.pointsAvailable < amount) return false;
        data.pointsAvailable -= amount;
        save(player.getUUID());
        return true;
    }

    /** Spend available Quest Shards (does not reduce lifetime earned). Returns false if too few. */
    public boolean spendQuestShards(ServerPlayer player, int amount) {
        PlayerSkillData data = get(player.getUUID());
        if (data.questShardsAvailable < amount) return false;
        data.questShardsAvailable -= amount;
        save(player.getUUID());
        return true;
    }

    /** One-way conversion: spend 3 Quest Shards per Skill Shard. Returns false if too few. */
    public boolean convertToSkillShards(ServerPlayer player, int skillAmount) {
        PlayerSkillData data = get(player.getUUID());
        int cost = skillAmount * QuestShop.CONVERT_RATIO;
        if (skillAmount <= 0 || data.questShardsAvailable < cost) return false;
        data.questShardsAvailable -= cost;
        data.grantPoints(skillAmount); // permanent: counts toward earned so resets keep it
        save(player.getUUID());
        return true;
    }

    public void setPoints(ServerPlayer player, int amount) {
        PlayerSkillData data = get(player.getUUID());
        int spent = data.pointsEarned - data.pointsAvailable;
        data.pointsAvailable = Math.max(0, amount);
        data.pointsEarned = data.pointsAvailable + Math.max(0, spent);
        save(player.getUUID());
    }

    // ---- persistence ----

    private Path playersDir() {
        MinecraftServer server = VanillaSkills.server;
        return server.getWorldPath(LevelResource.ROOT).resolve("vanillaskills").resolve("players");
    }

    private Path playerFile(UUID uuid) {
        return playersDir().resolve(uuid + ".json");
    }

    private PlayerSkillData loadFromDisk(UUID uuid) {
        Path file = playerFile(uuid);
        try {
            if (Files.exists(file)) {
                PlayerSkillData data = GSON.fromJson(Files.readString(file), PlayerSkillData.class);
                if (data != null) {
                    data.normalize();
                    return data;
                }
            }
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to load skill data for {}", uuid, e);
        }
        return new PlayerSkillData();
    }

    public void save(UUID uuid) {
        PlayerSkillData data = cache.get(uuid);
        if (data == null) return;
        try {
            Files.createDirectories(playersDir());
            Files.writeString(playerFile(uuid), GSON.toJson(data));
        } catch (IOException e) {
            VanillaSkills.LOGGER.error("Failed to save skill data for {}", uuid, e);
        }
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) save(uuid);
    }

    /** Save everything and drop the cache (on server stop, so singleplayer world-switches start clean). */
    public void saveAllAndClear() {
        saveAll();
        cache.clear();
    }

    public void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }
}
