package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

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

/**
 * The recipe index: every custom recipe as its finished item, in one clickable list.
 *
 * <p>The book used to be page-only — 30-odd recipes reachable only by clicking "Next" until the one you
 * wanted appeared. This is the contents page: pick the item you want to make and go straight to its grid,
 * then come back here, or back to the skill tree, from the same row of buttons.
 *
 * <p>Entries are the recipe RESULT so the list reads as "things I can make". Ordering follows
 * {@link RecipeBook#all()}, which is already arranged by progression (materials, then tiers, then
 * end-game), so the index doubles as a rough crafting order.
 */
public class RecipeIndexMenu extends ChestMenu {
    /** Entry slots: the first five rows. The sixth is the control bar. */
    private static final int PER_PAGE = 45;
    private static final int SKILLS_SLOT = 45;
    private static final int PREV_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int CLOSE_SLOT = 53;

    private final ServerPlayer player;
    private final SimpleContainer container;
    private final List<RecipeBook.Display> recipes;
    private final int page;
    private final int pages;

    public static void open(ServerPlayer player) {
        open(player, 0);
    }

    public static void open(ServerPlayer player, int page) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new RecipeIndexMenu(syncId, inv, (ServerPlayer) p, page),
                Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(
                        player, "vanillaskills.menu.recipe.index.title", "Recipes"))));
    }

    /** The index page a given recipe sits on, so "back" from a recipe returns where you came from. */
    public static int pageOf(int recipeIndex) {
        return Math.max(0, recipeIndex / PER_PAGE);
    }

    private RecipeIndexMenu(int syncId, Inventory inv, ServerPlayer player, int page) {
        super(MenuType.GENERIC_9x6, syncId, inv, new SimpleContainer(54), 6);
        this.player = player;
        this.container = (SimpleContainer) getContainer();
        this.recipes = RecipeBook.all();
        this.pages = Math.max(1, (recipes.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(page, pages - 1));
        populate();
    }

    private String t(String key, String fallback, Object... args) {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, key, fallback, args);
    }

    private void populate() {
        for (int i = 0; i < 54; i++) container.setItem(i, ItemStack.EMPTY);

        int start = page * PER_PAGE;
        int end = Math.min(recipes.size(), start + PER_PAGE);
        for (int i = start; i < end; i++) {
            container.setItem(i - start, entry(recipes.get(i)));
        }

        container.setItem(SKILLS_SLOT, button(Items.ARROW,
                t("vanillaskills.menu.recipe.skills", "Back to Skills"), ChatFormatting.YELLOW, null));
        container.setItem(INFO_SLOT, button(Items.PAPER,
                t("vanillaskills.menu.recipe.index.page", "Page %d / %d — %d recipes", page + 1, pages, recipes.size()),
                ChatFormatting.GRAY, t("vanillaskills.menu.recipe.index.hint", "Click an item to see how it is made.")));
        if (page > 0) {
            container.setItem(PREV_SLOT, button(Items.ARROW,
                    t("vanillaskills.menu.recipe.prev", "◀ Previous"), ChatFormatting.YELLOW, null));
        }
        if (page < pages - 1) {
            container.setItem(NEXT_SLOT, button(Items.ARROW,
                    t("vanillaskills.menu.recipe.next", "Next ▶"), ChatFormatting.YELLOW, null));
        }
        container.setItem(CLOSE_SLOT, button(Items.BARRIER,
                t("vanillaskills.menu.close", "Close"), ChatFormatting.RED, null));
    }

    /** One list entry: the finished item, titled by its recipe, with its description plus a click hint. */
    private ItemStack entry(RecipeBook.Display rec) {
        ItemStack stack = rec.result().copy();
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(t(RecipeBookMenu.recipeKey(rec.title()), rec.title()))
                        .withStyle(ChatFormatting.GOLD).withStyle(s -> s.withItalic(false)));

        List<Component> lore = new ArrayList<>();
        if (rec.desc() != null && rec.desc().length > 0) {
            String d = t(RecipeBookMenu.recipeKey(rec.title()) + ".desc", String.join("\n", rec.desc()));
            for (String line : d.split("\n")) {
                lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY).withStyle(s -> s.withItalic(false)));
            }
            lore.add(Component.empty());
        }
        lore.add(Component.literal(t("vanillaskills.menu.recipe.index.click", "Click to view the recipe"))
                .withStyle(ChatFormatting.YELLOW).withStyle(s -> s.withItalic(false)));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private ItemStack button(net.minecraft.world.item.Item item, String name, ChatFormatting color, String desc) {
        ItemStack stack = new ItemStack(item);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(color).withStyle(s -> s.withItalic(false)));
        if (desc != null) {
            stack.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal(desc).withStyle(ChatFormatting.GRAY).withStyle(s -> s.withItalic(false)))));
        }
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (!(clicker instanceof ServerPlayer sp)) { sendAllDataToRemote(); return; }
        switch (slotId) {
            case CLOSE_SLOT -> { sp.closeContainer(); return; }
            case SKILLS_SLOT -> { SkillTreeMenu.open(sp); return; }
            case PREV_SLOT -> { if (page > 0) open(sp, page - 1); return; }
            case NEXT_SLOT -> { if (page < pages - 1) open(sp, page + 1); return; }
            default -> {
                if (slotId >= 0 && slotId < PER_PAGE) {
                    int index = page * PER_PAGE + slotId;
                    if (index < recipes.size()) { RecipeBookMenu.open(sp, index); return; }
                }
                sendAllDataToRemote();
            }
        }
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
