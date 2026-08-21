package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.ComponentAutofill;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Makes clicking one of our recipes in the recipe book actually fill the grid.
 *
 * <p>Vanilla's placement is driven by {@code StackedItemContents}, which is keyed on item identity alone —
 * see {@link ComponentAutofill} for why that can never match a component-marked ingredient. Left to itself it
 * either places nothing or grabs the unmarked vanilla lookalike, producing a grid that looks correct and
 * crafts nothing.
 *
 * <p>Intercepting here works because recipe placement is <b>server-side</b>: the client only sends
 * {@code ServerboundPlaceRecipePacket}, and everything after that is ours to decide. That is the difference
 * between this and the several client-computed things in this mod that genuinely cannot be fixed from a
 * server (block-breaking speed, the anvil cost label).
 *
 * <p>Falls through to vanilla for every recipe that is not ours, and also whenever the fill cannot be
 * completed — a half-filled grid would be worse than an empty one.
 */
@Mixin(ServerPlaceRecipe.class)
public class RecipeAutofillMixin {

    @Inject(method = "placeRecipe", at = @At("HEAD"), cancellable = true)
    private static void vanillaskills$autofillComponentRecipes(
            ServerPlaceRecipe.CraftingMenuAccess<?> menu,
            int gridWidth,
            int gridHeight,
            List<Slot> inputGridSlots,
            List<Slot> slotsToClear,
            Inventory inventory,
            RecipeHolder<?> recipe,
            boolean useMaxItems,
            boolean allowDroppingItemsToClear,
            CallbackInfoReturnable<RecipeBookMenu.PostPlaceAction> cir) {

        if (!ComponentAutofill.handles(recipe)) return;

        // Clear first, exactly as vanilla does, so a partially-filled grid cannot combine with the new fill.
        // The items go back to the player rather than being wiped: this runs before the fill is known to be
        // affordable, so whatever was already on the grid must survive a fill that turns out to fail.
        for (Slot slot : slotsToClear) {
            ItemStack existing = slot.getItem();
            if (existing.isEmpty()) continue;
            ItemStack returned = existing.copy();
            slot.set(ItemStack.EMPTY);
            inventory.placeItemBackInInventory(returned, false);
        }
        menu.clearCraftingContent();

        boolean filled = ComponentAutofill.fill(recipe, inventory, inputGridSlots, gridWidth, gridHeight,
                inventory.player.level(), useMaxItems);

        // Either way this recipe is handled: vanilla's matcher would only undo the work or place the wrong
        // items. A failed fill simply leaves the grid empty and the client draws a ghost recipe.
        cir.setReturnValue(filled
                ? RecipeBookMenu.PostPlaceAction.NOTHING
                : RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE);
    }
}
