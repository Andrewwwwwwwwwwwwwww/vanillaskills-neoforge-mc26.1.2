package io.github.andrewwwwwwwwwwwwwww.vanillaskills.loot;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.FortuneTemplate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * Adds the Fortune Upgrade template to Ancient City and minecart (abandoned mineshaft) chests
 * at roughly the rarity of an Ancient City enchanted golden apple.
 *
 * The apple is weight 1 in a pool of total weight 84 rolled 5-10 times, giving ~8.6% per chest.
 * We match that with a dedicated single-roll pool: template weight 3 vs empty weight 32 =
 * 3/35 ~= 8.57% per chest. Injected via the loot-load event (non-destructive) and producing a vanilla
 * netherite template carrying our name + marker, so it stays fully server-side.
 */
public final class FortuneTemplateLoot {
    private FortuneTemplateLoot() {}

    private static final int TEMPLATE_WEIGHT = 3;
    private static final int EMPTY_WEIGHT = 32;

    public static void register() {
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            if (event.getKey().equals(BuiltInLootTables.ANCIENT_CITY)
                    || event.getKey().equals(BuiltInLootTables.ABANDONED_MINESHAFT)) {
                event.getTable().addPool(templatePool().build());
            }
        });
    }

    private static LootPool.Builder templatePool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(EmptyLootItem.emptyItem().setWeight(EMPTY_WEIGHT))
                .add(LootItem.lootTableItem(FortuneTemplate.BASE)
                        .setWeight(TEMPLATE_WEIGHT)
                        .apply(SetComponentsFunction.setComponent(DataComponents.ITEM_NAME, FortuneTemplate.displayName()))
                        .apply(SetComponentsFunction.setComponent(DataComponents.CUSTOM_DATA, CustomData.of(FortuneTemplate.markerTag())))
                        .apply(SetComponentsFunction.setComponent(DataComponents.ITEM_MODEL, FortuneTemplate.modelId()))
                        .apply(SetComponentsFunction.setComponent(DataComponents.LORE, FortuneTemplate.lore())));
    }
}
