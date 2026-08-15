package io.github.andrewwwwwwwwwwwwwww.vanillaskills.infuse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Infusing Table: what the enchanting table becomes once experience is gone.
 *
 * <p>Rather than rolling random enchantments from levels you no longer have, it reads the enchanted books
 * shelved in the <b>chiseled bookshelves</b> around it and offers exactly those, to apply directly and
 * deliberately. You can see what you are going to get before you pay for it.
 *
 * <p>Two deliberate rules:
 * <ul>
 *   <li><b>Books are not consumed.</b> A shelved library is infrastructure you build once, not ammunition.
 *       Shelving a Silk Touch book makes Silk Touch permanently available at this table.</li>
 *   <li><b>Cost is paid in shards</b>, configurable in both currency and amount, so the price of an
 *       enchantment can be tuned without touching code.</li>
 * </ul>
 *
 * <p>Uses the vanilla enchanting-table shelf layout — the same fifteen positions, at the same two heights,
 * with the same "nothing solid in between" rule — so an existing enchanting setup keeps working once its
 * bookshelves are swapped for chiseled ones.
 */
public final class InfusingTable {
    private InfusingTable() {}

    /** Horizontal offsets of the fifteen vanilla bookshelf positions, at y+0 and y+1. */
    private static final int[][] SHELF_OFFSETS = {
            {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
            {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2},
            {-2, -1}, {-2, 0}, {-2, 1},
            {2, -1}, {2, 0}, {2, 1}};

    /**
     * Every enchantment offered by the shelves around this table, at the highest level shelved.
     *
     * <p>Keyed by enchantment so two copies of the same book at different levels collapse to the better one.
     */
    public static Map<Holder<Enchantment>, Integer> availableAt(ServerLevel level, BlockPos tablePos) {
        Map<Holder<Enchantment>, Integer> out = new LinkedHashMap<>();
        for (int[] offset : SHELF_OFFSETS) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos shelf = tablePos.offset(offset[0], dy, offset[1]);
                if (!isReachable(level, tablePos, shelf, dy)) continue;
                collectFrom(level, shelf, out);
            }
        }
        return out;
    }

    /**
     * Vanilla blocks a bookshelf if something solid sits between it and the table, which is what lets
     * players wall off shelves they do not want counted. Same rule here.
     */
    private static boolean isReachable(ServerLevel level, BlockPos tablePos, BlockPos shelf, int dy) {
        if (!level.getBlockState(shelf).is(Blocks.CHISELED_BOOKSHELF)) return false;
        BlockPos between = new BlockPos(
                tablePos.getX() + (shelf.getX() - tablePos.getX()) / 2,
                tablePos.getY() + dy,
                tablePos.getZ() + (shelf.getZ() - tablePos.getZ()) / 2);
        return level.getBlockState(between).isAir();
    }

    private static void collectFrom(ServerLevel level, BlockPos shelf, Map<Holder<Enchantment>, Integer> out) {
        if (!(level.getBlockEntity(shelf) instanceof ChiseledBookShelfBlockEntity bookshelf)) return;
        for (ItemStack book : bookshelf.getItems()) {
            if (!book.is(Items.ENCHANTED_BOOK)) continue;
            ItemEnchantments stored = EnchantmentHelper.getEnchantmentsForCrafting(book);
            for (var entry : stored.entrySet()) {
                Holder<Enchantment> enchantment = entry.getKey();
                int level2 = entry.getIntValue();
                out.merge(enchantment, level2, Math::max);
            }
        }
    }

    /**
     * Whether infusing this enchantment at this level burns the book that supplied it.
     *
     * <p>Shelved books are normally permanent infrastructure — that is the whole point of the table. Fortune
     * IV and V are the deliberate exception: they are minted rather than enchanted, and a reusable source of
     * them would make every other way of getting Fortune pointless.
     *
     * <p>Driven by {@code infusingConsumedBooks} in {@code gameplay.json}, a list of
     * {@code "<enchantment>:<minLevel>"} entries, so the rule can be widened or dropped without a code change.
     */
    public static boolean consumesBook(Holder<Enchantment> enchantment, int level) {
        Identifier id = enchantment.unwrapKey().map(k -> k.identifier()).orElse(null);
        if (id == null) return false;
        for (String rule : io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.INFUSING_CONSUMED_BOOKS) {
            int split = rule.lastIndexOf(':');
            if (split <= 0) continue;
            Identifier ruleId = Identifier.tryParse(rule.substring(0, split));
            if (ruleId == null || !ruleId.equals(id)) continue;
            try {
                if (level >= Integer.parseInt(rule.substring(split + 1).trim())) return true;
            } catch (NumberFormatException ignored) {
                // A malformed rule must not break infusing — it simply does not match.
            }
        }
        return false;
    }

    /**
     * Take one shelved book carrying {@code enchantment} at {@code level} or better off the shelves.
     *
     * @return true if a book was consumed
     */
    public static boolean consumeBook(ServerLevel level, BlockPos tablePos,
                                      Holder<Enchantment> enchantment, int enchantLevel) {
        for (int[] offset : SHELF_OFFSETS) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos shelf = tablePos.offset(offset[0], dy, offset[1]);
                if (!isReachable(level, tablePos, shelf, dy)) continue;
                if (!(level.getBlockEntity(shelf) instanceof ChiseledBookShelfBlockEntity bookshelf)) continue;
                for (int slot = 0; slot < bookshelf.getItems().size(); slot++) {
                    ItemStack book = bookshelf.getItems().get(slot);
                    if (!book.is(Items.ENCHANTED_BOOK)) continue;
                    ItemEnchantments stored = EnchantmentHelper.getEnchantmentsForCrafting(book);
                    if (stored.getLevel(enchantment) < enchantLevel) continue;
                    // removeItem also refreshes the shelf's occupied blockstate, so the slot visibly empties.
                    bookshelf.removeItem(slot, 1);
                    return true;
                }
            }
        }
        return false;
    }

    /** Shard cost of applying one enchantment at the given level. */
    public static int costOf(int enchantmentLevel) {
        return Math.max(1, enchantmentLevel)
                * io.github.andrewwwwwwwwwwwwwww.vanillaskills.config.GameplayConfig.INFUSING_COST_PER_LEVEL;
    }

    /** True if this enchantment can go on this stack — vanilla's own compatibility rules, nothing custom. */
    public static boolean canApply(ItemStack stack, Holder<Enchantment> enchantment) {
        return canApply(stack, enchantment, 0, java.util.Set.of());
    }

    /**
     * Whether {@code enchantment} at {@code level} can be infused onto {@code stack}, given everything the
     * player has already picked in this session.
     *
     * <p>Three separate ways this can be false, and the last two were both live bugs:
     *
     * <ol>
     *   <li>the enchantment does not belong on this item at all;</li>
     *   <li>it conflicts with something already on the item, <b>or with another enchantment picked in the
     *       same session</b>. Only the first half used to be checked, so Fortune and Silk Touch could both
     *       be selected — neither was on the item yet, so both passed — and the player was charged for both
     *       while only one could survive {@code updateEnchantments};</li>
     *   <li>the item already has it at this level or higher. That used to be allowed and charged for, and
     *       then did nothing at all. A strictly higher level is still permitted, since that is an upgrade.</li>
     * </ol>
     *
     * @param level        the level on offer; 0 skips the already-have check (used by the plain overload)
     * @param alsoSelected other enchantments chosen in this session, which must not conflict
     */
    public static boolean canApply(ItemStack stack, Holder<Enchantment> enchantment, int level,
                                   java.util.Set<Holder<Enchantment>> alsoSelected) {
        if (stack.isEmpty()) return false;
        if (!enchantment.value().canEnchant(stack)) return false;

        ItemEnchantments current = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var entry : current.entrySet()) {
            Holder<Enchantment> present = entry.getKey();
            if (present.equals(enchantment)) {
                // Same enchantment: only an upgrade is worth paying for.
                if (level > 0 && entry.getIntValue() >= level) return false;
                continue;
            }
            if (!Enchantment.areCompatible(present, enchantment)) return false;
        }

        for (Holder<Enchantment> other : alsoSelected) {
            if (other.equals(enchantment)) continue;
            if (!Enchantment.areCompatible(other, enchantment)) return false;
        }
        return true;
    }

    /**
     * Apply an enchantment to a stack.
     *
     * <p>Routed through {@link EnchantmentHelper#updateEnchantments} rather than writing the component
     * directly, so it passes the same chokepoint everything else does — which is how the Mending removal
     * keeps working here without the Infusing Table needing to know about it.
     */
    public static void apply(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        EnchantmentHelper.updateEnchantments(stack, mutable -> {
            if (mutable.getLevel(enchantment) < level) mutable.set(enchantment, level);
        });
    }
}
