package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTiers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.DyeRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hardwood armor is a reskinned leather item, so without this it could still be dyed like leather.
 * Since it's effectively a different armor, this blocks the leather-armor dye recipe whenever a
 * Hardwood piece is in the grid.
 */
@Mixin(DyeRecipe.class)
public class DyeRecipeMixin {

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void vanillaskills$noDyeHardwood(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        for (int i = 0; i < input.size(); i++) {
            if (Markers.has(input.getItem(i), ArmorTiers.HARDWOOD.markerKey)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
