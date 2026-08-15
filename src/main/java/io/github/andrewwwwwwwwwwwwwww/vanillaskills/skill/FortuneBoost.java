package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Makes Fortune IV/V feel like real upgrades on ores. Vanilla's ore_drops formula only nudges the
 * average per level (diamond ore: III≈2.2x, IV≈2.5x, V≈2.83x), so the minted IV/V books barely
 * out-perform III. This grants one guaranteed extra BASE drop roll (un-multiplied, as if mined with
 * Fortune 0) per level above III when an ore is broken with the correct tool: IV = +1 roll, V = +2.
 * Diamond ore averages become IV≈3.5x and V≈4.8x. Runs on the pre-break event (state still live).
 */
public final class FortuneBoost {
    private FortuneBoost() {}

    private static final Set<Block> ORES = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.NETHER_QUARTZ_ORE);

    /** Chance for Fortune V to yield a bonus Ancient Debris (which normally ignores Fortune entirely). */
    private static final float ANCIENT_DEBRIS_V_CHANCE = 0.005f; // 0.5%

    /** Call from the pre-break handler; spawns the bonus base drops for Fortune IV/V tools. */
    public static void onBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.FORTUNE_BOOST) return;
        if (player.isCreative()) return;
        ItemStack tool = player.getMainHandItem();
        int fortune = fortuneLevel(tool);
        if (fortune <= 3) return;
        if (!player.hasCorrectToolForDrops(state)) return; // no free drops via un-harvestable picks

        Block block = state.getBlock();

        // Ancient Debris: vanilla Fortune does nothing here. Give Fortune V a flat 0.5% chance at a
        // second debris — a rare bonus, not a reliable multiplier.
        if (block == Blocks.ANCIENT_DEBRIS) {
            if (fortune >= 5 && level.getRandom().nextFloat() < ANCIENT_DEBRIS_V_CHANCE) {
                Block.popResource(level, pos, new ItemStack(Blocks.ANCIENT_DEBRIS));
            }
            return;
        }

        if (!ORES.contains(block)) return;
        for (int i = 3; i < fortune; i++) {
            // One CHANCE at an extra base drop per level above III, not a guaranteed one.
            //
            // Guaranteed drops stacked far too high in practice: a Fortune V pick already feeds level 5 into
            // vanilla's own ore_drops formula, and two more certain drops on top of that turned every ore
            // into a windfall. A coin flip per level keeps V ahead of IV and IV ahead of III without the
            // yield running away. Configurable, so this is tunable without another build.
            if (level.getRandom().nextFloat() >= io.github.andrewwwwwwwwwwwwwww.vanillaskills.config
                    .GameplayConfig.FORTUNE_BONUS_CHANCE) {
                continue;
            }
            // EMPTY tool = a plain un-enchanted roll of the ore's own loot (no fortune multiplication,
            // no silk-touch branch) — one honest extra base drop.
            for (ItemStack drop : Block.getDrops(state, level, pos, null, player, ItemStack.EMPTY)) {
                Block.popResource(level, pos, drop);
            }
        }
    }

    private static int fortuneLevel(ItemStack tool) {
        ItemEnchantments ench = tool.get(DataComponents.ENCHANTMENTS);
        if (ench == null || ench.isEmpty()) return 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> e : ench.entrySet()) {
            if (e.getKey().is(Enchantments.FORTUNE)) return e.getIntValue();
        }
        return 0;
    }
}
