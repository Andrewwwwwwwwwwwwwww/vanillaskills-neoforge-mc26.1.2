package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repurposes the experience bar as a live Skill Shard readout.
 *
 * <p>2.0 removes experience entirely, which leaves the XP bar frozen at zero — dead space in the middle of
 * the HUD. Sending a purely cosmetic {@link ClientboundSetExperiencePacket} whose level is the player's
 * banked Skill Shards turns it into the one thing a player most wants on screen, on a completely vanilla
 * client.
 *
 * <p>It also fixes the anvil. Vanilla's client-side result check is
 * {@code (instabuild || experienceLevel >= cost) && cost > 0}; with a permanently-zero level the client
 * greyed out every anvil result and drew the cost in red, even though the server was charging Skill Shards
 * and would have allowed the take. Feeding the client the shard balance makes that check agree with what the
 * server will actually do.
 *
 * <p><b>Display only.</b> Nothing here touches {@code ServerPlayer.experienceLevel}, which stays 0 — the
 * experience mixins keep it there. The server remains the sole authority on what anything costs.
 *
 * <p>Re-sent unconditionally twice a second rather than only when the balance changes. That is deliberate:
 * the client drops its copy on respawn and dimension change (vanilla re-sends the player's real level, 0,
 * once its own send-tracking is reset by the entity rebuild), and trying to enumerate those moments failed
 * twice — a cached "already sent" turns any missed case into a permanently blank bar. An unconditional
 * refresh is self-healing whatever the cause, and the packet is a dozen bytes.
 */
public final class ShardBar {
    private ShardBar() {}

    /** Last value pushed to each player, so an unchanged balance costs nothing. */
    private static final Map<UUID, Integer> lastSent = new ConcurrentHashMap<>();

    /** How often to reconcile, in ticks. Half a second is well under the time it takes to notice. */
    private static final int INTERVAL = 10;

    public static void tick(MinecraftServer server, long tickCount) {
        if (!GameplayConfig.SHARDS_IN_XP_BAR) return;
        if (tickCount % INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Unconditional re-send, NOT "only when the number changed".
            //
            // Two releases tried to enumerate the moments a client silently drops our value (respawn,
            // dimension change) and refresh at exactly those points. Both missed: the client can reset
            // its own copy for reasons the server cannot observe, and any cached "already sent" then
            // hides the loss forever. Re-sending every half second is self-healing whatever the cause —
            // and the packet is a dozen bytes, so even a full server is a rounding error of bandwidth.
            push(player, true);
        }
    }

    /** Push the current balance to one player. {@code force} re-sends even if the number has not changed. */
    public static void push(ServerPlayer player, boolean force) {
        if (!GameplayConfig.SHARDS_IN_XP_BAR) return;
        int shards = VanillaSkills.PLAYERS.skillShards(player);
        Integer previous = lastSent.get(player.getUUID());
        if (!force && previous != null && previous == shards) return;
        lastSent.put(player.getUUID(), shards);
        // Progress 0 and total 0: only the level number is meaningful, so the bar itself stays empty rather
        // than implying progress toward something.
        player.connection.send(new ClientboundSetExperiencePacket(0.0f, 0, shards));
    }

    /** Forget a player on disconnect, so the cache cannot grow without bound. */
    public static void forget(ServerPlayer player) {
        lastSent.remove(player.getUUID());
    }
}
