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
 * <p>The two block forms carry {@link Items#AMETHYST_BLOCK} as their <i>item</i>. What gets placed in the
 * world is chosen separately by {@code ShardBlocks.baseBlock} and is not the same block — the placed block is
 * picked for light and beacon behaviour, since the visible cube is an item-display entity that covers it
 * entirely (see REWORK §6.1).
 *
 * <p>Art is delivered by the resource pack; until it exists the models point at texture paths that are not
 * there yet, which renders as the magenta/black placeholder.
 */
public final class ShardItems {
    private ShardItems() {}

    public static final String USS_MARKER = "vs_unstable_shard";
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

    public static ItemStack unstableShard() {
        ItemStack stack = new ItemStack(Items.AMETHYST_SHARD);
        Markers.stamp(stack, USS_MARKER, "vanillaskills:unstable_skill_shard", unstableShardName());
        stack.set(DataComponents.LORE, unstableShardLore());
        return stack;
    }

    public static boolean isUnstableShard(ItemStack stack) {
        return stack.is(Items.AMETHYST_SHARD) && Markers.has(stack, USS_MARKER);
    }

    // ---- Unstable Skill Shard Block ----

    public static ItemStack unstableBlock() {
        ItemStack stack = new ItemStack(Items.AMETHYST_BLOCK);
        Markers.stamp(stack, USSB_MARKER, "vanillaskills:unstable_skill_shard_block",
                Markers.name("vanillaskills.item.unstable_skill_shard_block", "Unstable Skill Shard Block", COLOR));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                line("vanillaskills.item.unstable_skill_shard_block.desc1", "Nine Skill Shards, compressed."))));
        return stack;
    }

    public static boolean isUnstableBlock(ItemStack stack) {
        return stack.is(Items.AMETHYST_BLOCK) && Markers.has(stack, USSB_MARKER);
    }

    // ---- Stable Skill Shard Block ----

    public static ItemStack stableBlock() {
        // Diamond block base: it is already in #minecraft:beacon_base_blocks, so the Stable block works as a
        // beacon base without us tagging a block type and promoting every copy of it in the world.
        ItemStack stack = new ItemStack(Items.DIAMOND_BLOCK);
        Markers.stamp(stack, SSSB_MARKER, "vanillaskills:stable_skill_shard_block",
                Markers.name("vanillaskills.item.stable_skill_shard_block", "Stable Skill Shard Block", COLOR));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                line("vanillaskills.item.stable_skill_shard_block.desc1", "Harms hostile mobs nearby."),
                line("vanillaskills.item.stable_skill_shard_block.desc2", "Right-click a placed one to merge."),
                line("vanillaskills.item.stable_skill_shard_block.desc3", "Works as a beacon base."))));
        return stack;
    }

    public static boolean isStableBlock(ItemStack stack) {
        return stack.is(Items.DIAMOND_BLOCK) && Markers.has(stack, SSSB_MARKER);
    }

    private static Component line(String key, String fallback) {
        return Component.translatableWithFallback(key, fallback)
                .withStyle(ChatFormatting.GRAY).withStyle(s -> s.withItalic(false));
    }
}
