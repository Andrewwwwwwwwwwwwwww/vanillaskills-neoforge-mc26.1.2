package io.github.andrewwwwwwwwwwwwwww.vanillaskills.infuse;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui.Guis;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Infusing Table screen: pick any number of the enchantments your shelved books offer, then pay once.
 *
 * <p><b>It enchants the item in your main hand and has no item slots at all.</b> That is deliberate — a menu
 * holding a player's gear across an open/close cycle is exactly the shape that produced three separate
 * duplication bugs in Villager Shop, and none of that risk is worth the convenience of a slot.
 *
 * <p>Selections are toggled and shown live with a running total; nothing is charged and nothing is applied
 * until Confirm. If the held item changes underneath the screen, the menu notices and reopens rather than
 * enchanting the wrong thing.
 */
public class InfusingMenu extends ChestMenu {
    private static final int[] ENCHANT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34};
    private static final int ITEM_SLOT = 4;
    private static final int CONFIRM_SLOT = 49;
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;
    private static final int CLOSE_SLOT = 53;

    private final ServerPlayer player;
    private final SimpleContainer container;
    private final BlockPos tablePos;
    /** Available enchantments in a stable display order, resolved when the screen opened. */
    private final List<Map.Entry<Holder<Enchantment>, Integer>> offered;
    private final Set<Holder<Enchantment>> selected = new LinkedHashSet<>();
    /** The item this screen was opened against — used to notice a swap mid-session. */
    private final ItemStack boundItem;
    /** Which page of {@link #ENCHANT_SLOTS} is on screen. Selections are held per enchantment rather
     *  than per page, so paging never disturbs the basket or what it costs. */
    private int page;

    public static void open(ServerPlayer player, BlockPos tablePos) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Map<Holder<Enchantment>, Integer> available = InfusingTable.availableAt(level, tablePos);
        if (available.isEmpty()) {
            player.sendSystemMessage(Component.literal(Lang.tr(player, "vanillaskills.msg.infuse_no_books",
                    "No enchanted books shelved nearby. Put them in chiseled bookshelves around the table."))
                    .withStyle(ChatFormatting.RED));
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new InfusingMenu(syncId, inv, (ServerPlayer) p, tablePos, available),
                Component.literal(Lang.tr(player, "vanillaskills.menu.infuse.title", "Infusing Table"))));
    }

    private InfusingMenu(int syncId, Inventory inv, ServerPlayer player, BlockPos tablePos,
                         Map<Holder<Enchantment>, Integer> available) {
        super(MenuType.GENERIC_9x6, syncId, inv, new SimpleContainer(54), 6);
        this.player = player;
        this.container = (SimpleContainer) getContainer();
        this.tablePos = tablePos;
        this.offered = new ArrayList<>(available.entrySet());
        this.boundItem = player.getMainHandItem();
        populate();
    }

    /**
     * Lay the shelves' offer out for the item in hand: everything that fits first, in the order the
     * shelves gave them, then everything that does not.
     *
     * <p>Done ONCE, against the item the screen is bound to, rather than on every redraw. Re-sorting as
     * selections change would make icons jump out from under the cursor, and the screen already reopens
     * when the held item changes — so the order always matches what is being infused.
     *
     * <p>It also decides what lands on page one. Overflow now pages rather than being dropped, but a
     * well-stocked library can still run to several pages, and the enchantments that actually fit the
     * item should never be the ones you have to go looking for.
     */
    private static List<Map.Entry<Holder<Enchantment>, Integer>> order(
            Map<Holder<Enchantment>, Integer> available, ItemStack held) {
        Set<Holder<Enchantment>> none = Set.of();
        List<Map.Entry<Holder<Enchantment>, Integer>> all = new ArrayList<>(available.entrySet());
        if (GameplayConfig.INFUSING_HIDE_INCOMPATIBLE) {
            all.removeIf(e -> !InfusingTable.canApply(held, e.getKey(), e.getValue(), none));
            return all;
        }
        // List.sort is stable, so entries that fit equally well keep the order the shelves offered them in.
        all.sort(java.util.Comparator.comparingInt(
                e -> InfusingTable.canApply(held, e.getKey(), e.getValue(), none) ? 0 : 1));
        return all;
    }

    private String t(String key, String fallback, Object... args) {
        return Lang.tr(player, key, fallback, args);
    }

    private void populate() {
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, ItemStack.EMPTY);
        container.setItem(ITEM_SLOT, targetDisplay());
        int start = page * ENCHANT_SLOTS.length;
        for (int i = 0; i < ENCHANT_SLOTS.length && start + i < offered.size(); i++) {
            container.setItem(ENCHANT_SLOTS[i], enchantIcon(offered.get(start + i)));
        }
        if (pageCount() > 1) {
            if (page > 0) container.setItem(PREV_SLOT, pageButton(false));
            if (page < pageCount() - 1) container.setItem(NEXT_SLOT, pageButton(true));
        }
        container.setItem(CONFIRM_SLOT, confirmButton());
        container.setItem(CLOSE_SLOT, button(Items.BARRIER, t("vanillaskills.menu.close", "Close"), ChatFormatting.RED));
    }

    /** A copy of the held item, so the player can see what they are about to change. */
    private ItemStack targetDisplay() {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            ItemStack empty = new ItemStack(Items.BARRIER);
            empty.set(DataComponents.CUSTOM_NAME,
                    styled(t("vanillaskills.menu.infuse.no_item", "Hold the item you want to infuse"), ChatFormatting.RED));
            return empty;
        }
        ItemStack shown = held.copy();
        shown.set(DataComponents.LORE, new ItemLore(List.of(
                styled(t("vanillaskills.menu.infuse.target", "The item being infused"), ChatFormatting.GRAY))));
        return shown;
    }

    private ItemStack enchantIcon(Map.Entry<Holder<Enchantment>, Integer> entry) {
        Holder<Enchantment> enchantment = entry.getKey();
        int level = entry.getValue();
        ItemStack held = player.getMainHandItem();
        boolean chosen = selected.contains(enchantment);
        // Greyed out if it cannot go on the item, is already there at this level or better, or clashes with
        // something else currently picked — so a conflict is visible before it costs anything.
        boolean applicable = chosen || InfusingTable.canApply(held, enchantment, level, selected);

        // The icon carries the verdict at a glance: an Enchanted Book is one this item can take, a plain
        // Book is one it cannot. Selection adds the glint on top, so the three states never blur together.
        ItemStack icon = new ItemStack(applicable ? Items.ENCHANTED_BOOK : Items.BOOK);
        Guis.hideStats(icon);
        icon.set(DataComponents.CUSTOM_NAME, Component.empty()
                .append(Enchantment.getFullname(enchantment, level))
                .withStyle(chosen ? ChatFormatting.GREEN : applicable ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY)
                .withStyle(s -> s.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        lore.add(styled(t("vanillaskills.menu.infuse.cost", "Cost: %d %s",
                InfusingTable.costOf(level), currencyName()), ChatFormatting.LIGHT_PURPLE));
        // Warn before the click, not after: this is the only case where a shelved book does not come back.
        if (InfusingTable.consumesBook(enchantment, level)) {
            lore.add(styled(t("vanillaskills.menu.infuse.burns_book", "Consumes the book"), ChatFormatting.GOLD));
        }
        lore.add(Component.literal(""));
        if (!applicable) {
            // Say WHICH kind of "no" it is — "can't go on this item" is misleading when the real reason is
            // that you already picked something it clashes with, or already have it.
            lore.add(styled(rejectionReason(held, enchantment, level), ChatFormatting.RED));
        } else if (chosen) {
            lore.add(styled(t("vanillaskills.menu.infuse.deselect", "Click to remove"), ChatFormatting.YELLOW));
        } else {
            lore.add(styled(t("vanillaskills.menu.infuse.select", "Click to add"), ChatFormatting.YELLOW));
        }
        icon.set(DataComponents.LORE, new ItemLore(lore));
        if (chosen) icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return icon;
    }

    /** Pages needed to show every offer, never fewer than one (an empty screen is still page 1 of 1). */
    private int pageCount() {
        return Math.max(1, (offered.size() + ENCHANT_SLOTS.length - 1) / ENCHANT_SLOTS.length);
    }

    private ItemStack pageButton(boolean forward) {
        ItemStack stack = new ItemStack(Items.ARROW);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, styled(forward
                ? t("vanillaskills.menu.infuse.next", "Next ▶")
                : t("vanillaskills.menu.infuse.prev", "◀ Previous"), ChatFormatting.YELLOW));
        stack.set(DataComponents.LORE, new ItemLore(List.of(styled(
                t("vanillaskills.menu.infuse.page", "Page %d / %d — %d enchantments",
                        page + 1, pageCount(), offered.size()), ChatFormatting.GRAY))));
        return stack;
    }

    private ItemStack confirmButton() {
        int total = totalCost();
        boolean affordable = balance() >= total;
        ItemStack stack = new ItemStack(Items.ANVIL);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME,
                styled(t("vanillaskills.menu.infuse.confirm", "Infuse"), ChatFormatting.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(styled(t("vanillaskills.menu.infuse.selected", "Selected: %d", selected.size()), ChatFormatting.GRAY));
        lore.add(styled(t("vanillaskills.menu.infuse.total", "Total: %d %s", total, currencyName()),
                affordable ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
        lore.add(styled(t("vanillaskills.menu.infuse.keeps_books", "Your books are not consumed."), ChatFormatting.DARK_GRAY));
        lore.add(Component.literal(""));
        if (selected.isEmpty()) {
            lore.add(styled(t("vanillaskills.menu.infuse.pick_something", "Choose at least one enchantment"), ChatFormatting.RED));
        } else if (!affordable) {
            lore.add(styled(t("vanillaskills.menu.infuse.cant_afford", "Not enough %s", currencyName()), ChatFormatting.RED));
        } else {
            lore.add(styled(t("vanillaskills.menu.infuse.go", "Click to infuse"), ChatFormatting.GREEN));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private int totalCost() {
        int total = 0;
        for (var entry : offered) {
            if (selected.contains(entry.getKey())) total += InfusingTable.costOf(entry.getValue());
        }
        return total;
    }

    private static boolean questCurrency() {
        return "quest".equals(GameplayConfig.INFUSING_CURRENCY);
    }

    private int balance() {
        return questCurrency()
                ? VanillaSkills.PLAYERS.questShards(player)
                : VanillaSkills.PLAYERS.skillShards(player);
    }

    private String currencyName() {
        return questCurrency()
                ? t("vanillaskills.menu.quest_shards", "Quest Shards")
                : t("vanillaskills.menu.skill_shards", "Skill Shards");
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (!(clicker instanceof ServerPlayer sp)) { sendAllDataToRemote(); return; }
        if (slotId == CLOSE_SLOT) { sp.closeContainer(); return; }

        // The screen is bound to whatever was held when it opened; if that changed, re-open against the
        // new item rather than silently enchanting something the player is no longer looking at.
        if (!ItemStack.matches(boundItem, sp.getMainHandItem())) {
            sp.sendSystemMessage(Component.literal(t("vanillaskills.menu.infuse.item_changed",
                    "Your held item changed — reopening.")).withStyle(ChatFormatting.YELLOW));
            open(sp, tablePos);
            return;
        }

        if (slotId == CONFIRM_SLOT) {
            confirm(sp);
            return;
        }
        if (slotId == PREV_SLOT && page > 0) {
            page--;
            populate();
            sendAllDataToRemote();
            return;
        }
        if (slotId == NEXT_SLOT && page < pageCount() - 1) {
            page++;
            populate();
            sendAllDataToRemote();
            return;
        }
        int start = page * ENCHANT_SLOTS.length;
        for (int i = 0; i < ENCHANT_SLOTS.length && start + i < offered.size(); i++) {
            if (slotId != ENCHANT_SLOTS[i]) continue;
            Holder<Enchantment> enchantment = offered.get(start + i).getKey();
            int level = offered.get(start + i).getValue();
            // Deselecting is always allowed. Selecting is checked against the item AND everything else
            // already picked, so a conflicting pair can never both end up in the basket.
            if (!selected.remove(enchantment)) {
                if (!InfusingTable.canApply(sp.getMainHandItem(), enchantment, level, selected)) return;
                selected.add(enchantment);
            }
            populate();
            sendAllDataToRemote();
            return;
        }
        sendAllDataToRemote();
    }

    private void confirm(ServerPlayer sp) {
        ItemStack held = sp.getMainHandItem();
        if (held.isEmpty() || selected.isEmpty()) return;

        // Work out exactly what will actually be applied BEFORE charging, and bill only for that. Selection
        // is already validated, but the held item can change between picking and confirming, and the old code
        // charged for the whole basket and then silently skipped whatever no longer fit — so a rejected
        // enchantment was still paid for.
        Set<Holder<Enchantment>> accepted = new LinkedHashSet<>();
        int total = 0;
        for (var entry : offered) {
            if (!selected.contains(entry.getKey())) continue;
            if (!InfusingTable.canApply(held, entry.getKey(), entry.getValue(), accepted)) continue;
            accepted.add(entry.getKey());
            total += InfusingTable.costOf(entry.getValue());
        }
        if (accepted.isEmpty()) {
            sp.sendSystemMessage(Component.literal(t("vanillaskills.menu.infuse.nothing_to_do",
                    "Nothing there would change this item.")).withStyle(ChatFormatting.RED));
            return;
        }

        // Charge first and bail if it fails, so a rejected payment can never apply a free enchantment.
        boolean paid = questCurrency()
                ? VanillaSkills.PLAYERS.spendQuestShards(sp, total)
                : VanillaSkills.PLAYERS.spendSkillShards(sp, total);
        if (!paid) {
            sp.sendSystemMessage(Component.literal(t("vanillaskills.menu.infuse.cant_afford",
                    "Not enough %s", currencyName())).withStyle(ChatFormatting.RED));
            return;
        }

        int applied = 0;
        int burned = 0;
        for (var entry : offered) {
            if (!accepted.contains(entry.getKey())) continue;
            InfusingTable.apply(held, entry.getKey(), entry.getValue());
            applied++;
            // Most books are permanent; the configured few are burned as they are used.
            if (InfusingTable.consumesBook(entry.getKey(), entry.getValue())
                    && sp.level() instanceof ServerLevel serverLevel
                    && InfusingTable.consumeBook(serverLevel, tablePos, entry.getKey(), entry.getValue())) {
                burned++;
            }
        }
        if (burned > 0) {
            sp.sendSystemMessage(Component.literal(t("vanillaskills.msg.infuse_book_burned",
                    "%d book(s) were consumed.", burned)).withStyle(ChatFormatting.GOLD));
        }

        // Keeps "Enchanter" earnable now that the vanilla enchanting screen is gone.
        io.github.andrewwwwwwwwwwwwwww.vanillaskills.infuse.InfusingTrigger.award(sp);

        sp.sendSystemMessage(Component.literal(t("vanillaskills.msg.infused",
                "Infused %d enchantment(s) for %d %s.", applied, total, currencyName()))
                .withStyle(ChatFormatting.GREEN));
        sp.closeContainer();
    }

    /** Why this enchantment is unavailable, in the order a player would ask. */
    private String rejectionReason(ItemStack held, Holder<Enchantment> enchantment, int level) {
        if (!held.isEmpty() && enchantment.value().canEnchant(held)) {
            var current = net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getEnchantmentsForCrafting(held);
            for (var entry : current.entrySet()) {
                if (entry.getKey().equals(enchantment) && entry.getIntValue() >= level) {
                    return t("vanillaskills.menu.infuse.already_have", "Already on this item");
                }
            }
            for (Holder<Enchantment> other : selected) {
                if (!other.equals(enchantment) && !Enchantment.areCompatible(other, enchantment)) {
                    return t("vanillaskills.menu.infuse.conflicts", "Conflicts with another choice");
                }
            }
        }
        return t("vanillaskills.menu.infuse.incompatible", "Can't go on this item");
    }

    private ItemStack button(net.minecraft.world.item.Item item, String name, ChatFormatting color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, color));
        return stack;
    }

    private static Component styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
