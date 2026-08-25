package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.TaskShards;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The placement half of the task-shard trickle. Breaking runs through a Fabric event, but placement has
 * no equivalent callback, so this hooks {@link BlockItem#place} — the same funnel
 * {@link ShardBlockPlaceMixin} uses — and rolls only for placements that actually consumed the action.
 * Requires a real player: a dispenser placing blocks earns nobody anything.
 */
@Mixin(BlockItem.class)
public class TaskShardPlaceMixin {

    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void vanillaskills$taskShardOnPlace(BlockPlaceContext context,
                                                CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        if (!(context.getLevel() instanceof ServerLevel level)) return;
        if (!(context.getPlayer() instanceof ServerPlayer sp)) return;
        TaskShards.roll(level, sp, context.getClickedPos());
    }
}
