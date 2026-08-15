package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Combining an Elytra into the Dragon chestplate. The combined item is the Dragon chestplate with
 * the vanilla {@code glider} component added (so it lets you glide) plus the original Elytra stack
 * stored verbatim in custom_data — so the two can be separated later with their own enchantments
 * intact. The chestplate keeps its own enchantments on the item; the elytra keeps its enchantments
 * in the stored copy. They are never merged, so each works standalone after splitting.
 */
public final class DragonElytra {
    private DragonElytra() {}

    public static final String MARKER = "vs_dragon_elytra";
    private static final String STORED_ELYTRA = "vs_stored_elytra";

    private static final String ELYTRA_LABEL_TEXT = "+ Elytra"; // legacy literal (older combined items)
    private static final String ELYTRA_LABEL_KEY = "vanillaskills.gear.dragon.elytra_label";
    private static final Component ELYTRA_LABEL =
            Component.translatableWithFallback(ELYTRA_LABEL_KEY, ELYTRA_LABEL_TEXT)
                    .withStyle(s -> s.withColor(0x9D7BE0).withItalic(false));

    /** Our elytra label, whether it's the new translatable form or a legacy literal on an old item. */
    private static boolean isElytraLabel(Component c) {
        return (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && ELYTRA_LABEL_KEY.equals(t.getKey()))
                || ELYTRA_LABEL_TEXT.equals(c.getString());
    }

    public static boolean isCombined(ItemStack stack) {
        return Markers.has(stack, MARKER);
    }

    /** True if these two stacks (in either order) can be merged: a Dragon chestplate + an Elytra. */
    public static boolean canCombine(ItemStack a, ItemStack b) {
        return (isDragonChest(a) && b.is(Items.ELYTRA)) || (isDragonChest(b) && a.is(Items.ELYTRA));
    }

    /** True if the stack is an uncombined Dragon chestplate (eligible to fuse with an elytra). */
    public static boolean isDragonChest(ItemStack stack) {
        return ArmorTiers.DRAGON.isWorn(stack) && !isCombined(stack);
    }

    /** Build the combined chestplate from a Dragon chestplate + an Elytra (order-independent). */
    public static ItemStack combine(ItemStack a, ItemStack b) {
        ItemStack dragonChest = isDragonChest(a) ? a : b;
        ItemStack elytra = isDragonChest(a) ? b : a;

        ItemStack out = dragonChest.copy();
        out.setCount(1);
        CompoundTag data = customData(out);
        data.putBoolean(MARKER, true);
        data.put(STORED_ELYTRA, encode(elytra));
        CustomData.set(DataComponents.CUSTOM_DATA, out, data);
        out.set(DataComponents.GLIDER, Unit.INSTANCE);

        // Append a visible "+ Elytra" line so players know this chestplate also glides.
        List<Component> lore = new ArrayList<>();
        ItemLore existing = out.get(DataComponents.LORE);
        if (existing != null) lore.addAll(existing.lines());
        if (lore.stream().noneMatch(DragonElytra::isElytraLabel)) lore.add(ELYTRA_LABEL);
        out.set(DataComponents.LORE, new ItemLore(lore));
        return out;
    }

    /** Split a combined chestplate back into [bare Dragon chestplate, stored Elytra]. */
    public static ItemStack[] split(ItemStack combined) {
        ItemStack chest = combined.copy();
        chest.setCount(1);
        CompoundTag data = customData(chest);
        Tag stored = data.get(STORED_ELYTRA);
        data.remove(STORED_ELYTRA);
        data.remove(MARKER);
        CustomData.set(DataComponents.CUSTOM_DATA, chest, data);
        chest.remove(DataComponents.GLIDER);

        // Strip the "+ Elytra" label we added on combine.
        ItemLore existing = chest.get(DataComponents.LORE);
        if (existing != null) {
            List<Component> lore = new ArrayList<>();
            for (Component c : existing.lines()) {
                if (!isElytraLabel(c)) lore.add(c);
            }
            chest.set(DataComponents.LORE, new ItemLore(lore));
        }

        ItemStack elytra = stored != null ? decode(stored) : new ItemStack(Items.ELYTRA);
        return new ItemStack[]{chest, elytra};
    }

    private static CompoundTag customData(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null ? cd.copyTag() : new CompoundTag();
    }

    private static RegistryOps<Tag> ops() {
        return VanillaSkills.server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    }

    private static Tag encode(ItemStack stack) {
        return ItemStack.CODEC.encodeStart(ops(), stack).getOrThrow();
    }

    private static ItemStack decode(Tag tag) {
        return ItemStack.CODEC.parse(ops(), tag).result().orElse(new ItemStack(Items.ELYTRA));
    }
}
