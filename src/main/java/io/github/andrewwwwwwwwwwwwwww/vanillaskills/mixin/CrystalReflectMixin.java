package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.CrystalSet;
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
 * Crystalline (Diamond II) full-set melee reflect: when the wearer actually takes damage from a
 * living melee attacker and is wearing the full Crystalline set, a fraction of that damage is dealt
 * back to the attacker as thorns. Guards against thorns sources to avoid retaliation loops.
 */
@Mixin(LivingEntity.class)
public class CrystalReflectMixin {

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void vanillaskills$crystalReflect(ServerLevel level, DamageSource source, float amount,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;          // no damage was applied
        if (source.is(DamageTypes.THORNS)) return;   // don't retaliate against thorns

        LivingEntity self = (LivingEntity) (Object) this;
        if (!CrystalSet.isFullSet(self)) return;

        Entity attacker = source.getDirectEntity();
        if (attacker instanceof LivingEntity living && living != self) {
            float reflect = amount * CrystalSet.reflectFraction();
            if (reflect > 0.0f) {
                living.hurtServer(level, self.damageSources().thorns(self), reflect);
            }
        }
    }
}
