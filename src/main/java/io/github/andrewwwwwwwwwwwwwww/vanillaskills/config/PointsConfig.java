package io.github.andrewwwwwwwwwwwwwww.vanillaskills.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Controls how many skill points players earn from advancements.
 * Stored PER-WORLD at &lt;world&gt;/vanillaskills/points.json, alongside gameplay.json — NOT under
 * config/. A pack shipping overrides for it in config/vanillaskills/ has no effect.
 */
public class PointsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int perAdvancement = 2;          // fallback when an advancement has no display frame
    public int valueTask = 5;               // common square-frame advancements
    public int valueGoal = 20;              // rounded-frame goals
    public int valueChallenge = 50;         // purple challenge-frame advancements
    public boolean ignoreRecipeAdvancements = true;

    /**
     * Namespaces whose advancements are worth Skill Shards.
     *
     * <p>An allowlist rather than "everything", because shards are the whole economy: a server that adds a
     * pack with three hundred advancements would otherwise hand out a fortune, and VanillaTweaks-style
     * packs are common. Listing a namespace opts its advancements in at the usual per-frame rate.
     *
     * <p>{@code thp} ships in the default because The Hungering Portal is a sibling mod whose End-fight
     * advancements are meant to feed this progression. Remove it, or add your own, to taste.
     */
    public java.util.List<String> countedNamespaces =
            new java.util.ArrayList<>(java.util.List.of("minecraft", "vanillaskills", "thp"));
    public int startingPoints = 0;
    public Map<String, Integer> advancementOverrides = new HashMap<>();

    /** Value for an advancement: explicit override first, else by its frame type (task/goal/challenge). */
    public int pointsFor(net.minecraft.advancements.AdvancementHolder holder) {
        Integer override = advancementOverrides.get(holder.id().toString());
        if (override != null) return override;
        net.minecraft.advancements.AdvancementType type = holder.value().display()
                .map(net.minecraft.advancements.DisplayInfo::getType)
                .orElse(net.minecraft.advancements.AdvancementType.TASK);
        return switch (type) {
            case CHALLENGE -> valueChallenge;
            case GOAL -> valueGoal;
            default -> valueTask;
        };
    }

    /**
     * Payout defaults that changed between releases follow their default to the new value. A stored
     * value equal to any OLD default is what the mod wrote, not an admin's decision, so it upgrades —
     * same policy as the superseded pack URLs in gameplay.json. A custom value is never touched.
     * The per-frame history: task 1 → 2 → 5, goal 5 → 12 → 20, challenge 20 → 45 → 50,
     * frameless 1 → 2 (2.1.1 raised the current set).
     */
    private boolean migrateSupersededDefaults() {
        boolean changed = false;
        if (perAdvancement == 1) { perAdvancement = 2; changed = true; }
        if (valueTask == 1 || valueTask == 2) { valueTask = 5; changed = true; }
        if (valueGoal == 5 || valueGoal == 12) { valueGoal = 20; changed = true; }
        if (valueChallenge == 20 || valueChallenge == 45) { valueChallenge = 50; changed = true; }
        return changed;
    }

    private static Path path() {
        Path dir = VanillaSkills.worldDir();
        return dir == null ? null : dir.resolve("points.json");
    }

    public static PointsConfig load() {
        Path path = path();
        if (path != null) {
            try {
                if (Files.exists(path)) {
                    String json = Files.readString(path);
                    PointsConfig cfg = GSON.fromJson(json, PointsConfig.class);
                    if (cfg == null) cfg = new PointsConfig();
                    if (cfg.advancementOverrides == null) cfg.advancementOverrides = new HashMap<>();
                    if (cfg.migrateSupersededDefaults()) cfg.save();
                    return cfg;
                }
            } catch (Exception e) {
                VanillaSkills.LOGGER.error("Failed to load points.json, using defaults", e);
            }
        }
        PointsConfig cfg = defaults();
        cfg.save();
        return cfg;
    }

    public void save() {
        Path path = path();
        if (path == null) return; // no world loaded
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            VanillaSkills.LOGGER.error("Failed to save points.json", e);
        }
    }

    private static PointsConfig defaults() {
        PointsConfig cfg = new PointsConfig();
        cfg.perAdvancement = 2;
        cfg.valueTask = 5;        // common (square) advancements — a small amount
        cfg.valueGoal = 20;       // goals — a moderate amount
        cfg.valueChallenge = 50;  // purple challenges — a lot
        cfg.startingPoints = 5;
        cfg.ignoreRecipeAdvancements = true;

        // Per-advancement overrides REPLACE the frame-based value above. Roots are free entries.
        Map<String, Integer> o = cfg.advancementOverrides;
        o.put("minecraft:story/root", 0);
        o.put("minecraft:nether/root", 0);
        o.put("minecraft:end/root", 0);
        o.put("minecraft:adventure/root", 0);
        o.put("minecraft:husbandry/root", 0);
        // Story
        o.put("minecraft:story/smelt_iron", 16);
        o.put("minecraft:story/mine_diamond", 24);
        o.put("minecraft:story/enter_the_nether", 24);
        // Nether
        o.put("minecraft:nether/obtain_blaze_rod", 24);
        o.put("minecraft:nether/get_wither_skull", 30);
        o.put("minecraft:nether/summon_wither", 50);
        o.put("minecraft:nether/create_beacon", 40);
        o.put("minecraft:nether/obtain_ancient_debris", 30);
        o.put("minecraft:nether/netherite_armor", 50);
        o.put("minecraft:nether/all_potions", 40);
        o.put("minecraft:nether/all_effects", 80);
        // End
        o.put("minecraft:end/enter_end", 24);
        o.put("minecraft:end/kill_dragon", 70);
        o.put("minecraft:end/respawn_dragon", 24);
        o.put("minecraft:end/elytra", 40);
        o.put("minecraft:end/levitate", 50);
        // Adventure
        o.put("minecraft:adventure/adventuring_time", 60);
        o.put("minecraft:adventure/kill_all_mobs", 60);
        o.put("minecraft:adventure/totem_of_undying", 30);
        o.put("minecraft:adventure/hero_of_the_village", 30);
        // Husbandry
        o.put("minecraft:husbandry/balanced_diet", 50);
        o.put("minecraft:husbandry/breed_all_animals", 50);
        o.put("minecraft:husbandry/obtain_netherite_hoe", 40);
        o.put("minecraft:husbandry/complete_catalogue", 24);

        // VanillaSkills' own advancements (extra, mod-themed point sources).
        o.put("vanillaskills:root", 0); // tick-granted to everyone — no free points
        o.put("vanillaskills:metallurgist", 10);
        o.put("vanillaskills:set_hardwood", 16);
        o.put("vanillaskills:set_rose_gold", 20);
        o.put("vanillaskills:set_steel", 24);
        o.put("vanillaskills:set_crystal", 30);
        o.put("vanillaskills:set_dragon", 50);
        o.put("vanillaskills:fortune_template", 16);
        o.put("vanillaskills:dragon_template", 20);
        o.put("vanillaskills:dragon_ingot", 16);
        o.put("vanillaskills:armored_elytra", 24);
        o.put("vanillaskills:steel_shield", 16);
        o.put("vanillaskills:specialist", 30);
        o.put("vanillaskills:completionist", 50);
        return cfg;
    }
}
