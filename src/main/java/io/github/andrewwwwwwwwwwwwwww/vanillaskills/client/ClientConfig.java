package io.github.andrewwwwwwwwwwwwwww.vanillaskills.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Global (NOT per-world) client-side settings, stored in {@code config/vanillaskills-client.json}.
 * Separate from {@link io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig} (which is
 * per-world and server-authoritative); these are local client preferences. Currently just the
 * optional narrator disable.
 */
public class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("vanillaskills-client.json");

    /**
     * Opt-in. When true, the narrator backend is stubbed (see {@code GameNarratorMixin}). Default
     * {@code false} so accessibility users and vanilla behaviour are completely unaffected unless a
     * player deliberately turns it on.
     */
    public boolean disableNarrator = false;

    /** Live mirror the client mixin reads, so it never has to touch disk on a narrate call. */
    public static volatile boolean DISABLE_NARRATOR = false;

    public static ClientConfig load() {
        ClientConfig cfg = new ClientConfig();
        try {
            if (Files.exists(PATH)) {
                ClientConfig parsed = GSON.fromJson(Files.readString(PATH), ClientConfig.class);
                if (parsed != null) cfg = parsed;
            }
        } catch (Exception ignored) {
        }
        DISABLE_NARRATOR = cfg.disableNarrator;
        return cfg;
    }

    public void save() {
        DISABLE_NARRATOR = this.disableNarrator;
        try {
            Files.writeString(PATH, GSON.toJson(this));
        } catch (Exception ignored) {
        }
    }
}
