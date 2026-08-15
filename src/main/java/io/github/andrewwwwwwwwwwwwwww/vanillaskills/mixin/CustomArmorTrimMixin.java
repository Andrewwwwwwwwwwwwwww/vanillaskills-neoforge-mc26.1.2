package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Lets our custom armour (Rose Gold / Crystalline / Dragon ...) be trimmed in the smithing table.
 *
 * <p>Custom armour is a vanilla base item stamped with a hidden marker, and {@link IngredientMixin}
 * hides every marked item from {@code Ingredient.test} so our alloys can't satisfy vanilla recipes.
 * That same block makes the vanilla trim recipe's base ingredient ({@code #trimmable_armor}) reject
 * our armour, so no recipe matches and the result clears.
 *
 * <p>We restore trims by matching the trim recipe ourselves on the template + addition only (both
 * vanilla items, so not blocked), then running {@link SmithingTrimRecipe#assemble} on the marked
 * base — which copies the base and adds the trim component, preserving the custom identity. We skip
 * only the base-ingredient check, which is the one our marker would fail. Vanilla armour is left to
 * the normal recipe path; this runs only for our marked pieces.
 */
@Mixin(SmithingMenu.class)
public abstract class CustomArmorTrimMixin {

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$trimCustomArmor(CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack base = self.getSlot(SmithingMenu.BASE_SLOT).getItem();
        if (!Markers.isOurs(base)) return;

        ItemStack template = self.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack addition = self.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        if (template.isEmpty() || addition.isEmpty()) return;
        // a trim needs a material-providing addition; anything else (e.g. a Dragon upgrade) isn't ours
        if (!addition.has(DataComponents.PROVIDES_TRIM_MATERIAL)) return;

        Player player = ((ItemCombinerMenuAccessor) self).vanillaskills$getPlayer();
        if (!(player.level() instanceof ServerLevel server)) return;

        for (RecipeHolder<?> holder : server.recipeAccess().getRecipes()) {
            if (!(holder.value() instanceof SmithingTrimRecipe trim)) continue;
            Optional<Ingredient> tmpl = trim.templateIngredient();
            Optional<Ingredient> add = trim.additionIngredient();
            // match on template + addition (vanilla, unblocked); skip the base check that our marker fails
            if (tmpl.isEmpty() || !tmpl.get().test(template)) continue;
            if (add.isEmpty() || !add.get().test(addition)) continue;

            // assemble() runs applyTrim() on the marked base -> custom piece + trim component.
            // Returns EMPTY when that exact trim is already applied, which correctly clears the slot.
            self.getSlot(SmithingMenu.RESULT_SLOT).set(trim.assemble(new SmithingRecipeInput(template, base, addition)));
            ci.cancel();
            return;
        }
    }
}
