package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Records a Skill Shard block whenever one is placed, whatever placed it.
 *
 * <h2>Why this exists</h2>
 * A Stable block's aura is driven by iterating VanillaSkills' tracked position list, and for a long time
 * {@code ShardBlocks.register} was called from exactly one place: the mod's own right-click handler. Any
 * block that reached the world by another route — a dispenser, a plugin, an admin command — was a real,
 * correctly-textured block with <b>no behaviour at all</b>: no aura, no merge, no drops. It measured as a
 * zombie standing beside a Stable block for eight seconds and taking zero damage.
 *
 * <p>{@code BlockItem#place} is the single funnel every item-placed block goes through, so hooking it
 * catches players and dispensers alike and leaves vanilla to do the actual placing — which means vanilla's
 * own rules apply for free: collision with entities, replaceability, adjacent-block checks, sounds.
 *
 * <p>Registration is idempotent ({@code register} returns early on a position it already knows), so it is
 * safe even where the mod's own handler also fires.
 *
 * <p>⚠ Not a complete net: {@code /setblock}, {@code /fill} and worldgen bypass {@code BlockItem} entirely.
 * Those are admin and generation paths rather than gameplay, and {@code ShardBlocks.kindAt} answers from the
 * block itself, so such a block is still recognised for mining and drops — it just has no aura until it is
 * re-placed. Worldgen ore is handled separately by {@code ShardOre}, which never needed tracking.
 */
@Mixin(BlockItem.class)
public class ShardBlockPlaceMixin {

    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At("RETURN"))
    private void vanillaskills$trackShardBlock(BlockPlaceContext context,
                                               CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        if (!(context.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = context.getClickedPos();
        ShardBlocks.Kind kind = VanillaSkills.SHARDS.kindAt(level, pos);
        if (kind == null) return; // not one of ours

        VanillaSkills.SHARDS.register(level, pos, kind);
    }
}
