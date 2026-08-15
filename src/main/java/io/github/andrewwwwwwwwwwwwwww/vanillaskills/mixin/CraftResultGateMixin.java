package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks taking a VanillaSkills armor/tool from a crafting result slot until the player has
 * unlocked the matching crafting skill. The recipe still computes and the item shows in the
 * result slot (as a teaser), but {@code mayPickup} returns false so it can't be taken. Only the
 * crafting {@link ResultSlot} is affected; all other slots behave normally.
 */
@Mixin(Slot.class)
public class CraftResultGateMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$gateCustomCrafting(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (self instanceof ResultSlot && CraftingGate.isLocked(player, self.getItem())) {
            cir.setReturnValue(false);
        }
    }
}
