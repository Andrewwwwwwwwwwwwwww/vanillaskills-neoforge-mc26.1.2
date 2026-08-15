package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fortune Finder: the vanilla Luck attribute now improves ALL naturally generated container loot.
 *
 * <p>{@code LootTable.fill} is the single funnel every loot-tagged container unpacks through —
 * chests, barrels, trial-chamber corridor chests, chest minecarts, chest boats — and vanilla
 * already passes the opening player's luck into {@code LootParams} (the tables just never use it).
 * After the normal fill, players get {@code floor(0.4 × luck)} bonus items (+1 per 2.5 luck, so +2
 * at a maxed Fortune Finder lane), rolled fresh from the container's own loot table and placed
 * into random empty slots. Bad Luck (negative) simply yields no bonus.
 */
@Mixin(LootTable.class)
public abstract class ContainerLuckMixin {

    @Shadow
    public abstract ObjectArrayList<ItemStack> getRandomItems(LootParams params);

    @Inject(method = "fill", at = @At("TAIL"))
    private void vanillaskills$luckBonusItems(Container container, LootParams params, long seed, CallbackInfo ci) {
        int bonus = Mth.floor(0.4f * params.getLuck());
        if (bonus <= 0) return;

        IntArrayList emptySlots = new IntArrayList();
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) emptySlots.add(i);
        }
        if (emptySlots.isEmpty()) return;

        RandomSource random = params.getLevel().getRandom();
        int placed = 0;
        // Fresh unseeded roll of the same table, so bonus items match the container's loot pool.
        for (ItemStack stack : this.getRandomItems(params)) {
            if (placed >= bonus || emptySlots.isEmpty()) break;
            if (stack.isEmpty() || vanillaskills$isBlankBook(stack)) continue;
            int slot = emptySlots.removeInt(random.nextInt(emptySlots.size()));
            container.setItem(slot, stack);
            placed++;
        }
    }

    /** An enchanted book with no stored enchantments is worthless "blank" loot — never place it. */
    @org.spongepowered.asm.mixin.Unique
    private static boolean vanillaskills$isBlankBook(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return false;
        var ench = stack.get(DataComponents.STORED_ENCHANTMENTS);
        return ench == null || ench.isEmpty();
    }
}
