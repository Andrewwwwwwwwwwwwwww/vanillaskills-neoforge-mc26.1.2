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
        boolean changed = reidentify(stack);
        changed |= demoteCustomName(stack);
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
     * Work out which tier and piece a stack is, and put its identity components back.
     *
     * <p>{@link #upgrade}'s original path could only help a stack that still carried one of our markers.
     * A piece that had lost its marker — or had one written by a version whose key has since changed — was
     * unrecognisable, so it kept the pre-2.0 {@code custom_model_data} whose pack overrides 2.0 removed and
     * rendered as a plain vanilla sword or helmet forever.
     *
     * <p>Identity is recoverable without the marker because the model id is derived, not stored:
     * {@code vanillaskills:<tier>_<piece>}. So a stack naming one of ours in either {@code ITEM_MODEL} or
     * the legacy {@code custom_model_data} can be matched back to its tier and re-stamped.
     *
     * <p>The base item must still match the tier's item for that slot, which is what stops a renamed vanilla
     * diamond sword being adopted just because someone wrote our model id onto it.
     */
    private static boolean reidentify(ItemStack stack) {
        if (stack.isEmpty()) return false;

        String claimed = claimedModelId(stack);
        if (claimed == null) return false;

        for (ArmorTier tier : ArmorTiers.TIERS) {
            ArmorPiece piece = tier.pieceOf(stack);   // null unless the base item is this tier's for a slot
            if (piece == null) continue;
            if (!claimed.equals("vanillaskills:" + tier.id + "_" + piece.lower())) continue;
            return tier.applyIdentity(stack, piece);
        }
        for (io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTier tier :
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers.TIERS) {
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolKind kind = tier.kindOf(stack);
            if (kind == null) continue;
            if (!claimed.equals("vanillaskills:" + tier.id + "_" + kind.lower())) continue;
            return tier.applyIdentity(stack, kind);
        }
        return false;
    }

    /** The vanillaskills model id a stack claims, from either the current or the legacy component. */
    private static String claimedModelId(ItemStack stack) {
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        if (model != null && model.getNamespace().equals("vanillaskills")) return model.toString();

        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd == null) return null;
        for (String s : cmd.strings()) {
            if (s != null && s.startsWith("vanillaskills:")) return s;
        }
        return null;
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
        // Only our own legacy branding is demoted: the pre-2.0 stamp wrote the same text into
        // CUSTOM_NAME that ITEM_NAME carries today, so equal text means it is ours to strip. Text
        // that differs is a player's anvil rename — the sweep runs constantly, so without this
        // check every rename of VanillaSkills gear silently reverted moments after it was paid for.
        net.minecraft.network.chat.Component custom = stack.get(DataComponents.CUSTOM_NAME);
        net.minecraft.network.chat.Component intrinsic = stack.get(DataComponents.ITEM_NAME);
        if (custom == null || intrinsic == null
                || !custom.getString().equals(intrinsic.getString())) return false;
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
            ItemStack stack = container.getItem(i);
            if (upgrade(stack)) changed++;
            changed += sweepNested(stack);
        }
        return changed;
    }

    /**
     * Migrate gear stored <i>inside</i> a carried item — a shulker box, mainly.
     *
     * <p>Without this, a box of old gear stays untouched however many times its owner logs in, and the
     * pieces only fix themselves once taken out and left loose in the inventory. Since a shulker box is how
     * people actually store spare kit, that was where most un-migrated gear was sitting.
     *
     * <p>{@code CONTAINER} is an immutable component, so the contents are rebuilt and written back only when
     * something actually changed.
     */
    private static int sweepNested(ItemStack stack) {
        net.minecraft.world.item.component.ItemContainerContents contents =
                stack.get(DataComponents.CONTAINER);
        if (contents == null) return 0;

        java.util.List<ItemStack> items = new java.util.ArrayList<>(contents.allItemsCopyStream().toList());
        int changed = 0;
        for (ItemStack inner : items) {
            if (upgrade(inner)) changed++;
        }
        if (changed > 0) {
            stack.set(DataComponents.CONTAINER,
                    net.minecraft.world.item.component.ItemContainerContents.fromItems(items));
        }
        return changed;
    }
}
