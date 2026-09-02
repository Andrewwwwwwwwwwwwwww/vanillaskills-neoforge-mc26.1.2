package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gives back the Skill Shards spent on nodes a later version deleted.
 *
 * <p>A node that leaves the tree takes its unlock with it: the id stays in the player's unlocked set but
 * {@code tree.byId} no longer resolves it, so the effect stops and the shards stay spent. Nothing else
 * notices, which makes it a silent loss. This pays those shards back once, on the player's next join.
 *
 * <h2>Working out what it cost</h2>
 * Prices are not authored — {@code applyEconomy} derives every one of them from the authored weight so
 * the whole Skill-Shard tree sums to exactly the total earnable P. Deleting nodes therefore changes what
 * every remaining node costs, and the deleted node's own price is gone with it. Reconstructing it is
 * still exact: the divisor used at the time was the current scalable weight PLUS the weights that have
 * since been removed, so
 *
 * <pre>oldFactor = (P - nightVision) / (scalableWeight(tree) + removedWeight)</pre>
 *
 * <p>and the node cost {@code round(weight * oldFactor)}. That reproduces the old figure on any server,
 * whatever its P — a hardcoded number would only have been right on the one it was measured on.
 */
public final class RemovedNodes {
    private RemovedNodes() {}

    /** Node id -> the authored weight it carried when it was still in the tree. */
    private static final Map<String, Integer> REMOVED = new LinkedHashMap<>();
    static {
        REMOVED.put("aquatic_7", 38);
        REMOVED.put("aquatic_8", 51);
        REMOVED.put("aquatic_9", 61);
    }

    /**
     * Refund and forget any removed nodes this player had unlocked. Returns the shards handed back.
     * Safe to call on every join: it only acts on ids still present in the unlocked set, and it drops
     * them as it pays, so a second call finds nothing.
     */
    public static int reconcile(ServerPlayer player, PlayerSkillData data) {
        if (data == null || data.unlocked.isEmpty()) return 0;

        int refund = 0;
        for (Map.Entry<String, Integer> entry : REMOVED.entrySet()) {
            if (!data.unlocked.remove(entry.getKey())) continue;
            refund += formerCost(entry.getValue());
        }
        if (refund <= 0) return 0;

        data.pointsAvailable += refund;
        VanillaSkills.PLAYERS.save(player.getUUID());
        player.sendSystemMessage(Component.literal(Lang.tr(player,
                "vanillaskills.msg.node_refund",
                "Refunded %d Skill Shards from skills that are no longer in the tree.", refund))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return refund;
    }

    /** What a node of this weight cost under the economy that still included every removed node. */
    private static int formerCost(int weight) {
        int p = SkillTreeManager.economyP;
        // Below the threshold applyEconomy leaves hand-tuned costs alone, so the weight WAS the price.
        if (p <= 80) return Math.max(1, weight);

        SkillTree tree = VanillaSkills.TREE.tree();
        if (tree == null) return Math.max(1, weight);

        int divisor = SkillTreeManager.scalableWeight(tree) + totalRemovedWeight();
        if (divisor <= 0) return Math.max(1, weight);

        double factor = (double) (p - SkillTreeManager.NIGHT_VISION_COST) / divisor;
        return Math.max(1, (int) Math.round(Math.max(1, weight) * factor));
    }

    private static int totalRemovedWeight() {
        int sum = 0;
        for (int w : REMOVED.values()) sum += w;
        return sum;
    }
}
