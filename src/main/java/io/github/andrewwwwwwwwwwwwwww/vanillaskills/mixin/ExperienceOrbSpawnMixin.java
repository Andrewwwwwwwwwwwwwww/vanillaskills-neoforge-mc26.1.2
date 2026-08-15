package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The backstop for experience removal: no orb ever enters the world.
 *
 * <p>{@code ExperienceOrbMixin} covers {@link ExperienceOrb#award} and {@code awardWithDirection}, which is
 * how <b>most</b> vanilla code grants experience — mob deaths, ore breaking, and furnaces all route through
 * {@code award}. But not everything does. {@code Animal.spawnChildFromBreeding} constructs an
 * {@code ExperienceOrb} directly and adds it to the level, bypassing those helpers entirely, so breeding kept
 * dropping orbs after every other source had been shut off.
 *
 * <p>Rather than chase each direct-construction site as it turns up, this gates the one place they all have
 * to pass through. Anything that builds an orb by hand still gets rejected here.
 *
 * <p>Returns false, which is what vanilla returns when an entity fails to be added, so callers that check
 * the result behave as though the spawn simply did not take.
 */
@Mixin(ServerLevel.class)
public class ExperienceOrbSpawnMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$refuseExperienceOrbs(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (GameplayConfig.EXPERIENCE_ENABLED) return;
        if (entity instanceof ExperienceOrb) {
            cir.setReturnValue(false);
        }
    }
}
