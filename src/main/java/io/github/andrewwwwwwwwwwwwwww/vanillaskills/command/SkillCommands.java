package io.github.andrewwwwwwwwwwwwwww.vanillaskills.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.PointsConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui.SkillTreeMenu;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.PlayerSkillData;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillEffect;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillNode;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillTree;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * /skill                             open the skill tree (all players)
 * /skill toggle nightvision|stepup   toggle an unlocked toggleable skill (all players)
 * /skill skillshards &lt;player&gt; ...    (op) add|set|reset a player's Skill Shards
 * /skill questshards &lt;player&gt; ...    (op) add|set|reset a player's Quest Shards
 * /skill give &lt;item&gt; [n] [player]    (op) give any VanillaSkills item; "list" prints every id
 * /skill reset|recalc &lt;player&gt;       (op) refund all unlocks / reprice against current config
 * /skill reload                      (op) reload tree + configs from disk
 * /skill mending on|off              (op) enable or strip Mending for this world
 */
public final class SkillCommands {
    private SkillCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("skill")
                .executes(SkillCommands::openSelf);

        // Toggle unlocked toggleable skills on/off (per player, persisted): /skill toggle <skill>
        root.then(Commands.literal("toggle")
                .then(Commands.literal("nightvision").executes(SkillCommands::toggleNightVision))
                .then(Commands.literal("stepup").executes(SkillCommands::toggleStepUp)));

        // Op-only and shaped exactly like /skill questshards. Players read their balance off the skill
        // tree and the experience bar, so a self-view command here would just be a third way to say it.
        root.then(Commands.literal("skillshards")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> adjustPoints(ctx, true))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> adjustPoints(ctx, false))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetPoints(ctx, false)))));

        root.then(Commands.literal("questshards")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> adjustQuestShards(ctx, true))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> adjustQuestShards(ctx, false))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetPoints(ctx, true)))));

        root.then(Commands.literal("reset")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(SkillCommands::reset)));

        root.then(Commands.literal("recalc")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(SkillCommands::recalc)));

        root.then(Commands.literal("reload")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(SkillCommands::reload));

        // /skill give <item> [count] [player] — the only way to obtain our items, since none are
        // registered and so /give cannot name them.
        root.then(Commands.literal("give")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list").executes(SkillCommands::giveList))
                .then(Commands.argument("item", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String typed = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);
                            for (String id : io.github.andrewwwwwwwwwwwwwww.vanillaskills.command.ModItems.all().keySet()) {
                                if (id.startsWith(typed)) builder.suggest(id);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> give(ctx, 1, null))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 6400))
                                .executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "count"), null))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "count"),
                                                EntityArgument.getPlayer(ctx, "player")))))));

        root.then(Commands.literal("mending")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("on").executes(ctx -> setMending(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> setMending(ctx, false))));



        dispatcher.register(root);
    }

    // ---- player-facing ----

    private static int openSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        SkillTreeMenu.open(ctx.getSource().getPlayerOrException());
        return 1;
    }


    private static int toggleNightVision(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        boolean unlocked = data != null && data.unlocked.stream().anyMatch(id -> id.startsWith("nightvision"));
        if (!unlocked) {
            ctx.getSource().sendFailure(Component.literal(Lang.tr(player,
                    "vanillaskills.msg.no_nightvision", "You haven't unlocked Night Vision yet.")));
            return 0;
        }
        data.nightVisionDisabled = !data.nightVisionDisabled;
        VanillaSkills.PLAYERS.save(player.getUUID());
        if (data.nightVisionDisabled) {
            player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            ctx.getSource().sendSuccess(() -> Component.literal(Lang.tr(player,
                    "vanillaskills.msg.nightvision_off",
                    "Night Vision OFF — run /skill toggle nightvision again to turn it back on.")), false);
        } else {
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillEffects
                    .refreshStatusEffects(player, data, VanillaSkills.TREE.tree()); // instant re-apply
            ctx.getSource().sendSuccess(() -> Component.literal(Lang.tr(player,
                    "vanillaskills.msg.nightvision_on", "Night Vision ON.")), false);
        }
        return 1;
    }

    private static int toggleStepUp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.StepHeight.hasStepSkill(data)) {
            ctx.getSource().sendFailure(Component.literal(Lang.tr(player,
                    "vanillaskills.msg.no_stepup", "You haven't unlocked the Mountaineer (step-up) skill yet.")));
            return 0;
        }
        data.stepUpDisabled = !data.stepUpDisabled;
        VanillaSkills.PLAYERS.save(player.getUUID());
        // Force StepHeight to re-evaluate next tick (applies/removes the modifier).
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.StepHeight.invalidate(player.getUUID());
        if (data.stepUpDisabled) {
            ctx.getSource().sendSuccess(() -> Component.literal(Lang.tr(player,
                    "vanillaskills.msg.stepup_off",
                    "Step-up OFF — you'll step like vanilla. Run /skill toggle stepup to re-enable. "
                    + "(Note: step-up is always off while you're sneaking.)")), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal(Lang.tr(player,
                    "vanillaskills.msg.stepup_on",
                    "Step-up ON — auto-step ledges (still off while sneaking).")), false);
        }
        return 1;
    }

    private static int showOwnPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Skill Shards: " + data.pointsAvailable + " (earned " + data.pointsEarned + ")"
                + "   Quest Shards: " + data.questShardsAvailable)
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    // ---- op: player management ----

    private static int adjustPoints(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        if (add) VanillaSkills.PLAYERS.addPoints(target, amount);
        else VanillaSkills.PLAYERS.setPoints(target, amount);
        PlayerSkillData data = VanillaSkills.PLAYERS.get(target.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getName().getString() + " now has " + data.pointsAvailable + " Skill Shards."), true);
        return 1;
    }

    private static int adjustQuestShards(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        if (add) VanillaSkills.PLAYERS.addQuestShards(target, amount);
        else VanillaSkills.PLAYERS.setQuestShards(target, amount);
        PlayerSkillData data = VanillaSkills.PLAYERS.get(target.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getName().getString() + " now has " + data.questShardsAvailable + " Quest Shards."), true);
        return 1;
    }

    private static int resetPoints(CommandContext<CommandSourceStack> ctx, boolean quest) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        if (quest) VanillaSkills.PLAYERS.setQuestShards(target, 0);
        else VanillaSkills.PLAYERS.setPoints(target, 0);
        String which = quest ? "Quest Shards" : "Skill Shards";
        ctx.getSource().sendSuccess(() -> Component.literal(
                target.getName().getString() + "'s " + which + " reset to 0."), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        VanillaSkills.PLAYERS.reset(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Reset skills for " + target.getName().getString() + "."), true);
        return 1;
    }

    private static int recalc(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        int delta = VanillaSkills.PLAYERS.recalc(target);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Recalculated Skill Shards for " + target.getName().getString() + " (" + (delta >= 0 ? "+" : "") + delta + ")."), true);
        return 1;
    }

    /** Print every giveable id, so the list is discoverable without tab-completing blindly. */
    private static int giveList(CommandContext<CommandSourceStack> ctx) {
        java.util.Set<String> ids = io.github.andrewwwwwwwwwwwwwww.vanillaskills.command.ModItems.all().keySet();
        ctx.getSource().sendSuccess(() -> Component.literal(
                ids.size() + " VanillaSkills items:").withStyle(ChatFormatting.GREEN), false);
        ctx.getSource().sendSuccess(() -> Component.literal(String.join(", ", ids))
                .withStyle(ChatFormatting.GRAY), false);
        return ids.size();
    }

    /**
     * Give one of our items to a player.
     *
     * <p>Counts above a stack are split across several stacks rather than making one oversized stack, so
     * what lands in the inventory behaves like anything else. Anything that will not fit drops at the
     * player's feet rather than vanishing.
     */
    private static int give(CommandContext<CommandSourceStack> ctx, int count, ServerPlayer explicitTarget)
            throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "item");
        ServerPlayer target = explicitTarget != null ? explicitTarget : ctx.getSource().getPlayerOrException();

        ItemStack sample = io.github.andrewwwwwwwwwwwwwww.vanillaskills.command.ModItems.create(id);
        if (sample == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown VanillaSkills item '" + id + "'. Try /skill give list.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int remaining = count;
        while (remaining > 0) {
            ItemStack stack = io.github.andrewwwwwwwwwwwwwww.vanillaskills.command.ModItems.create(id);
            int n = Math.min(remaining, stack.getMaxStackSize());
            stack.setCount(n);
            remaining -= n;
            target.getInventory().placeItemBackInInventory(stack);
        }

        final int given = count;
        final String name = sample.getHoverName().getString();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Gave " + given + " x " + name + " to " + target.getName().getString())
                .withStyle(ChatFormatting.GREEN), true);
        return given;
    }



    private static boolean isOreOrOurs(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (VanillaSkills.SHARDS.kindAt(level, pos) != null) {
            return true;
        }
        if (state.is(net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS)
                || state.is(net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE)) {
            return true;
        }
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getPath().endsWith("_ore");
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        PointsConfig points = PointsConfig.load();
        VanillaSkills.PLAYERS.setPointsConfig(points);
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.load();
        VanillaSkills.TREE.load();
        if (ctx.getSource().getServer() != null) {
            for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                VanillaSkills.PLAYERS.applyAll(player);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Reloaded skill tree (" + VanillaSkills.TREE.tree().size() + " nodes) and Skill Shard config.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setMending(CommandContext<CommandSourceStack> ctx, boolean enabled) {
        // Load the current per-world config (keeps all other settings), flip Mending, and save it.
        // We intentionally do NOT apply it live — it takes effect on the next world load, which the
        // message tells the op to do, so the toggle is predictable and matches the on-disk state.
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig cfg =
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.load();
        cfg.mendingEnabled = enabled;
        cfg.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Mending set to " + (enabled ? "ENABLED" : "REMOVED")
                + " for this world. Restart the world/server for it to take effect."
                + (enabled ? " (Existing villager trades won't change — reroll librarians for new mending offers.)" : ""))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        return 1;
    }


    // ---- op: tree editor ----


    private static SkillNode requireNode(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        SkillNode node = VanillaSkills.TREE.tree().byId(id);
        if (node == null) ctx.getSource().sendFailure(Component.literal("No skill node with id '" + id + "'."));
        return node;
    }



    // ---- lane (category) editing ----

    private static io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillCategory requireCategory(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillCategory cat = VanillaSkills.TREE.tree().category(id);
        if (cat == null) ctx.getSource().sendFailure(Component.literal("No lane with id '" + id + "'."));
        return cat;
    }














}
