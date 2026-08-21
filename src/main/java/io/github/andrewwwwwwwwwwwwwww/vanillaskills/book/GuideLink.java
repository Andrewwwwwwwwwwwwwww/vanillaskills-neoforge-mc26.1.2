package io.github.andrewwwwwwwwwwwwwww.vanillaskills.book;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;

/**
 * Sends the player a clickable link to the wiki instead of opening the written book.
 *
 * <p>The book is a good offline answer but a poor manual: every page has to fit fourteen lines, the text
 * lives in two places that drift apart, and a fix only reaches players who update the mod. The wiki has
 * none of those limits and is already the page kept current on release.
 *
 * <p>{@link GuideBook} stays as the fallback. Clearing {@code guideUrl} in gameplay.json puts the book back,
 * which matters for a server with no outbound internet, for a player whose client blocks chat links, and if
 * the page ever moves.
 */
public final class GuideLink {
    private GuideLink() {}

    /** True if a wiki link is configured and usable; false means fall back to the book. */
    public static boolean available() {
        String url = GameplayConfig.GUIDE_URL;
        if (url == null || url.isBlank()) return false;
        // The client only honours http/https, and refuses anything else outright.
        return url.startsWith("https://") || url.startsWith("http://");
    }

    /**
     * Message the player with the wiki link.
     *
     * <p>The menu is closed first: a chat line sent while a container is open is hidden behind it, and the
     * link cannot be clicked until the screen is gone.
     */
    public static void open(ServerPlayer player) {
        String url = GameplayConfig.GUIDE_URL.trim();
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            // A malformed URL in the config must not cost the player their guide.
            GuideBook.open(player);
            return;
        }

        player.closeContainer();

        player.sendSystemMessage(Component.literal(
                        Lang.tr(player, "vanillaskills.guide.link_intro", "The VanillaSkills guide lives here:"))
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal(
                        Lang.tr(player, "vanillaskills.guide.link_label", "Open the wiki"))
                .withStyle(s -> s
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(uri))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(url)))));
    }
}
