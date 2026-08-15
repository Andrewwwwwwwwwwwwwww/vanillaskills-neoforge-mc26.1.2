package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

/**
 * Exposes {@code Entity.DATA_CUSTOM_NAME}, which is private.
 *
 * <p>Needed to send a player a custom name for an entity <b>without changing the entity</b>. The horse
 * screen takes its caption from the horse's name and 26.2 gives the server no title to set
 * ({@code ClientboundMountScreenOpenPacket} carries none), so the only way to put stats in that GUI is to
 * tell one client the horse is called something else for as long as the screen is open.
 *
 * <p>An {@code @Accessor} on the static field is the whole requirement — the value is then packed into a
 * {@code SynchedEntityData.DataValue} and sent as an ordinary entity-data update, so the client handles it
 * through its normal path and nothing about the entity on the server is touched.
 */
@Mixin(Entity.class)
public interface EntityCustomNameAccessor {

    @Accessor("DATA_CUSTOM_NAME")
    static EntityDataAccessor<Optional<Component>> vanillaskills$customNameAccessor() {
        throw new AssertionError("mixin did not apply");
    }
}
