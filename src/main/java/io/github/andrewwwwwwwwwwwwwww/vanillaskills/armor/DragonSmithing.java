package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.DragonUpgradeTemplate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The smithing-table upgrade from netherite armor to Dragon armor:
 *   [Dragon Upgrade Template] + [netherite armor piece] + [Dragon Ingot] -> Dragon armor piece.
 * Enchantments are carried over from the netherite piece. Driven by {@code DragonSmithingMixin}.
 */
public final class DragonSmithing {
    private DragonSmithing() {}

    /** True if the three smithing inputs form a valid netherite -> Dragon upgrade. */
    public static boolean matches(ItemStack template, ItemStack base, ItemStack addition) {
        return DragonUpgradeTemplate.isTemplate(template)
                && DragonIngot.isDragonIngot(addition)
                && pieceFor(base) != null;
    }

    /** Build the Dragon armor result for the given netherite base piece, carrying its enchantments. */
    public static ItemStack assemble(ItemStack base) {
        ArmorPiece piece = pieceFor(base);
        if (piece == null) return ItemStack.EMPTY;
        ItemStack out = ArmorTiers.DRAGON.create(piece);
        ItemEnchantments enchantments = base.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            out.set(DataComponents.ENCHANTMENTS, enchantments);
        }
        return out;
    }

    private static ArmorPiece pieceFor(ItemStack base) {
        if (base.isEmpty() || ArmorTiers.DRAGON.isWorn(base)) return null; // only plain netherite armor
        if (base.is(Items.NETHERITE_HELMET)) return ArmorPiece.HELMET;
        if (base.is(Items.NETHERITE_CHESTPLATE)) return ArmorPiece.CHESTPLATE;
        if (base.is(Items.NETHERITE_LEGGINGS)) return ArmorPiece.LEGGINGS;
        if (base.is(Items.NETHERITE_BOOTS)) return ArmorPiece.BOOTS;
        return null;
    }
}
