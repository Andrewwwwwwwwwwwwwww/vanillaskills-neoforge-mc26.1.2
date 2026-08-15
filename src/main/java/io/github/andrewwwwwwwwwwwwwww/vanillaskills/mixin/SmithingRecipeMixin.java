package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.FortuneTemplate;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops the Fortune Upgrade template from functioning in the smithing table. The template is
 * (under the hood) a vanilla netherite upgrade template, so without this it could still be used
 * to apply a netherite upgrade. {@code matches} is a default method on the SmithingRecipe
 * interface used by both transform (netherite upgrade) and trim recipes, so blocking it here
 * covers every smithing recipe when our marked template is in the template slot.
 */
@Mixin(SmithingRecipe.class)
public interface SmithingRecipeMixin {

    @Inject(
            method = "matches(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void vanillaskills$blockFortuneTemplate(SmithingRecipeInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (FortuneTemplate.isTemplate(input.template())
                || io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.DragonUpgradeTemplate.isTemplate(input.template())) {
            cir.setReturnValue(false);
        }
    }
}
