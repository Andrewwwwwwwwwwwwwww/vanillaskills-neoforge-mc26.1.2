package io.github.andrewwwwwwwwwwwwwww.vanillaskills.book;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestShop;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the mod's guide as a written book and opens it on the player's screen without changing
 * their inventory: the book is sent to the client's view of the held slot, the open-book packet
 * is sent, then the real held item is re-sent to correct the client.
 *
 * <p>Pages are kept short so none overflow a written-book page (~14 lines). {@code {MENDING}},
 * {@code {XP}}, {@code {GRAD}}, {@code {CONVERT}}, {@code {QUESTS}} and {@code {SHARDBLOCK}} are filled
 * from the live config at build time so the guide never contradicts the current settings.
 *
 * <p>Page order is load-bearing: each page is keyed by its index for translation, so new pages go on the
 * end and existing ones are edited in place.
 */
public final class GuideBook {
    private GuideBook() {}

    private static final String[] PAGES = {
            """
            VanillaSkills

            A server-side progression overhaul.

            Skill Shards: advancements, ore,
            exploring.
            Quest Shards: bounties.

            Type /help for commands.""",

            """
            Skill Tree (/skill)

            Spend Skill Shards on lanes of perks: health, speed, mining, combat, and more.

            Unlocks are permanent - choose wisely.""",

            """
            Skill Tree

            Click a node to buy it and everything below it on that path.

            Bottom-left shows your Shards; bottom-right, your stats.""",

            """
            Earning Skill Shards

            Advancements are the main source - each counts once. Tasks a little, goals more, challenges a lot.

            Also: shard ore, chests, barters, spawners and the trader.""",

            """
            Bounty Board
            (/quests or /bounty)

            {QUESTS} bounties at a time, refreshing on a timer.

            Gather items or slay mobs to earn Quest Shards.""",

            """
            Bounty Board

            New players begin with {GRAD} fixed starter quests - always available, no rotation.

            Finish them all to graduate to the shared main board.""",

            """
            Quest Shop

            Open it from the bounty board - a rotating set of boost items, bought with Quest or Skill Shards.

            A converter trades {CONVERT} Quest Shards for 1 Skill Shard.""",

            """
            Crafting Ladders

            Armorsmith & Toolsmith (paid in Quest Shards) gate crafting EVERY gear tier - vanilla and custom.

            Wood & stone stay free. Found or traded gear always works.""",

            """
            Deepslate

            Deepslate and its ores need a Steel-tier or better pickaxe (Steel, Diamond, Crystalline, Netherite, Dragon).

            Unlock Steel in the Toolsmith lane to dig the deep layer.""",

            """
            Gear Materials

            Hardwood armor: from Wood blocks (all-bark, like Oak Wood) - not logs or planks.

            Rose Gold ingot: 4 gold + 4 copper = 4.""",

            """
            Gear Materials

            Steel ingot: an iron block in a furnace or blast furnace = 3.

            Steel Shield: shield + Steel Ingot in an anvil.

            Crystallized Diamond: 4 amethyst + 2 Unstable Shards + 2 diamonds + 1 amethyst block = 2.""",

            """
            Set Bonuses

            Rose Gold: immune to bad effects & fire; piglins stay neutral.

            Crystalline: reflects 25% melee damage, plus Strength & Resistance I.

            Dragon: immune to fire, lava & breath.""",

            """
            Dragon Gear

            Slay the Ender Dragon for 8 Dragon Scales — 32 for the world's first kill. Four scales around a Netherite Ingot make a Dragon Ingot.

            Sneak in midair to dive-dash.""",

            """
            Dragon Upgrade

            Find a Dragon Upgrade template in End City treasure (~4%).

            Smithing: template + netherite armor + Dragon Ingot = Dragon armor (keeps enchants).""",

            """
            Dragon Elytra

            Drop a Dragon chestplate and an Elytra onto an anvil to fuse them into a gliding chestplate.

            A grindstone splits them again.""",

            """
            Fortune IV & V

            Find a Fortune Upgrade template in Ancient City or mineshaft chests.

            Two Fortune III books + the template (lapis & diamond blocks) = Fortune IV. IV + IV = V.""",

            """
            Potions

            Brewmaster (5 nodes): beneficial potions last up to +50% longer.

            Potions stack to 16.""",

            """
            Mending & Recipes

            {MENDING}

            Open the Recipes icon on the skill screen to see every custom recipe.""",

            """
            Experience

            {XP}

            Your XP bar shows your banked Skill Shards instead.""",

            """
            Skill Shards

            Shards can be held as items. Right-click one to bank it.

            {SHARDBLOCK} shards craft an Unstable Skill Shard Block.""",

            """
            Stable Shard Blocks

            Ring an Unstable block with redstone and tinted glass to make it Stable.

            A placed Stable block damages nearby hostile mobs.""",

            """
            Infusing Table

            The enchanting table, without experience.

            It offers the enchantments shelved in chiseled bookshelves around it, and never consumes them.

            Paid in Shards.""",

            """
            Crates

            Fishing can pull up a crate alongside the catch - the biome decides which one.

            Inside: a random haul, sometimes Skill Shards."""
    };

    public static ItemStack create() {
        return create(null);
    }

    /** Build the guide, translated for {@code player} (null = English). Each page is one lang key
     *  {@code vanillaskills.guide.page.<n>}; the two mending strings are keyed too. */
    public static ItemStack create(ServerPlayer player) {
        String mending = GameplayConfig.MENDING_ENABLED
                ? io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, "vanillaskills.guide.mending_on", "Mending works normally on this server.")
                : io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, "vanillaskills.guide.mending_off", "Mending is removed - it never appears anywhere.");
        String experience = GameplayConfig.EXPERIENCE_ENABLED
                ? io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, "vanillaskills.guide.xp_on", "Experience works as in vanilla on this server.")
                : io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, "vanillaskills.guide.xp_off", "Experience is removed: no orbs, no levels, and nothing grants it.");
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int pi = 0; pi < PAGES.length; pi++) {
            String page = io.github.andrewwwwwwwwwwwwwww.vanillaskills.text.Lang.tr(player, "vanillaskills.guide.page." + pi, PAGES[pi]);
            // Fill the live-config tokens so the guide always matches the current settings.
            String text = page
                    .replace("{MENDING}", mending)
                    .replace("{XP}", experience)
                    .replace("{GRAD}", String.valueOf(io.github.andrewwwwwwwwwwwwwww.vanillaskills.skill.QuestPool.starter().size()))
                    .replace("{CONVERT}", String.valueOf(QuestShop.CONVERT_RATIO))
                    .replace("{QUESTS}", String.valueOf(GameplayConfig.QUESTS_PER_ROTATION))
                    .replace("{SHARDBLOCK}", String.valueOf(
                            io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.SHARDS_PER_BLOCK));
            pages.add(Filterable.passThrough(Component.literal(text)));
        }
        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough("VanillaSkills Guide"), "VanillaSkills", 0, pages, false);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    public static void open(ServerPlayer player) {
        ItemStack book = create(player);
        int slot = 36 + player.getInventory().getSelectedSlot();
        int containerId = player.inventoryMenu.containerId;
        player.connection.send(new ClientboundContainerSetSlotPacket(
                containerId, player.inventoryMenu.incrementStateId(), slot, book));
        player.connection.send(new ClientboundOpenBookPacket(InteractionHand.MAIN_HAND));
        player.connection.send(new ClientboundContainerSetSlotPacket(
                containerId, player.inventoryMenu.incrementStateId(), slot, player.getMainHandItem()));
    }
}
