package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * Puts a horse's real stats in the title of its inventory screen.
 *
 * <p>Horse quality is otherwise invisible: two identical-looking horses can differ enormously in speed and
 * jump, and vanilla gives you no way to tell without racing them.
 *
 * <p><b>How, given 26.2 offers no title to set.</b> The screen is opened with
 * {@code ClientboundMountScreenOpenPacket(containerId, columns, entityId)}, which carries <b>no title
 * field</b> — the client builds the caption from the horse itself. So instead of setting a title, this tells
 * the one player opening the screen that the horse is named "&lt;name&gt; [Speed … | Jump … | HP …]", by
 * sending them an ordinary entity-data update, and sends the real name back when they close it.
 *
 * <p>Three properties make this safe:
 * <ul>
 *   <li><b>Nothing on the server changes.</b> The horse's actual {@code CUSTOM_NAME} is never written, so no
 *       name tag is consumed or overwritten and the change cannot persist to disk.</li>
 *   <li><b>Only the viewer sees it.</b> The packet goes to one connection, so nobody else sees a renamed
 *       horse.</li>
 *   <li><b>It is self-correcting.</b> The restore is sent on close, and even if that were ever missed, the
 *       next real entity-data update from the server overwrites it.</li>
 * </ul>
 */
@Mixin(ServerPlayer.class)
public abstract class HorseStatsMixin {

    /** The horse this player is currently being shown a fake name for, or null. */
    @Unique
    private AbstractHorse vanillaskills$renamedHorse;

    @Inject(method = "openHorseInventory", at = @At("HEAD"))
    private void vanillaskills$showHorseStats(AbstractHorse horse, net.minecraft.world.Container container,
                                              CallbackInfo ci) {
        if (!GameplayConfig.HORSE_STATS) return;
        // Not every equine carries every attribute, and getAttributeValue throws on a missing one. A
        // cosmetic readout must never be able to break opening the screen or mounting.
        if (!horse.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)
                || !horse.getAttributes().hasAttribute(Attributes.JUMP_STRENGTH)) {
            return;
        }

        ServerPlayer self = (ServerPlayer) (Object) this;
        double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * 43.17;   // -> blocks/second
        double jump = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
        double jumpBlocks = -0.1817333 * jump * jump * jump + 3.689713 * jump * jump + 2.128956 * jump - 0.343930;

        Component labelled = Component.empty()
                .append(horse.getName())
                .append(Component.literal(String.format("  [%.2f b/s | %.2f jump | %.0f HP]",
                        speed, jumpBlocks, horse.getMaxHealth())).withStyle(ChatFormatting.AQUA));

        vanillaskills$sendName(self, horse, Optional.of(labelled));
        this.vanillaskills$renamedHorse = horse;
    }

    /** Put the real name back the moment the screen closes. */
    @Inject(method = "doCloseContainer", at = @At("HEAD"))
    private void vanillaskills$restoreHorseName(CallbackInfo ci) {
        AbstractHorse horse = this.vanillaskills$renamedHorse;
        if (horse == null) return;
        this.vanillaskills$renamedHorse = null;
        vanillaskills$sendName((ServerPlayer) (Object) this, horse,
                Optional.ofNullable(horse.getCustomName()));
    }

    /** Send one player a custom name for one entity, without touching the entity. */
    @Unique
    private static void vanillaskills$sendName(ServerPlayer player, AbstractHorse horse,
                                               Optional<Component> name) {
        if (player.connection == null) return;
        player.connection.send(new ClientboundSetEntityDataPacket(horse.getId(), List.of(
                SynchedEntityData.DataValue.create(
                        EntityCustomNameAccessor.vanillaskills$customNameAccessor(), name))));
    }
}
