package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Naturally generated Skill Shard ore.
 *
 * <h2>Why this needs no position tracking</h2>
 * Placed shard blocks are identified by a per-position record, but worldgen runs during chunk generation
 * and has no natural hook to write one. Rather than bolt on a chunk scan, the ore uses a block that
 * <b>identifies itself</b>: {@link Blocks#REINFORCED_DEEPSLATE} cannot be crafted, cannot be obtained (its
 * loot table is empty, even with Silk Touch), and therefore cannot be placed by a player. The only way one
 * exists is if the game put it there.
 *
 * <p>Vanilla puts it in exactly one place — ancient city floors, around Y −52 — which sits far below the
 * Overworld band this ore generates in, and it never appears in the Nether or the End at all. So a height
 * and dimension check cleanly separates our ore from vanilla's without any bookkeeping, and there is no
 * place-and-break exploit because the block cannot be obtained in the first place.
 *
 * <p>The bands here must stay in step with the placed-feature JSONs in
 * {@code data/vanillaskills/worldgen/placed_feature/}; both are configurable.
 */
public final class ShardOre {
    private ShardOre() {}

    /** True if this block is our generated ore rather than vanilla's ancient-city reinforced deepslate. */
    public static boolean isOre(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.REINFORCED_DEEPSLATE)) return false;
        int y = pos.getY();
        return switch (level.dimension().identifier().toString()) {
            case "minecraft:overworld" ->
                    y >= GameplayConfig.SHARD_ORE_OVERWORLD_MIN_Y && y <= GameplayConfig.SHARD_ORE_OVERWORLD_MAX_Y;
            // Reinforced deepslate never generates in either of these, so any we find is ours. The height
            // check still mirrors the placed feature so the two cannot drift apart unnoticed.
            case "minecraft:the_nether" -> y <= GameplayConfig.SHARD_ORE_NETHER_MAX_Y;
            case "minecraft:the_end" -> true;
            default -> false;
        };
    }
}
