package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin.client;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.client.ClientConfig;
import net.minecraft.client.GameNarrator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opt-in narrator disable (off by default; toggled in the VanillaSkills client config / Mod Menu screen).
 *
 * <p>On Windows, the narrator backend's native text-to-speech COM call ({@code NarratorWindows.clear()})
 * runs on the render thread and can stall it for tens of milliseconds <em>every time a screen opens</em>
 * (chest, crafting, furnace, even the options menu) — and the vanilla "Off" narrator setting only silences
 * speech, it doesn't stop that call. When the player opts in, we cancel every entry into the narrator so
 * the backend is never touched. Default off, so accessibility users are never affected.
 */
@Mixin(GameNarrator.class)
public class GameNarratorMixin {

    @Inject(
            method = {
                    "saySystemNow(Lnet/minecraft/network/chat/Component;)V",
                    "saySystemNow(Ljava/lang/String;)V",
                    "sayChatQueued",
                    "saySystemChatQueued",
                    "saySystemQueued",
                    "clear"
            },
            at = @At("HEAD"),
            cancellable = true)
    private void vanillaskills$skipNarrator(CallbackInfo ci) {
        if (ClientConfig.DISABLE_NARRATOR) {
            ci.cancel();
        }
    }
}
