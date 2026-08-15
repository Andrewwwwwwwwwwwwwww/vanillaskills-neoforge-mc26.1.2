package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Shared helpers for "marked" items: vanilla items stamped with a hidden custom_data flag so
 * we can identify our custom tiers/materials without registering new items (keeps it
 * server-side / vanilla-client safe).
 */
public final class Markers {
    private Markers() {}

    public static CompoundTag markerTag(String key) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(key, true);
        return tag;
    }

    public static void applyMarker(ItemStack stack, String key) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, markerTag(key));
    }

    public static boolean has(ItemStack stack, String key) {
        if (stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.matchedBy(markerTag(key));
    }

    /** True if the stack carries any of our markers (custom_data key starting with "vs_"). */
    public static boolean isOurs(ItemStack stack) {
        if (stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        for (String key : data.copyTag().keySet()) {
            if (key.startsWith("vs_")) return true;
        }
        return false;
    }

    /**
     * Stamp a stack as one of ours: the hidden marker, the custom model, and the display name.
     *
     * <p>This is the single place the three identity components are set, so the pattern cannot drift
     * apart across the ten or so item types that use it.
     *
     * <p>Two deliberate component choices:
     * <ul>
     *   <li>{@code ITEM_MODEL} rather than {@code CUSTOM_MODEL_DATA}. The stack points straight at a
     *       model in our own namespace, so we no longer override any {@code assets/minecraft/items/}
     *       file. Those overrides had to hand-copy vanilla's dye-tint and armour-trim handling, which
     *       is what broke undyed leather in 0.19.15 and wiped trim icons in 1.0.2 — and they collided
     *       with any add-on pack touching the same files.</li>
     *   <li>{@code ITEM_NAME} rather than {@code CUSTOM_NAME}. This is the item's intrinsic name, so
     *       it does not render italic and the anvil does not treat the gear as player-renamed.</li>
     * </ul>
     *
     * @param modelId namespaced model id, e.g. {@code "vanillaskills:steel_ingot"}
     */
    public static void stamp(ItemStack stack, String markerKey, String modelId, Component displayName) {
        applyMarker(stack, markerKey);
        stack.set(DataComponents.ITEM_MODEL, Identifier.parse(modelId));
        stack.set(DataComponents.ITEM_NAME, displayName);
    }

    /** A non-italic colored display name (rgb is 0xRRGGBB). */
    public static Component name(String text, int rgb) {
        return Component.literal(text).withStyle(s -> s.withColor(rgb).withItalic(false));
    }

    /**
     * A translatable non-italic colored display name for gear/items.
     *
     * <p>Item names are baked into the stack's data components, so they can't be translated
     * per-player server-side the way menu text is. Using a translation key lets the CLIENT render
     * it in its own language — the keys ship in the mod jar and in the auto-pushed resource pack.
     * The fallback means anyone without those (e.g. a vanilla client that declined the pack) sees
     * the plain English name instead of a raw key.
     */
    public static Component name(String key, String fallback, int rgb) {
        return Component.translatableWithFallback(key, fallback)
                .withStyle(s -> s.withColor(rgb).withItalic(false));
    }
}
