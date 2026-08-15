package io.github.andrewwwwwwwwwwwwwww.vanillaskills.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /help — lists the player-facing commands VanillaSkills adds.
 * /help admin — (op) lists those plus the op/admin commands.
 */
public final class HelpCommand {
    private HelpCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("help")
                .executes(ctx -> { showPlayer(ctx.getSource()); return 1; })
                .then(Commands.literal("admin")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ctx -> { showAdmin(ctx.getSource()); return 1; })));
    }

    private static void showPlayer(CommandSourceStack source) {
        header(source, "help.title", "VanillaSkills — Commands");
        line(source, "/skill", "help.skill", "open your skill tree");
        line(source, "/quests", "help.quests", "open your bounty board (alias /bounty)");
        line(source, "/skill toggle nightvision", "help.skill_nightvision", "turn permanent Night Vision on or off");
        line(source, "/skill toggle stepup", "help.skill_stepup", "turn the Mountaineer step-up on or off");
        source.sendSystemMessage(Component.literal(tr(source, "help.ops_hint",
                "Ops: /help admin for admin commands.")).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void showAdmin(CommandSourceStack source) {
        header(source, "help.admin_title", "VanillaSkills — Admin Commands");
        source.sendSystemMessage(Component.literal(tr(source, "help.section_player", "Player commands:"))
                .withStyle(ChatFormatting.GRAY));
        line(source, "/skill", "help.skill", "open your skill tree");
        line(source, "/quests", "help.quests", "open your bounty board (alias /bounty)");
        line(source, "/skill toggle nightvision|stepup", "help.skill_toggle", "per-player Night Vision / step-up toggles");
        source.sendSystemMessage(Component.literal(tr(source, "help.section_admin", "Admin / op commands:"))
                .withStyle(ChatFormatting.GRAY));
        line(source, "/skill skillshards <player> add|set|reset <n>", "help.a_skillshards", "grant/set/clear Skill Shards");
        line(source, "/skill questshards <player> add|set|reset <n>", "help.a_questshards", "grant/set/clear Quest Shards");
        line(source, "/skill reset <player>", "help.a_reset", "refund all of a player's unlocks");
        line(source, "/skill recalc <player>", "help.a_recalc", "recompute earned Shards from advancements");
        line(source, "/skill reload", "help.a_reload", "reload the points + tree config from disk");
        line(source, "/skill mending on|off", "help.a_mending", "allow or strip the Mending enchantment");
        line(source, "/skill give <item> [n] [player]", "help.a_give", "give any VanillaSkills item ('list' shows all ids)");
        line(source, "/quests board [remove|refresh]", "help.a_board", "place / remove / re-render a bounty board");
        line(source, "/quests reroll", "help.a_reroll", "force a fresh set of universal bounties now");
        line(source, "/quests graduate <player>", "help.a_graduate", "move a player to the main bounty board");
        line(source, "/quests starter <player>", "help.a_starter", "send a player back to the starter board");
    }

    /** Translate for the command's player (console falls back to the default language). */
    private static String tr(CommandSourceStack source, String key, String fallback) {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(
                source.getPlayer(), "vanillaskills." + key, fallback);
    }

    private static void header(CommandSourceStack source, String key, String fallback) {
        source.sendSystemMessage(Component.literal("— " + tr(source, key, fallback) + " —")
                .withStyle(ChatFormatting.GOLD));
    }

    /** Command literals stay verbatim (they're typed); only the description is translated. */
    private static void line(CommandSourceStack source, String cmd, String key, String desc) {
        source.sendSystemMessage(Component.literal(cmd).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" — " + tr(source, key, desc)).withStyle(ChatFormatting.GRAY)));
    }
}
