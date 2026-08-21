package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Makes placed Skill Shard blocks immune to explosions — creepers above all.
 *
 * <p>Reinforced deepslate is blast-proof in vanilla, but lodestone is not, and both carry a tracked record
 * a creeper would happily delete along with the block. Rather than special-casing creepers, this reports an effectively infinite
 * blast resistance for any position VanillaSkills is tracking, which covers TNT, ghasts, beds and end
 * crystals too.
 *
 * <p>This is the position-aware hook — {@code Block.getExplosionResistance()} only knows the block type, and
 * every amethyst block in the world is not supposed to become blast-proof.
 *
 * <p>⚠ Worth remembering when testing near spawn: spawnmanager's own explosion mixin cancels <em>all</em>
 * explosions inside the spawn-protection radius, which once caused a VanillaSkills release to be withdrawn
 * over a bug that did not exist. Test this away from spawn.
 */
@Mixin(ExplosionDamageCalculator.class)
public class ShardBlastProofMixin {

    @Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$shardBlocksResistExplosions(Explosion explosion, BlockGetter blockGetter,
                                                           BlockPos pos, BlockState state, FluidState fluid,
                                                           CallbackInfoReturnable<Optional<Float>> cir) {
        if (!(blockGetter instanceof ServerLevel level)) return;
        if (VanillaSkills.SHARDS == null) return;
        if (VanillaSkills.SHARDS.kindAt(level, pos) == null) return;
        cir.setReturnValue(Optional.of(Float.MAX_VALUE));
    }
}
