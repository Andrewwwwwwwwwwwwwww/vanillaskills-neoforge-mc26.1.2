package io.github.andrewwwwwwwwwwwwwww.vanillaskills.gui;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.VanillaSkills;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorPiece;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.ArmorTiers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolKind;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTier;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool.ToolTiers;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.PlayerSkillData;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillCategory;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillNode;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.SkillTree;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * The skill GUI. With {@code category == null} it's the lane-select screen (one icon per lane);
 * with a category set it shows that lane's nodes.
 *
 * <p>The in-game node editor and lane-layout modes were removed in 2.0: the tree is content, and content
 * is meant to be authored rather than dragged around at runtime.
 */
public class SkillTreeMenu extends ChestMenu {
    private static final int POINTS_SLOT = 45;  // bottom-left corner
    private static final int STATS_SLOT = 53;   // bottom-right corner
    private static final int BACK_SLOT = 49;    // bottom-centre (lane view)
    private static final int WITHDRAW_SLOT = 36; // directly above the shard counter

    private final ServerPlayer player;
    private final SimpleContainer container;
    private final String category;   // null = lane-select view
    /** Withdrawing converts banked Skill Shards into items, so the first click only arms the button. */
    private boolean withdrawArmed;
    /** Home screen only: slot -> add-on extension id, resolved fresh on every populate(). */
    private final java.util.Map<Integer, String> extensionSlots = new java.util.HashMap<>();

    public static void open(ServerPlayer player) {
        openInternal(player, null);
    }

    public static void openCategory(ServerPlayer player, String categoryId) {
        openInternal(player, categoryId);
    }

    private static void openInternal(ServerPlayer player, String category) {
        SkillTree tree = VanillaSkills.TREE.tree();
        String base;
        if (category != null) {
            SkillCategory cat = tree.category(category);
            // Lane view: translate via the lane key, same as the lane icons use.
            base = cat != null
                    ? io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, "vanillaskills.lane." + cat.id, cat.title)
                    : io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, "vanillaskills.menu.skilltree.title", "Skills");
        } else {
            base = io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player,
                    "vanillaskills.menu.skilltree.title", tree.title == null ? "Skills" : tree.title);
        }
        Component title = Component.literal(base);
        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, p) -> new SkillTreeMenu(syncId, inv, (ServerPlayer) p, category), title));
    }

    public SkillTreeMenu(int syncId, Inventory inv, ServerPlayer player, String category) {
        super(menuTypeFor(VanillaSkills.TREE.tree().rows), syncId, inv,
                new SimpleContainer(VanillaSkills.TREE.tree().slotCount()), clampRows(VanillaSkills.TREE.tree().rows));
        this.player = player;
        this.category = category;
        this.container = (SimpleContainer) getContainer();
        populate();
    }

    private static int clampRows(int rows) {
        return Math.max(1, Math.min(6, rows));
    }

    private static MenuType<ChestMenu> menuTypeFor(int rows) {
        return switch (clampRows(rows)) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private void populate() {
        SkillTree tree = VanillaSkills.TREE.tree();
        PlayerSkillData data = VanillaSkills.PLAYERS.get(player.getUUID());
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) container.setItem(i, ItemStack.EMPTY);

        if (category == null) {
            for (SkillCategory cat : tree.categories()) {
                // Config-disabled crafting lanes are hidden entirely.
                if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate.laneDisabled(cat.id)) continue;
                container.setItem(cat.slot, buildCategoryItem(cat, data));
            }
            container.setItem(4, skillsHeader());
            container.setItem(POINTS_SLOT, buildCounter(data));
            container.setItem(WITHDRAW_SLOT, buildWithdrawButton(data));
            container.setItem(STATS_SLOT, buildStatsHead());
            placeExtensions();
        } else {
            for (SkillNode node : tree.nodesIn(category)) {
                container.setItem(node.slot, buildNodeItem(node, data));
            }
            container.setItem(BACK_SLOT, backButton());
            SkillCategory cat = tree.category(category);
            if (cat != null) {
                int hs = headerSlot();               // top-center, else nearest free top-row slot
                if (hs >= 0) container.setItem(hs, laneHeader(cat, data));
            }
            container.setItem(POINTS_SLOT, buildCounter(data));
            container.setItem(STATS_SLOT, buildStatsHead());
        }
    }

    /**
     * Home screen only: lay out buttons registered by add-on mods (see {@code SkillMenuExtensions}).
     *
     * <p>Runs after the lanes and the header/Points/Stats controls are placed, so "is this slot
     * free?" reflects the tree's own lane layout, so an add-on button is placed around the lanes rather
     * than over them. An entry with nowhere to go is skipped.
     */
    private void placeExtensions() {
        extensionSlots.clear();
        if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.api.SkillMenuExtensions.isEmpty()) return;
        for (var entry : io.github.andrewwwwwwwwwwwwwww.vanillaskills.api.SkillMenuExtensions.all()) {
            ItemStack icon;
            try {
                icon = entry.icon().apply(player);
            } catch (Exception e) {
                VanillaSkills.LOGGER.warn("Skill menu extension '{}' failed to build its icon", entry.id(), e);
                continue;
            }
            if (icon == null || icon.isEmpty()) continue; // entry opted out for this player
            int slot = freeExtensionSlot(entry.preferredSlot());
            if (slot < 0) continue;                       // no room; better than clobbering a lane
            container.setItem(slot, icon);
            extensionSlots.put(slot, entry.id());
        }
    }

    /** The preferred slot if usable, else the next usable slot scanning backwards. -1 if none. */
    private int freeExtensionSlot(int preferred) {
        if (isExtensionSlotFree(preferred)) return preferred;
        for (int i = container.getContainerSize() - 1; i >= 0; i--) {
            if (isExtensionSlotFree(i)) return i;
        }
        return -1;
    }

    private boolean isExtensionSlotFree(int slot) {
        if (slot < 0 || slot >= container.getContainerSize()) return false;
        if (isReservedSlot(slot)) return false;
        if (extensionSlots.containsKey(slot)) return false;
        return container.getItem(slot).isEmpty();
    }

    /** Control slots that lanes and add-on buttons must never take over. */
    private boolean isReservedSlot(int slot) {
        return slot == POINTS_SLOT || slot == STATS_SLOT || slot == WITHDRAW_SLOT || slot == 4
                || slot == container.getContainerSize() - 1;
    }

    // ---- lane select ----

    private String t(String key, String fallback, Object... args) {
        return io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, key, fallback, args);
    }

    /** Translated lane display name (key vanillaskills.lane.<id>, fallback to the tree's title). */
    private String laneName(SkillCategory cat) {
        return t("vanillaskills.lane." + cat.id, cat.title);
    }

    /** Translated node title. Node titles are "<LaneName> <roman>", so swapping in the translated lane
     *  name localizes them all for free — no per-node keys. A per-node key still overrides if present. */
    private String nodeTitle(SkillNode node) {
        String override = t("vanillaskills.node." + node.id, "");
        if (!override.isEmpty()) return override;
        SkillCategory cat = VanillaSkills.TREE.tree().category(node.category);
        if (cat != null && cat.title != null && node.title.startsWith(cat.title)) {
            return laneName(cat) + node.title.substring(cat.title.length());
        }
        return node.title;
    }

    private ItemStack buildCategoryItem(SkillCategory cat, PlayerSkillData data) {
        SkillTree tree = VanillaSkills.TREE.tree();
        // Recipes is a pseudo-lane that opens the custom-recipe book.
        if ("recipes".equals(cat.id)) {
            ItemStack r = new ItemStack(resolveItem(cat.icon));
            Guis.hideStats(r);
            r.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.lane.recipes","Recipes"), ChatFormatting.GOLD));
            r.set(DataComponents.LORE, new ItemLore(List.of(
                    styled(t("vanillaskills.lane.recipes.desc","All custom crafting recipes"), ChatFormatting.GRAY),
                    styled(t("vanillaskills.menu.skilltree.view","Click to view"), ChatFormatting.YELLOW))));
            return r;
        }
        if ("guide".equals(cat.id)) {
            ItemStack r = new ItemStack(resolveItem(cat.icon));
            Guis.hideStats(r);
            r.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.lane.guide","Guide"), ChatFormatting.GOLD));
            r.set(DataComponents.LORE, new ItemLore(List.of(
                    styled(t("vanillaskills.lane.guide.desc","How VanillaSkills works"), ChatFormatting.GRAY),
                    styled(t("vanillaskills.menu.click_to_open","Click to open"), ChatFormatting.YELLOW))));
            return r;
        }
        if ("quests".equals(cat.id)) {
            ItemStack r = new ItemStack(resolveItem(cat.icon));
            Guis.hideStats(r);
            r.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.lane.quests","Bounty Board"), ChatFormatting.GOLD));
            r.set(DataComponents.LORE, new ItemLore(List.of(
                    styled(t("vanillaskills.lane.quests.desc","Quests & the Quest Shop"), ChatFormatting.GRAY),
                    styled(t("vanillaskills.menu.click_to_open","Click to open"), ChatFormatting.YELLOW))));
            return r;
        }
        int total = 0, unlocked = 0;
        boolean quest = false;
        for (SkillNode node : tree.nodesIn(cat.id)) {
            total++;
            if (data.hasUnlocked(node.id)) unlocked++;
            if (node.isQuestCurrency()) quest = true;
        }
        ItemStack stack = new ItemStack(resolveItem(cat.icon));
        Guis.hideStats(stack);
        // A locked lane (e.g. Night Vision) stays sealed until its earned-Shard gate is met — and we
        // deliberately don't reveal the requirement, so players can't bee-line to it.
        if (isLaneLocked(cat, data)) {
            stack.set(DataComponents.CUSTOM_NAME, styled(laneName(cat), ChatFormatting.DARK_GRAY));
            stack.set(DataComponents.LORE, new ItemLore(List.of(styled(t("vanillaskills.menu.skilltree.locked","🔒 Locked"), ChatFormatting.RED))));
            return stack;
        }
        // Crafting lanes are tinted purple (Quest Shards); skill lanes stay aqua (Skill Shards).
        ChatFormatting nameColor = quest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA;
        stack.set(DataComponents.CUSTOM_NAME, styled(laneName(cat), nameColor));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled(t("vanillaskills.menu.skilltree.unlocked","%d/%d unlocked", unlocked, total), ChatFormatting.GRAY),
                styled(quest ? t("vanillaskills.menu.quest_shards","Quest Shards") : t("vanillaskills.menu.skill_shards","Skill Shards"), quest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA),
                styled(t("vanillaskills.menu.click_to_open","Click to open"), ChatFormatting.YELLOW))));
        if (total > 0 && unlocked == total) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    /** True if any node in this lane is still gated behind an earned-Shard requirement the player hasn't met. */
    private static boolean isLaneLocked(SkillCategory cat, PlayerSkillData data) {
        for (SkillNode node : VanillaSkills.TREE.tree().nodesIn(cat.id)) {
            if (node.minEarned > 0 && data.pointsEarned < node.minEarned) return true;
        }
        return false;
    }

    // ---- lane view (nodes) ----

    /** Short blurb per lane explaining what its skills do — shown on the lane header at the top. */
    private static final java.util.Map<String, String[]> LANE_DESCRIPTIONS = java.util.Map.ofEntries(
            java.util.Map.entry("health", new String[]{"Raises your maximum health.", "+2 hearts per tier, up to +20 hearts."}),
            java.util.Map.entry("speed", new String[]{"Move faster on foot.", "+2% walk speed per tier, up to +30%."}),
            java.util.Map.entry("mining", new String[]{"Mine blocks faster.", "Stacking mining efficiency — max it out to",
                    "instamine stone with an Efficiency V pickaxe."}),
            java.util.Map.entry("luck", new String[]{"Raises your Luck (+0.5 per tier, up to +5).",
                    "Better loot from chests, vaults & fishing."}),
            java.util.Map.entry("damage", new String[]{"Hit harder in melee.", "+0.5 flat & +3% weapon damage per tier",
                    "(up to +5 & +30%). Scales with your weapon."}),
            java.util.Map.entry("guardian", new String[]{"Take less damage.", "+1 armor per tier, up to +10."}),
            java.util.Map.entry("reach", new String[]{"Place and hit from further away.",
                    "+0.5 block & entity reach per tier (max +2.5)."}),
            java.util.Map.entry("mountaineer", new String[]{"Auto-step up ledges up to 1.1 blocks tall.",
                    "Sneak to walk normally. Toggle: /skill toggle stepup."}),
            java.util.Map.entry("aquatic", new String[]{"Underwater mastery: longer breath,",
                    "faster swimming, and quicker underwater mining."}),
            java.util.Map.entry("armorsmith", new String[]{"Unlocks crafting each armor tier,",
                    "Hardwood up to Dragon. Paid in Quest Shards."}),
            java.util.Map.entry("toolsmith", new String[]{"Unlocks crafting each tool tier,",
                    "Hardwood up to Dragon. Paid in Quest Shards."}),
            java.util.Map.entry("brewmaster", new String[]{"Beneficial potions last longer —",
                    "+10% duration per tier, up to +50%."}),
            java.util.Map.entry("evasion", new String[]{"Chance to fully dodge incoming arrows.",
                    "+2% per tier, up to 20%."}),
            java.util.Map.entry("cultivator", new String[]{"Bonus crops when harvesting mature crops.",
                    "+20% chance per tier, up to a guaranteed extra."}),
            java.util.Map.entry("nightvision", new String[]{"A capstone granting permanent Night Vision.",
                    "Toggle with /skill toggle nightvision."}));

    /** The lane-view header: the branch's icon, name, what it does, and unlock progress. */
    private ItemStack laneHeader(SkillCategory cat, PlayerSkillData data) {
        SkillTree tree = VanillaSkills.TREE.tree();
        int total = 0, unlocked = 0;
        boolean quest = false;
        for (SkillNode node : tree.nodesIn(cat.id)) {
            total++;
            if (data.hasUnlocked(node.id)) unlocked++;
            if (node.isQuestCurrency()) quest = true;
        }
        ChatFormatting color = quest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA;
        ItemStack stack = new ItemStack(resolveItem(cat.icon));
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, styled(laneName(cat), color));
        List<Component> lore = new ArrayList<>();
        // Description translates as one key per lane (lines joined by \n), fallback to the built-in English.
        String englishDesc = String.join("\n", LANE_DESCRIPTIONS.getOrDefault(cat.id,
                new String[]{"Spend Shards to unlock this branch's skills."}));
        for (String line : t("vanillaskills.lane." + cat.id + ".header", englishDesc).split("\n")) {
            lore.add(styled(line, ChatFormatting.GRAY));
        }
        lore.add(Component.literal(""));
        lore.add(styled(t("vanillaskills.menu.skilltree.unlocked", "%d/%d unlocked", unlocked, total), color));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    /** Top-center (slot 4) for the lane header, or the nearest free slot in the top row. */
    private int headerSlot() {
        if (container.getItem(4).isEmpty()) return 4;
        for (int i = 0; i < 9; i++) if (container.getItem(i).isEmpty()) return i;
        return -1;
    }

    private ItemStack buildNodeItem(SkillNode node, PlayerSkillData data) {
        boolean unlocked = data.hasUnlocked(node.id);
        boolean prereqMet = node.requires.stream().allMatch(data::hasUnlocked);

        boolean quest = node.isQuestCurrency();
        String curName = quest ? t("vanillaskills.menu.quest_shards","Quest Shards") : t("vanillaskills.menu.skill_shards","Skill Shards");
        ChatFormatting curColor = quest ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.YELLOW;
        int balance = quest ? data.questShardsAvailable : data.pointsAvailable;

        boolean gated = node.minEarned > 0 && data.pointsEarned < node.minEarned;
        int chain = VanillaSkills.PLAYERS.chainCost(player, node.id); // total a left-click would charge
        boolean affordableChain = balance >= chain;

        ItemStack stack = iconStackFor(node);
        Guis.hideStats(stack);
        ChatFormatting nameColor = unlocked ? ChatFormatting.GREEN
                : gated ? ChatFormatting.DARK_GRAY
                : !prereqMet ? ChatFormatting.GRAY
                : affordableChain ? curColor : ChatFormatting.RED;
        stack.set(DataComponents.CUSTOM_NAME, styled(nodeTitle(node) + (unlocked ? " ✔" : ""), nameColor));

        List<Component> lore = new ArrayList<>();
        // Node descriptions live in the per-world tree (English); a lang key overrides them for
        // translation — "vanillaskills.node.<id>.desc", multi-line via \n (same pattern as titles).
        String descOverride = t("vanillaskills.node." + node.id + ".desc", "");
        if (!descOverride.isEmpty()) {
            for (String line : descOverride.split("\n")) lore.add(styled(line, ChatFormatting.GRAY));
        } else {
            for (String line : node.description) lore.add(styled(line, ChatFormatting.GRAY));
        }
        lore.add(Component.literal(""));
        if (unlocked) {
            lore.add(styled(t("vanillaskills.menu.skilltree.node_unlocked","Unlocked"), ChatFormatting.GREEN));
        } else if (gated) {
            lore.add(styled(t("vanillaskills.menu.skilltree.locked","🔒 Locked"), ChatFormatting.RED)); // requirement intentionally hidden
        } else {
            lore.add(styled(t("vanillaskills.menu.skilltree.cost","Cost: %d %s", chain, curName), affordableChain ? curColor : ChatFormatting.RED));
            if (!prereqMet) lore.add(styled(t("vanillaskills.menu.skilltree.buys_chain","Buys this + the skills below it"), ChatFormatting.DARK_GRAY));
            else lore.add(styled(t("vanillaskills.menu.skilltree.left_unlock","Left-click to unlock"), ChatFormatting.DARK_GRAY));
            if (!affordableChain) lore.add(styled(t("vanillaskills.menu.skilltree.not_enough","Not enough %s", curName), ChatFormatting.RED));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (unlocked) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }


    private ItemStack buildCounter(PlayerSkillData data) {
        ItemStack stack = new ItemStack(Items.EXPERIENCE_BOTTLE);
        stack.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.menu.skilltree.your_shards","Your Shards"), ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled(t("vanillaskills.menu.skilltree.skill_bal","Skill Shards: %d", data.pointsAvailable), ChatFormatting.AQUA),
                styled(t("vanillaskills.menu.skilltree.quest_bal","Quest Shards: %d", data.questShardsAvailable), ChatFormatting.LIGHT_PURPLE),
                Component.literal(""),
                styled(t("vanillaskills.menu.skilltree.earn_hint","Click to see how to earn Skill Shards"), ChatFormatting.GRAY))));
        return stack;
    }

    /**
     * The withdraw button: turns banked Skill Shards into physical Unstable Skill Shards.
     *
     * <p>Guarded by a two-step click because the conversion is one the player would not want to make by
     * accident. The first click only arms it. <b>Once confirmed it stays armed until the screen closes</b> —
     * including while the player clicks other things — so emptying a large balance is one confirmation
     * followed by ordinary clicking, not a confirmation per shard.
     */
    private ItemStack buildWithdrawButton(PlayerSkillData data) {
        ItemStack stack = io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.unstableShard();
        Guis.hideStats(stack);
        stack.set(DataComponents.ITEM_NAME,
                styled(t("vanillaskills.menu.skilltree.withdraw", "Withdraw a Skill Shard"), ChatFormatting.LIGHT_PURPLE));
        List<Component> lore = new ArrayList<>();
        int amount = io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBank.withdrawAmount();
        lore.add(styled(t("vanillaskills.menu.skilltree.withdraw.desc",
                "Turn %d Skill Shard(s) into physical shards.", amount), ChatFormatting.GRAY));
        lore.add(styled(t("vanillaskills.menu.skilltree.withdraw.back",
                "Right-click a shard in hand to bank it again."), ChatFormatting.DARK_GRAY));
        lore.add(Component.literal(""));
        if (data.pointsAvailable <= 0) {
            lore.add(styled(t("vanillaskills.menu.skilltree.withdraw.none",
                    "You have no Skill Shards to withdraw."), ChatFormatting.RED));
        } else if (withdrawArmed) {
            lore.add(styled(t("vanillaskills.menu.skilltree.withdraw.ready",
                    "Ready — click to withdraw"), ChatFormatting.GREEN));
            lore.add(styled(t("vanillaskills.menu.skilltree.withdraw.ready2",
                    "Stays ready until you close this screen."), ChatFormatting.DARK_GRAY));
        } else {
            lore.add(styled(t("vanillaskills.menu.skilltree.withdraw.arm",
                    "Click, then click again to confirm"), ChatFormatting.YELLOW));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        if (withdrawArmed) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private ItemStack buildStatsHead() {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.menu.skilltree.your_stats","Your Stats"), ChatFormatting.AQUA));
        stack.set(DataComponents.LORE, new ItemLore(List.of(styled(t("vanillaskills.menu.skilltree.stats_hint","Click to view your current stats"), ChatFormatting.GRAY))));
        return stack;
    }

    private ItemStack backButton() {
        ItemStack stack = new ItemStack(Items.ARROW);
        stack.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.menu.skilltree.back_lanes","Back to Lanes"), ChatFormatting.YELLOW));
        return stack;
    }


    // ---- edit handling (within a lane) ----





    // ---- shared ----

    private static MutableComponent styled(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(s -> s.withItalic(false));
    }

    private static Item resolveItem(String iconId) {
        if (iconId == null) return Items.STONE;
        Identifier id = Identifier.tryParse(iconId);
        if (id == null) return Items.STONE;
        return BuiltInRegistries.ITEM.get(id).map(Holder::value).orElse(Items.STONE);
    }

    /**
     * Icon for a node. Armorsmith/Toolsmith ladder nodes on a VanillaSkills tier (Hardwood, Rose Gold,
     * Steel, Crystalline, Dragon) use the real marked tier item so the resource pack retextures it —
     * otherwise they'd show the vanilla base texture. Vanilla-tier and all other nodes use their icon id.
     */
    private static ItemStack iconStackFor(SkillNode node) {
        ItemStack custom = customTierIcon(node);
        return custom != null ? custom : new ItemStack(resolveItem(node.icon));
    }

    private static ItemStack customTierIcon(SkillNode node) {
        boolean armor = "armorsmith".equals(node.category);
        boolean tool = "toolsmith".equals(node.category);
        if (!armor && !tool) return null;
        int idx;
        try {
            idx = Integer.parseInt(node.id.substring(node.category.length() + 1)) - 1;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
        // Ladder order: Hardwood Copper Gold RoseGold Iron Steel Diamond Crystalline Netherite Dragon.
        // Only the VanillaSkills tiers (0,3,5,7,9) have custom textures; the rest stay vanilla.
        if (armor) {
            ArmorTier t = switch (idx) {
                case 0 -> ArmorTiers.HARDWOOD; case 3 -> ArmorTiers.ROSE_GOLD; case 5 -> ArmorTiers.STEEL;
                case 7 -> ArmorTiers.CRYSTAL; case 9 -> ArmorTiers.DRAGON; default -> null;
            };
            return t == null ? null : t.create(ArmorPiece.CHESTPLATE);
        }
        ToolTier t = switch (idx) {
            case 0 -> ToolTiers.HARDWOOD; case 3 -> ToolTiers.ROSE_GOLD; case 5 -> ToolTiers.STEEL;
            case 7 -> ToolTiers.CRYSTAL; case 9 -> ToolTiers.DRAGON; default -> null;
        };
        return t == null ? null : t.create(ToolKind.PICKAXE);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clicker) {
        if (slotId >= 0 && slotId < container.getContainerSize() && clicker instanceof ServerPlayer sp) {
            if (category == null) {
                if (handleLaneSelectClick(sp, slotId)) return;
            } else {
                if (handleLaneViewClick(sp, slotId, button)) return;
            }
        }
        populate();
        sendAllDataToRemote();
    }

    /** Layout mode: pick up a lane, then click an empty spot to move it or another lane to swap. */

    private ItemStack skillsHeader() {
        ItemStack stack = new ItemStack(Items.NETHER_STAR);
        Guis.hideStats(stack);
        stack.set(DataComponents.CUSTOM_NAME, styled(t("vanillaskills.menu.skilltree.header","✦ Skills ✦"), ChatFormatting.GOLD));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                styled(t("vanillaskills.menu.skilltree.header_desc","Spend Skill Shards & Quest Shards here"), ChatFormatting.GRAY))));
        return stack;
    }


    /** @return true if a sub-screen was opened (this menu is being replaced). */
    private boolean handleLaneSelectClick(ServerPlayer sp, int slotId) {
        if (slotId == WITHDRAW_SLOT) {
            if (!withdrawArmed) {
                withdrawArmed = true; // first click arms only — nothing is converted yet
            } else {
                io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBank.withdraw(sp,
                        io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardBank.withdrawAmount());
            }
            return false; // stay here; populate() redraws the button in its new state
        }
        // Deliberately NOT disarmed by clicking elsewhere: once confirmed, the player can keep withdrawing
        // freely for as long as this screen is open, rather than re-confirming every single shard.

        if (slotId == POINTS_SLOT) {
            PointsScreen.open(sp);
            return true;
        }
        if (slotId == STATS_SLOT) {
            StatsScreen.open(sp);
            return true;
        }
        String extensionId = extensionSlots.get(slotId);
        if (extensionId != null) {
            for (var entry : io.github.andrewwwwwwwwwwwwwww.vanillaskills.api.SkillMenuExtensions.all()) {
                if (!entry.id().equals(extensionId)) continue;
                try {
                    entry.onClick().accept(sp);
                } catch (Exception e) {
                    VanillaSkills.LOGGER.warn("Skill menu extension '{}' threw on click", extensionId, e);
                }
                return true; // the add-on owns the screen now (it usually opens its own menu)
            }
            return false; // unregistered since this menu was built
        }
        SkillCategory cat = VanillaSkills.TREE.tree().categoryAtSlot(slotId);
        if (cat != null) {
            if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.CraftingGate.laneDisabled(cat.id)) {
                return false; // lane hidden by config — its slot is empty for players
            }
            if ("recipes".equals(cat.id)) {
                RecipeBookMenu.open(sp, 0);
                return true;
            }
            if ("guide".equals(cat.id)) {
                // Prefer the wiki: it is the copy that stays current. The book is the offline fallback.
                if (io.github.andrewwwwwwwwwwwwwww.vanillaskills.book.GuideLink.available()) {
                    io.github.andrewwwwwwwwwwwwwww.vanillaskills.book.GuideLink.open(sp);
                } else {
                    io.github.andrewwwwwwwwwwwwwww.vanillaskills.book.GuideBook.open(sp);
                }
                return true;
            }
            if ("quests".equals(cat.id)) {
                QuestMenu.open(sp);
                return true;
            }
            if (isLaneLocked(cat, VanillaSkills.PLAYERS.get(sp.getUUID()))) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(sp,"vanillaskills.msg.lane_locked","🔒 That path is still locked."))
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            openCategory(sp, cat.id);
            return true;
        }
        return false;
    }

    private boolean handleLaneViewClick(ServerPlayer sp, int slotId, int button) {
        if (slotId == BACK_SLOT) {
            openInternal(sp, null);
            return true;
        }
        if (slotId == POINTS_SLOT) {
            PointsScreen.open(sp);
            return true;
        }
        if (slotId == STATS_SLOT) {
            StatsScreen.open(sp);
            return true;
        }
        SkillNode node = VanillaSkills.TREE.tree().nodeInCategoryAtSlot(category, slotId);
        if (node != null) {
            VanillaSkills.PLAYERS.unlockChain(sp, node.id);   // buy this node + everything below it
        }
        return false;
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
