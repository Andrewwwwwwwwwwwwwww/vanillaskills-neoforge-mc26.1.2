package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes Mending from the game at the single universal chokepoint where any
 * enchantment is applied to an item at runtime.
 *
 * Every path that adds an enchantment to a stack funnels through
 * {@link ItemEnchantments.Mutable#set} / {@link ItemEnchantments.Mutable#upgrade}:
 *   - Loot tables (EnchantRandomlyFunction / EnchantWithLevelsFunction -> updateEnchantments)
 *   - Enchantment providers (mob equipment, etc. -> updateEnchantments)
 *   - The enchanting table (-> updateEnchantments)
 *   - Villager librarian book trades (createBook -> ItemStack.enchant -> updateEnchantments)
 *   - The /enchant command and ItemStack.enchant in general
 *
 * By refusing to write Mending here, it can never appear on any newly generated
 * or enchanted item, which covers both villager trades and chest loot.
 */
@Mixin(ItemEnchantments.Mutable.class)
public class ItemEnchantmentsMutableMixin {

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$blockMendingSet(Holder<Enchantment> enchantment, int level, CallbackInfo ci) {
        if (!GameplayConfig.MENDING_ENABLED && enchantment.is(Enchantments.MENDING)) {
            ci.cancel();
        }
    }

    @Inject(method = "upgrade", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$blockMendingUpgrade(Holder<Enchantment> enchantment, int level, CallbackInfo ci) {
        if (!GameplayConfig.MENDING_ENABLED && enchantment.is(Enchantments.MENDING)) {
            ci.cancel();
        }
    }
}
