package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.RoseGoldSet;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rose Gold full-set immunity to negative effects, evaluated live at the moment an effect would be
 * applied: while all four pieces are worn, harmful effects simply can't take hold. Because it's
 * checked per-application (not a cached flag), removing any piece reverts it immediately.
 */
@Mixin(LivingEntity.class)
public class RoseGoldEffectMixin {

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$roseGoldImmunity(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) return;
        if (RoseGoldSet.isFullSet((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
