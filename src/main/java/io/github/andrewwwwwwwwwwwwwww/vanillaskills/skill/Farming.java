package io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cultivator skill helper: maps a fully-grown, NATURALLY GROWN harvestable crop to the product item
 * that should be dropped as a bonus. Returns null for immature crops, non-crops, or player-placed
 * blocks (anti-dup — see {@link #naturallyGrown}).
 */
public final class Farming {
    private Farming() {}

    public static Item matureCropProduct(LevelReader level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        // True crops with a growth stage: a freshly-placed one is immature and yields nothing, so
        // these can't be place-and-break duped — the maturity check is the anti-dup here.
        if (block instanceof CropBlock crop) {
            if (!crop.isMaxAge(state)) return null;
            if (block == Blocks.WHEAT) return Items.WHEAT;
            if (block == Blocks.CARROTS) return Items.CARROT;
            if (block == Blocks.POTATOES) return Items.POTATO;
            if (block == Blocks.BEETROOTS) return Items.BEETROOT;
            return null;
        }
        if (block == Blocks.NETHER_WART && state.getValue(NetherWartBlock.AGE) >= 3) {
            return Items.NETHER_WART;
        }
        if (block instanceof CocoaBlock && state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE) {
            return Items.COCOA_BEANS;
        }

        // Melon & pumpkin are placeable blocks (place-and-break would dup), so only reward the bonus
        // when the fruit actually grew from a stem — i.e. an attached melon/pumpkin stem points at it.
        if (block == Blocks.MELON) {
            return naturallyGrown(level, pos) ? Items.MELON_SLICE : null;
        }
        if (block == Blocks.PUMPKIN) {
            return naturallyGrown(level, pos) ? Items.PUMPKIN : null; // capped via bonusCap()
        }

        // Chorus plant blocks can't be placed by players (only grown from a flower on end stone), so
        // there's no place-and-break dup — safe to reward.
        if (block == Blocks.CHORUS_PLANT) return Items.CHORUS_FRUIT;

        // Sugar cane & cactus are intentionally NOT rewarded: placing == harvesting for them, with no
        // reliable placed-vs-grown signal, so any bonus is an infinite dup.
        return null;
    }

    /** True if a melon/pumpkin at {@code pos} has an attached stem facing it (i.e. it grew, wasn't placed). */
    private static boolean naturallyGrown(LevelReader level, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos stemPos = pos.relative(dir);
            BlockState stem = level.getBlockState(stemPos);
            if (stem.getBlock() instanceof AttachedStemBlock
                    && stem.getValue(AttachedStemBlock.FACING) == dir.getOpposite()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Max bonus items a single break may grant for this crop. Whole pumpkins are chunky, so they're
     * capped at +2 regardless of Cultivator level; everything else scales freely with level.
     */
    public static int bonusCap(BlockState state) {
        return state.getBlock() == Blocks.PUMPKIN ? 2 : Integer.MAX_VALUE;
    }

    /**
     * Cultivator bonus roll for right-click harvests (sweet berries, glow berries), which never hit
     * the block-break event — called from the harvest mixins. Same odds as the break-event path:
     * one independent 50% roll per Cultivator level.
     */
    public static void rollBonus(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                                 net.minecraft.server.level.ServerPlayer sp, Item product) {
        int farmLevel = CraftingGate.farmingLevel(sp);
        if (farmLevel <= 0) return;
        int bonus = 0;
        for (int i = 0; i < farmLevel; i++) {
            if (sp.getRandom().nextFloat() < 0.5f) bonus++;
        }
        if (bonus > 0) {
            Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(product, bonus));
        }
    }
}
