package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for publishing VanillaSkills' hand-written recipes into the <b>vanilla</b> recipe book.
 *
 * <p>These recipes are {@code CustomRecipe}s because their inputs are component-marked items, which a
 * vanilla {@code Ingredient} cannot express. That has always kept them out of the recipe book — but only
 * the <i>matching</i> half needs an Ingredient. Display is separate: {@link SlotDisplay} resolves to whole
 * {@link ItemStack}s, components and all, so the book can show a marked Steel Ingot with its own texture.
 *
 * <p>Autofill needs one more thing on top of this: {@code placementInfo()}, which is item-keyed and which
 * {@code CustomRecipe} leaves empty — an empty one makes vanilla drop the click before placement is even
 * attempted. {@code RecipePlacement} derives a real one from these same displays.
 */
public final class RecipeDisplays {
    private RecipeDisplays() {}

    /** One grid cell. Empty stacks become the empty slot rather than an empty-stack template. */
    public static SlotDisplay slot(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? SlotDisplay.Empty.INSTANCE
                : new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(stack));
    }

    /** A book entry of any grid size. {@code grid} is row-major and may contain nulls or empty stacks. */
    public static RecipeDisplay shaped(int width, int height, ItemStack[] grid, ItemStack result, Item station) {
        List<SlotDisplay> slots = new ArrayList<>(width * height);
        for (int i = 0; i < width * height; i++) slots.add(slot(i < grid.length ? grid[i] : ItemStack.EMPTY));
        return new ShapedCraftingRecipeDisplay(width, height, slots, slot(result), slot(new ItemStack(station)));
    }

    /** A 3x3 book entry. {@code grid} is row-major and may contain nulls or empty stacks. */
    public static RecipeDisplay shaped(ItemStack[] grid, ItemStack result, Item station) {
        return shaped(3, 3, grid, result, station);
    }

    /** A 3x3 book entry crafted at a normal crafting table. */
    public static RecipeDisplay shaped(ItemStack[] grid, ItemStack result) {
        return shaped(grid, result, Items.CRAFTING_TABLE);
    }
}
