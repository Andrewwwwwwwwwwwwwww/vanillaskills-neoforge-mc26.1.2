package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/**
 * Keeps <b>blank</b> Enchanted Books — ones carrying no stored enchantments — out of generated loot.
 *
 * <p>These are a side effect of removing Mending. {@code ItemEnchantmentsMutableMixin} refuses to write
 * Mending onto anything, which is the right guarantee, but a loot table's {@code enchant_randomly} has
 * already committed to producing a book by then: it picks one enchantment at random, the write is
 * refused, and what lands in the chest is an Enchanted Book with an empty enchantment list.
 *
 * <p><b>Vanilla hits the same problem and solves it the same way.</b> The librarian's book trade ends
 * with a {@code minecraft:filtered} function whose {@code on_fail} is {@code minecraft:discard},
 * throwing away exactly this case — which is why trades were never affected. Loot tables have no such
 * guard: all 19 vanilla tables that call {@code enchant_randomly} (every chest table that offers books,
 * plus piglin bartering) are unprotected. This applies vanilla's own answer to them.
 *
 * <p>Filtering at the generation chokepoint rather than overriding those 19 files, for the same reasons
 * {@code LootExperienceBottleMixin} gives: overriding a vanilla table freezes its contents at whatever
 * version was copied, and this also covers datapack and add-on tables we do not control.
 *
 * <p>Unconditional, unlike the Mending toggle itself — a book with no enchantments on it is never
 * intended loot, whatever produced it.
 */
@Mixin(LootTable.class)
public class LootBlankBookMixin {

    @ModifyVariable(
            method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true, index = 2)
    private Consumer<ItemStack> vanillaskills$stripBlankBooks(Consumer<ItemStack> original) {
        return stack -> {
            if (!vanillaskills$isBlankBook(stack)) original.accept(stack);
        };
    }

    private static boolean vanillaskills$isBlankBook(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return false;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        return stored == null || stored.isEmpty();
    }
}
