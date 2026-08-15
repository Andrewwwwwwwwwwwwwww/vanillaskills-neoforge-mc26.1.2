package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
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
        int total = VanillaSkills.PLAYERS.totalEarnable();
        List<ItemStack> items = new ArrayList<>();

        String title = Lang.tr(player, "vanillaskills.points.title", "Earning Skill Shards");

        items.add(item(Items.KNOWLEDGE_BOOK, title, ChatFormatting.GOLD, lines(player,
                "vanillaskills.points.how",
                "Skill Shards come from completing advancements.\n"
                + "Each advancement counts once — they can't be farmed.\n"
                + "An advancement's value depends on its difficulty.")));

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
            items.add(item(Items.EXPERIENCE_BOTTLE,
                    Lang.tr(player, "vanillaskills.points.starting", "Starting Bonus"), ChatFormatting.GREEN, List.of(
                    Lang.tr(player, "vanillaskills.points.starting.desc",
                            "+%d Skill Shards when you first join", cfg.startingPoints))));
        }

        items.add(item(Items.BEACON,
                Lang.tr(player, "vanillaskills.points.total", "The Grand Total"), ChatFormatting.GOLD, lines(player,
                "vanillaskills.points.total.desc",
                "There are about %d Skill Shards to earn.\n"
                + "Doing every advancement unlocks the WHOLE\n"
                + "skill tree — it's priced to match.", total)));

        items.add(item(Items.WRITABLE_BOOK,
                Lang.tr(player, "vanillaskills.points.bounties", "Daily Bounties"), ChatFormatting.LIGHT_PURPLE, lines(player,
                "vanillaskills.points.bounties.desc",
                "The bounty board gives Quest Shards,\n"
                + "which you can convert to Skill Shards\n"
                + "(3:1) at the shop — extra progress every day.")));

        InfoMenu.open(player, styled(title, ChatFormatting.AQUA), 6, items);
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
