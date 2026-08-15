package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refuses any direct grant of experience to a player.
 *
 * <p>{@code ExperienceOrbMixin} already stops orbs spawning, which covers the world-facing sources.
 * This closes the paths that hand experience straight to a player without an orb — the {@code /xp}
 * command being the main one — so the level counter can never move.
 */
@Mixin(Player.class)
public class PlayerExperienceMixin {

    @Inject(method = "giveExperiencePoints", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$noExperiencePoints(int amount, CallbackInfo ci) {
        if (!GameplayConfig.EXPERIENCE_ENABLED) ci.cancel();
    }

    @Inject(method = "giveExperienceLevels", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$noExperienceLevels(int amount, CallbackInfo ci) {
        if (!GameplayConfig.EXPERIENCE_ENABLED) ci.cancel();
    }
}
