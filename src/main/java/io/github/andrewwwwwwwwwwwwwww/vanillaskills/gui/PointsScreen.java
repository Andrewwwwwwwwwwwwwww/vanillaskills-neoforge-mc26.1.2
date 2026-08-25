package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.PointsConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** Shows the ways players earn skill points (read from points.json). */
public final class PointsScreen {
    private PointsScreen() {}

    public static void open(ServerPlayer player) {
        PointsConfig cfg = VanillaSkills.PLAYERS.pointsConfig();
        List<ItemStack> items = new ArrayList<>();

        String title = Lang.tr(player, "vanillaskills.points.title", "Earning Skill Shards");

        items.add(item(Items.KNOWLEDGE_BOOK, title, ChatFormatting.GOLD, lines(player,
                "vanillaskills.points.how",
                "Advancements are the main source, and the only one\n"
                + "that can't be farmed — each counts once, worth more\n"
                + "the harder it is. The rest of this page is the trickle.")));

        items.add(item(Items.PAPER,
                Lang.tr(player, "vanillaskills.points.tasks", "Tasks (common)"), ChatFormatting.WHITE, List.of(
                Lang.tr(player, "vanillaskills.points.tasks.desc", "Ordinary square-icon advancements."),
                each(player, cfg.valueTask))));

        items.add(item(Items.GOLD_INGOT,
                Lang.tr(player, "vanillaskills.points.goals", "Goals"), ChatFormatting.YELLOW, List.of(
                Lang.tr(player, "vanillaskills.points.goals.desc", "Rounded-icon goals — a bigger ask."),
                each(player, cfg.valueGoal))));

        items.add(item(Items.NETHER_STAR,
                Lang.tr(player, "vanillaskills.points.challenges", "Challenges (purple)"), ChatFormatting.LIGHT_PURPLE, List.of(
                Lang.tr(player, "vanillaskills.points.challenges.desc", "The hardest, purple-framed advancements."),
                each(player, cfg.valueChallenge))));

        int custom = VanillaSkills.PLAYERS.customAdvancementTotal();
        List<String> vsLore = new ArrayList<>(lines(player, "vanillaskills.points.vs_goals.desc",
                "Craft full armor sets, discover the upgrade\n"
                + "templates, forge a Dragon Ingot, and finish\n"
                + "skill paths for bonus Skill Shards."));
        vsLore.add(Lang.tr(player, "vanillaskills.points.vs_goals.total", "Worth %d Skill Shards in total.", custom));
        items.add(item(Items.DIAMOND_CHESTPLATE,
                Lang.tr(player, "vanillaskills.points.vs_goals", "VanillaSkills Goals"), ChatFormatting.AQUA, vsLore));

        if (cfg.startingPoints > 0) {
            items.add(item(Items.GOLDEN_APPLE,
                    Lang.tr(player, "vanillaskills.points.starting", "Starting Bonus"), ChatFormatting.GREEN, List.of(
                    Lang.tr(player, "vanillaskills.points.starting.desc",
                            "+%d Skill Shards when you first join", cfg.startingPoints))));
        }

        // --- The physical sources. Everything below hands over an Unstable Skill Shard you can hold,
        // --- rather than crediting the balance directly; right-click one to bank it.
        items.add(item(Items.REINFORCED_DEEPSLATE,
                Lang.tr(player, "vanillaskills.points.ore", "Skill Shard Ore"), ChatFormatting.DARK_AQUA, lines(player,
                "vanillaskills.points.ore.desc",
                "Y %d to %d in the Overworld, below Y %d in the\n"
                + "Nether, and throughout the End. Drops %d.\n"
                + "Needs a Netherite, Crystalline or Dragon pickaxe.",
                GameplayConfig.SHARD_ORE_OVERWORLD_MIN_Y, GameplayConfig.SHARD_ORE_OVERWORLD_MAX_Y,
                GameplayConfig.SHARD_ORE_NETHER_MAX_Y, GameplayConfig.SHARD_ORE_DROP)));

        items.add(item(Items.CHEST,
                Lang.tr(player, "vanillaskills.points.exploration", "Exploration"), ChatFormatting.YELLOW, lines(player,
                "vanillaskills.points.exploration.desc",
                "About %s of chests in ancient cities, stronghold\n"
                + "libraries, End City treasure, fortresses, bastions,\n"
                + "mansions and dungeons. Piglin barters: about %s.",
                pct(GameplayConfig.SHARD_CHEST_WEIGHT, GameplayConfig.SHARD_CHEST_EMPTY_WEIGHT),
                pct(GameplayConfig.SHARD_BARTER_WEIGHT, GameplayConfig.SHARD_BARTER_EMPTY_WEIGHT))));

        if (GameplayConfig.SPAWNER_DROPS_SHARD_BLOCK) {
            items.add(item(Items.SPAWNER,
                    Lang.tr(player, "vanillaskills.points.spawners", "Monster Spawners"), ChatFormatting.RED, lines(player,
                    "vanillaskills.points.spawners.desc",
                    "Breaking a spawner drops an Unstable Skill\n"
                    + "Shard Block — %d shards, once per spawner.\n"
                    + "They no longer drop experience.",
                    io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.SHARDS_PER_BLOCK)));
        }

        if (GameplayConfig.TASK_SHARD_CHANCE > 0) {
            items.add(item(Items.IRON_PICKAXE,
                    Lang.tr(player, "vanillaskills.points.tasks", "Hard Work"), ChatFormatting.GOLD, lines(player,
                    "vanillaskills.points.tasks.desc",
                    "Mining, building and harvesting crops each\n"
                    + "have about a %s chance to shake a shard\n"
                    + "loose — at most one every %d minutes.\n"
                    + "Luck (Fortune Finder) raises your odds.",
                    String.format("%.1f%%", GameplayConfig.TASK_SHARD_CHANCE * 100.0),
                    Math.max(1, Math.round(GameplayConfig.TASK_SHARD_COOLDOWN_SECONDS / 60.0f)))));
        }

        items.add(item(Items.EMERALD,
                Lang.tr(player, "vanillaskills.points.trader", "Wandering Trader"), ChatFormatting.GREEN, lines(player,
                "vanillaskills.points.trader.desc",
                "Buys raw materials from you and pays in Skill\n"
                + "Shards — iron, netherite scrap, crops and blocks.\n"
                + "Each offer stocks once and does not restock.")));

        if (GameplayConfig.CRATE_FISHING_WEIGHT > 0) {
            items.add(item(Items.FISHING_ROD,
                    Lang.tr(player, "vanillaskills.points.crates", "Crates"), ChatFormatting.BLUE, lines(player,
                    "vanillaskills.points.crates.desc",
                    "About %s of catches pull up a crate alongside\n"
                    + "the fish. Some contain Skill Shards, and the\n"
                    + "biome you fish in decides which crate you get.",
                    pct(GameplayConfig.CRATE_FISHING_WEIGHT, GameplayConfig.CRATE_FISHING_EMPTY_WEIGHT))));
        }

        items.add(item(Items.WRITABLE_BOOK,
                Lang.tr(player, "vanillaskills.points.bounties", "Daily Bounties"), ChatFormatting.LIGHT_PURPLE, lines(player,
                "vanillaskills.points.bounties.desc",
                "The bounty board gives Quest Shards,\n"
                + "which you can convert to Skill Shards\n"
                + "(%d:1) at the shop — extra progress every day.",
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestShop.CONVERT_RATIO)));

        // Six per row: the first row is what you earn by playing, the second what the world hands you.
        InfoMenu.open(player, styled(title, ChatFormatting.AQUA), 6, items, 6);
    }

    /** A weight-against-empty rate as a readable percentage, e.g. 1 vs 60 -> "1.6%". */
    private static String pct(int weight, int emptyWeight) {
        int total = weight + Math.max(1, emptyWeight);
        double p = 100.0 * weight / total;
        return (p < 10 ? String.format(java.util.Locale.ROOT, "%.1f", p)
                       : String.valueOf(Math.round(p))) + "%";
    }

    /** "+N Skill Shard(s) each" with a separate singular key so every language can pluralize. */
    private static String each(ServerPlayer player, int value) {
        return value == 1
                ? Lang.tr(player, "vanillaskills.points.each_one", "+1 Skill Shard each")
                : Lang.tr(player, "vanillaskills.points.each", "+%d Skill Shards each", value);
    }

    /** A multi-line lore string (one key, lines separated with \n). */
    private static List<String> lines(ServerPlayer player, String key, String fallback, Object... args) {
        return List.of(Lang.tr(player, key, fallback, args).split("\n"));
    }

    private static ItemStack item(net.minecraft.world.item.Item icon, String name, ChatFormatting color, List<String> lore) {
        ItemStack stack = new ItemStack(icon);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, color));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) lines.add(styled(line, ChatFormatting.GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lines));
        return stack;
    }

    private static Component styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
