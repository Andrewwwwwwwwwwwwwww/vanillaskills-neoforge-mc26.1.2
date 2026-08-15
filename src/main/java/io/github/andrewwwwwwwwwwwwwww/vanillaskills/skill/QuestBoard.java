package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The shared bounty board: 3 active quests that re-roll on a timer (default every 5 hours). Persisted
 * to world/vanillaskills/questboard.json so it survives restarts.
 */
public class QuestBoard {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static int activeCount() { return io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.QUESTS_PER_ROTATION; }
    private final Random random = new Random();

    private State state = new State();

    /** Serialized board state. */
    private static class State {
        long rotationId = 0;
        long nextRotationMs = 0;
        /** LEGACY (pre-2.0): positions in the old hardcoded rotating pool. Converted to ids on load, then unused. */
        int[] activeIndices = new int[0];
        /** The dealt quests, by stable id — immune to the pool being reordered or edited. */
        String[] activeIds = new String[0];
    }

    public long rotationId() {
        return state.rotationId;
    }

    public long nextRotationMs() {
        return state.nextRotationMs;
    }

    /** The currently-dealt quests, in board-slot order. Ids that no longer exist are skipped. */
    public List<Quest> active() {
        List<Quest> out = new ArrayList<>();
        for (String id : state.activeIds) {
            Quest q = QuestPool.byId(id);
            if (q != null) out.add(q);
        }
        return out;
    }

    public Quest active(int i) {
        List<Quest> a = active();
        return (i >= 0 && i < a.size()) ? a.get(i) : null;
    }

    public void load() {
        Path path = path();
        try {
            if (Files.exists(path)) {
                State loaded = GSON.fromJson(Files.readString(path), State.class);
                if (loaded != null) state = loaded;
            }
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to load questboard.json", e);
        }
        migrateLegacyIndices();
        long now = System.currentTimeMillis();
        if (state.activeIds.length != activeCount() || state.nextRotationMs <= now) {
            reroll(now);
        }
    }

    /**
     * Convert a pre-2.0 board, which stored positions in the old hardcoded rotating pool, to stable
     * quest ids.
     *
     * <p>Resolved against {@link QuestPool#LEGACY_ALL_IDS} — the frozen pre-2.0 ordering — rather than
     * the live datapack pool, because the old numbers only mean anything against the ordering that
     * wrote them. Reading the live pool would silently remap the board whenever a pack reorders quests.
     */
    private void migrateLegacyIndices() {
        if (state.activeIds == null) state.activeIds = new String[0];
        if (state.activeIds.length > 0 || state.activeIndices == null || state.activeIndices.length == 0) return;
        List<String> legacy = QuestPool.LEGACY_ALL_IDS;
        List<String> ids = new ArrayList<>();
        for (int idx : state.activeIndices) {
            if (idx >= 0 && idx < legacy.size()) ids.add(legacy.get(idx));
        }
        state.activeIds = ids.toArray(new String[0]);
        state.activeIndices = new int[0];
        VanillaSkills.LOGGER.info("Migrated bounty board to quest ids: {}", String.join(", ", ids));
        save();
    }

    public void tick(MinecraftServer server) {
        if (System.currentTimeMillis() >= state.nextRotationMs) {
            reroll(System.currentTimeMillis());
            server.getPlayerList().getPlayers().forEach(p -> p.sendSystemMessage(Component.literal(
                    "New bounties are available! Use /quests").withStyle(ChatFormatting.GOLD)));
        }
    }

    /** Force a fresh rotation right now (op/testing). */
    public void forceReroll() {
        reroll(System.currentTimeMillis());
    }

    private void reroll(long now) {
        state.rotationId++;
        state.nextRotationMs = now + GameplayConfig.BOUNTY_REFRESH_MS;
        state.activeIds = pickDistinct(activeCount());
        save();
    }

    private String[] pickDistinct(int count) {
        // The universal board draws from the full pool (early-game gating now lives on the starter board).
        // Snapshot it once: a /reload mid-draw would otherwise swap the list under us.
        List<Quest> pool = new ArrayList<>(QuestPool.all());
        // Weighted distinct sampling (rarer quests like the freebie have lower weight).
        List<Quest> chosen = new ArrayList<>();
        int n = Math.min(count, pool.size());
        for (int k = 0; k < n; k++) {
            int total = 0;
            for (Quest q : pool) total += Math.max(1, q.weight());
            int r = random.nextInt(total);
            int removeAt = pool.size() - 1;
            for (int j = 0; j < pool.size(); j++) {
                r -= Math.max(1, pool.get(j).weight());
                if (r < 0) { removeAt = j; break; }
            }
            chosen.add(pool.remove(removeAt));
        }
        String[] out = new String[chosen.size()];
        for (int i = 0; i < out.length; i++) out[i] = chosen.get(i).id();
        return out;
    }

    public void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(state));
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to save questboard.json", e);
        }
    }

    private static Path path() {
        MinecraftServer server = VanillaSkills.server;
        return server.getWorldPath(LevelResource.ROOT).resolve("vanillaskills").resolve("questboard.json");
    }
}
