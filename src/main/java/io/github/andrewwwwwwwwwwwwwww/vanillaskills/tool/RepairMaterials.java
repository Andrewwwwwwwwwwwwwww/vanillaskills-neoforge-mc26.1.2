package io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Repairable;

import java.util.List;
import java.util.Map;

/**
 * Gives the vanilla items that ship with no repair material one that fits, so they stop being
 * combine-or-nothing at the anvil. Vanilla leaves nine damageable items with no {@code repairable}
 * component at all — a trident can only ever be mended with a second trident — which under
 * material-based anvil pricing makes them the only gear that cannot take the cheap repair path.
 *
 * <p>The component is stamped onto the <b>stack</b> (like the custom tiers' repair rules) rather than
 * onto the item's defaults, because stack components are synced: a vanilla client's anvil screen
 * previews the repair correctly. Stamping happens in the same player sweep that migrates legacy gear,
 * so anything held, looted or crafted is covered within a tick.
 *
 * <p>The two "on a stick" rods are deliberately left combine-only. An already-present component —
 * ours from an earlier sweep, or one a datapack added — is never overwritten. Toggle with
 * {@code vanillaRepairMaterials}; turning it off stops stamping but does not strip stacks already
 * stamped.
 */
public final class RepairMaterials {
    private RepairMaterials() {}

    private static final Map<Item, Item> MATERIALS = Map.of(
            Items.TRIDENT, Items.PRISMARINE,
            Items.BOW, Items.STRING,
            Items.CROSSBOW, Items.STRING,
            Items.FISHING_ROD, Items.STRING,
            Items.FLINT_AND_STEEL, Items.IRON_INGOT,
            Items.SHEARS, Items.IRON_INGOT,
            Items.BRUSH, Items.COPPER_INGOT);

    /** Stamps every matching item in the player's inventory and ender chest. */
    public static void sweep(ServerPlayer player) {
        if (!GameplayConfig.VANILLA_REPAIR_MATERIALS) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) stamp(inv.getItem(i));
        for (int i = 0; i < player.getEnderChestInventory().getContainerSize(); i++) {
            stamp(player.getEnderChestInventory().getItem(i));
        }
    }

    private static void stamp(ItemStack stack) {
        if (stack.isEmpty()) return;
        Item material = MATERIALS.get(stack.getItem());
        if (material == null) return;
        if (stack.has(DataComponents.REPAIRABLE)) return;
        stack.set(DataComponents.REPAIRABLE,
                new Repairable(HolderSet.direct(List.of(material.builtInRegistryHolder()))));
    }
}
