package io.github.andrewwwwwwwwwwwwwww.vanillaskills.text;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side localization. VanillaSkills renders its UI in chest menus, so the text is generated on
 * the SERVER — client resource-pack lang files can never reach it. Instead, the server translates
 * per player using the language their client reports:
 *
 * <ol>
 *   <li>Bundled defaults: {@code assets/vanillaskills/lang/<locale>.json} inside the mod jar.</li>
 *   <li>Server overrides: {@code <world>/vanillaskills/lang/<locale>.json} — drop a community
 *       translation (e.g. {@code ru_ru.json}) there, no jar editing needed. Overrides win.</li>
 * </ol>
 *
 * Missing keys fall back to en_us, then to the English literal baked into the code — so an
 * untranslated or partially-translated locale is always safe. Maps are cached per locale;
 * {@link #invalidate()} clears the cache (called on config load / {@code /skill reload}).
 */
public final class Lang {
    private Lang() {}

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Map<String, Map<String, String>> CACHE = new ConcurrentHashMap<>();

    /** Translate {@code key} for this player's client language; {@code fallback} is the built-in
     *  English text. Optional args are formatted with {@link String#format} ({@code %s}/{@code %d}). */
    public static String tr(ServerPlayer player, String key, String fallback, Object... args) {
        String locale = player == null ? "en_us" : player.clientInformation().language().toLowerCase(Locale.ROOT);
        String s = map(locale).get(key);
        if (s == null && !"en_us".equals(locale)) s = map("en_us").get(key);
        if (s == null) s = fallback;
        if (args.length == 0) return s;
        try {
            return String.format(s, args);
        } catch (Exception e) {
            return String.format(fallback, args); // a translation with broken placeholders never crashes the UI
        }
    }

    /** Stable translation key for a quest title, e.g. "Gather 32 Emeralds" -> "vanillaskills.quest.gather_32_emeralds".
     *  Delegates to {@code Quest.slug} so the translation key and the quest id can never drift apart. */
    public static String questKey(String title) {
        return "vanillaskills.quest."
                + io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quest.slug(title);
    }

    public static void invalidate() {
        CACHE.clear();
    }

    private static Map<String, String> map(String locale) {
        return CACHE.computeIfAbsent(locale, Lang::load);
    }

    private static Map<String, String> load(String locale) {
        Map<String, String> out = new HashMap<>();
        // 1) bundled in the jar
        try (InputStream in = Lang.class.getResourceAsStream("/assets/vanillaskills/lang/" + locale + ".json")) {
            if (in != null) {
                Map<String, String> m = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), MAP_TYPE);
                if (m != null) out.putAll(m);
            }
        } catch (Exception e) {
            VanillaSkills.LOGGER.warn("Failed to read bundled lang {}", locale, e);
        }
        // 2) per-world server override (community translations drop in here)
        try {
            Path dir = VanillaSkills.worldDir();
            if (dir != null) {
                Path file = dir.resolve("lang").resolve(locale + ".json");
                if (Files.exists(file)) {
                    Map<String, String> m = GSON.fromJson(Files.readString(file), MAP_TYPE);
                    if (m != null) out.putAll(m);
                }
            }
        } catch (Exception e) {
            VanillaSkills.LOGGER.warn("Failed to read world lang override {}", locale, e);
        }
        return out;
    }
}
