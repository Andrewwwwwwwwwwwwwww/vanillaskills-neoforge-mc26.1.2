package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard;

import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Markers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/**
 * The physical forms of a Skill Shard.
 *
 * <p>Skill Shards normally live as a balance on the player. Withdrawing turns them into
 * <b>Unstable Skill Shards</b> — real items that can be carried, traded, stored and found in the world —
 * and right-clicking one banks it again.
 *
 * <pre>
 *   9 Unstable Skill Shards        -> Unstable Skill Shard Block (USSB)
 *   USSB + 2 tinted glass + 4 redstone -> Stable Skill Shard Block (SSSB)
 * </pre>
 *
 * <p>All three follow the mod's standard pattern: a vanilla base item stamped with a hidden {@code vs_*}
 * marker, a custom model in our own namespace, and a translatable name — so nothing new is registered and
 * vanilla clients stay compatible.
 *
 * <p>The two block forms ARE their vanilla blocks: reinforced deepslate and lodestone, both taken over by
 * VanillaSkills and retextured in the pushed pack. Item and placed block are the same thing, so there is no
 * separate model entity and no marker to check — holding a lodestone <i>is</i> holding a Stable Skill Shard
 * Block, whether it came from us, the creative menu or a command.
 *
 * <p>Art is delivered by the resource pack; until it exists the models point at texture paths that are not
 * there yet, which renders as the magenta/black placeholder.
 */
public final class ShardItems {
    private ShardItems() {}

    public static final String USS_MARKER = "vs_unstable_shard";
    /** A written book stacks to 16; a currency wants 64. Overridden per-stack by MAX_STACK_SIZE. */
    public static final int USS_STACK_SIZE = 64;

    /** Hides the written-book content line, so no "by <author>" or "Original" appears on a shard. */
    public static final net.minecraft.world.item.component.TooltipDisplay USS_TOOLTIP =
            net.minecraft.world.item.component.TooltipDisplay.DEFAULT
                    .withHidden(DataComponents.WRITTEN_BOOK_CONTENT, true);

    public static final String USSB_MARKER = "vs_unstable_shard_block";
    public static final String SSSB_MARKER = "vs_stable_shard_block";

    /** Unstable violet — shared by all three so they read as one material family. */
    private static final int COLOR = 0x9B6BE8;

    /** How many Unstable Skill Shards make one block, both ways. */
    public static final int SHARDS_PER_BLOCK = 9;

    // ---- Unstable Skill Shard ----

    /**
     * The shard's display name, built without touching an {@link ItemStack}.
     *
     * <p>⚠ Loot-table modification runs on a worker thread <b>before item components are bound</b>, and
     * constructing any {@code ItemStack} there throws {@code "Components not bound yet"} — which took the
     * whole server down with "Failed to load datapacks". Anything that needs the shard's cosmetics at load
     * time must use these accessors rather than reading them back off a built stack.
     */
    public static Component unstableShardName() {
        return Markers.name("vanillaskills.item.unstable_skill_shard", "Unstable Skill Shard", COLOR);
    }

    /** The shard's lore, built without touching an {@link ItemStack}. See {@link #unstableShardName()}. */
    public static ItemLore unstableShardLore() {
        return new ItemLore(List.of(
                line("vanillaskills.item.unstable_skill_shard.desc1", "A Skill Shard given physical form."),
                line("vanillaskills.item.unstable_skill_shard.desc2", "Right-click to bank it again.")));
    }

    /**
     * A withdrawn Skill Shard.
     *
     * <h2>Why a written book</h2>
     * This was an {@code amethyst_shard} until 2.0, which put it in four vanilla recipes —
     * {@code amethyst_block}, {@code tinted_glass}, {@code spyglass} and {@code calibrated_sculk_sensor}.
     * {@code IngredientMixin} stopped a marked shard actually satisfying them, but a vanilla client computes
     * recipe-book craftability itself and cannot see our marker, so it advertised those recipes as craftable
     * and there was no server-side way to correct the display.
     *
     * <p>{@code written_book} appears in <b>no</b> crafting recipe. Its one behaviour is {@code use} calling
     * {@code Player.openItemGui}, which is server-side and already pre-empted by the {@code UseItemCallback}
     * that banks the shard — so opening the book is replaced by banking it rather than fought.
     *
     * <p>Book cloning cannot duplicate shards: {@code BookCloningRecipe.matches} goes through
     * {@code Ingredient.test}, which {@code IngredientMixin} already forces false for anything marked.
     *
     * <p>Two components paper over the base item: {@code MAX_STACK_SIZE} lifts the written book's 16 to a
     * currency-appropriate 64, and {@code TOOLTIP_DISPLAY} hides the book-content line so no author or
     * generation text appears.
     */
    public static ItemStack unstableShard() {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        Markers.stamp(stack, USS_MARKER, "vanillaskills:unstable_skill_shard", unstableShardName());
        stack.set(DataComponents.LORE, unstableShardLore());
        stack.set(DataComponents.MAX_STACK_SIZE, USS_STACK_SIZE);
        stack.set(DataComponents.TOOLTIP_DISPLAY, USS_TOOLTIP);
        return stack;
    }

    public static boolean isUnstableShard(ItemStack stack) {
        return stack.is(Items.WRITTEN_BOOK) && Markers.has(stack, USS_MARKER);
    }

    // ---- Unstable Skill Shard Block ----

    public static ItemStack unstableBlock() {
        ItemStack stack = new ItemStack(Items.REINFORCED_DEEPSLATE);
        Markers.stamp(stack, USSB_MARKER, "vanillaskills:unstable_skill_shard_block",
                Markers.name("vanillaskills.item.unstable_skill_shard_block", "Unstable Skill Shard Block", COLOR));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                line("vanillaskills.item.unstable_skill_shard_block.desc1", "Nine Skill Shards, compressed."))));
        return stack;
    }

    public static boolean isUnstableBlock(ItemStack stack) {
        // No marker test. VanillaSkills owns reinforced deepslate outright — the datapack stops vanilla
        // generating it and it has no recipe and an empty loot table — so the item type alone is proof.
        // Requiring a marker was what left a plain one behaving as an ordinary block.
        return stack.is(Items.REINFORCED_DEEPSLATE);
    }

    // ---- Stable Skill Shard Block ----

    public static ItemStack stableBlock() {
        // Diamond block base: it is already in #minecraft:beacon_base_blocks, so the Stable block works as a
        // beacon base without us tagging a block type and promoting every copy of it in the world.
        ItemStack stack = new ItemStack(Items.LODESTONE);
        Markers.stamp(stack, SSSB_MARKER, "vanillaskills:stable_skill_shard_block",
                Markers.name("vanillaskills.item.stable_skill_shard_block", "Stable Skill Shard Block", COLOR));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                line("vanillaskills.item.stable_skill_shard_block.desc1", "Harms hostile mobs nearby."),
                line("vanillaskills.item.stable_skill_shard_block.desc2", "Right-click a placed one to merge."),
                line("vanillaskills.item.stable_skill_shard_block.desc3", "Works as a beacon base."))));
        return stack;
    }

    public static boolean isStableBlock(ItemStack stack) {
        // See isUnstableBlock: lodestone is ours, so the item type alone is proof.
        return stack.is(Items.LODESTONE);
    }

    private static Component line(String key, String fallback) {
        return Component.translatableWithFallback(key, fallback)
                .withStyle(ChatFormatting.GRAY).withStyle(s -> s.withItalic(false));
    }
}
