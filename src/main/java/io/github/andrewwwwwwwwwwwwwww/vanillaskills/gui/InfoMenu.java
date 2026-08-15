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

import java.util.List;

/**
 * A simple read-only chest sub-screen: shows the given items and a "Back" button (bottom-right)
 * that reopens the skill tree. All item movement is blocked.
 */
public class InfoMenu extends ChestMenu {
    private final ServerPlayer player;
    private final SimpleContainer container;

    public static void open(ServerPlayer player, Component title, int rows, List<ItemStack> items) {
        int clamped = Math.max(1, Math.min(6, rows));
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new InfoMenu(syncId, inv, (ServerPlayer) p, clamped, items), title));
    }

    private InfoMenu(int syncId, Inventory inv, ServerPlayer player, int rows, List<ItemStack> items) {
        super(menuTypeFor(rows), syncId, inv, new SimpleContainer(rows * 9), rows);
        this.player = player;
        this.container = (SimpleContainer) getContainer();

        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, ItemStack.EMPTY);

        // Lay the content items in centered rows of up to 7, starting below the top border.
        int perRow = 7;
        int chunks = Math.max(1, (int) Math.ceil(items.size() / (double) perRow));
        int startRow = Math.max(1, (rows - chunks) / 2); // vertically centered band
        int idx = 0;
        for (int c = 0; c < chunks && idx < items.size(); c++) {
            int remaining = items.size() - idx;
            int rowCount = Math.min(perRow, remaining);
            int startCol = (9 - rowCount) / 2;
            int row = Math.min(rows - 2, startRow + c);
            for (int k = 0; k < rowCount; k++) {
                container.setItem(row * 9 + startCol + k, items.get(idx++));
            }
        }

        ItemStack back = new ItemStack(Items.ARROW);
        back.set(DataComponents.CUSTOM_NAME, Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,"vanillaskills.menu.back","Back"))
                .withStyle(ChatFormatting.YELLOW).withStyle(s -> s.withItalic(false)));
        container.setItem(container.getContainerSize() - 1, back);
    }

    private static MenuType<ChestMenu> menuTypeFor(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (slotId == container.getContainerSize() - 1 && clicker instanceof ServerPlayer sp) {
            SkillTreeMenu.open(sp);
            return;
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
}
