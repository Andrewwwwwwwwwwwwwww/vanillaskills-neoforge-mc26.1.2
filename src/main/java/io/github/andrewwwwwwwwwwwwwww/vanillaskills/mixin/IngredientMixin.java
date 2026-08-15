package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes our custom marked items invisible to every vanilla recipe ingredient. Our alloys/armor/
 * tools/template are marked vanilla items (e.g. a steel ingot is a marked iron ingot), so without
 * this they would satisfy vanilla recipes — letting players craft copper/iron blocks from the
 * ingots, and colliding with vanilla iron armor/tool recipes. Our own recipes match by marker
 * (not Ingredient), so they are unaffected.
 */
@Mixin(Ingredient.class)
public class IngredientMixin {

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$blockMarkedItems(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Markers.isOurs(stack)) {
            cir.setReturnValue(false);
        }
    }
}
