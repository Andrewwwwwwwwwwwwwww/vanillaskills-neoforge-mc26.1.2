package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonSmithing;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Netherite -> Dragon armor smithing upgrade. The smithing slots accept our marked Dragon Template
 * (a netherite template) and Dragon Ingot (a netherite ingot) natively, because SmithingMenu gates
 * placement via RecipePropertySet (item membership), not Ingredient.test. Vanilla finds no matching
 * recipe and would clear the result via an early return, so we inject at HEAD and cancel: when the
 * three inputs form a Dragon upgrade we produce the Dragon piece and skip vanilla. The vanilla
 * onTake still consumes all three inputs.
 */
@Mixin(SmithingMenu.class)
public abstract class DragonSmithingMixin {

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$dragonUpgrade(CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack template = self.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = self.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = self.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();

        if (!DragonSmithing.matches(template, base, addition)) return;
        // requires the Armorsmith "Dragon" node; player lives on the superclass, read via accessor
        if (!CraftingGate.canSmithDragon(((ItemCombinerMenuAccessor) self).vanillaskills$getPlayer())) return;

        self.getSlot(SmithingMenu.RESULT_SLOT).set(DragonSmithing.assemble(base));
        ci.cancel();
    }
}
