package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Evasion skill: a player has a chance (up to 20%) to completely dodge incoming arrow damage,
 * scaling with how many Evasion nodes they've unlocked. Rolled per hit on the server.
 */
@Mixin(LivingEntity.class)
public class ArrowDodgeMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$arrowDodge(ServerLevel level, DamageSource source, float amount,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (!source.is(DamageTypes.ARROW)) return;
        if (!((Object) this instanceof ServerPlayer player)) return;

        float chance = CraftingGate.arrowDodgeChance(player);
        if (chance > 0.0f && player.getRandom().nextFloat() < chance) {
            cir.setReturnValue(false); // dodged — take no damage
        }
    }
}
