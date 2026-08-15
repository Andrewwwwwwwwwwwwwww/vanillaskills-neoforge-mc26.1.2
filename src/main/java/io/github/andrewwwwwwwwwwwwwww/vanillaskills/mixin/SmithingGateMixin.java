package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gates smithing-table output behind the per-tier craft skill (e.g. vanilla Netherite armour/tools
 * need the Armorsmith/Toolsmith "Netherite" node). Runs after vanilla computes the result; if the
 * result is a tier the player can't craft yet, the result slot is cleared. (Dragon upgrades are
 * produced earlier by DragonSmithingMixin, which cancels the method, so this never clears them.)
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingGateMixin {

    @Inject(method = "createResult", at = @At("TAIL"))
    private void vanillaskills$gateSmithing(CallbackInfo ci) {
        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        ItemStack result = self.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        if (result.isEmpty()) return;
        // Trims (and any in-place smithing edit) keep the same base item — they aren't crafting a new
        // tier, so they must NOT be gated. Only true tier upgrades change the item (diamond -> netherite).
        ItemStack base = self.getSlot(SmithingMenu.BASE_SLOT).getItem();
        if (base.getItem() == result.getItem()) return;
        var player = ((ItemCombinerMenuAccessor) self).vanillaskills$getPlayer();
        if (CraftingGate.isLocked(player, result)) {
            self.getSlot(SmithingMenu.RESULT_SLOT).set(ItemStack.EMPTY);
        }
    }
}
