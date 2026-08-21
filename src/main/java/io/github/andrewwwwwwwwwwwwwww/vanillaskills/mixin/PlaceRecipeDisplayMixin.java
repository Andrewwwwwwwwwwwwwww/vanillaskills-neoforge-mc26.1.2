package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.ComponentAutofill;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records <b>which</b> recipe-book entry the player clicked, so the autofill can lay out that one.
 *
 * <p>{@code handlePlaceRecipe} resolves the clicked {@code RecipeDisplayId} to a display and then throws it
 * away, passing only the parent recipe down to {@code handlePlacement}. For vanilla that loses nothing — a
 * placeable recipe has one display. Ours publish several, so without this the autofill has to guess, and it
 * would guess wrong for anything whose cheaper variant the player can also afford: clicking the Stable Skill
 * Shard Block would quietly lay out the Unstable one.
 *
 * <p>The value is consumed once, by {@link ComponentAutofill#fill}, on the same tick and the same thread.
 * It is cleared on the way in as well, so a click that never reaches placement cannot leave a stale display
 * behind for the next one.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class PlaceRecipeDisplayMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"))
    private void vanillaskills$rememberClickedDisplay(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        MinecraftServer server = this.player.level().getServer();
        // HEAD is ahead of ensureRunningOnSameThread, so the first pass can be on the netty thread; that
        // pass is discarded and the packet re-runs on the main thread, which is the one we want to act on.
        if (server == null || !server.isSameThread()) return;

        ComponentAutofill.forgetClickedDisplay();
        RecipeManager.ServerDisplayInfo info = server.getRecipeManager().getRecipeFromDisplay(packet.recipe());
        if (info == null) return;
        if (!ComponentAutofill.handles(info.parent())) return;
        ComponentAutofill.rememberClickedDisplay(info.display().display());
    }
}
