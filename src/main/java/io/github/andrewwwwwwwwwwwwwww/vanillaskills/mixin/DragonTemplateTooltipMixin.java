package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.DragonUpgradeTemplate;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * The Dragon Upgrade template is built on a vanilla netherite smithing template, so it would show
 * netherite's "Applies to / Ingredients" tooltip lines. This suppresses that vanilla description for
 * our marked template; its own custom name + lore (rendered through ItemStack, not the item) stay.
 */
@Mixin(SmithingTemplateItem.class)
public class DragonTemplateTooltipMixin {

    @Inject(method = "appendHoverText", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$hideTemplateInfo(ItemStack stack, Item.TooltipContext context,
                                                TooltipDisplay display, Consumer<Component> adder,
                                                TooltipFlag flag, CallbackInfo ci) {
        if (DragonUpgradeTemplate.isTemplate(stack)) {
            ci.cancel();
        }
    }
}
