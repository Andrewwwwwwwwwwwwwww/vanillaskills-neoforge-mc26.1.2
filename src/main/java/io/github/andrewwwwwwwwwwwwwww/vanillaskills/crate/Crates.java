package io.github.andrewwwwwwwwwwwwwww.vanillaskills.crate;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.data.CrateDef;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.data.VsContent;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.List;

/**
 * Fishing crates: sealed containers pulled out of the water, opened by hand for a roll of loot.
 *
 * <p>Crates are datapack-defined ({@link CrateDef}) and their contents are ordinary loot tables, so a pack
 * can add a crate, change what one holds, or restrict one to a biome without any code. The mod only supplies
 * the item, the fishing hook and the opening.
 *
 * <p>All crates share one inert base item and are told apart by their {@code vs_crate_*} marker, with their
 * look coming from {@code ITEM_MODEL} — the same pattern as every other custom item here. The base is
 * deliberately something with no right-click behaviour of its own, so opening a crate cannot collide with
 * eating or placing it.
 */
public final class Crates {
    private Crates() {}

    /** Inert, stackable, not placeable and not edible — nothing to collide with the open-on-right-click. */
    private static final net.minecraft.world.item.Item BASE = Items.BRICK;

    private static final String MARKER_PREFIX = "vs_crate_";

    public static String markerFor(String crateId) {
        return MARKER_PREFIX + crateId;
    }

    /** Build a crate item for a definition. */
    public static ItemStack create(CrateDef def) {
        ItemStack stack = new ItemStack(BASE);
        Markers.stamp(stack, markerFor(def.id), "vanillaskills:crate_" + def.id,
                Markers.name("vanillaskills.crate." + def.id, def.name, def.rgb()));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatableWithFallback("vanillaskills.crate.open_hint", "Right-click to open.")
                        .withStyle(ChatFormatting.GRAY).withStyle(s -> s.withItalic(false)))));
        return stack;
    }

    /** The definition this stack is a crate for, or null. */
    public static CrateDef defFor(ItemStack stack) {
        if (!stack.is(BASE) || stack.isEmpty()) return null;
        for (CrateDef def : VsContent.crates()) {
            if (Markers.has(stack, markerFor(def.id))) return def;
        }
        return null;
    }

    /**
     * Open one crate from the held stack.
     *
     * <p>Consumes exactly one crate and rolls its loot table once. The crate is taken <b>before</b> the loot
     * is granted, so a failed or empty roll can never leave the player holding a crate they already opened.
     *
     * @return true if a crate was opened
     */
    public static boolean open(ServerPlayer player, ItemStack held) {
        CrateDef def = defFor(held);
        if (def == null) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;

        Identifier tableId = Identifier.tryParse(def.lootTable);
        if (tableId == null) return false;
        LootTable table = level.getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, tableId));
        if (table == LootTable.EMPTY) {
            player.sendSystemMessage(Component.literal(Lang.tr(player, "vanillaskills.msg.crate_no_table",
                    "This crate's loot table is missing: %s", def.lootTable)).withStyle(ChatFormatting.RED));
            return false;
        }

        held.shrink(1);

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withLuck(player.getLuck())
                .create(LootContextParamSets.GIFT);

        List<ItemStack> loot = List.copyOf(table.getRandomItems(params));
        player.sendSystemMessage(Component.literal(Lang.tr(player, "vanillaskills.msg.crate_opened",
                "Opened %s — %d item(s) inside.",
                Lang.tr(player, "vanillaskills.crate." + def.id, def.name), loot.size()))
                .withStyle(ChatFormatting.GREEN));

        if (!io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.CRATE_REEL_ENABLED
                || loot.isEmpty()) {
            for (ItemStack drop : loot) player.getInventory().placeItemBackInInventory(drop);
            return true;
        }

        // What the machine shows: this roll's non-shard items, one per lane, padded out to the crate's
        // MAXIMUM with empty lanes. Padding to the maximum matters — a machine that showed two lanes when you
        // won two things would give the result away before a single reel stopped.
        List<ItemStack> won = displayable(loot);
        Sampling sampling = sample(table, params);
        int lanes = Math.max(sampling.maxDisplayable, won.size());
        if (lanes == 0 || sampling.distinct.isEmpty()) {
            for (ItemStack drop : loot) player.getInventory().placeItemBackInInventory(drop);
            return true;
        }

        List<ItemStack> results = new java.util.ArrayList<>(won);
        while (results.size() < lanes) results.add(ItemStack.EMPTY);   // lanes that pay out nothing

        CrateReel.start(player, def, sampling.distinct, results, loot);
        return true;
    }

    /**
     * The items from a roll that are worth putting on a reel: everything except Skill Shards.
     *
     * <p>Every crate contains shards, so a lane that always shows the same thing carries no information — it
     * only makes the machine wider.
     */
    private static List<ItemStack> displayable(List<ItemStack> loot) {
        List<ItemStack> out = new java.util.ArrayList<>();
        for (ItemStack stack : loot) {
            if (ShardItems.isUnstableShard(stack)) continue;
            out.add(stack.copy());
        }
        return out;
    }

    /** What repeated rolls of a table tell us about it. */
    private record Sampling(int maxDisplayable, List<ItemStack> distinct) {}

    /**
     * Learn a crate's shape by rolling it, because a loot table cannot be asked about itself.
     *
     * <p>Neither the number of pool rolls nor the set of possible items is reachable through any public API,
     * so both are derived empirically: the widest non-shard result seen becomes the lane count, and the
     * distinct items seen become what the reels scroll through. Doing it this way means a pack that rewrites
     * a crate's table gets a correctly-sized machine showing its own items, with no code change.
     *
     * <p>The sample is deliberately larger than the display pool needs, so a rare-but-possible extra item
     * still tends to widen the machine rather than being missed.
     */
    private static Sampling sample(LootTable table, LootParams params) {
        int target = io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.CRATE_REEL_POOL;
        List<ItemStack> distinct = new java.util.ArrayList<>();
        int maxDisplayable = 0;

        for (int attempt = 0; attempt < 40; attempt++) {
            List<ItemStack> rolled = displayable(table.getRandomItems(params));
            maxDisplayable = Math.max(maxDisplayable, rolled.size());
            for (ItemStack candidate : rolled) {
                if (distinct.size() >= target) continue;
                boolean seen = false;
                for (ItemStack existing : distinct) {
                    if (ItemStack.isSameItem(existing, candidate)) {
                        seen = true;
                        break;
                    }
                }
                if (!seen) distinct.add(candidate.copy());
            }
        }
        return new Sampling(maxDisplayable, distinct);
    }
}
