package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.ComponentAutofill;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipePlacement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gives each of our recipe-book entries its own craftability requirements.
 *
 * <p>Vanilla computes {@code craftingRequirements} once per <i>recipe</i> and copies it onto every
 * <i>display</i> that recipe publishes. That is fine for vanilla, where a placeable recipe has exactly one
 * display, but several of ours publish many — {@code shard_crafting} alone shows four, and the armour and
 * tool recipes show one per tier. Left alone they would all inherit the first display's cost, so the book
 * would light up the Stable block because you could afford the Unstable one.
 *
 * <p>Runs on the returned list rather than on the construction, so display ids — which are positional — are
 * preserved exactly; only the requirements field of our own entries is swapped.
 */
@Mixin(RecipeManager.class)
public class RecipeCraftabilityMixin {

    @Inject(method = "unpackRecipeInfo", at = @At("RETURN"), cancellable = true)
    private static void vanillaskills$perDisplayCraftability(
            Iterable<RecipeHolder<?>> recipes,
            FeatureFlagSet enabledFeatures,
            CallbackInfoReturnable<List<RecipeManager.ServerDisplayInfo>> cir) {

        List<RecipeManager.ServerDisplayInfo> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List<RecipeManager.ServerDisplayInfo> rebuilt = new ArrayList<>(original.size());
        boolean changed = false;

        for (RecipeManager.ServerDisplayInfo info : original) {
            RecipeDisplayEntry entry = info.display();
            if (ComponentAutofill.handles(info.parent())) {
                Optional<List<Ingredient>> requirements = RecipePlacement.requirementsFor(entry.display());
                if (!requirements.equals(entry.craftingRequirements())) {
                    rebuilt.add(new RecipeManager.ServerDisplayInfo(
                            new RecipeDisplayEntry(entry.id(), entry.display(), entry.group(),
                                    entry.category(), requirements),
                            info.parent()));
                    changed = true;
                    continue;
                }
            }
            rebuilt.add(info);
        }

        if (changed) cir.setReturnValue(rebuilt);
    }
}
