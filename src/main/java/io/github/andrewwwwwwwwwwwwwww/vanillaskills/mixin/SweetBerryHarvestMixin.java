package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Farming;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cultivator bonus for sweet berries. Berry bushes are harvested by RIGHT-click, so the block-break
 * bonus path never fires for them — this hooks the harvest interaction instead. Mirrors vanilla's
 * own harvest condition (age >= 2 yields berries).
 */
@Mixin(SweetBerryBushBlock.class)
public abstract class SweetBerryHarvestMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    private void vanillaskills$bonusBerries(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) return;
        if (state.getValue(SweetBerryBushBlock.AGE) < 2) return; // vanilla only harvests at age 2+
        Farming.rollBonus(serverLevel, pos, sp, Items.SWEET_BERRIES);
    }
}
