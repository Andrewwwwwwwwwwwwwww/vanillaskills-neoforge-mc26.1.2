package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;

/** Small shared helpers for the chest-menu GUIs. */
public final class Guis {
    private Guis() {}

    /**
     * Hide an icon's intrinsic attribute tooltip (e.g. "+6 armor", "+7 attack damage") so skill-tree
     * and info-screen icons read cleanly — only the custom name + lore we set should show.
     */
    public static void hideStats(ItemStack stack) {
        stack.set(DataComponents.TOOLTIP_DISPLAY,
                TooltipDisplay.DEFAULT.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true));
    }
}
