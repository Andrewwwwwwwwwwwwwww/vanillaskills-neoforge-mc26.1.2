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

import java.util.List;

/** A paginated book of the mod's custom crafting recipes — one 3x3 recipe per page. */
public class RecipeBookMenu extends ChestMenu {
    private static final int[] GRID_SLOTS = {11, 12, 13, 20, 21, 22, 29, 30, 31};
    private static final int ARROW_SLOT = 23;
    private static final int RESULT_SLOT = 24;
    private static final int TITLE_SLOT = 4;
    private static final int BACK_SLOT = 45;
    private static final int PREV_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int CLOSE_SLOT = 53;

    private final ServerPlayer player;
    private final SimpleContainer container;
    private final List<RecipeBook.Display> recipes;
    private int page;

    public static void open(ServerPlayer player, int page) {
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new RecipeBookMenu(syncId, inv, (ServerPlayer) p, page),
                Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.menu.recipe.title","Recipes"))));
    }

    private RecipeBookMenu(int syncId, Inventory inv, ServerPlayer player, int page) {
        super(MenuType.GENERIC_9x6, syncId, inv, new SimpleContainer(54), 6);
        this.player = player;
        this.container = (SimpleContainer) getContainer();
        this.recipes = RecipeBook.all();
        this.page = Math.max(0, Math.min(page, recipes.size() - 1));
        populate();
    }

    private String t(String key, String fallback, Object... args) {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, key, fallback, args);
    }

    /** Stable recipe key: "Rose Gold Ingot (x4)" -> vanillaskills.recipe.rose_gold_ingot_x4 */
    private static String recipeKey(String title) {
        return "vanillaskills.recipe." + title.toLowerCase().replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");
    }

    private void populate() {
        for (int i = 0; i < 54; i++) container.setItem(i, ItemStack.EMPTY);

        RecipeBook.Display rec = recipes.get(page);
        container.setItem(TITLE_SLOT, titleItem(rec));

        for (int i = 0; i < 9; i++) {
            ItemStack g = rec.grid()[i];
            if (g != null && !g.isEmpty()) container.setItem(GRID_SLOTS[i], g.copy());
        }
        container.setItem(ARROW_SLOT, button(Items.SPECTRAL_ARROW, "→", ChatFormatting.WHITE, null));
        container.setItem(RESULT_SLOT, rec.result().copy());

        container.setItem(BACK_SLOT, button(Items.ARROW, t("vanillaskills.menu.recipe.back","Back to Skills"), ChatFormatting.YELLOW, null));
        container.setItem(INFO_SLOT, button(Items.PAPER, t("vanillaskills.menu.recipe.page","Recipe %d / %d", page + 1, recipes.size()),
                ChatFormatting.GRAY, null));
        if (page > 0) container.setItem(PREV_SLOT, button(Items.ARROW, t("vanillaskills.menu.recipe.prev","◀ Previous"), ChatFormatting.YELLOW, null));
        if (page < recipes.size() - 1) container.setItem(NEXT_SLOT, button(Items.ARROW, t("vanillaskills.menu.recipe.next","Next ▶"), ChatFormatting.YELLOW, null));
        container.setItem(CLOSE_SLOT, button(Items.BARRIER, t("vanillaskills.menu.close","Close"), ChatFormatting.RED, null));
    }

    /** The station icon at the top, titled, with the recipe's "what it's for" description as lore. */
    private ItemStack titleItem(RecipeBook.Display rec) {
        ItemStack stack = new ItemStack(rec.station());
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(t(recipeKey(rec.title()), rec.title()))
                .withStyle(ChatFormatting.GOLD).withStyle(s -> s.withItalic(false)));
        if (rec.desc() != null && rec.desc().length > 0) {
            java.util.List<Component> lore = new java.util.ArrayList<>();
            String d = t(recipeKey(rec.title()) + ".desc", String.join("\n", rec.desc()));
            for (String line : d.split("\n")) {
                lore.add(Component.literal(line).withStyle(ChatFormatting.GRAY).withStyle(s -> s.withItalic(false)));
            }
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }

    private ItemStack button(net.minecraft.world.item.Item item, String name, ChatFormatting color, String desc) {
        ItemStack stack = new ItemStack(item);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(color).withStyle(s -> s.withItalic(false)));
        if (desc != null) stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal(desc).withStyle(ChatFormatting.GRAY).withStyle(s -> s.withItalic(false)))));
        return stack;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (!(clicker instanceof ServerPlayer sp)) { sendAllDataToRemote(); return; }
        switch (slotId) {
            case CLOSE_SLOT -> { sp.closeContainer(); return; }
            case BACK_SLOT -> { SkillTreeMenu.open(sp); return; }
            case PREV_SLOT -> { if (page > 0) open(sp, page - 1); return; }
            case NEXT_SLOT -> { if (page < recipes.size() - 1) open(sp, page + 1); return; }
            default -> sendAllDataToRemote();
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
