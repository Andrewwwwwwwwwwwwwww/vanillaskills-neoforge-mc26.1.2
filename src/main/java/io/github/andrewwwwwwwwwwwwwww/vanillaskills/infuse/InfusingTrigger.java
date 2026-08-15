package io.github.andrewwwwwwwwwwwwwww.vanillaskills.infuse;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps vanilla's "Enchanter" advancement reachable now that the enchanting table is gone.
 *
 * <p>{@code minecraft:story/enchant_item} has exactly one criterion — the {@code minecraft:enchanted_item}
 * trigger, which only fires from a real enchanting table. Left alone it would become the one vanilla
 * advancement this mod makes impossible, so infusing awards it directly instead.
 *
 * <p>It is worth 2 Skill Shards under the default points config, so the practical stake is small; the point
 * is that the vanilla advancement tree stays completable rather than quietly acquiring a dead end.
 */
public final class InfusingTrigger {
    private InfusingTrigger() {}

    private static final String ENCHANTER = "minecraft:story/enchant_item";

    /** Award "Enchanter" if the player does not already have it. Silently does nothing if unavailable. */
    public static void award(ServerPlayer player) {
        MinecraftServer server = VanillaSkills.server;
        if (server == null) return;
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            if (!holder.id().toString().equals(ENCHANTER)) continue;
            if (player.getAdvancements().getOrStartProgress(holder).isDone()) return;
            for (String criterion : holder.value().criteria().keySet()) {
                player.getAdvancements().award(holder, criterion);
            }
            return;
        }
    }
}
