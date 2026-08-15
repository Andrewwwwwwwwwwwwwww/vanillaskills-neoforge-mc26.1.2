package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTiers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes anvil repair of our custom tiers require the tier's own material. Our alloys are marked
 * copper ingots, and the vanilla {@code repairable} component matches by item type only — so
 * without this, plain copper (or the other copper alloy) would also repair Rose Gold / Steel.
 *
 * When the item being repaired carries a tier marker, validity is decided solely by that tier's
 * material predicate (e.g. Rose Gold armor only accepts Rose Gold Ingots). Non-tier items are
 * untouched and use vanilla behaviour.
 */
@Mixin(ItemStack.class)
public class ItemStackRepairMixin {

    @Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$tierMaterialRepair(ItemStack candidate, CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        for (ArmorTier tier : ArmorTiers.TIERS) {
            if (Markers.has(self, tier.markerKey)) {
                cir.setReturnValue(tier.material.test(candidate));
                return;
            }
        }
        for (ToolTier tier : ToolTiers.TIERS) {
            if (Markers.has(self, tier.markerKey)) {
                cir.setReturnValue(tier.material.test(candidate));
                return;
            }
        }
        if (Markers.has(self, io.github.andrewwwwwwwwwwwwwww.vanillaskills.shield.SteelShield.MARKER)) {
            cir.setReturnValue(io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys.isSteelIngot(candidate));
        }
    }
}
