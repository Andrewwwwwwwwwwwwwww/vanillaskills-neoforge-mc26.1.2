package io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * One special recipe that handles every (tier x piece) armor craft. It matches an armor shape
 * (from {@link ArmorPiece}) where every material cell is the same tier's crafting material, then
 * outputs that tier's piece. Works for all current and future tiers without new recipes.
 */
public class ArmorCraftingRecipe extends CustomRecipe {
    public static final ArmorCraftingRecipe INSTANCE = new ArmorCraftingRecipe();
    public static final RecipeSerializer<ArmorCraftingRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, ArmorCraftingRecipe>unit(INSTANCE));

    private record Match(ArmorTier tier, ArmorPiece piece) {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Match match = find(input);
        return match == null ? ItemStack.EMPTY : match.tier().create(match.piece());
    }

    private static Match find(CraftingInput input) {
        for (ArmorPiece piece : ArmorPiece.values()) {
            if (input.width() != piece.width || input.height() != piece.height) continue;
            ArmorTier tier = null;
            boolean ok = true;
            for (int i = 0; i < piece.filled.length; i++) {
                ItemStack cell = input.getItem(i);
                if (piece.filled[i]) {
                    if (cell.isEmpty()) { ok = false; break; }
                    ArmorTier cellTier = ArmorTiers.tierForMaterial(cell);
                    if (cellTier == null) { ok = false; break; }
                    if (tier == null) tier = cellTier;
                    else if (tier != cellTier) { ok = false; break; }
                } else if (!cell.isEmpty()) {
                    ok = false;
                    break;
                }
            }
            // Dragon armor is smithing-only (netherite + Dragon Upgrade Template + Dragon Ingot),
            // so it is never table-craftable from its material.
            if (ok && tier != null && tier != ArmorTiers.DRAGON) return new Match(tier, piece);
        }
        return null;
    }

    /**
     * One book entry per (tier x piece) this recipe can actually make.
     *
     * <p>Dragon is excluded because it is smithing-only, matching {@link #find}. Each tier shows a
     * representative sample of its crafting material, so the book displays a marked Steel Ingot rather
     * than a plain iron one.
     */
    @Override
    public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> out = new java.util.ArrayList<>();
        for (ArmorTier tier : ArmorTiers.TIERS) {
            if (tier == ArmorTiers.DRAGON) continue;
            ItemStack material = ArmorTiers.sampleMaterial(tier);
            if (material.isEmpty()) continue;
            for (ArmorPiece piece : ArmorPiece.values()) {
                ItemStack[] grid = new ItemStack[piece.filled.length];
                for (int i = 0; i < piece.filled.length; i++) {
                    grid[i] = piece.filled[i] ? material.copy() : ItemStack.EMPTY;
                }
                out.add(io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipeDisplays.shaped(
                        piece.width, piece.height, grid, tier.create(piece),
                        net.minecraft.world.item.Items.CRAFTING_TABLE));
            }
        }
        return out;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<ArmorCraftingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
