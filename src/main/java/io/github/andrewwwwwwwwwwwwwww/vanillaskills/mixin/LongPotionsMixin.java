package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * "Brewmaster" skill: a player who has unlocked the {@code long_potions} flag gets longer
 * beneficial potion effects — long enough to exceed the vanilla 8-minute cap. Only beneficial,
 * non-infinite, potion-length (>= 30s) effects are lengthened, so mob debuffs are never extended
 * and short/beacon effects are left alone.
 */
@Mixin(LivingEntity.class)
public class LongPotionsMixin {

    private static final int MIN_DURATION_TICKS = 600; // 30s — skips beacon/short effects

    @ModifyVariable(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true)
    private MobEffectInstance vanillaskills$extendBeneficialPotions(MobEffectInstance instance) {
        if (instance == null) return null;
        if (!((Object) this instanceof ServerPlayer player)) return instance;
        float multiplier = CraftingGate.potionDurationMultiplier(player);
        if (multiplier <= 1.0f) return instance;
        if (instance.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) return instance;
        if (instance.isInfiniteDuration() || instance.getDuration() < MIN_DURATION_TICKS) return instance;

        int extended = (int) Math.min(Integer.MAX_VALUE, instance.getDuration() * multiplier);
        return new MobEffectInstance(instance.getEffect(), extended, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon());
    }
}
