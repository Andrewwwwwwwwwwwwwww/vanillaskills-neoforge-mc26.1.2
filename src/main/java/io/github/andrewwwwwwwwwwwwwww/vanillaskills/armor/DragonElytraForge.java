package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * Fusing an Elytra onto a Dragon chestplate via dropped items (modelled on the Vanilla Tweaks
 * "Armored Elytra" datapack, since anvil-GUI elytra combining isn't natively supported):
 *   - Drop a Dragon chestplate + an Elytra together on top of an ANVIL block -> they merge into one
 *     gliding Dragon chestplate.
 *   - Drop a combined chestplate on a GRINDSTONE block -> it splits back into the chestplate + elytra.
 * Scanned on a timer (see VanillaSkills tick). Fully server-side, vanilla-client safe.
 */
public final class DragonElytraForge {
    private DragonElytraForge() {}

    private static final double MERGE_DISTANCE_SQR = 4.0;
    private static final int PICKUP_DELAY = 10;

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends ItemEntity> items = level.getEntities(EntityType.ITEM, ItemEntity::isAlive);
            if (items.isEmpty()) continue;
            mergeOnAnvils(level, items);
            splitOnGrindstones(level, items);
        }
    }

    private static void mergeOnAnvils(ServerLevel level, List<? extends ItemEntity> items) {
        for (ItemEntity elytra : items) {
            if (!elytra.isAlive() || !elytra.getItem().is(Items.ELYTRA) || DragonElytra.isCombined(elytra.getItem())) continue;
            if (!isOn(level, elytra, BlockTags.ANVIL)) continue;

            for (ItemEntity chest : items) {
                if (chest == elytra || !chest.isAlive() || !DragonElytra.isDragonChest(chest.getItem())) continue;
                if (chest.distanceToSqr(elytra) > MERGE_DISTANCE_SQR || !isOn(level, chest, BlockTags.ANVIL)) continue;

                ItemStack combined = DragonElytra.combine(elytra.getItem(), chest.getItem());
                spawn(level, elytra, combined);
                elytra.discard();
                chest.discard();
                level.playSound(null, elytra.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
                break;
            }
        }
    }

    private static void splitOnGrindstones(ServerLevel level, List<? extends ItemEntity> items) {
        for (ItemEntity item : items) {
            if (!item.isAlive() || !DragonElytra.isCombined(item.getItem())) continue;
            if (!isOnGrindstone(level, item)) continue;

            ItemStack[] parts = DragonElytra.split(item.getItem());
            spawn(level, item, parts[0]);
            spawn(level, item, parts[1]);
            item.discard();
            level.playSound(null, item.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
        }
    }

    private static void spawn(ServerLevel level, ItemEntity at, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity e = new ItemEntity(level, at.getX(), at.getY() + 0.1, at.getZ(), stack);
        e.setDeltaMovement(0, 0, 0);
        e.setPickUpDelay(PICKUP_DELAY);
        level.addFreshEntity(e);
    }

    private static boolean isOn(ServerLevel level, ItemEntity item, net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag) {
        return level.getBlockState(blockBelow(item)).is(tag);
    }

    private static boolean isOnGrindstone(ServerLevel level, ItemEntity item) {
        return level.getBlockState(blockBelow(item)).is(Blocks.GRINDSTONE);
    }

    private static BlockPos blockBelow(ItemEntity item) {
        return BlockPos.containing(item.getX(), item.getY() - 0.01, item.getZ());
    }
}
