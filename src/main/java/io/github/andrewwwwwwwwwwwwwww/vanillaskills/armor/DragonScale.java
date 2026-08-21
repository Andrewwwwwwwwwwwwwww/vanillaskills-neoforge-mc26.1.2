package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


/**
 * Dragon Scale: the Dragon (Netherite II) tier's crafting + repair material. A marked phantom
 * membrane (leathery "scale" base, retextured via the model hook), dropped by the Ender Dragon on
 * death. Kept server-side / vanilla-client safe like the other markers.
 */
public final class DragonScale {
    private DragonScale() {}

    public static final String MARKER = "vs_dragon_scale";
    private static final int COLOR = 0xC23BD6;

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Items.PHANTOM_MEMBRANE);
        Markers.stamp(stack, MARKER, "vanillaskills:dragon_scale",
                Markers.name("vanillaskills.item.dragon_scale", "Dragon Scale", COLOR));
        return stack;
    }

    public static boolean isDragonScale(ItemStack stack) {
        return stack.is(Items.PHANTOM_MEMBRANE) && Markers.has(stack, MARKER);
    }
}
