package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Feat;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Feats;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quests;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/** The Feats tab: a read-only checklist of one-time achievements (discoveries, bosses, the End).
 *  Feat titles/descriptions translate via {@code vanillaskills.feat.<id>} / {@code ...<id>.desc}. */
public class FeatsMenu extends ChestMenu {
    // 5-5-1 centered layout (rows of 5 sit dead-centre; the last feat is centered under them).
    private static final int[] FEAT_SLOTS = {
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            31};
    private static final int TITLE_SLOT = 4;
    private static final int BACK_SLOT = 36;
    private static final int CLOSE_SLOT = 44;

    private final ServerPlayer player;
    private final SimpleContainer container;

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new FeatsMenu(syncId, inv, (ServerPlayer) p),
                Component.literal(Lang.tr(player, "vanillaskills.menu.feats.title", "Feats"))));
    }

    private FeatsMenu(int syncId, Inventory inv, ServerPlayer player) {
        super(MenuType.GENERIC_9x5, syncId, inv, new SimpleContainer(45), 5);
        this.player = player;
        this.container = (SimpleContainer) getContainer();
        populate();
    }

    private void populate() {
        for (int i = 0; i < 45; i++) container.setItem(i, ItemStack.EMPTY);
        container.setItem(TITLE_SLOT, titleItem());
        List<Feat> all = Feats.all();
        for (int i = 0; i < all.size() && i < FEAT_SLOTS.length; i++) {
            container.setItem(FEAT_SLOTS[i], featItem(all.get(i)));
        }
        // Feats are datapack-driven now, so a pack can define more than this fixed layout holds.
        // They still award normally — they just have nowhere to render — so say so rather than
        // dropping them silently.
        if (all.size() > FEAT_SLOTS.length) {
            io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills.LOGGER.warn(
                    "{} feats are loaded but the Feats screen only has {} slots — the rest are earnable but not shown",
                    all.size(), FEAT_SLOTS.length);
        }
        container.setItem(BACK_SLOT, button(Items.ARROW,
                t("vanillaskills.menu.feats.back", "Back to Bounty Board"), ChatFormatting.YELLOW));
        container.setItem(CLOSE_SLOT, button(Items.BARRIER, t("vanillaskills.menu.close", "Close"), ChatFormatting.RED));
    }

    private String t(String key, String fallback, Object... args) {
        return Lang.tr(player, key, fallback, args);
    }

    private ItemStack titleItem() {
        int done = 0;
        for (Feat f : Feats.all()) if (Feats.isDone(player, f.id())) done++;
        ItemStack stack = new ItemStack(Items.WITHER_SKELETON_SKULL);
        stack.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.menu.feats.title", "Feats"), ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled(t("vanillaskills.menu.feats.info1", "One-time achievements — earned once, kept forever."), ChatFormatting.GRAY),
                styled(t("vanillaskills.menu.feats.info2", "They award Quest Shards automatically when you do them."), ChatFormatting.GRAY),
                Component.literal(""),
                styled(t("vanillaskills.menu.feats.progress", "Completed: %d/%d", done, Feats.all().size()), ChatFormatting.AQUA))));
        return stack;
    }

    private ItemStack featItem(Feat f) {
        boolean done = Feats.isDone(player, f.id());
        ItemStack stack = new ItemStack(Quests.item(f.icon()));
        stack.set(DataComponents.CUSTOM_NAME, styled(
                t("vanillaskills.feat." + f.id(), f.title()) + (done ? " ✔" : ""),
                done ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        List<Component> lore = new ArrayList<>();
        lore.add(styled(t("vanillaskills.feat." + f.id() + ".desc", f.desc()), ChatFormatting.GRAY));
        lore.add(Component.literal(""));
        lore.add(styled(t("vanillaskills.menu.quests.reward.many", "Reward: +%d Quest Shards", f.reward()), ChatFormatting.LIGHT_PURPLE));
        lore.add(Component.literal(""));
        lore.add(styled(done ? t("vanillaskills.menu.quests.completed", "Completed")
                        : t("vanillaskills.menu.feats.not_earned", "Not yet earned"),
                done ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (done) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private ItemStack button(net.minecraft.world.item.Item item, String name, ChatFormatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, color));
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (clicker instanceof ServerPlayer sp) {
            if (slotId == CLOSE_SLOT) { sp.closeContainer(); return; }
            if (slotId == BACK_SLOT) { QuestMenu.open(sp); return; }
        }
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static Component styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }
}
