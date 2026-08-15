package io.github.andrewwwwwwwwwwwwwww.vanillaskills.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Public extension point for add-on mods: register an extra button on the skill-tree home screen.
 *
 * <p>Add-ons call {@link #register} once during mod init. Every time a player opens the skill tree,
 * each registered entry is asked for an icon ({@code icon}) and gets a slot on the home screen;
 * clicking it runs {@code onClick} with the clicking player. This keeps add-ons out of
 * {@code SkillTreeMenu}'s internals, so its layout can keep changing without breaking them.
 *
 * <p>Slot placement is best-effort and collision-safe: an entry is placed at its
 * {@code preferredSlot} only if that slot is still free once the tree's lanes have been laid out
 * , otherwise the next free slot is used. An
 * entry that cannot be placed at all is silently skipped rather than overwriting anything.
 *
 * <p>Buttons appear on the home screen only — not inside a lane view, and never in edit or layout
 * mode, where the screen belongs to the operator editing the tree.
 *
 * <p>Example:
 * <pre>{@code
 * SkillMenuExtensions.register("vsc_casino", 44,
 *         player -> myCasinoIcon(player),
 *         player -> CasinoMenu.open(player));
 * }</pre>
 *
 * <p>Thread safety: registration is synchronized and expected during init; reads happen on the
 * server thread when a menu is built.
 */
public final class SkillMenuExtensions {
    private SkillMenuExtensions() {}

    /**
     * One registered add-on button.
     *
     * @param id            unique id, namespaced by the owning mod (e.g. {@code "vsc_casino"})
     * @param preferredSlot the container slot to use when free; 44 sits directly above the Stats head
     * @param icon          builds the button's {@link ItemStack} for a given player (called per open,
     *                      so it can show live state such as a balance); returning an empty stack
     *                      hides the button for that player
     * @param onClick       runs when the button is clicked; typically opens another menu
     */
    public record Entry(String id, int preferredSlot,
                        Function<ServerPlayer, ItemStack> icon,
                        Consumer<ServerPlayer> onClick) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    /**
     * Register (or replace) an add-on button. Call once during mod init.
     *
     * @throws IllegalArgumentException if {@code id} is blank or any argument is null
     */
    public static synchronized void register(String id, int preferredSlot,
                                             Function<ServerPlayer, ItemStack> icon,
                                             Consumer<ServerPlayer> onClick) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (icon == null || onClick == null) throw new IllegalArgumentException("icon/onClick must not be null");
        ENTRIES.put(id, new Entry(id, preferredSlot, icon, onClick));
    }

    /** Remove a previously registered button. Returns true if one was removed. */
    public static synchronized boolean unregister(String id) {
        return ENTRIES.remove(id) != null;
    }

    /** All registered entries, in registration order. Never null. */
    public static synchronized Collection<Entry> all() {
        return List.copyOf(ENTRIES.values());
    }

    /** True when no add-on has registered a button (the common case — lets callers skip work). */
    public static synchronized boolean isEmpty() {
        return ENTRIES.isEmpty();
    }
}
