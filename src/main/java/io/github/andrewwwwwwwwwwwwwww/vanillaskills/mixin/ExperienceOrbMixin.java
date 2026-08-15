package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops experience orbs ever entering the world.
 *
 * <p>Every vanilla source of experience — mob kills, ores, furnaces, spawners, breeding, fishing,
 * bottles o' enchanting, villager trades, the grindstone — funnels through these two statics, and
 * {@code award} itself simply delegates to {@code awardWithDirection}. Cancelling the delegate alone
 * would be sufficient; both are cancelled so a future refactor that stops delegating cannot quietly
 * start leaking orbs again.
 *
 * <p>Blocking the spawn rather than the pickup means no orb entities are created at all, so there is
 * no visual litter and no entity churn.
 */
@Mixin(ExperienceOrb.class)
public class ExperienceOrbMixin {

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private static void vanillaskills$noAward(ServerLevel level, Vec3 pos, int amount, CallbackInfo ci) {
        if (!GameplayConfig.EXPERIENCE_ENABLED) ci.cancel();
    }

    @Inject(method = "awardWithDirection", at = @At("HEAD"), cancellable = true)
    private static void vanillaskills$noAwardWithDirection(ServerLevel level, Vec3 pos, Vec3 direction,
                                                           int amount, CallbackInfo ci) {
        if (!GameplayConfig.EXPERIENCE_ENABLED) ci.cancel();
    }
}
