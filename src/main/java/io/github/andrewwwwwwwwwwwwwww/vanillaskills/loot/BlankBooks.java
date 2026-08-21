package io.github.andrewwwwwwwwwwwwwww.vanillaskills.loot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Recognises the <b>blank</b> Enchanted Book — one carrying no stored enchantments.
 *
 * <p>These are a side effect of removing Mending: a loot table's {@code enchant_randomly} has already
 * committed to producing a book by the time the enchantment write is refused, so what lands in the chest
 * is an Enchanted Book with an empty list. Both {@code LootBlankBookMixin} and {@code ContainerLuckMixin}
 * need to spot them, and both mix into {@code LootTable} — declaring the test privately in each meant Mixin
 * merged one and skipped the other with a "method overwrite conflict" warning on every boot. Shared here so
 * there is one definition and no collision.
 */
public final class BlankBooks {
    private BlankBooks() {}

    /** True if this is an Enchanted Book with nothing enchanted onto it — never intended loot. */
    public static boolean isBlank(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return false;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        return stored == null || stored.isEmpty();
    }
}
