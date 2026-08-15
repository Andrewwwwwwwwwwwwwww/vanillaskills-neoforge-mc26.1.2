package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code ItemCombinerMenu.player} (a protected field on the superclass) so subclass mixins
 * like {@code DragonSmithingMixin} can read who's using the smithing table. The field can't be
 * {@code @Shadow}ed from a SmithingMenu mixin because Shadow only resolves fields declared on the
 * target class, not inherited ones.
 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {

    @Accessor("player")
    Player vanillaskills$getPlayer();
}
