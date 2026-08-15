package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * The custom crafting metals. They are vanilla ingots stamped with a name + marker + model hook
 * (so a resource pack can retexture them), kept consistent with their gear's look: Rose Gold is a
 * gold ingot (matches the golden armor/tools), Steel is an iron ingot. The IngredientMixin stops
 * them being used in vanilla recipes, so there are no collisions or block "laundering".
 */
public final class Alloys {
    private Alloys() {}

    public static final String ROSE_GOLD_MARKER = "vs_rose_gold_ingot";
    public static final String STEEL_MARKER = "vs_steel_ingot";
    public static final String CRYSTAL_MARKER = "vs_crystallized_diamond";

    private static final int ROSE_GOLD_COLOR = 0xE8B7A6;
    private static final int STEEL_COLOR = 0xB8C0C8;
    private static final int CRYSTAL_COLOR = 0xB389E8;

    public static ItemStack roseGoldIngot() {
        return stamp(Items.GOLD_INGOT, ROSE_GOLD_MARKER, "Rose Gold Ingot", ROSE_GOLD_COLOR, "vanillaskills:rose_gold_ingot");
    }

    public static boolean isRoseGoldIngot(ItemStack stack) {
        return stack.is(Items.GOLD_INGOT) && Markers.has(stack, ROSE_GOLD_MARKER);
    }

    public static ItemStack steelIngot() {
        return stamp(Items.IRON_INGOT, STEEL_MARKER, "Steel Ingot", STEEL_COLOR, "vanillaskills:steel_ingot");
    }

    public static boolean isSteelIngot(ItemStack stack) {
        return stack.is(Items.IRON_INGOT) && Markers.has(stack, STEEL_MARKER);
    }

    public static ItemStack crystallizedDiamond() {
        return stamp(Items.DIAMOND, CRYSTAL_MARKER, "Crystallized Diamond", CRYSTAL_COLOR, "vanillaskills:crystallized_diamond");
    }

    public static boolean isCrystallizedDiamond(ItemStack stack) {
        return stack.is(Items.DIAMOND) && Markers.has(stack, CRYSTAL_MARKER);
    }

    private static ItemStack stamp(net.minecraft.world.item.Item base, String marker, String name, int color, String modelHook) {
        ItemStack stack = new ItemStack(base);
        // modelHook is "vanillaskills:<id>" — reuse that id for the translation key.
        Markers.stamp(stack, marker, modelHook,
                Markers.name("vanillaskills.item." + modelHook.substring(modelHook.indexOf(':') + 1), name, color));
        return stack;
    }
}
