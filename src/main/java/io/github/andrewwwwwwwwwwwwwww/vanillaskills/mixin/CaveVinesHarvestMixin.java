package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Farming;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cultivator bonus for glow berries. Cave vines are picked by RIGHT-click (both the head and body
 * blocks route through {@link CaveVines#use}), so the block-break bonus path never fires — this
 * hooks the shared pick method instead. Only fires when the vine actually has berries.
 */
@Mixin(CaveVines.class)
public interface CaveVinesHarvestMixin {

    @Inject(method = "use", at = @At("HEAD"))
    private static void vanillaskills$bonusGlowBerries(Entity entity, BlockState state, Level level, BlockPos pos,
                                                       CallbackInfoReturnable<InteractionResult> cir) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer sp)) return;
        if (!state.getValue(CaveVines.BERRIES)) return;
        Farming.rollBonus(serverLevel, pos, sp, Items.GLOW_BERRIES);
    }
}
