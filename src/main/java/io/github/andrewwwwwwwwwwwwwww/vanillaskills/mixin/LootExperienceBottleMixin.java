package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/**
 * Keeps Bottles o' Enchanting out of generated loot.
 *
 * <p>With experience removed the bottle is a dead item, so it should not be taking up a chest slot.
 * Three vanilla loot tables contain it — ancient city, pillager outpost and shipwreck treasure — but
 * this filters at the generation chokepoint rather than overriding those three files, for two
 * reasons: overriding a vanilla loot table freezes its contents at the version we copied (the same
 * trap that caused the armour-trim regression), and filtering here also covers datapack and add-on
 * tables we do not control.
 *
 * <p>{@code getRandomItemsRaw(LootContext, Consumer)} is the single funnel — every public
 * {@code getRandomItems} overload routes into it, directly or via the {@code LootParams} form — so
 * wrapping its consumer catches every path with one injection.
 */
@Mixin(LootTable.class)
public class LootExperienceBottleMixin {

    @ModifyVariable(
            method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true, index = 2)
    private Consumer<ItemStack> vanillaskills$stripExperienceBottles(Consumer<ItemStack> original) {
        if (GameplayConfig.EXPERIENCE_ENABLED) return original;
        return stack -> {
            if (!stack.is(Items.EXPERIENCE_BOTTLE)) original.accept(stack);
        };
    }
}
