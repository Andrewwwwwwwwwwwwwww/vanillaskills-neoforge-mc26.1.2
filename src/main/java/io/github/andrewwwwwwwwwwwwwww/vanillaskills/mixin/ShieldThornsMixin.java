package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.shield.SteelShield;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Steel-Infused Shield thorns: when an attack is successfully blocked with the steel shield, the
 * melee attacker takes a little damage back. {@code applyItemBlocking} returns the amount of damage
 * blocked (>0 = a block happened). Guards against thorns damage to avoid retaliation loops.
 */
@Mixin(LivingEntity.class)
public class ShieldThornsMixin {

    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void vanillaskills$shieldThorns(ServerLevel level, DamageSource source, float damage,
                                            CallbackInfoReturnable<Float> cir) {
        if (cir.getReturnValueF() <= 0.0f) return;        // nothing was blocked
        if (source.is(DamageTypes.THORNS)) return;        // don't retaliate against thorns
        LivingEntity self = (LivingEntity) (Object) this;
        if (!SteelShield.isSteelShield(self.getUseItem())) return;
        Entity attacker = source.getDirectEntity();
        if (attacker instanceof LivingEntity living && living != self) {
            living.hurtServer(level, self.damageSources().thorns(self), SteelShield.thornsDamage());
        }
    }
}
