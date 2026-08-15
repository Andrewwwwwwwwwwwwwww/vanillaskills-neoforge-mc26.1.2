package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.DragonSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dragon (Netherite II) full-set immunity to fire, lava, and dragon's breath. Cancels the damage
 * at the source while the full set is worn (DragonSet.tick also keeps the wearer from visibly
 * burning). Reverts instantly when a piece is removed since it's checked live per hit.
 */
@Mixin(LivingEntity.class)
public class DragonImmunityMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$dragonFireImmunity(ServerLevel level, DamageSource source, float amount,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (!source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypes.DRAGON_BREATH)) return;
        if (DragonSet.isFullSet((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
