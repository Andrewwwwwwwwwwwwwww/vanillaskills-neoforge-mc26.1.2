package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides our marked items from vanilla's recipe-matching accounting.
 *
 * <h2>The bug this fixes</h2>
 * {@link IngredientMixin} makes a marked item fail every vanilla {@code Ingredient.test}, which is what
 * stops an Unstable Skill Shard being crafted into an amethyst block or a Steel Ingot into an iron block.
 * But vanilla decides <b>what to autofill</b> and <b>which recipes to light up</b> through
 * {@code StackedItemContents}, which is keyed on {@code Holder<Item>} — item identity alone. It never
 * consults {@code Ingredient.test}.
 *
 * <p>So the two disagreed. The recipe book counted four Unstable Skill Shards as four amethyst shards, lit
 * the amethyst block recipe up as craftable, moved all four into the grid on click — and then the recipe
 * refused them, leaving the player staring at a full grid and an empty result with no way to craft. The
 * same trap applied to every marked item sharing a type with a vanilla ingredient.
 *
 * <p>Skipping marked stacks here fixes both halves at once: the recipe is no longer advertised as craftable,
 * and nothing tries to move those items into the grid. Our own recipes are unaffected — they match by marker
 * rather than by {@code Ingredient}, and their autofill goes through {@code ComponentAutofill}.
 */
@Mixin(StackedItemContents.class)
public class StackedContentsMarkerMixin {

    @Inject(method = "accountStack(Lnet/minecraft/world/item/ItemStack;I)V", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$ignoreMarkedStacks(ItemStack stack, int maxCount, CallbackInfo ci) {
        if (Markers.isOurs(stack)) ci.cancel();
    }

    @Inject(method = "accountSimpleStack(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void vanillaskills$ignoreMarkedSimpleStacks(ItemStack stack, CallbackInfo ci) {
        if (Markers.isOurs(stack)) ci.cancel();
    }
}
