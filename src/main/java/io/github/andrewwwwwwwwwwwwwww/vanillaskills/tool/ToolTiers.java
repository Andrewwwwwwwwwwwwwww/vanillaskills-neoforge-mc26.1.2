package io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTiers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.minecraft.world.item.Items.*;

/**
 * The five tool tiers, sharing materials and repair rules with the armor tiers. Each tier is a vanilla
 * tool base, so its harvest tier matches that base: Hardwood = stone, Rose Gold = gold (lowest harvest
 * tier but the fastest mining speed), Steel = iron, Crystalline = diamond, Dragon = netherite.
 */
public final class ToolTiers {
    private ToolTiers() {}

    private static final Set<Item> WOOD_SET = Set.of(ArmorTiers.WOOD_ITEMS);

    // Ordered to match ToolKind: pickaxe, axe, shovel, hoe, sword, spear (spear uses the vanilla spear item).
    private static final Item[] STONE_TOOLS = {STONE_PICKAXE, STONE_AXE, STONE_SHOVEL, STONE_HOE, STONE_SWORD, STONE_SPEAR};
    private static final Item[] GOLD_TOOLS = {GOLDEN_PICKAXE, GOLDEN_AXE, GOLDEN_SHOVEL, GOLDEN_HOE, GOLDEN_SWORD, GOLDEN_SPEAR};
    private static final Item[] IRON_TOOLS = {IRON_PICKAXE, IRON_AXE, IRON_SHOVEL, IRON_HOE, IRON_SWORD, IRON_SPEAR};
    private static final Item[] DIAMOND_TOOLS = {DIAMOND_PICKAXE, DIAMOND_AXE, DIAMOND_SHOVEL, DIAMOND_HOE, DIAMOND_SWORD, DIAMOND_SPEAR};
    private static final Item[] NETHERITE_TOOLS = {NETHERITE_PICKAXE, NETHERITE_AXE, NETHERITE_SHOVEL, NETHERITE_HOE, NETHERITE_SWORD, NETHERITE_SPEAR};

    // Hardwood = stone tier (stone speed 4, sword 5). Durability 160 sits between Stone (131) and
    // Copper (190) — must not beat Copper.
    public static final ToolTier HARDWOOD = new ToolTier(
            "hardwood", "Hardwood", 0x9A6B3F, "vs_tool_hardwood",
            STONE_TOOLS, 160, 0.0, 0.1, 0.0, itemSet(ArmorTiers.WOOD_ITEMS), stack -> WOOD_SET.contains(stack.getItem()));

    /** Ores Rose Gold reaches despite its gold base — gold and iron, plus the raw storage blocks so you can
     *  re-mine what you smelted. Delivered by the per-stack tool component, so vanilla gold tools are
     *  untouched. */
    private static final net.minecraft.world.level.block.Block[] ROSE_GOLD_EXTRA_HARVEST = {
            net.minecraft.world.level.block.Blocks.GOLD_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_GOLD_ORE,
            net.minecraft.world.level.block.Blocks.NETHER_GOLD_ORE,
            net.minecraft.world.level.block.Blocks.GOLD_BLOCK,
            net.minecraft.world.level.block.Blocks.RAW_GOLD_BLOCK,
            net.minecraft.world.level.block.Blocks.IRON_ORE,
            net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE,
            net.minecraft.world.level.block.Blocks.IRON_BLOCK,
            net.minecraft.world.level.block.Blocks.RAW_IRON_BLOCK};

    // Rose Gold = gold tier (gold speed 12 — fastest miner, sword 4 -> 5.5). Durability 220 between
    // Copper (190) and Iron (250): the "fast like gold, far sturdier" tier. 2.0: also mines gold and iron.
    public static final ToolTier ROSE_GOLD = new ToolTier(
            "rose_gold", "Rose Gold", 0xE8B7A6, "vs_tool_rose_gold",
            GOLD_TOOLS, 220, 1.5, 0.2, 0.0, itemSet(GOLD_INGOT), Alloys::isRoseGoldIngot,
            ROSE_GOLD_EXTRA_HARVEST);

    // Steel = iron tier (iron speed 6, sword 6 -> 6.5). Durability 800 between Iron (250) and Diamond (1561).
    public static final ToolTier STEEL = new ToolTier(
            "steel", "Steel", 0xB8C0C8, "vs_tool_steel",
            IRON_TOOLS, 800, 0.5, 0.1, 0.0, itemSet(IRON_INGOT), Alloys::isSteelIngot);

    // Crystalline = diamond tier (diamond speed 8, sword 7 -> 7.5). Durability 1800 between Diamond (1561)
    // and Netherite (2031).
    public static final ToolTier CRYSTAL = new ToolTier(
            "crystal", "Crystalline", 0xB389E8, "vs_tool_crystal",
            DIAMOND_TOOLS, 1800, 0.5, 0.1, 0.0, itemSet(DIAMOND), Alloys::isCrystallizedDiamond);

    // Dragon = netherite tier (top), highest durability; strongest strikes. Crafted from Dragon Ingots.
    // The pickaxe carries +18 mining_efficiency so Efficiency V + Haste II + full Prospector instamine
    // deepslate (needs effective mining speed >= 90: (9 + 26 + 12 + 18) * 1.4 = 91).
    public static final ToolTier DRAGON = new ToolTier(
            "dragon", "Dragon", 0xC23BD6, "vs_tool_dragon",
            NETHERITE_TOOLS, 3500, 1.5, 0.1, 18.0, itemSet(NETHERITE_INGOT),
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonIngot::isDragonIngot);

    public static final List<ToolTier> TIERS = List.of(HARDWOOD, ROSE_GOLD, STEEL, CRYSTAL, DRAGON);

    /** A representative crafting material for a tier — see {@code ArmorTiers.sampleMaterial}. */
    public static ItemStack sampleMaterial(ToolTier tier) {
        if (tier == HARDWOOD) return new ItemStack(net.minecraft.world.item.Items.OAK_WOOD);
        if (tier == ROSE_GOLD) return Alloys.roseGoldIngot();
        if (tier == STEEL) return Alloys.steelIngot();
        if (tier == CRYSTAL) return Alloys.crystallizedDiamond();
        if (tier == DRAGON) return io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonIngot.create();
        return ItemStack.EMPTY;
    }

    public static ToolTier tierForMaterial(ItemStack stack) {
        for (ToolTier tier : TIERS) {
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
