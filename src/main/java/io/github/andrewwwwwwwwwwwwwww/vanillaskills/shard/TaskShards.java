package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A rare trickle of Skill Shards from ordinary work: mining or placing a block, or harvesting a crop,
 * has a small chance to shake an Unstable Skill Shard loose at the spot.
 *
 * <p>Two guards keep it a trickle rather than a farm. The chance itself is low (roughly one action in
 * five hundred by default), and a payout starts a per-player cooldown during which further actions do
 * not even roll — so no amount of instamining or dirt-spamming beats the cap, and the expected income
 * stays a pleasant surprise rather than a strategy. Creative players earn nothing.
 *
 * <p>Both knobs are config: {@code taskShardChance} (0 disables the whole mechanic) and
 * {@code taskShardCooldownSeconds}. The cooldown map is memory only — a restart forgiving the tail of
 * a cooldown is not worth persisting state for.
 */
public final class TaskShards {
    private TaskShards() {}

    /** Overworld game-time tick of each player's last payout — one clock across dimensions. */
    private static final Map<UUID, Long> LAST_PAYOUT = new ConcurrentHashMap<>();

    /** Rolls the task-shard chance for one qualifying action, paying out at {@code pos} on a hit. */
    public static void roll(ServerLevel level, ServerPlayer player, BlockPos pos) {
        if (GameplayConfig.TASK_SHARD_CHANCE <= 0.0) return;
        if (player.hasInfiniteMaterials()) return;

        long now = level.getServer().overworld().getGameTime();
        Long last = LAST_PAYOUT.get(player.getUUID());
        if (last != null && now - last < GameplayConfig.TASK_SHARD_COOLDOWN_SECONDS * 20L) return;

        // Luck sweetens the odds: Fortune Finder (+0.5/node) and Luck potions raise the luck
        // attribute, and each point multiplies the base chance by taskShardLuckBonus more. Bad Omen
        // of Unluck can push luck negative — that legitimately worsens the odds, floored at zero.
        double luck = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK);
        double chance = Math.min(1.0, GameplayConfig.TASK_SHARD_CHANCE
                * Math.max(0.0, 1.0 + luck * GameplayConfig.TASK_SHARD_LUCK_BONUS));
        if (player.getRandom().nextDouble() >= chance) return;

        LAST_PAYOUT.put(player.getUUID(), now);
        Block.popResource(level, pos, ShardItems.unstableShard());
    }

    /** Forget all cooldowns (server stopping). */
    public static void clear() {
        LAST_PAYOUT.clear();
    }
}
