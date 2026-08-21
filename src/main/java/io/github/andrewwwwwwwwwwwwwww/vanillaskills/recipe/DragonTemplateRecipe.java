package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import net.minecraft.world.item.crafting.PlacementInfo;
import com.mojang.serialization.MapCodec;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonIngot;
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
 * Duplicates the Dragon Upgrade template (output 2), consuming the surrounding ingredients:
 *
 *   C T C     C = chorus flower    T = existing Dragon Upgrade template
 *   C N C     N = netherite ingot
 *   R S R     R = end rod          S = shulker shell
 */
public class DragonTemplateRecipe extends CustomRecipe {
    public static final DragonTemplateRecipe INSTANCE = new DragonTemplateRecipe();
    public static final RecipeSerializer<DragonTemplateRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, DragonTemplateRecipe>unit(INSTANCE));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        return input.getItem(0).is(Items.CHORUS_FLOWER)
                && input.getItem(2).is(Items.CHORUS_FLOWER)
                && input.getItem(3).is(Items.CHORUS_FLOWER)
                && input.getItem(5).is(Items.CHORUS_FLOWER)
                && DragonUpgradeTemplate.isTemplate(input.getItem(1))
                && input.getItem(4).is(Items.NETHERITE_INGOT) && !DragonIngot.isDragonIngot(input.getItem(4))
                && input.getItem(6).is(Items.END_ROD)
                && input.getItem(8).is(Items.END_ROD)
                && input.getItem(7).is(Items.SHULKER_SHELL);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack out = DragonUpgradeTemplate.create();
        out.setCount(2);
        return out;
    }

    @Override
    public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        net.minecraft.world.item.ItemStack chorus = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.CHORUS_FLOWER);
        net.minecraft.world.item.ItemStack endRod = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.END_ROD);
        net.minecraft.world.item.ItemStack netherite = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.NETHERITE_INGOT);
        net.minecraft.world.item.ItemStack shulker = new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.SHULKER_SHELL);
        net.minecraft.world.item.ItemStack out = DragonUpgradeTemplate.create();
        out.setCount(2);
        return java.util.List.of(RecipeDisplays.shaped(new net.minecraft.world.item.ItemStack[]{
                chorus.copy(), DragonUpgradeTemplate.create(), chorus.copy(),
                chorus.copy(), netherite.copy(), chorus.copy(),
                endRod.copy(), shulker.copy(), endRod.copy()}, out,
                net.minecraft.world.item.Items.CRAFTING_TABLE));
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    @Override
    public RecipeSerializer<DragonTemplateRecipe> getSerializer() {
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
