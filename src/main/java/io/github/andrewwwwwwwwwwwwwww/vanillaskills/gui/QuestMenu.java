package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.Quest;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestPool;
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

/**
 * The bounty board. Pre-graduation it shows ALL fixed starter quests (always active, complete each
 * once, no rotation); after graduating it shows the shared 6-quest rotating board. All text is
 * translated per player via {@link Lang} (server-side, keyed off the client's language).
 */
public class QuestMenu extends ChestMenu {
    // Graduated layout (9x4): 6 quests in a 3-3 block below the clock (rows 1-2, columns 2/4/6).
    private static final int[] MAIN_SLOTS = {11, 13, 15, 20, 22, 24};
    // Starter layout (9x5): a centered 5-wide x 3-row block of quests (columns 2-6).
    // Exactly QuestPool.STARTER_CAPACITY slots — the datapack pool is trimmed to this length on load,
    // so every starter quest is always reachable and graduation is always attainable.
    private static final int[] STARTER_SLOTS = {
            11, 12, 13, 14, 15,
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33};
    private static final int INFO_SLOT = 4;

    private final ServerPlayer player;
    private final SimpleContainer container;
    private final boolean starterBoard;
    private final int[] questSlots;
    private final int shopSlot;
    private final int backSlot;
    private final int closeSlot;
    private final int featsSlot;
    /** Rotation this menu was built against — clicks on a stale board reopen instead of claiming. */
    private long rotationAtBuild;

    public static void open(ServerPlayer player) {
        Quests.sync(player);
        boolean starter = !Quests.isGraduated(player);
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new QuestMenu(syncId, inv, (ServerPlayer) p, starter),
                Component.literal(Lang.tr(player, "vanillaskills.menu.quests.title", "Bounty Board"))));
    }

    private QuestMenu(int syncId, Inventory inv, ServerPlayer player, boolean starterBoard) {
        super(starterBoard ? MenuType.GENERIC_9x5 : MenuType.GENERIC_9x4, syncId, inv,
                new SimpleContainer(starterBoard ? 45 : 36), starterBoard ? 5 : 4);
        this.player = player;
        this.container = (SimpleContainer) getContainer();
        this.starterBoard = starterBoard;
        this.questSlots = starterBoard ? STARTER_SLOTS : MAIN_SLOTS;
        int last = container.getContainerSize() - 1; // 44 or 35
        this.closeSlot = last;                       // bottom-right
        this.backSlot = last - 8;                    // bottom-left
        this.shopSlot = last - 4;                    // bottom-center
        this.featsSlot = 8;                          // top-right corner
        populate();
    }

    private void populate() {
        this.rotationAtBuild = VanillaSkills.QUESTS.rotationId();
        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, ItemStack.EMPTY);
        container.setItem(INFO_SLOT, infoItem());
        List<Quest> active = Quests.activeFor(player);
        for (int i = 0; i < active.size() && i < questSlots.length; i++) {
            container.setItem(questSlots[i], questItem(i, active.get(i)));
        }
        container.setItem(shopSlot, shopButton());
        if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.FEATS_ENABLED) {
            container.setItem(featsSlot, button(Items.WITHER_SKELETON_SKULL,
                    t("vanillaskills.menu.feats.button", "Feats"), ChatFormatting.GOLD,
                    t("vanillaskills.menu.feats.button.desc", "One-time achievements: discoveries, bosses & the End.")));
        }
        container.setItem(backSlot, button(Items.NETHER_STAR,
                t("vanillaskills.menu.quests.skilltree", "Skill Tree"), ChatFormatting.AQUA,
                t("vanillaskills.menu.quests.skilltree.desc", "Open the skill tree.")));
        container.setItem(closeSlot, button(Items.BARRIER, t("vanillaskills.menu.close", "Close"), ChatFormatting.RED, null));
    }

    private String t(String key, String fallback, Object... args) {
        return Lang.tr(player, key, fallback, args);
    }

    private ItemStack infoItem() {
        int quest = VanillaSkills.PLAYERS.questShards(player);
        List<Component> lore = new ArrayList<>();
        if (starterBoard) {
            int total = QuestPool.starter().size();
            lore.add(styled(t("vanillaskills.menu.quests.starter.info1", "Your starter quests — all always available,"), ChatFormatting.GRAY));
            lore.add(styled(t("vanillaskills.menu.quests.starter.info2", "no rotation, complete them in any order."), ChatFormatting.GRAY));
            lore.add(styled(t("vanillaskills.menu.quests.starter.progress", "Finish all %d to join the main board: %d/%d",
                    total, Quests.graduationProgress(player), total), ChatFormatting.AQUA));
        } else {
            long remaining = VanillaSkills.QUESTS.nextRotationMs() - System.currentTimeMillis();
            String when = remaining <= 0 ? t("vanillaskills.menu.quests.any_moment", "any moment")
                    : (remaining / 3_600_000) + "h " + (remaining % 3_600_000 / 60_000) + "m";
            lore.add(styled(t("vanillaskills.menu.quests.info1", "The main bounty board — shared by everyone."), ChatFormatting.GRAY));
            lore.add(styled(t("vanillaskills.menu.quests.rotation", "New bounties in %s", when), ChatFormatting.YELLOW));
        }
        lore.add(Component.literal(""));
        lore.add(styled(t("vanillaskills.menu.quests.shards", "Your Quest Shards: %d", quest), ChatFormatting.LIGHT_PURPLE));
        lore.add(styled(t("vanillaskills.menu.quests.click_hint", "Click a quest to claim its reward."), ChatFormatting.DARK_GRAY));

        ItemStack stack = new ItemStack(Items.CLOCK);
        stack.set(DataComponents.CUSTOM_NAME, styled(starterBoard
                ? t("vanillaskills.menu.quests.starter.title", "Starter Quests")
                : t("vanillaskills.menu.quests.title", "Bounty Board"), ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private ItemStack shopButton() {
        ItemStack stack = new ItemStack(Items.EMERALD);
        stack.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.menu.shop.button", "Quest Shop"), ChatFormatting.GREEN));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled(t("vanillaskills.menu.shop.button.desc1", "Spend Quest Shards on boost items,"), ChatFormatting.GRAY),
                styled(t("vanillaskills.menu.shop.button.desc2", "or convert them into Skill Shards."), ChatFormatting.GRAY),
                Component.literal(""),
                styled(t("vanillaskills.menu.click_to_open", "Click to open"), ChatFormatting.GREEN))));
        return stack;
    }

    private ItemStack button(net.minecraft.world.item.Item item, String name, ChatFormatting color, String desc) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, styled(name, color));
        if (desc != null) stack.set(DataComponents.LORE, new ItemLore(List.of(styled(desc, ChatFormatting.GRAY))));
        return stack;
    }

    private ItemStack questItem(int index, Quest q) {
        boolean claimed = Quests.isClaimed(player, index);
        int progress = Quests.progress(player, index);
        boolean ready = progress >= q.amount();
        String title = t(Lang.questKey(q.title()), q.title());

        ItemStack stack = new ItemStack(iconFor(q));
        ChatFormatting nameColor = claimed ? ChatFormatting.DARK_GRAY : ready ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        stack.set(DataComponents.CUSTOM_NAME, styled(title + (claimed ? " ✔" : ""), nameColor));

        List<Component> lore = new ArrayList<>();
        if (q.type() != Quest.Type.FREEBIE) {
            String verb = switch (q.type()) {
                case KILL -> t("vanillaskills.menu.quests.verb.kill", "Slain");
                case SKILL -> t("vanillaskills.menu.quests.verb.skill", "Skills unlocked");
                case STAT -> t("vanillaskills.menu.quests.verb.stat", "Progress");
                default -> t("vanillaskills.menu.quests.verb.gather", "Gathered");
            };
            lore.add(styled(verb + ": " + progress + "/" + q.amount(), ChatFormatting.GRAY));
        }
        lore.add(styled(q.reward() == 1
                ? t("vanillaskills.menu.quests.reward.one", "Reward: +%d Quest Shard", q.reward())
                : t("vanillaskills.menu.quests.reward.many", "Reward: +%d Quest Shards", q.reward()), ChatFormatting.LIGHT_PURPLE));
        lore.add(Component.literal(""));
        if (claimed) {
            lore.add(styled(starterBoard
                    ? t("vanillaskills.menu.quests.completed", "Completed")
                    : t("vanillaskills.menu.quests.claimed", "Claimed — come back next rotation"), ChatFormatting.DARK_GRAY));
        } else if (ready) {
            lore.add(styled(q.type() == Quest.Type.GATHER
                    ? t("vanillaskills.menu.quests.turn_in", "Click to turn in & claim")
                    : t("vanillaskills.menu.quests.claim", "Click to claim"), ChatFormatting.GREEN));
        } else {
            String hint = switch (q.type()) {
                case GATHER -> t("vanillaskills.menu.quests.hint.gather", "Bring the items here");
                case SKILL -> t("vanillaskills.menu.quests.hint.skill", "Unlock skills in the skill tree (/skill)");
                case STAT -> t("vanillaskills.menu.quests.hint.stat", "Counts from when this appeared — keep going");
                default -> t("vanillaskills.menu.quests.hint.kill", "Keep hunting");
            };
            lore.add(styled(hint, ChatFormatting.GRAY));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (claimed) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static net.minecraft.world.item.Item iconFor(Quest q) {
        if (q.type() == Quest.Type.FREEBIE) return Items.EMERALD;
        if (q.type() == Quest.Type.SKILL) return Items.NETHER_STAR;
        if (q.type() == Quest.Type.STAT) return q.target().contains("swim") ? Items.WATER_BUCKET
                : q.target().contains("jump") ? Items.RABBIT_FOOT : Items.LEATHER_BOOTS;
        if (q.type() == Quest.Type.GATHER) return Quests.item(q.target());
        if (q.target().equals(Quest.ANY_HOSTILE)) return Items.IRON_SWORD;
        net.minecraft.world.item.Item egg = Quests.item(q.target() + "_spawn_egg");
        return egg == Items.PAPER ? Items.IRON_SWORD : egg;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (clicker instanceof ServerPlayer sp) {
            if (slotId == closeSlot) { sp.closeContainer(); return; }
            if (slotId == backSlot) { SkillTreeMenu.open(sp); return; }
            if (slotId == shopSlot) { ShopMenu.open(sp); return; }
            if (slotId == featsSlot && io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.FEATS_ENABLED) {
                FeatsMenu.open(sp); return;
            }
            for (int i = 0; i < questSlots.length; i++) {
                if (slotId == questSlots[i]) {
                    // Stale-board guard: if graduation status or the rotation changed while this menu
                    // was open, the rendered quests no longer match the live board — a claim here would
                    // hit the WRONG quest (and could consume the wrong items). Reopen instead.
                    boolean liveStarter = !Quests.isGraduated(sp);
                    if (liveStarter != starterBoard
                            || (!starterBoard && rotationAtBuild != VanillaSkills.QUESTS.rotationId())) {
                        sp.sendSystemMessage(Component.literal(Lang.tr(sp, "vanillaskills.menu.quests.stale",
                                "The board changed — reopening.")).withStyle(ChatFormatting.YELLOW));
                        open(sp);
                        return;
                    }
                    Quests.claim(sp, i);
                    // Graduating mid-menu changes the board layout; reopen with the right size.
                    if (starterBoard && Quests.isGraduated(sp)) {
                        open(sp);
                        return;
                    }
                    populate();
                    sendAllDataToRemote();
                    return;
                }
            }
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
