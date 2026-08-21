package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@code Display#setTransformation}, which is private in 26.2.
 *
 * <p>Without it a display entity can only ever be shown at its natural size and orientation: scale and
 * rotation both live in the transformation, and there is no public setter for either. The crate reel needs
 * both — items sized down so a ring of them reads as a wheel rather than a wall, and rotated to stand
 * upright facing the player.
 *
 * <p>An {@code @Invoker} is the least invasive way in. It generates a public bridge to the existing method
 * rather than reimplementing it, so the synced-data write and its change tracking stay exactly as vanilla
 * does them — which matters, because the transformation is interpolated client-side and a hand-rolled write
 * would be easy to get subtly wrong.
 */
@Mixin(Display.class)
public interface DisplayTransformAccessor {

    @Invoker("setTransformation")
    void vanillaskills$setTransformation(Transformation transformation);

    /**
     * Exposes {@code Display#setBrightnessOverride}, also private.
     *
     * <p>A display entity is lit by the light level at its own position. A block overlay sits <i>inside</i>
     * the solid block it is covering, where the light level is 0 — so without an override it renders almost
     * black no matter how bright the world around it is. That is what made the Stable block (on a
     * non-emitting diamond block base) look nearly unlit, and what made the Unstable block read as plain
     * crying obsidian: the overlay was drawn, just in the dark.
     */
    @Invoker("setBrightnessOverride")
    void vanillaskills$setBrightnessOverride(net.minecraft.util.Brightness brightness);
}
