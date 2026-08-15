package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small once-per-world flags, stored at {@code <world>/vanillaskills/worldstate.json}.
 *
 * <p>Distinct from the config files: this is <b>state</b> the world has accumulated, not settings an
 * operator chose. Kept separate so regenerating or hand-editing a config can never accidentally re-arm a
 * one-time event.
 */
public class WorldState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private State state = new State();

    private static class State {
        /** Set the first time a player kills the Ender Dragon in this world. */
        boolean firstDragonKilled = false;
    }

    /**
     * Claim the world's first dragon kill.
     *
     * <p>Returns true exactly once per world, then false forever after — the claim and the flag are set
     * together so two kills resolved in the same tick cannot both win the bonus.
     */
    public boolean claimFirstDragonKill() {
        if (state.firstDragonKilled) return false;
        state.firstDragonKilled = true;
        save();
        return true;
    }

    public boolean firstDragonKilled() {
        return state.firstDragonKilled;
    }

    public void load() {
        Path path = path();
        try {
            if (Files.exists(path)) {
                State loaded = GSON.fromJson(Files.readString(path), State.class);
                if (loaded != null) state = loaded;
            }
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to load worldstate.json", e);
            state = new State();
        }
    }

    public void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(state));
        } catch (Exception e) {
            VanillaSkills.LOGGER.error("Failed to save worldstate.json", e);
        }
    }

    private static Path path() {
        MinecraftServer server = VanillaSkills.server;
        return server.getWorldPath(LevelResource.ROOT).resolve("vanillaskills").resolve("worldstate.json");
    }
}
