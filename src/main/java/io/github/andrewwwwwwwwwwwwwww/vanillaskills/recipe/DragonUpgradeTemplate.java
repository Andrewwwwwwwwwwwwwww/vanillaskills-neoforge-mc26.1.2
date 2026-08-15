package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * The "Dragon Upgrade" smithing template — a vanilla NETHERITE_UPGRADE_SMITHING_TEMPLATE carrying a
 * custom name, lore, hidden marker and model hook. Found in End City treasure chests. Used to
 * upgrade netherite armor to Dragon armor (with a Dragon Ingot). A real template base so the
 * smithing template slot accepts it; SmithingRecipeMixin blocks it from doing a vanilla netherite
 * upgrade, and our own handler performs the Dragon upgrade.
 */
public final class DragonUpgradeTemplate {
    private DragonUpgradeTemplate() {}

    public static final String MARKER_KEY = "vs_dragon_template";
    public static final Item BASE = Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE;

    private static final CompoundTag MARKER = markerTag();

    public static CompoundTag markerTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(MARKER_KEY, true);
        return tag;
    }

    public static Component displayName() {
        return Component.translatableWithFallback("vanillaskills.item.dragon_upgrade_template", "Dragon Upgrade").withStyle(s -> s.withColor(0xC23BD6).withItalic(false));
    }

    /** The custom model that gives the template its texture. Also stamped by the loot pool. */
    public static Identifier modelId() {
        return Identifier.fromNamespaceAndPath("vanillaskills", "dragon_template");
    }

    /** The template's description lore. */
    public static ItemLore lore() {
        return new ItemLore(List.of(
                line("vanillaskills.item.dragon_upgrade_template.desc1", "Smithing: upgrade netherite armor", ChatFormatting.GRAY),
                line("vanillaskills.item.dragon_upgrade_template.desc2", "to Dragon armor with a Dragon Ingot.", ChatFormatting.GRAY),
                line("vanillaskills.item.dragon_upgrade_template.note", "Found in End City treasure.", ChatFormatting.DARK_GRAY)));
    }

    public static ItemStack create() {
        ItemStack stack = new ItemStack(BASE);
        stack.set(DataComponents.ITEM_NAME, displayName());
        CustomData.set(DataComponents.CUSTOM_DATA, stack, markerTag());
        stack.set(DataComponents.ITEM_MODEL, modelId());
        stack.set(DataComponents.LORE, lore());
        return stack;
    }

    public static boolean isTemplate(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(BASE)) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.matchedBy(MARKER);
    }

    private static Component line(String key, String fallback, ChatFormatting color) {
        return Component.translatableWithFallback(key, fallback).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
