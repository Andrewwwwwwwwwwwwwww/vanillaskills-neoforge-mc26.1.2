package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.minecraft.world.item.Items.*;

/**
 * Definitions for the three current armor tiers and the material lookup used by the recipe.
 * Stat numbers are intentionally easy to tune here.
 */
public final class ArmorTiers {
    private ArmorTiers() {}

    /** "Wood" blocks (all-bark cubes + hyphae, stripped and not) — Hardwood's crafting material. */
    public static final Item[] WOOD_ITEMS = {
            OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD, JUNGLE_WOOD, ACACIA_WOOD, DARK_OAK_WOOD,
            MANGROVE_WOOD, CHERRY_WOOD, PALE_OAK_WOOD, CRIMSON_HYPHAE, WARPED_HYPHAE,
            STRIPPED_OAK_WOOD, STRIPPED_SPRUCE_WOOD, STRIPPED_BIRCH_WOOD, STRIPPED_JUNGLE_WOOD,
            STRIPPED_ACACIA_WOOD, STRIPPED_DARK_OAK_WOOD, STRIPPED_MANGROVE_WOOD,
            STRIPPED_CHERRY_WOOD, STRIPPED_PALE_OAK_WOOD, STRIPPED_CRIMSON_HYPHAE, STRIPPED_WARPED_HYPHAE
    };
    private static final Set<Item> WOOD_SET = Set.of(WOOD_ITEMS);

    // Per-piece arrays are ordered: helmet, chestplate, leggings, boots.

    // Hardwood: between Leather (7 armour, dur x5) and Copper (10, dur x11) — must not beat Copper.
    // +10% movement at full set keeps it the "light & fast" early tier.
    public static final ArmorTier HARDWOOD = new ArmorTier(
            "hardwood", "Hardwood", 0x9A6B3F, "vs_armor_hardwood",
            new Item[]{LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS},
            // 2.0: per-piece speed 0.025 -> 0.04, so the full set is +16% rather than +10%. Hardwood is the
            // "light and fast" early tier and needs a reason to be worn over Copper's higher armour.
            new int[]{2, 3, 3, 1}, 0.0, 0.0, 0.04, new int[]{99, 144, 135, 117}, // 9 armour, dur x9
            itemSet(WOOD_ITEMS), stack -> WOOD_SET.contains(stack.getItem()), null);

    // Rose Gold: between Gold (11 armour, dur x7) and Iron (15, dur x15) — a sturdier, quicker gold.
    public static final ArmorTier ROSE_GOLD = new ArmorTier(
            "rose_gold", "Rose Gold", 0xE8B7A6, "vs_armor_rose_gold",
            new Item[]{GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS},
            new int[]{2, 5, 4, 2}, 0.0, 0.0, 0.0, new int[]{143, 208, 195, 169}, // 13 armour, dur x13
            itemSet(GOLD_INGOT), Alloys::isRoseGoldIngot, RoseGoldSet::baseLore);

    // Steel: between Iron and Diamond by armour (18) but no toughness — toughness starts at Crystalline.
    public static final ArmorTier STEEL = new ArmorTier(
            "steel", "Steel", 0xB8C0C8, "vs_armor_steel",
            new Item[]{IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS},
            // 2.0: per-piece penalty -0.01 -> -0.025, so the full set is a flat -10% rather than -4%.
            // 18 armour with almost no downside made Steel dominate its bracket; the weight is the trade-off.
            new int[]{3, 7, 5, 3}, 0.0, 0.0, -0.025, new int[]{330, 481, 451, 390}, // 18 armour, dur x30
            itemSet(IRON_INGOT), Alloys::isSteelIngot, null);

    // Crystalline sits BETWEEN diamond (20 armor / 2 tough / 0 kb) and netherite (20 / 3 / 0.1):
    // same armour total, toughness 2.5, and a small 0.05 knockback resist (below netherite's 0.1).
    public static final ArmorTier CRYSTAL = new ArmorTier(
            "crystal", "Crystalline", 0xB389E8, "vs_armor_crystal",
            new Item[]{DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS},
            new int[]{3, 8, 6, 3}, 2.5, 0.05, 0.0, new int[]{385, 560, 490, 455},
            itemSet(DIAMOND), Alloys::isCrystallizedDiamond, CrystalSet::baseLore);

    // Dragon is the top tier — clearly ABOVE netherite: +1 armour per piece (24 vs 20 total),
    // toughness 4 (vs 3), and 0.15 knockback resist (vs 0.1).
    public static final ArmorTier DRAGON = new ArmorTier(
            "dragon", "Dragon", 0xC23BD6, "vs_armor_dragon",
            new Item[]{NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS},
            new int[]{4, 9, 7, 4}, 4.0, 0.15, 0.0, new int[]{555, 796, 722, 650},
            itemSet(NETHERITE_INGOT), DragonIngot::isDragonIngot, DragonSet::baseLore); // smithing-only; repaired with Dragon Ingots

    public static final List<ArmorTier> TIERS = List.of(HARDWOOD, ROSE_GOLD, STEEL, CRYSTAL, DRAGON);

    /**
     * A representative crafting material for a tier, for display purposes.
     *
     * <p>The tiers match their material with a {@link java.util.function.Predicate}, which can test a stack
     * but cannot produce one — so the recipe book needs this to show what to put in the grid.
     */
    public static ItemStack sampleMaterial(ArmorTier tier) {
        if (tier == HARDWOOD) return new ItemStack(OAK_WOOD);
        if (tier == ROSE_GOLD) return Alloys.roseGoldIngot();
        if (tier == STEEL) return Alloys.steelIngot();
        if (tier == CRYSTAL) return Alloys.crystallizedDiamond();
        if (tier == DRAGON) return DragonIngot.create();
        return ItemStack.EMPTY;
    }

    /** The tier whose crafting material this stack is, or null. */
    public static ArmorTier tierForMaterial(ItemStack stack) {
        for (ArmorTier tier : TIERS) {
            if (tier.material.test(stack)) return tier;
        }
        return null;
    }

    private static HolderSet<Item> itemSet(Item... items) {
        List<Holder<Item>> holders = new ArrayList<>();
        for (Item item : items) holders.add(item.builtInRegistryHolder());
        return HolderSet.direct(holders);
    }
}
