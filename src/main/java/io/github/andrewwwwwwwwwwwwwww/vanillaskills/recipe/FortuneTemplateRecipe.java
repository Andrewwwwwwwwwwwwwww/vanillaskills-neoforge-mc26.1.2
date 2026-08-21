package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import net.minecraft.world.item.crafting.PlacementInfo;
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
 * Duplicates the Fortune Upgrade template (output 2), consuming the surrounding ingredients:
 *
 *   G T G     G = glow berries
 *   S D S     T = Fortune Upgrade template
 *   G E G     S = sculk
 *             D = diamond block
 *             E = emerald block
 */
public class FortuneTemplateRecipe extends CustomRecipe {
    public static final FortuneTemplateRecipe INSTANCE = new FortuneTemplateRecipe();
    public static final RecipeSerializer<FortuneTemplateRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, FortuneTemplateRecipe>unit(INSTANCE));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        return input.getItem(0).is(Items.GLOW_BERRIES)
                && input.getItem(2).is(Items.GLOW_BERRIES)
                && input.getItem(6).is(Items.GLOW_BERRIES)
                && input.getItem(8).is(Items.GLOW_BERRIES)
                && input.getItem(3).is(Items.SCULK)
                && input.getItem(5).is(Items.SCULK)
                && input.getItem(4).is(Items.DIAMOND_BLOCK)
                && input.getItem(7).is(Items.EMERALD_BLOCK)
                && FortuneTemplate.isTemplate(input.getItem(1));
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack out = FortuneTemplate.create();
        out.setCount(2);
        return out;
    }

    @Override
    public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        net.minecraft.world.item.ItemStack berry = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.GLOW_BERRIES);
        net.minecraft.world.item.ItemStack sculk = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.SCULK);
        net.minecraft.world.item.ItemStack diaBlock = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.DIAMOND_BLOCK);
        net.minecraft.world.item.ItemStack emeraldBlock = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.EMERALD_BLOCK);
        net.minecraft.world.item.ItemStack out = FortuneTemplate.create();
        out.setCount(2);
        return java.util.List.of(RecipeDisplays.shaped(new net.minecraft.world.item.ItemStack[]{
                berry.copy(), FortuneTemplate.create(), berry.copy(),
                sculk.copy(), diaBlock.copy(), sculk.copy(),
                berry.copy(), emeraldBlock.copy(), berry.copy()}, out,
                net.minecraft.world.item.Items.CRAFTING_TABLE));
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<FortuneTemplateRecipe> getSerializer() {
        return SERIALIZER;
    }

    /**
     * Show this recipe in the recipe book.
     *
     * <p>{@link net.minecraft.world.item.crafting.CustomRecipe} declares itself special, and vanilla excludes
     * a special recipe from the book entirely — which is why these never appeared no matter what the player
     * was carrying. Unlocking worked the whole time; there was simply nothing to render.
     *
     * <p>"Special" exists for recipes whose result depends on inputs the book cannot draw, such as firework
     * or shulker-box dyeing. Ours have a fixed, drawable result and publish a {@code display()} for it, so
     * they are ordinary recipes as far as the book is concerned.
     */
    @Override
    public boolean isSpecial() {
        return false;
    }


    /**
     * Publish real, item-keyed placement data instead of {@code CustomRecipe}'s {@code NOT_PLACEABLE}.
     *
     * <p>Without this the recipe is unusable from the book in both directions: the packet handler drops the
     * click before placement is ever attempted, and the client is told the recipe costs nothing and so lights
     * it up as craftable for everybody. See {@code RecipePlacement} for what an Ingredient can and cannot say
     * about a component-marked item.
     */
    @Override
    public PlacementInfo placementInfo() {
        return RecipePlacement.placementFor(this);
    }
}
