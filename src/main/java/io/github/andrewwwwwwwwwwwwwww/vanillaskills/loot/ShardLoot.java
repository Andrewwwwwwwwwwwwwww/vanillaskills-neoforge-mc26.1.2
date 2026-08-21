package io.github.andrewwwwwwwwwwwwwww.vanillaskills.loot;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.Set;

/**
 * Scatters Unstable Skill Shards thinly through the world: a rare find in structure chests, and an
 * uncommon result from bartering with piglins.
 *
 * <p>Both rates are deliberately low and both are config-driven — shards are the skill-tree currency, so
 * this is a trickle that rewards exploration, not a replacement for earning them. Setting either weight to
 * 0 removes that source entirely.
 *
 * <p>⚠ Edition-specific: this is the NeoForge copy, using {@link LootTableLoadEvent}. The Fabric edition is
 * the same logic over {@code LootTableEvents.MODIFY}. Keep the weights and table list in step.
 */
public final class ShardLoot {
    private ShardLoot() {}

    /** Structure chests that can turn up a shard. Deliberately mid-to-late game locations. */
    private static final Set<ResourceKey<LootTable>> CHESTS = Set.of(
            BuiltInLootTables.ANCIENT_CITY,
            BuiltInLootTables.STRONGHOLD_LIBRARY,
            BuiltInLootTables.END_CITY_TREASURE,
            BuiltInLootTables.NETHER_BRIDGE,
            BuiltInLootTables.BASTION_TREASURE,
            BuiltInLootTables.WOODLAND_MANSION,
            BuiltInLootTables.SIMPLE_DUNGEON);

    public static void register() {
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            if (CHESTS.contains(event.getKey()) && GameplayConfig.SHARD_CHEST_WEIGHT > 0) {
                event.getTable().addPool(
                        pool(GameplayConfig.SHARD_CHEST_WEIGHT, GameplayConfig.SHARD_CHEST_EMPTY_WEIGHT).build());
            } else if (event.getKey().equals(BuiltInLootTables.PIGLIN_BARTERING)
                    && GameplayConfig.SHARD_BARTER_WEIGHT > 0) {
                // Bartering is a single-roll table, so adding a pool of our own would grant an extra item on
                // every trade. Weighting against an empty entry keeps the usual "one item per ingot" feel.
                event.getTable().addPool(
                        pool(GameplayConfig.SHARD_BARTER_WEIGHT, GameplayConfig.SHARD_BARTER_EMPTY_WEIGHT).build());
            }
        });
    }

    private static LootPool.Builder pool(int shardWeight, int emptyWeight) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(EmptyLootItem.emptyItem().setWeight(Math.max(1, emptyWeight)))
                .add(LootItem.lootTableItem(Items.WRITTEN_BOOK)
                        .setWeight(shardWeight)
                        // Same components ShardItems stamps, so a found shard is indistinguishable from a
                        // withdrawn one — including being bankable by right-click.
                        //
                        // ⚠ Built from the component accessors, NOT by reading them back off a real stack.
                        // This runs on a worker thread while loot tables load, before item components are
                        // bound, so `new ItemStack(...)` here throws "Components not bound yet" and takes the
                        // server down with "Failed to load datapacks".
                        .apply(SetComponentsFunction.setComponent(DataComponents.CUSTOM_DATA,
                                CustomData.of(Markers.markerTag(ShardItems.USS_MARKER))))
                        .apply(SetComponentsFunction.setComponent(DataComponents.ITEM_MODEL,
                                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                        "vanillaskills", "unstable_skill_shard")))
                        .apply(SetComponentsFunction.setComponent(DataComponents.ITEM_NAME,
                                ShardItems.unstableShardName()))
                        .apply(SetComponentsFunction.setComponent(DataComponents.LORE,
                                ShardItems.unstableShardLore())))
                        // The shard is a written book (see ShardItems#unstableShard): lift its 16 stack to
                        // 64 and hide the book-content tooltip line, exactly as the item builder does.
                        .apply(SetComponentsFunction.setComponent(DataComponents.MAX_STACK_SIZE,
                                io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.USS_STACK_SIZE))
                        .apply(SetComponentsFunction.setComponent(DataComponents.TOOLTIP_DISPLAY,
                                io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.USS_TOOLTIP));
    }
}
