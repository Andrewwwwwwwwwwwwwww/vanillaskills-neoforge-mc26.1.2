package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;

/**
 * The four armor pieces, each with its equipment slot and the crafting-grid shape that makes it
 * (filled cells, row-major over the recipe's trimmed bounding box). {@code true} = material cell.
 */
public enum ArmorPiece {
    HELMET(EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, 3, 2, new boolean[]{
            true, true, true,
            true, false, true}),
    CHESTPLATE(EquipmentSlot.CHEST, EquipmentSlotGroup.CHEST, 3, 3, new boolean[]{
            true, false, true,
            true, true, true,
            true, true, true}),
    LEGGINGS(EquipmentSlot.LEGS, EquipmentSlotGroup.LEGS, 3, 3, new boolean[]{
            true, true, true,
            true, false, true,
            true, false, true}),
    BOOTS(EquipmentSlot.FEET, EquipmentSlotGroup.FEET, 3, 2, new boolean[]{
            true, false, true,
            true, false, true});

    public final EquipmentSlot slot;
    public final EquipmentSlotGroup group;
    public final int width;
    public final int height;
    public final boolean[] filled;

    ArmorPiece(EquipmentSlot slot, EquipmentSlotGroup group, int width, int height, boolean[] filled) {
        this.slot = slot;
        this.group = group;
        this.width = width;
        this.height = height;
        this.filled = filled;
    }

    public String lower() {
        return name().toLowerCase();
    }
}
