package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.List;

/**
 * One-time Feats: boss kills, structure discoveries, and entering the End. All server-side. Boss kills
 * are pushed from the kill handler; discoveries and dimension entry are polled on a throttled server
 * tick (every 2s, only for feats a player hasn't earned, only in the relevant dimension).
 */
public final class Feats {
    private Feats() {}

    private static final int CHECK_INTERVAL_TICKS = 40; // poll discovery/dimension feats every ~2s

    /**
     * Every currently-loaded feat.
     *
     * <p>Feats are datapack-driven — the mod ships its own eleven as a bundled datapack at
     * {@code data/vanillaskills/vanillaskills/feat/builtin.json}, so built-in and pack-added feats
     * take exactly the same path and a pack can add to or replace the defaults. Read live rather than
     * cached, so {@code /reload} takes effect immediately.
     */
    public static List<Feat> all() {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.data.VsContent.feats();
    }

    public static boolean isDone(ServerPlayer player, String id) {
        return VanillaSkills.PLAYERS.get(player.getUUID()).featsDone.contains(id);
    }

    private static void award(ServerPlayer player, Feat feat) {
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        if (!data.featsDone.add(feat.id())) return; // already earned
        VanillaSkills.PLAYERS.addQuestShards(player, feat.reward()); // also persists the data
        String featName = io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                "vanillaskills.feat." + feat.id(), feat.title());
        player.sendSystemMessage(Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                "vanillaskills.msg.feat_unlocked", "★ Feat unlocked: %s  +%d Quest Shards",
                featName, feat.reward())).withStyle(ChatFormatting.GOLD));
    }

    /** Boss-kill feats — call from the entity-kill handler with the player's victim. */
    public static void onKill(ServerPlayer killer, Entity dead) {
        if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.FEATS_ENABLED) return;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
        for (Feat f : all()) {
            if (f.type() == Feat.Type.KILL && f.target().equals(id) && !isDone(killer, f.id())) {
                award(killer, f);
            }
        }
    }

    /** Throttled poll for discovery + dimension feats; also refreshes STAT-quest baselines. */
    public static void serverTick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;
        boolean feats = io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.FEATS_ENABLED;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Quests.sync(p);              // captures STAT-quest baselines (still needed with feats off)
            if (feats) checkLocationFeats(p);
        }
    }

    private static void checkLocationFeats(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        String dim = level.dimension().identifier().toString();
        BlockPos pos = player.blockPosition();
        for (Feat f : all()) {
            if (data.featsDone.contains(f.id())) continue;
            switch (f.type()) {
                case DIMENSION -> { if (dim.equals(f.target())) award(player, f); }
                case DISCOVER -> {
                    if (f.dimension() != null && !f.dimension().equals(dim)) continue; // wrong dimension, skip cheaply
                    Identifier loc = Identifier.tryParse(f.target());
                    if (loc == null) continue;
                    ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, loc);
                    Structure structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                            .get(key).map(Holder::value).orElse(null);
                    if (structure == null) continue;
                    StructureStart start = level.structureManager().getStructureAt(pos, structure);
                    // getStructureAt matches the structure's whole (large) bounding box — require the
                    // player to actually be inside a generated PIECE so it only fires on real arrival,
                    // not hundreds of blocks away.
                    if (start.isValid() && insidePiece(start, pos)) award(player, f);
                }
                default -> { /* KILL feats are pushed from onKill, not polled */ }
            }
        }
    }

    /** True only if pos lies inside one of the structure's generated pieces (a room/corridor), not just
     *  within its overall bounding box — so discovery fires on real arrival, not from far away. */
    private static boolean insidePiece(StructureStart start, BlockPos pos) {
        for (var piece : start.getPieces()) {
            if (piece.getBoundingBox().isInside(pos)) return true;
        }
        return false;
    }
}
