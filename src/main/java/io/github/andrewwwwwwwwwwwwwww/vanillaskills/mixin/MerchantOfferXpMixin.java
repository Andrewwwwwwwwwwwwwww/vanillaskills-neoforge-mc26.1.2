package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops villager trades rewarding the player with experience.
 *
 * <p>{@code ExperienceOrbMixin} would already swallow the resulting orb, but vanilla checks this flag
 * <em>before</em> deciding to create one — so answering here means no orb is ever constructed, rather
 * than one being built and then discarded.
 *
 * <p>This governs the player's reward only. The villager's own trading XP, which is what levels a
 * villager up and unlocks its higher tiers, is a separate value and is deliberately left alone.
 */
@Mixin(MerchantOffer.class)
public class MerchantOfferXpMixin {

    @Inject(method = "shouldRewardExp", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$noTradeExperience(CallbackInfoReturnable<Boolean> cir) {
        if (!GameplayConfig.EXPERIENCE_ENABLED) cir.setReturnValue(false);
    }
}
