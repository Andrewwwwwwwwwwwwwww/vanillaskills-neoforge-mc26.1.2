package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

/**
 * A beacon standing on a Stable Skill Shard Block reaches much further and hits harder.
 *
 * <p>Runs at TAIL and <b>adds</b> a second, boosted application rather than cancelling and reimplementing
 * vanilla's method. A {@link MobEffectInstance} with a higher amplifier supersedes the weaker one the beacon
 * just applied, and the wider box is a superset of the normal one — so the result is the intended upgrade
 * with none of the risk of re-deriving vanilla's logic and drifting from it later.
 *
 * <p>Vanilla's own numbers, confirmed from the compiled method rather than assumed: range is
 * {@code levels * 10 + 10}, duration is {@code (9 + levels * 2) * 20} ticks, and the amplifier is 0, or 1
 * when {@code levels >= 4} and both effects are the same. Both multipliers here are configurable.
 */
@Mixin(BeaconBlockEntity.class)
public class BeaconShardBaseMixin {

    @Inject(method = "applyEffects", at = @At("TAIL"))
    private static void vanillaskills$boostOnShardBase(Level level, BlockPos pos, int levels,
                                                       Holder<MobEffect> primary, Holder<MobEffect> secondary,
                                                       CallbackInfo ci) {
        if (primary == null) return;
        // A ServerLevel is never client-side, so this doubles as the side check.
        if (!(level instanceof ServerLevel serverLevel)) return;
        // The block directly beneath the beacon stands in for "the pyramid is built from our blocks" — cheap,
        // and a player who bothered to put one there has clearly built the base out of them.
        if (VanillaSkills.SHARDS == null
                || VanillaSkills.SHARDS.kindAt(serverLevel, pos.below()) != ShardBlocks.Kind.STABLE) {
            return;
        }

        int rangeMult = GameplayConfig.SHARD_BEACON_RANGE_MULT;
        int ampBonus = GameplayConfig.SHARD_BEACON_AMPLIFIER_BONUS;
        if (rangeMult <= 1 && ampBonus <= 0) return; // boost turned off in config

        double range = (levels * 10 + 10) * (double) Math.max(1, rangeMult);
        int duration = (9 + levels * 2) * 20;
        int vanillaAmplifier = (levels >= 4 && Objects.equals(primary, secondary)) ? 1 : 0;

        AABB box = new AABB(pos).inflate(range).expandTowards(0.0, level.getHeight(), 0.0);
        List<Player> players = level.getEntitiesOfClass(Player.class, box);
        for (Player player : players) {
            player.addEffect(new MobEffectInstance(primary, duration, vanillaAmplifier + ampBonus, true, true));
        }
        if (levels >= 4 && secondary != null && !Objects.equals(primary, secondary)) {
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(secondary, duration, ampBonus, true, true));
            }
        }
    }
}
