package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;

/**
 * Closes off the Nether roof.
 *
 * <p>Standing on top of the bedrock ceiling trivialises Nether travel and sidesteps the whole dimension, so
 * anyone up there takes steady damage — deliberately using vanilla's own {@code outside_border} damage type,
 * which already reads as "you are somewhere you should not be" and is unblockable by armour or resistance.
 *
 * <p>Creative and spectator players are exempt, so builders and moderators can still work up there.
 *
 * <p>⚠ This does not stop players <em>reaching</em> the roof — breaking the bedrock is deliberately still
 * possible and is a separate question.
 */
public final class NetherRoof {
    private NetherRoof() {}

    /** Ticks between damage pulses. One second, matching how the world border punishes you. */
    public static final int INTERVAL = 20;

    public static void tick(MinecraftServer server, long tickCount) {
        if (!GameplayConfig.NETHER_ROOF_DAMAGE) return;
        if (tickCount % INTERVAL != 0) return;
        float amount = GameplayConfig.NETHER_ROOF_DAMAGE_AMOUNT;
        if (amount <= 0.0f) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;
            if (!(player.level() instanceof ServerLevel level)) continue;
            if (level.dimension() != Level.NETHER) continue;
            if (player.getY() < GameplayConfig.NETHER_ROOF_Y) continue;

            DamageSource source = level.damageSources().source(DamageTypes.OUTSIDE_BORDER);
            player.hurtServer(level, source, amount);
        }
    }
}
