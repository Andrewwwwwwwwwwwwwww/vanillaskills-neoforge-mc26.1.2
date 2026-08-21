package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Item-keyed placement data for VanillaSkills' hand-written recipes.
 *
 * <p><b>Why this has to exist.</b> {@code CustomRecipe.placementInfo()} returns
 * {@code PlacementInfo.NOT_PLACEABLE}, and vanilla reads that in two places that between them decide
 * whether a recipe is usable from the book at all:
 *
 * <ul>
 *   <li>{@code ServerGamePacketListenerImpl.handlePlaceRecipe} returns early on
 *       {@code placementInfo().isImpossibleToPlace()} — <b>before</b> {@code handlePlacement}, so clicking
 *       one of our recipes did nothing whatsoever and no amount of work further down the placement path
 *       could have run.</li>
 *   <li>{@code RecipeManager.unpackRecipeInfo} sends {@code placementInfo().ingredients()} to the client as
 *       the recipe's {@code craftingRequirements}. An <i>empty but present</i> list means "needs nothing",
 *       and {@code RecipeDisplayEntry.canCraft} then answers true for every player regardless of inventory —
 *       which is why every one of our recipes lit up as craftable.</li>
 * </ul>
 *
 * <p>The numbers here come from the recipe's own {@link RecipeDisplay}, so the book, the craftable check and
 * the autofill all describe the same recipe.
 *
 * <p><b>What it can and cannot express.</b> An {@link Ingredient} is keyed on item identity alone, so the
 * requirement it publishes is "nine written books", not "nine Unstable Skill Shards". A player holding nine
 * ordinary signed books will therefore still see the recipe highlighted. That over-count is the floor of what
 * is expressible to a vanilla client, and it is a far smaller lie than "craftable, always". The exact
 * component-aware matching is done server-side by {@link ComponentAutofill} when the recipe is clicked.
 */
public final class RecipePlacement {
    private RecipePlacement() {}

    /**
     * Placement data for a recipe, derived from the first grid display it publishes.
     *
     * <p>Only needs to be non-empty for {@code handlePlaceRecipe} to let the click through; the actual layout
     * is then done by {@link ComponentAutofill}, which reads whole stacks rather than Ingredients. Per-display
     * accuracy for the craftable highlight is applied separately by {@code RecipeCraftabilityMixin}, because
     * a recipe has one {@code placementInfo} but may publish many displays.
     */
    public static PlacementInfo placementFor(Recipe<?> recipe) {
        for (RecipeDisplay display : recipe.display()) {
            if (display instanceof ShapedCraftingRecipeDisplay shaped) {
                List<Optional<Ingredient>> cells = cellsOf(shaped);
                if (!cells.isEmpty()) return PlacementInfo.createFromOptionals(cells);
            }
        }
        return PlacementInfo.NOT_PLACEABLE;
    }

    /**
     * The ingredient list a client should test its inventory against for one display, or empty for a display
     * we cannot describe — {@code RecipeDisplayEntry.canCraft} reads an absent value as "never craftable",
     * which is the right answer when we do not know.
     */
    public static Optional<List<Ingredient>> requirementsFor(RecipeDisplay display) {
        if (!(display instanceof ShapedCraftingRecipeDisplay shaped)) return Optional.empty();
        List<Ingredient> required = new ArrayList<>();
        for (Optional<Ingredient> cell : cellsOf(shaped)) {
            cell.ifPresent(required::add);
        }
        return required.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(required));
    }

    /**
     * One entry per grid cell, in the display's own order: the cell's item as an Ingredient, or empty for a
     * blank cell. Returns an empty list if any filled cell is something other than a concrete stack, since a
     * partial requirement would understate the cost.
     */
    private static List<Optional<Ingredient>> cellsOf(ShapedCraftingRecipeDisplay shaped) {
        List<Optional<Ingredient>> cells = new ArrayList<>(shaped.ingredients().size());
        boolean anyFilled = false;
        for (SlotDisplay slot : shaped.ingredients()) {
            if (slot instanceof SlotDisplay.Empty) {
                cells.add(Optional.empty());
                continue;
            }
            if (!(slot instanceof SlotDisplay.ItemStackSlotDisplay stackSlot)) return List.of();
            cells.add(Optional.of(Ingredient.of(stackSlot.stack().item().value())));
            anyFilled = true;
        }
        return anyFilled ? cells : List.of();
    }
}
