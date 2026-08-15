package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Brings gear crafted before 2.0 onto the new identity components.
 *
 * <p>Pre-2.0 items got their texture from a {@code custom_model_data} string plus an override file in
 * {@code assets/minecraft/items/}. Those overrides are gone, so without this an old Steel Sword sitting
 * in a chest would suddenly render as a plain iron sword. The marker is unchanged, so such items are
 * still recognised by every gameplay system — only their appearance needs repointing.
 *
 * <p><b>Only the model is migrated, deliberately.</b> The display name is left exactly as it is: the old
 * names were already stamped non-italic, so moving them to {@code ITEM_NAME} would look identical while
 * risking the clobbering of a genuine player rename from an anvil. There is nothing to gain and something
 * to lose.
 *
 * <p>The first check is the cheapest one available — post-2.0 items carry no {@code custom_model_data} at
 * all, so once a world has been swept this costs a single component lookup per stack.
 */
public final class LegacyGear {
    private LegacyGear() {}

    /** Repoint one stack's model if it is a pre-2.0 VanillaSkills item. Returns true if it changed. */
    public static boolean upgrade(ItemStack stack) {
        boolean changed = demoteCustomName(stack);
        changed |= restat(stack);

        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd == null) return changed;               // already migrated, or never ours
        if (!Markers.isOurs(stack)) return changed;    // another mod's custom item — leave it alone
        if (cmd.strings().isEmpty()) return changed;

        Identifier model = Identifier.tryParse(cmd.strings().get(0));
        if (model == null) return changed;
        if (!stack.has(DataComponents.ITEM_MODEL)) {
            stack.set(DataComponents.ITEM_MODEL, model);
        }
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        return true;
    }

    /**
     * Move one of our items off {@code CUSTOM_NAME} and onto {@code ITEM_NAME}.
     *
     * <p>A stack with {@code CUSTOM_NAME} is, to the game, an item somebody renamed at an anvil: it renders
     * italic, and an item frame holding it shows a floating nameplate — the client's {@code shouldShowName}
     * tests {@code getCustomName() != null} specifically. Our gear is supposed to have an <i>intrinsic</i>
     * name, which is what {@code ITEM_NAME} is for and why 2.0 moved to it.
     *
     * <p>Some items were minted before that move — the Rose Gold Ingot data recipe still stamped
     * {@code custom_name} until recently — and those carry it permanently until something strips it.
     *
     * <p>Only ever touches stacks carrying our own marker, so a genuinely player-renamed vanilla item is
     * never affected. If the item somehow has no {@code ITEM_NAME} to fall back on, the custom name is left
     * alone rather than leaving the item nameless.
     */
    private static boolean demoteCustomName(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_NAME)) return false;
        if (!Markers.isOurs(stack)) return false;
        if (!stack.has(DataComponents.ITEM_NAME)) return false;
        stack.remove(DataComponents.CUSTOM_NAME);
        return true;
    }

    /**
     * Bring a piece of VanillaSkills gear onto its tier's current durability and attribute modifiers.
     *
     * <p>Gear stats are baked into each stack when it is crafted, so retuning a tier in gameplay.json would
     * otherwise only reach newly-made pieces and leave everyone's existing kit on the old numbers. This
     * closes that gap, and doubles as the path that brings 1.x gear onto 2.0 stats.
     *
     * <p>Deliberately narrow: only {@code MAX_DAMAGE} and {@code ATTRIBUTE_MODIFIERS} are rewritten, so
     * enchantments, current damage and anvil renames all survive. Controlled by {@code gearRestamp}.
     */
    private static boolean restat(ItemStack stack) {
        if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.GEAR_RESTAMP) return false;
        if (stack.isEmpty() || !Markers.isOurs(stack)) return false;

        for (ArmorTier tier : ArmorTiers.TIERS) {
            if (!tier.isWorn(stack)) continue;
            ArmorPiece piece = tier.pieceOf(stack);
            if (piece == null) return false;
            tier.applyStats(stack, piece);
            return true;
        }
        for (io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTier tier :
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers.TIERS) {
            if (!Markers.has(stack, tier.markerKey)) continue;
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolKind kind = tier.kindOf(stack);
            if (kind == null) return false;
            tier.applyStats(stack, kind);
            return true;
        }
        return false;
    }

    /**
     * Sweep a player's inventory and ender chest.
     *
     * <p>Items in world containers are not scanned — that would mean walking every loaded chest. They
     * are migrated the first time a player picks them up, since this runs periodically as well as on
     * join.
     */
    public static int sweep(ServerPlayer player) {
        return sweep(player.getInventory()) + sweep(player.getEnderChestInventory());
    }

    private static int sweep(Container container) {
        int changed = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (upgrade(container.getItem(i))) changed++;
        }
        return changed;
    }
}
