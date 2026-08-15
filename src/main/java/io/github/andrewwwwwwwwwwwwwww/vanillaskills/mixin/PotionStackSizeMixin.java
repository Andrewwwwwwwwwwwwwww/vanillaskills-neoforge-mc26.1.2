package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets potions (regular, splash, lingering) stack up to 16. {@code getMaxStackSize} is a default
 * method on the {@code ItemInstance} interface (ItemStack does not declare it), so we inject there.
 * Only stacks of identical potions merge (vanilla compares components), so different effects never
 * combine.
 */
@Mixin(ItemInstance.class)
public interface PotionStackSizeMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void vanillaskills$stackablePotions(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() >= 16) return;
        if (!((Object) this instanceof ItemStack self)) return;
        if (self.is(Items.POTION) || self.is(Items.SPLASH_POTION) || self.is(Items.LINGERING_POTION)) {
            cir.setReturnValue(16);
        }
    }
}
