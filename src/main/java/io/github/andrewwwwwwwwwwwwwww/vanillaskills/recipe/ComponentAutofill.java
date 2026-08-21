package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import net.minecraft.core.NonNullList;
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
 * <p><b>Every item placed is an item taken.</b> Ingredients only ever move out of the player's inventory;
 * nothing here can create one. That is not a detail — an earlier version conjured missing ingredients for
 * creative players, which handed out free Rose Gold and Steel from the book, so the rule is now absolute
 * and there is no gamemode that relaxes it. A cell the player cannot pay for fails the whole fill, and a
 * failed fill moves nothing at all.
 *
 * <p>⚠ The highlight the book draws is still only as precise as an item id. A recipe is shown craftable
 * when the player holds the right base items, which for a marked ingredient can be one item too generous —
 * gold ingots read as Rose Gold Ingots. Clicking is where the components are actually checked, so an
 * over-optimistic highlight costs a ghost recipe, not a wrong craft and not a free ingot.
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
     * The recipe-book entry the player actually clicked, handed over by {@code PlaceRecipeDisplayMixin}.
     *
     * <p>Vanilla drops it before placement because a placeable vanilla recipe only ever has one display.
     * Server thread only, written and read within a single packet, and consumed on read.
     */
    private static RecipeDisplay clickedDisplay;

    public static void rememberClickedDisplay(RecipeDisplay display) {
        clickedDisplay = display;
    }

    public static void forgetClickedDisplay() {
        clickedDisplay = null;
    }

    /**
     * Fill the crafting grid for one of our recipes.
     *
     * <p>Fills the display the player clicked. Only when that is unknown — a placement not reached through
     * the recipe book — does it fall back to trying each published display in turn.
     *
     * @param useMaxItems shift-click: lay out as many complete copies as the inventory can pay for
     * @return true if the grid was filled
     */
    public static boolean fill(RecipeHolder<?> holder, Inventory inventory, List<Slot> gridSlots,
                               int gridWidth, int gridHeight,
                               net.minecraft.world.level.Level level, boolean useMaxItems) {
        // fromLevel supplies both parameters a SlotDisplay can ask for (registries and fuel values), so
        // resolving never fails for want of context.
        ContextMap context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level);

        RecipeDisplay clicked = clickedDisplay;
        clickedDisplay = null;
        if (clicked != null) {
            // The player named a variant; failing it must not silently lay out a different one.
            return clicked instanceof ShapedCraftingRecipeDisplay shaped
                    && tryFill(shaped, context, inventory, gridSlots, gridWidth, gridHeight, useMaxItems);
        }

        for (RecipeDisplay display : holder.value().display()) {
            if (!(display instanceof ShapedCraftingRecipeDisplay shaped)) continue;
            if (tryFill(shaped, context, inventory, gridSlots, gridWidth, gridHeight, useMaxItems)) return true;
        }
        return false;
    }

    private static boolean tryFill(ShapedCraftingRecipeDisplay shaped, ContextMap context,
                                   Inventory inventory, List<Slot> gridSlots,
                                   int gridWidth, int gridHeight, boolean useMaxItems) {
        int width = shaped.width();
        int height = shaped.height();
        if (width > gridWidth || height > gridHeight) return false;
        if (shaped.ingredients().size() != width * height) return false;
        if (gridWidth * gridHeight > gridSlots.size()) return false;

        // Resolve the display into the concrete stack wanted per grid position.
        List<ItemStack> wanted = new ArrayList<>(shaped.ingredients().size());
        for (SlotDisplay slot : shaped.ingredients()) {
            List<ItemStack> options = slot.resolveForStacks(context);
            wanted.add(options.isEmpty() ? ItemStack.EMPTY : options.get(0));
        }

        // Group cells by what they want, and count how many cells want each thing. Demand has to be counted
        // per ingredient rather than per inventory slot: nine Skill Shards sitting in one stack must be able
        // to fill nine cells, which the old one-slot-per-cell search could not do.
        List<ItemStack> distinct = new ArrayList<>();
        List<Integer> demand = new ArrayList<>();
        int[] group = new int[wanted.size()];
        for (int i = 0; i < wanted.size(); i++) {
            ItemStack need = wanted.get(i);
            if (need.isEmpty()) {
                group[i] = -1;
                continue;
            }
            int found = -1;
            for (int d = 0; d < distinct.size(); d++) {
                if (ItemStack.isSameItemSameComponents(distinct.get(d), need)) {
                    found = d;
                    break;
                }
            }
            if (found < 0) {
                distinct.add(need);
                demand.add(0);
                found = distinct.size() - 1;
            }
            group[i] = found;
            demand.set(found, demand.get(found) + 1);
        }
        if (distinct.isEmpty()) return false;

        // Equipment is deliberately out of reach — sourcing from getContainerSize() would strip worn armour
        // to pay for a craft.
        NonNullList<ItemStack> items = inventory.getNonEquipmentItems();

        // How many complete copies the player can actually pay for, decided before anything moves.
        int copies = Integer.MAX_VALUE;
        for (int d = 0; d < distinct.size(); d++) {
            copies = Math.min(copies, countMatching(items, distinct.get(d)) / demand.get(d));
            copies = Math.min(copies, distinct.get(d).getMaxStackSize());
        }
        if (copies < 1) return false;          // an ingredient is missing: move nothing
        if (!useMaxItems) copies = 1;

        for (int i = 0; i < wanted.size(); i++) {
            if (group[i] < 0) continue;
            int gridIndex = (i / width) * gridWidth + (i % width);
            gridSlots.get(gridIndex).set(pull(items, wanted.get(i), copies));
        }
        inventory.setChanged();
        return true;
    }

    /**
     * How many of this exact ingredient — same item AND same components — the player is carrying.
     *
     * <p>The component check is the entire point: a plain gold ingot and a Rose Gold Ingot are the same
     * item, and counting the wrong one is what makes the book over-promise.
     */
    private static int countMatching(NonNullList<ItemStack> items, ItemStack need) {
        int total = 0;
        for (ItemStack candidate : items) {
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, need)) {
                total += candidate.getCount();
            }
        }
        return total;
    }

    /**
     * Take {@code count} of an ingredient out of the inventory, across as many stacks as it takes.
     *
     * <p>Only ever called once {@link #countMatching} has proved the whole fill is affordable, so it cannot
     * come up short and cannot be the thing that invents an item.
     */
    private static ItemStack pull(NonNullList<ItemStack> items, ItemStack need, int count) {
        ItemStack out = ItemStack.EMPTY;
        int remaining = count;
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack candidate = items.get(i);
            if (candidate.isEmpty() || !ItemStack.isSameItemSameComponents(candidate, need)) continue;
            ItemStack taken = candidate.split(Math.min(remaining, candidate.getCount()));
            if (candidate.isEmpty()) items.set(i, ItemStack.EMPTY);
            remaining -= taken.getCount();
            if (out.isEmpty()) out = taken;
            else out.grow(taken.getCount());
        }
        return out;
    }
}
