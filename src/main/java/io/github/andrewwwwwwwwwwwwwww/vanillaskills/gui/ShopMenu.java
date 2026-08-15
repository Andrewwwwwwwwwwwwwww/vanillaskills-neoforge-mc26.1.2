package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestShop;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestShop.ShopOffer;
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

/** The Quest Shop: a daily-rotating catalog plus a permanent Quest→Skill Shard converter. */
public class ShopMenu extends ChestMenu {
    private static final int[] ITEM_SLOTS = {10, 12, 14, 16, 19, 21, 23, 25};
    private static final int INFO_SLOT = 4;
    private static final int BACK_SLOT = 27;
    private static final int CONVERT_SLOT = 31;
    private static final int CLOSE_SLOT = 35;

    private final ServerPlayer player;
    private final SimpleContainer container;
    private List<ShopOffer> offers;

    public static void open(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new ShopMenu(syncId, inv, (ServerPlayer) p),
                Component.literal(Lang.tr(player, "vanillaskills.menu.shop.title", "Quest Shop"))));
    }

    private String t(String key, String fallback, Object... args) {
        return Lang.tr(player, key, fallback, args);
    }

    /** Stable key for a custom set offer's label, e.g. "Iron Armor Set" -> "vanillaskills.shop.iron_armor_set". */
    private static String offerKey(String title) {
        return "vanillaskills.shop." + title.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /** The offer's display name, fully translatable client-side. Custom sets use their label key;
     *  plain item offers use the item's OWN (translatable) name plus the stack count, so vanilla item
     *  names show in each player's language rather than a server-resolved English string. */
    private Component offerName(ShopOffer offer) {
        Component c;
        if (offer.label() != null) {
            c = Component.translatableWithFallback(offerKey(offer.label()), offer.label());
        } else {
            var g = offer.icon();
            Component item = new ItemStack(Quests.item(g.itemId())).getHoverName();
            c = g.count() > 1
                    // %s for BOTH, not %d: Minecraft translation components only understand %s and %1$s.
                    // A %d is passed through verbatim, which is why this rendered as the literal "%dx %s".
                    ? Component.translatableWithFallback("vanillaskills.shop.stack", "%s x %s", g.count(), item)
                    : item.copy();
        }
        return c.copy().withStyle(ChatFormatting.WHITE).withStyle(s -> s.withItalic(false));
    }

    private ShopMenu(int syncId, Inventory inv, ServerPlayer player) {
        super(MenuType.GENERIC_9x4, syncId, inv, new SimpleContainer(36), 4);
        this.player = player;
        this.container = (SimpleContainer) getContainer();
        this.offers = QuestShop.dailyOffers();
        populate();
    }

    private void populate() {
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, ItemStack.EMPTY);

        container.setItem(INFO_SLOT, infoItem());
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            ItemStack stack = i < offers.size() ? offerItem(offers.get(i)) : ItemStack.EMPTY;
            container.setItem(ITEM_SLOTS[i], stack);
        }
        container.setItem(CONVERT_SLOT, convertItem());
        container.setItem(BACK_SLOT, button(Items.ARROW, t("vanillaskills.menu.shop.back", "Back to Bounties"),
                ChatFormatting.YELLOW, t("vanillaskills.menu.shop.back.desc", "Return to the quest board.")));
        container.setItem(CLOSE_SLOT, button(Items.BARRIER, t("vanillaskills.menu.close", "Close"), ChatFormatting.RED, null));
    }

    private ItemStack infoItem() {
        int skill = VanillaSkills.PLAYERS.skillShards(player);
        int quest = VanillaSkills.PLAYERS.questShards(player);
        long ms = QuestShop.msUntilRotation();
        String when = (ms / 3_600_000) + "h " + (ms % 3_600_000 / 60_000) + "m";
        ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
        stack.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.menu.shop.your_shards", "Your Shards"), ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled(t("vanillaskills.menu.shop.quest_shards", "Quest Shards: %d", quest), ChatFormatting.LIGHT_PURPLE),
                styled(t("vanillaskills.menu.shop.skill_shards", "Skill Shards: %d", skill), ChatFormatting.AQUA),
                Component.literal(""),
                styled(t("vanillaskills.menu.shop.new_stock", "New shop stock in %s", when), ChatFormatting.GRAY))));
        return stack;
    }

    private ItemStack offerItem(ShopOffer offer) {
        int quest = VanillaSkills.PLAYERS.questShards(player);
        int skill = VanillaSkills.PLAYERS.skillShards(player);
        boolean canQuest = quest >= offer.price();
        boolean canSkill = skill >= offer.skillPrice();

        ItemStack stack = new ItemStack(Quests.item(offer.icon().itemId()), offer.icon().count());
        stack.set(DataComponents.CUSTOM_NAME, offerName(offer));

        List<Component> lore = new ArrayList<>();
        if (offer.grants().size() > 1) {
            lore.add(styled(t("vanillaskills.menu.shop.full_set", "Includes the full set"), ChatFormatting.GRAY));
            lore.add(Component.literal(""));
        }
        lore.add(styled(t("vanillaskills.menu.shop.left_click", "Left-click: %d Quest Shards", offer.price()),
                canQuest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY));
        lore.add(styled(t("vanillaskills.menu.shop.right_click", "Right-click: %d Skill Shards", offer.skillPrice()),
                canSkill ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        if (!canQuest && !canSkill) {
            lore.add(Component.literal(""));
            lore.add(styled(t("vanillaskills.menu.shop.cant_afford", "You can't afford this yet"), ChatFormatting.RED));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private ItemStack convertItem() {
        int quest = VanillaSkills.PLAYERS.questShards(player);
        boolean can = quest >= QuestShop.CONVERT_RATIO;
        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        stack.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.menu.shop.convert", "Convert Shards"), ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled(t("vanillaskills.menu.shop.convert.rate", "%d Quest Shards → 1 Skill Shard", QuestShop.CONVERT_RATIO), ChatFormatting.GRAY),
                Component.literal(""),
                styled(can ? t("vanillaskills.menu.shop.convert.do", "Click to convert")
                                : t("vanillaskills.menu.shop.convert.need", "Need %d Quest Shards", QuestShop.CONVERT_RATIO),
                        can ? ChatFormatting.GREEN : ChatFormatting.RED))));
        if (can) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private ItemStack button(net.minecraft.world.item.Item item, String name, ChatFormatting color, String desc) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, color));
        if (desc != null) stack.set(DataComponents.LORE, new ItemLore(List.of(styled(desc, ChatFormatting.GRAY))));
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (!(clicker instanceof ServerPlayer sp)) { sendAllDataToRemote(); return; }
        if (slotId == CLOSE_SLOT) { sp.closeContainer(); return; }
        if (slotId == BACK_SLOT) { QuestMenu.open(sp); return; }
        if (slotId == CONVERT_SLOT) {
            QuestShop.convertOne(sp);
            refresh();
            return;
        }
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (slotId == ITEM_SLOTS[i] && i < offers.size()) {
                boolean paySkill = button != 0; // left-click = Quest Shards, right-click = Skill Shards
                QuestShop.purchase(sp, offers.get(i), paySkill);
                refresh();
                return;
            }
        }
        sendAllDataToRemote();
    }

    private void refresh() {
        populate();
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
