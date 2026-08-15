package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe-book autofill for recipes whose ingredients are identified by data components.
 *
 * <p><b>Why vanilla cannot do this.</b> Autofill works through {@code StackedItemContents}, which is
 * {@code StackedContents<Holder<Item>>}, and {@code Ingredient} exposes {@code acceptsItem(Holder<Item>)}.
 * Both are keyed on <b>item identity alone</b> — components are structurally invisible to them. Our
 * ingredients are vanilla items distinguished only by a {@code vs_*} marker, so vanilla's matcher either
 * cannot see them or, worse, would happily grab the unmarked vanilla lookalike and produce a grid our own
 * {@code matches()} then rejects.
 *
 * <p><b>Why this can work anyway.</b> Placement is entirely server-side: the client sends
 * {@code ServerboundPlaceRecipePacket} and {@code ServerPlaceRecipe} does the moving. So the grid can be
 * filled correctly even though the client had no way to know it was possible.
 *
 * <p>The source of truth is the recipe's own {@link RecipeDisplay} — the same data that draws it in the book.
 * That already contains real {@code ItemStack}s with components, which is exactly what matching needs and
 * exactly what {@code Ingredient} cannot express.
 *
 * <p>⚠ The book may still show these recipes greyed out, because craftability highlighting is computed
 * client-side by that same component-blind matcher. Clicking works regardless.
 */
public final class ComponentAutofill {
    private ComponentAutofill() {}

    /** True if this is one of ours — a {@link CustomRecipe} that publishes a display we can read. */
    public static boolean handles(RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        if (!(recipe instanceof CustomRecipe)) return false;
        List<RecipeDisplay> displays = recipe.display();
        if (displays.isEmpty()) return false;
        // Only shaped grids: the shapeless path has no slot positions to fill.
        return displays.stream().anyMatch(d -> d instanceof ShapedCraftingRecipeDisplay);
    }

    /**
     * Fill the crafting grid for one of our recipes.
     *
     * <p>Tries each published display in turn — a tier recipe publishes one per material, so the first the
     * player can actually afford is the one used. Nothing is moved unless a whole display can be satisfied,
     * so a partial fill can never leave the grid in a state that crafts the wrong thing.
     *
     * @return true if the grid was filled
     */
    public static boolean fill(RecipeHolder<?> holder, Inventory inventory, List<Slot> gridSlots,
                               net.minecraft.world.level.Level level, boolean creative) {
        // fromLevel supplies both parameters a SlotDisplay can ask for (registries and fuel values), so
        // resolving never fails for want of context.
        ContextMap context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level);

        for (RecipeDisplay display : holder.value().display()) {
            if (!(display instanceof ShapedCraftingRecipeDisplay shaped)) continue;
            if (tryFill(shaped, context, inventory, gridSlots, creative)) return true;
        }
        return false;
    }

    private static boolean tryFill(ShapedCraftingRecipeDisplay shaped, ContextMap context,
                                   Inventory inventory, List<Slot> gridSlots, boolean creative) {
        int width = shaped.width();
        int height = shaped.height();
        int gridSize = (int) Math.sqrt(gridSlots.size());
        if (width > gridSize || height > gridSize) return false;

        // Resolve the display into the concrete stack wanted per grid position.
        List<ItemStack> wanted = new ArrayList<>();
        for (SlotDisplay slot : shaped.ingredients()) {
            List<ItemStack> options = slot.resolveForStacks(context);
            wanted.add(options.isEmpty() ? ItemStack.EMPTY : options.get(0));
        }

        // Plan the whole fill before touching anything: find a source slot for every non-empty cell, with
        // no slot used twice. Only commit once the plan is complete.
        int[] source = new int[wanted.size()];
        boolean[] taken = new boolean[inventory.getContainerSize()];
        for (int i = 0; i < wanted.size(); i++) {
            ItemStack need = wanted.get(i);
            if (need.isEmpty()) {
                source[i] = -1;
                continue;
            }
            int found = findMatching(inventory, need, taken);
            if (found < 0 && !creative) return false;   // cannot complete this display
            source[i] = found;
            if (found >= 0) taken[found] = true;
        }

        for (int i = 0; i < wanted.size(); i++) {
            ItemStack need = wanted.get(i);
            if (need.isEmpty()) continue;
            int row = i / width;
            int col = i % width;
            int gridIndex = row * gridSize + col;
            if (gridIndex >= gridSlots.size()) continue;

            ItemStack place;
            if (source[i] >= 0) {
                ItemStack from = inventory.getItem(source[i]);
                place = from.split(1);
            } else {
                place = need.copy();                    // creative: conjure it
                place.setCount(1);
            }
            gridSlots.get(gridIndex).set(place);
        }
        return true;
    }

    /**
     * First inventory slot holding a stack that is the same item AND carries the same components.
     *
     * <p>The component check is the entire point: a plain amethyst shard and an Unstable Skill Shard are the
     * same item, and picking the wrong one produces a grid that looks right and crafts nothing.
     */
    private static int findMatching(Inventory inventory, ItemStack need, boolean[] taken) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (taken[i]) continue;
            ItemStack candidate = inventory.getItem(i);
            if (candidate.isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(candidate, need)) return i;
        }
        return -1;
    }

}
