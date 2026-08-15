package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fortune Finder for trial-chamber vaults. Vaults don't fill a container (they eject items), so the
 * {@code LootTable.fill} hook never sees them — this appends {@code floor(0.4 × luck)} bonus items
 * to the ejected reward instead, taken from a second roll of the vault's own loot table (done by
 * re-entering the vanilla resolver, so loot params stay exactly vanilla). Same rate as containers:
 * +1 item per 2.5 luck, +2 at a maxed Fortune Finder lane.
 */
@Mixin(VaultBlockEntity.Server.class)
public abstract class VaultLuckMixin {

    @Unique
    private static final ThreadLocal<Boolean> VANILLASKILLS$REROLLING = ThreadLocal.withInitial(() -> false);

    @Shadow
    private static List<ItemStack> resolveItemsToEject(ServerLevel level, VaultConfig config, BlockPos pos,
                                                       Player player, ItemInstance key) {
        throw new AssertionError();
    }

    @Inject(method = "resolveItemsToEject", at = @At("RETURN"), cancellable = true)
    private static void vanillaskills$luckBonusItems(ServerLevel level, VaultConfig config, BlockPos pos,
                                                     Player player, ItemInstance key,
                                                     CallbackInfoReturnable<List<ItemStack>> cir) {
        if (VANILLASKILLS$REROLLING.get()) return;
        int bonus = Mth.floor(0.4f * player.getLuck());
        if (bonus <= 0) return;

        List<ItemStack> out = new ArrayList<>(cir.getReturnValue());
        VANILLASKILLS$REROLLING.set(true);
        try {
            int placed = 0;
            for (ItemStack stack : resolveItemsToEject(level, config, pos, player, key)) {
                if (placed >= bonus) break;
                if (stack.isEmpty() || vanillaskills$isBlankBook(stack)) continue;
                out.add(stack);
                placed++;
            }
        } finally {
            VANILLASKILLS$REROLLING.set(false);
        }
        cir.setReturnValue(out);
    }

    /** An enchanted book with no stored enchantments is worthless "blank" loot — never eject it. */
    @Unique
    private static boolean vanillaskills$isBlankBook(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return false;
        var ench = stack.get(DataComponents.STORED_ENCHANTMENTS);
        return ench == null || ench.isEmpty();
    }
}
