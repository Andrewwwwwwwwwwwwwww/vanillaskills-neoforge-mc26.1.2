package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The two Skill Shard compression recipes.
 *
 * <pre>
 *   S S S            R G R
 *   S S S  -> USSB   . U .  -> SSSB
 *   S S S            R G R
 *
 *   S = Unstable Skill Shard   U = Unstable Skill Shard Block
 *   R = redstone               G = tinted glass
 * </pre>
 *
 * <p>Both are {@link CustomRecipe}s rather than data-driven shaped recipes because their inputs are
 * component-marked items, and a vanilla {@code Ingredient} is a bare {@code HolderSet<Item>} with no
 * component predicate — it cannot tell an Unstable Skill Shard from a plain amethyst shard. That is the
 * same reason the armour and tool recipes are hand-written.
 */
public class ShardCraftingRecipe extends CustomRecipe {
    public static final ShardCraftingRecipe INSTANCE = new ShardCraftingRecipe();
    public static final RecipeSerializer<ShardCraftingRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, ShardCraftingRecipe>unit(INSTANCE));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        // Decompress first, and BEFORE the 3x3 guard. CraftingInput is trimmed to the occupied bounding box,
        // so a single block on its own arrives as width=1/height=1 and never reaches the 3x3 patterns.
        ItemStack loose = matchDecompress(input);
        if (!loose.isEmpty()) return loose;

        if (input.width() != 3 || input.height() != 3) return ItemStack.EMPTY;
        ItemStack compressed = matchCompress(input);
        if (!compressed.isEmpty()) return compressed;
        ItemStack stabilised = matchStabilise(input);
        if (!stabilised.isEmpty()) return stabilised;
        return matchCrystallizedDiamond(input);
    }

    /**
     * One Unstable Skill Shard Block on its own, back into the nine shards that made it.
     *
     * <p>The reverse of {@link #matchCompress}. Vanilla gives every compressed block an uncrafting recipe;
     * without one, storing shards as blocks would be a one-way trip and nobody would risk it. Matched here
     * rather than as a data recipe because the block is a component-marked item, which a vanilla
     * {@code Ingredient} cannot see.
     */
    private static ItemStack matchDecompress(CraftingInput input) {
        int found = -1;
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).isEmpty()) continue;
            if (found >= 0) return ItemStack.EMPTY; // more than one item in the grid
            found = i;
        }
        if (found < 0 || !ShardItems.isUnstableBlock(input.getItem(found))) return ItemStack.EMPTY;
        ItemStack out = ShardItems.unstableShard();
        out.setCount(ShardItems.SHARDS_PER_BLOCK);
        return out;
    }

    /**
     * Crystallized Diamond: amethyst shards in the corners, Unstable Skill Shards top and bottom middle,
     * diamonds flanking an amethyst block.
     *
     * <pre>
     *   A U A     A = amethyst shard   U = Unstable Skill Shard
     *   D B D     D = diamond          B = amethyst block
     *   A U A
     * </pre>
     *
     * <p>Lives here rather than in its own data recipe because the two shard slots are component-marked
     * items, which a vanilla {@code Ingredient} cannot match. That is also why the old
     * {@code crystallized_diamond.json} had to go.
     */
    private static ItemStack matchCrystallizedDiamond(CraftingInput input) {
        for (int corner : new int[]{0, 2, 6, 8}) {
            if (!input.getItem(corner).is(Items.AMETHYST_SHARD)
                    || ShardItems.isUnstableShard(input.getItem(corner))) {
                return ItemStack.EMPTY; // a plain shard, not one of ours
            }
        }
        if (!ShardItems.isUnstableShard(input.getItem(1))) return ItemStack.EMPTY;
        if (!ShardItems.isUnstableShard(input.getItem(7))) return ItemStack.EMPTY;
        if (!input.getItem(3).is(Items.DIAMOND) || !input.getItem(5).is(Items.DIAMOND)) return ItemStack.EMPTY;
        if (!input.getItem(4).is(Items.AMETHYST_BLOCK)) return ItemStack.EMPTY;

        ItemStack out = io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys.crystallizedDiamond();
        out.setCount(2);
        return out;
    }

    /** Nine Unstable Skill Shards filling the grid. */
    private static ItemStack matchCompress(CraftingInput input) {
        for (int i = 0; i < 9; i++) {
            if (!ShardItems.isUnstableShard(input.getItem(i))) return ItemStack.EMPTY;
        }
        return ShardItems.unstableBlock();
    }

    /** Redstone in the corners, tinted glass top and bottom middle, an Unstable block in the centre. */
    private static ItemStack matchStabilise(CraftingInput input) {
        for (int corner : new int[]{0, 2, 6, 8}) {
            if (!input.getItem(corner).is(Items.REDSTONE)) return ItemStack.EMPTY;
        }
        for (int glass : new int[]{1, 7}) {
            if (!input.getItem(glass).is(Items.TINTED_GLASS)) return ItemStack.EMPTY;
        }
        if (!ShardItems.isUnstableBlock(input.getItem(4))) return ItemStack.EMPTY;
        // The two side cells must be empty, so this cannot collide with a fuller pattern.
        if (!input.getItem(3).isEmpty() || !input.getItem(5).isEmpty()) return ItemStack.EMPTY;
        return ShardItems.stableBlock();
    }

    /** All four patterns, published to the vanilla recipe book. */
    @Override
    public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        ItemStack uss = ShardItems.unstableShard();
        ItemStack redstone = new ItemStack(Items.REDSTONE);
        ItemStack glass = new ItemStack(Items.TINTED_GLASS);
        ItemStack shard = new ItemStack(Items.AMETHYST_SHARD);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack e = ItemStack.EMPTY;

        ItemStack crystal = io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys.crystallizedDiamond();
        crystal.setCount(2);

        ItemStack nineShards = ShardItems.unstableShard();
        nineShards.setCount(ShardItems.SHARDS_PER_BLOCK);

        return java.util.List.of(
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipeDisplays.shaped(
                        new ItemStack[]{uss.copy(), uss.copy(), uss.copy(),
                                        uss.copy(), uss.copy(), uss.copy(),
                                        uss.copy(), uss.copy(), uss.copy()},
                        ShardItems.unstableBlock()),
                // The reverse: one block, anywhere in the grid, back into nine shards. Shown as 1x1 so the
                // book does not imply the block has to sit in a particular cell.
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipeDisplays.shaped(
                        1, 1, new ItemStack[]{ShardItems.unstableBlock()},
                        nineShards, Items.CRAFTING_TABLE),
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipeDisplays.shaped(
                        new ItemStack[]{redstone.copy(), glass.copy(), redstone.copy(),
                                        e, ShardItems.unstableBlock(), e,
                                        redstone.copy(), glass.copy(), redstone.copy()},
                        ShardItems.stableBlock()),
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipeDisplays.shaped(
                        new ItemStack[]{shard.copy(), uss.copy(), shard.copy(),
                                        diamond.copy(), new ItemStack(Items.AMETHYST_BLOCK), diamond.copy(),
                                        shard.copy(), uss.copy(), shard.copy()},
                        crystal));
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<ShardCraftingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
