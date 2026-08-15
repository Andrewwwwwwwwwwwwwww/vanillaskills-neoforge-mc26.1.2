package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks the vanilla grid repair-combine recipe for our marked gear. Custom tools/armor are marked
 * vanilla items, so RepairItemRecipe happily combined two damaged Steel swords into a FRESH vanilla
 * sword — stripping the marker, name, and model (i.e. "reverting to the base item"). Combining
 * custom gear now simply doesn't match in the grid; the anvil remains the way to merge/repair it.
 */
@Mixin(RepairItemRecipe.class)
public abstract class RepairCombineMixin {

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"), cancellable = true)
    private void vanillaskills$blockMarkedCombine(CraftingInput input, Level level,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        for (int i = 0; i < input.size(); i++) {
            if (Markers.isOurs(input.getItem(i))) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
