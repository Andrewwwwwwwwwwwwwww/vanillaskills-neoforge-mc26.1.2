package io.github.andrewwwwwwwwwwwwwww.vanillaskills.client;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Mod Menu config screen: the gameplay/pacing settings from gameplay.json, as click-to-cycle buttons
 * (widget-only — 26.2 reworked GUI rendering, so no manual draw calls). Saves on close and applies the
 * values; in singleplayer that's live since the client and integrated server share the JVM. On a
 * multiplayer server the server's own gameplay.json is authoritative — this edits your local copy only.
 */
public class VanillaSkillsConfigScreen extends Screen {
    private static final int[] BOUNTY_HOURS = {1, 2, 3, 5, 8, 12, 24};
    private static final int[] SHOP_HOURS = {6, 12, 24, 48, 72};
    private static final int[] RATIOS = {1, 2, 3, 4, 5};
    private static final int[] GRADUATE = {5, 10, 15, 20, 25};

    private final Screen parent;
    private GameplayConfig cfg;
    private ClientConfig clientCfg;

    public VanillaSkillsConfigScreen(Screen parent) {
        super(Component.translatableWithFallback("vanillaskills.config.title", "VanillaSkills"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 280, x = this.width / 2 - w / 2, top = this.height / 4, gap = 24;
        if (clientCfg == null) clientCfg = ClientConfig.load();
        int row = 0;

        // Client-global setting — available everywhere (no world loaded / multiplayer included).
        addRenderableWidget(Button.builder(narratorLabel(), b -> {
            clientCfg.disableNarrator = !clientCfg.disableNarrator;
            b.setMessage(narratorLabel());
        }).bounds(x, top + gap * row++, w, 20).build());

        if (this.minecraft.getSingleplayerServer() == null) {
            // Gameplay settings are per-world; nothing to edit with no single-player world loaded.
            // (On a multiplayer server the server's own config is authoritative.)
            Button notice = Button.builder(
                    Component.translatableWithFallback("vanillaskills.config.singleplayer_only", "Open a single-player world to edit gameplay settings"), b -> {})
                    .bounds(x, top + gap * row++, w, 20).build();
            notice.active = false;
            addRenderableWidget(notice);
        } else {
            if (cfg == null) cfg = GameplayConfig.load();

            addRenderableWidget(Button.builder(mendingLabel(), b -> {
                cfg.mendingEnabled = !cfg.mendingEnabled;
                b.setMessage(mendingLabel());
            }).bounds(x, top + gap * row++, w, 20).build());

            addRenderableWidget(Button.builder(bountyLabel(), b -> {
                cfg.bountyRefreshHours = cycle(BOUNTY_HOURS, cfg.bountyRefreshHours);
                b.setMessage(bountyLabel());
            }).bounds(x, top + gap * row++, w, 20).build());

            addRenderableWidget(Button.builder(shopLabel(), b -> {
                cfg.shopRefreshHours = cycle(SHOP_HOURS, cfg.shopRefreshHours);
                b.setMessage(shopLabel());
            }).bounds(x, top + gap * row++, w, 20).build());

            addRenderableWidget(Button.builder(ratioLabel(), b -> {
                cfg.convertRatio = cycle(RATIOS, cfg.convertRatio);
                b.setMessage(ratioLabel());
            }).bounds(x, top + gap * row++, w, 20).build());

            addRenderableWidget(Button.builder(graduateLabel(), b -> {
                cfg.graduateAt = cycle(GRADUATE, cfg.graduateAt);
                b.setMessage(graduateLabel());
            }).bounds(x, top + gap * row++, w, 20).build());
        }

        addRenderableWidget(Button.builder(Component.translatableWithFallback("vanillaskills.config.done", "Done"), b -> onClose())
                .bounds(x, top + gap * row + 10, w, 20).build());
    }

    /** Advance to the next preset after {@code current}; if current isn't a preset, snap to the first. */
    private static int cycle(int[] presets, int current) {
        for (int i = 0; i < presets.length; i++) {
            if (presets[i] == current) return presets[(i + 1) % presets.length];
        }
        return presets[0];
    }

    // This screen is client-side, so it uses ordinary translation keys resolved from the client's
    // own language files, with English fallbacks for anyone missing them.
    private static Component tr(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(key, fallback, args);
    }

    private Component mendingLabel() {
        return tr("vanillaskills.config.mending", "Mending: %s",
                tr(cfg.mendingEnabled ? "vanillaskills.config.mending.on" : "vanillaskills.config.mending.off",
                        cfg.mendingEnabled ? "Enabled" : "Removed (default)"));
    }
    private Component bountyLabel() { return tr("vanillaskills.config.bounty_refresh", "Bounty board refresh: %sh", cfg.bountyRefreshHours); }
    private Component shopLabel() { return tr("vanillaskills.config.shop_refresh", "Quest Shop refresh: %sh", cfg.shopRefreshHours); }
    private Component ratioLabel() { return tr("vanillaskills.config.convert", "Convert: %s Quest → 1 Skill Shard", cfg.convertRatio); }
    private Component graduateLabel() { return tr("vanillaskills.config.graduate", "Graduate after: %s quests", cfg.graduateAt); }
    private Component narratorLabel() {
        return tr("vanillaskills.config.narrator", "Narrator: %s",
                tr(clientCfg.disableNarrator ? "vanillaskills.config.narrator.off" : "vanillaskills.config.narrator.on",
                        clientCfg.disableNarrator ? "Disabled — smoother GUI opening" : "Default (vanilla)"));
    }

    @Override
    public void onClose() {
        if (clientCfg != null) clientCfg.save();
        if (cfg != null) {
            cfg.save();
            GameplayConfig.load(); // re-read + apply all live values
        }
        this.minecraft.setScreenAndShow(parent);
    }
}
