package io.github.andrewwwwwwwwwwwwwww.vanillaskills.world;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Bone meal spreads spore blossoms.
 *
 * <p>Spore blossoms are the one lush-cave plant with no way to make more of them: they grow nowhere, spread
 * to nothing, and bone meal does not touch them, so the only supply is whatever the world generated. Bone
 * meal on one now grows another on a ceiling nearby, the way it spreads any other plant, which makes them a
 * crop rather than a finite resource — and gives a reason to keep a lush cave rather than strip it.
 *
 * <p>Deliberately spreads to a <i>random</i> valid ceiling within a small radius rather than duplicating in
 * place, so a blossom farm is a cave you tend rather than a single block you click.
 */
public final class SporeBlossoms {
    private SporeBlossoms() {}

    /** How far a blossom can seed, horizontally and vertically. */
    private static final int SPREAD_RADIUS = 4;
    private static final int SPREAD_HEIGHT = 3;
    /** How many places are considered before giving up, so a sealed room does not eat the bone meal. */
    private static final int CANDIDATE_LIMIT = 64;
    /** Air needed under a new blossom, so one never grows into the middle of a wall. */
    private static final int CLEARANCE = 2;

    /**
     * Handles a right-click with bone meal. Returns true if it was used, in which case the interaction is
     * finished and the caller should not let vanilla have it.
     */
    public static boolean tryBoneMeal(ServerLevel level, BlockPos pos, Player player, ItemStack held) {
        if (!GameplayConfig.BONEMEAL_SPORE_BLOSSOMS) return false;
        if (!held.is(Items.BONE_MEAL)) return false;
        if (!level.getBlockState(pos).is(Blocks.SPORE_BLOSSOM)) return false;

        BlockPos target = findSpot(level, pos);
        if (target == null) return false; // nowhere to grow: the bone meal is not spent

        level.setBlockAndUpdate(target, Blocks.SPORE_BLOSSOM.defaultBlockState());
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, target, 0);
        if (!player.hasInfiniteMaterials()) held.shrink(1);
        return true;
    }

    /** A ceiling near {@code from} with room beneath it and no blossom on it yet. */
    private static BlockPos findSpot(ServerLevel level, BlockPos from) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -SPREAD_RADIUS; dx <= SPREAD_RADIUS; dx++) {
            for (int dz = -SPREAD_RADIUS; dz <= SPREAD_RADIUS; dz++) {
                for (int dy = -SPREAD_HEIGHT; dy <= SPREAD_HEIGHT; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    BlockPos pos = from.offset(dx, dy, dz);
                    if (canGrowAt(level, pos)) candidates.add(pos);
                    if (candidates.size() >= CANDIDATE_LIMIT) break;
                }
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(level.getRandom().nextInt(candidates.size()));
    }

    private static boolean canGrowAt(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        // Hanging from something solid, as a spore blossom does.
        BlockState ceiling = level.getBlockState(pos.above());
        if (!ceiling.isFaceSturdy(level, pos.above(), Direction.DOWN)) return false;
        // And with room to hang into.
        for (int i = 1; i <= CLEARANCE; i++) {
            if (!level.getBlockState(pos.below(i)).isAir()) return false;
        }
        return true;
    }
}
