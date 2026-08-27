package io.github.andrewwwwwwwwwwwwwww.vanillaskills.loot;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * Puts crates in the water: a rare extra catch alongside whatever the rod already pulled up.
 *
 * <p>This injects <b>one</b> entry — a reference to {@code vanillaskills:crate_fishing} — and nothing else.
 * Which crate you get, how rare each one is, which biomes have their own, and how much the Unboxing
 * enchantment helps are all decided inside that table, which ships as an ordinary datapack file. A pack can
 * rewrite any of it without a code change, and the two knobs here only control how often the fishing table
 * defers to it at all.
 *
 * <p>The reference matters for a second reason: loot tables finish loading <i>before</i> the mod's own
 * datapack content does, so a pool built here from the loaded crate list would be empty on the first load of
 * every server. Naming a table sidesteps the ordering entirely — the reference is resolved when it is rolled.
 *
 * <p>⚠ Edition-specific: this is the NeoForge copy, using {@link LootTableLoadEvent}. The Fabric edition is
 * the same logic over {@code LootTableEvents.MODIFY}. Keep the weights in step.
 */
public final class CrateLoot {
    private CrateLoot() {}

    /** The table that picks a crate. Shipped by the mod; overridable by any pack. */
    public static final ResourceKey<LootTable> CRATE_FISHING = ResourceKey.create(
            Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("vanillaskills", "crate_fishing"));

    public static void register() {
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            if (!event.getKey().equals(BuiltInLootTables.FISHING)) return;
            if (GameplayConfig.CRATE_FISHING_WEIGHT <= 0) return;

            // A pool of our own, so a crate is a bonus on top of the normal catch rather than replacing it.
            // Weighted against empty, which is what keeps crates rare.
            //
            // Both weights are scaled ×10 so `quality` — vanilla's "add this per point of fishing luck to
            // the entry's weight" — nudges the odds by ~10% relative per point instead of doubling them.
            // Fishing luck is Luck of the Sea's level plus the luck attribute, so the Fortune Finder lane
            // counts too: at defaults, 2.4% base → ~3.2% with LotS III → ~4.3% with a maxed lane on top.
            // Unboxing deliberately stays out of this roll: it decides WHICH crate, not how often.
            event.getTable().addPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(EmptyLootItem.emptyItem()
                            .setWeight(Math.max(1, GameplayConfig.CRATE_FISHING_EMPTY_WEIGHT) * 10))
                    .add(NestedLootTable.lootTableReference(CRATE_FISHING)
                            .setWeight(GameplayConfig.CRATE_FISHING_WEIGHT * 10)
                            .setQuality(GameplayConfig.CRATE_FISHING_LUCK_QUALITY))
                    .build());
        });
    }
}
