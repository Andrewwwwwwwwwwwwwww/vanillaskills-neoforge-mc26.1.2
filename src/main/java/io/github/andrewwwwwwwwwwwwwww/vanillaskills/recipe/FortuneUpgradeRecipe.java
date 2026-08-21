package io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe;

import net.minecraft.world.item.crafting.PlacementInfo;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * Upgrades a pair of Fortune books one level using the Fortune Upgrade template:
 *
 *   L D L     L = lapis block
 *   B T B     D = diamond block
 *   L D L     B = enchanted book with Fortune N (both same level, N in 3..4)
 *             T = Fortune Upgrade smithing template (consumed)
 *
 * Output: a single enchanted book with Fortune (N+1). max_level stays 3, so this is the only
 * way to obtain Fortune IV/V; the book is applied to a tool in an anvil (see AnvilMenuMixin).
 */
public class FortuneUpgradeRecipe extends CustomRecipe {
    public static final FortuneUpgradeRecipe INSTANCE = new FortuneUpgradeRecipe();
    public static final RecipeSerializer<FortuneUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, FortuneUpgradeRecipe>unit(INSTANCE));

    private static final int MAX_LEVEL = 5;

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;

        // Fixed positions (row-major): 0..8. The template and the two matching books are common to both
        // tiers; the frame differs, so Fortune V costs materially more than Fortune IV.
        if (!FortuneTemplate.isTemplate(input.getItem(4))) return false;

        int left = bookFortuneLevel(input.getItem(3));
        int right = bookFortuneLevel(input.getItem(5));
        if (left < 3 || left >= MAX_LEVEL || left != right) return false;

        if (left == 3) {
            // III -> IV: lapis corners, diamond blocks top and bottom.
            return input.getItem(0).is(Items.LAPIS_BLOCK)
                    && input.getItem(2).is(Items.LAPIS_BLOCK)
                    && input.getItem(6).is(Items.LAPIS_BLOCK)
                    && input.getItem(8).is(Items.LAPIS_BLOCK)
                    && input.getItem(1).is(Items.DIAMOND_BLOCK)
                    && input.getItem(7).is(Items.DIAMOND_BLOCK);
        }
        // IV -> V: diamond blocks in every corner and a Stable Skill Shard Block top and bottom, so the
        // final level is gated behind the shard economy rather than just more mining.
        return input.getItem(0).is(Items.DIAMOND_BLOCK)
                && input.getItem(2).is(Items.DIAMOND_BLOCK)
                && input.getItem(6).is(Items.DIAMOND_BLOCK)
                && input.getItem(8).is(Items.DIAMOND_BLOCK)
                && io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.isStableBlock(input.getItem(1))
                && io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.isStableBlock(input.getItem(7));
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack sourceBook = input.getItem(3);
        ItemEnchantments stored = sourceBook.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null) return ItemStack.EMPTY;

        Holder<Enchantment> fortune = null;
        int level = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            if (entry.getKey().is(Enchantments.FORTUNE)) {
                fortune = entry.getKey();
                level = entry.getIntValue();
                break;
            }
        }
        if (fortune == null) return ItemStack.EMPTY;

        ItemStack out = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(fortune, Math.min(MAX_LEVEL, level + 1));
        out.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return out;
    }

    /** Both upgrade steps, each with its own frame — IV on lapis, V on diamond blocks and Stable blocks. */
    @Override
    public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        ItemStack lapis = new ItemStack(Items.LAPIS_BLOCK);
        ItemStack diaBlock = new ItemStack(Items.DIAMOND_BLOCK);
        ItemStack sssb = io.github.andrewwwwwwwwwwwwwww.vanillaskills.shard.ShardItems.stableBlock();
        ItemStack template = FortuneTemplate.create();
        return java.util.List.of(
                RecipeDisplays.shaped(new ItemStack[]{
                        lapis.copy(), diaBlock.copy(), lapis.copy(),
                        displayBook(3), template.copy(), displayBook(3),
                        lapis.copy(), diaBlock.copy(), lapis.copy()}, displayBook(4),
                        Items.CRAFTING_TABLE),
                RecipeDisplays.shaped(new ItemStack[]{
                        diaBlock.copy(), sssb.copy(), diaBlock.copy(),
                        displayBook(4), template.copy(), displayBook(4),
                        diaBlock.copy(), sssb.copy(), diaBlock.copy()}, displayBook(5),
                        Items.CRAFTING_TABLE));
    }

    /**
     * A display-only "Fortune N" book.
     *
     * <p>Labelled rather than genuinely enchanted: building a real one needs a {@code Holder<Enchantment>}
     * from the registry, and {@code display()} can be called without a reliable registry to hand. The label
     * conveys the same thing in the book and cannot go stale.
     */
    private static ItemStack displayBook(int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        String roman = switch (level) { case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(level); };
        book.set(net.minecraft.core.component.DataComponents.ITEM_NAME,
                net.minecraft.network.chat.Component.translatableWithFallback(
                        "vanillaskills.item.fortune_book", "Fortune %s", roman)
                        .withStyle(net.minecraft.ChatFormatting.AQUA)
                        .withStyle(s -> s.withItalic(false)));
        book.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return book;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<FortuneUpgradeRecipe> getSerializer() {
        return SERIALIZER;
    }

    private static int bookFortuneLevel(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) return 0;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) return 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            if (entry.getKey().is(Enchantments.FORTUNE)) return entry.getIntValue();
        }
        return 0;
    }

    /**
     * Show this recipe in the recipe book.
     *
     * <p>{@link net.minecraft.world.item.crafting.CustomRecipe} declares itself special, and vanilla excludes
     * a special recipe from the book entirely — which is why these never appeared no matter what the player
     * was carrying. Unlocking worked the whole time; there was simply nothing to render.
     *
     * <p>"Special" exists for recipes whose result depends on inputs the book cannot draw, such as firework
     * or shulker-box dyeing. Ours have a fixed, drawable result and publish a {@code display()} for it, so
     * they are ordinary recipes as far as the book is concerned.
     */
    @Override
    public boolean isSpecial() {
        return false;
    }


    /**
     * Publish real, item-keyed placement data instead of {@code CustomRecipe}'s {@code NOT_PLACEABLE}.
     *
     * <p>Without this the recipe is unusable from the book in both directions: the packet handler drops the
     * click before placement is ever attempted, and the client is told the recipe costs nothing and so lights
     * it up as craftable for everybody. See {@code RecipePlacement} for what an Ingredient can and cannot say
     * about a component-marked item.
     */
    @Override
    public PlacementInfo placementInfo() {
        return RecipePlacement.placementFor(this);
    }
}
