package io.github.andrewwwwwwwwwwwwwww.vanillaskills.tool;

import net.minecraft.world.item.crafting.PlacementInfo;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipePlacement;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * One special recipe for every (tier x tool kind). Matches a tool shape where the material cells
 * are all the same tier's crafting material and the stick cells are sticks, then outputs that
 * tier's tool.
 */
public class ToolCraftingRecipe extends CustomRecipe {
    public static final ToolCraftingRecipe INSTANCE = new ToolCraftingRecipe();
    public static final RecipeSerializer<ToolCraftingRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, ToolCraftingRecipe>unit(INSTANCE));

    private record Match(ToolTier tier, ToolKind kind) {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Match match = find(input);
        return match == null ? ItemStack.EMPTY : match.tier().create(match.kind());
    }

    private static Match find(CraftingInput input) {
        for (ToolKind kind : ToolKind.values()) {
            for (ToolKind.Shape shape : kind.shapes) {
                if (input.width() != shape.width() || input.height() != shape.height()) continue;
                ToolTier tier = matchShape(input, shape);
                if (tier != null) return new Match(tier, kind);
            }
        }
        return null;
    }

    private static ToolTier matchShape(CraftingInput input, ToolKind.Shape shape) {
        ToolTier tier = null;
        int cells = shape.width() * shape.height();
        for (int i = 0; i < cells; i++) {
            ItemStack cell = input.getItem(i);
            if (shape.isMat(i)) {
                if (cell.isEmpty()) return null;
                ToolTier cellTier = ToolTiers.tierForMaterial(cell);
                if (cellTier == null) return null;
                if (tier == null) tier = cellTier;
                else if (tier != cellTier) return null;
            } else if (shape.isStick(i)) {
                if (!cell.is(Items.STICK)) return null;
            } else if (!cell.isEmpty()) {
                return null;
            }
        }
        return tier;
    }

    /** One book entry per (tier x tool kind), using each kind's first shape as the canonical layout. */
    @Override
    public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> out = new java.util.ArrayList<>();
        ItemStack stick = new ItemStack(Items.STICK);
        for (ToolTier tier : ToolTiers.TIERS) {
            ItemStack material = ToolTiers.sampleMaterial(tier);
            if (material.isEmpty()) continue;
            for (ToolKind kind : ToolKind.values()) {
                if (kind.shapes.isEmpty()) continue;
                ToolKind.Shape shape = kind.shapes.get(0);
                int cells = shape.width() * shape.height();
                ItemStack[] grid = new ItemStack[cells];
                for (int i = 0; i < cells; i++) {
                    grid[i] = shape.isMat(i) ? material.copy()
                            : shape.isStick(i) ? stick.copy()
                            : ItemStack.EMPTY;
                }
                out.add(io.github.andrewwwwwwwwwwwwwww.vanillaskills.recipe.RecipeDisplays.shaped(
                        shape.width(), shape.height(), grid, tier.create(kind), Items.CRAFTING_TABLE));
            }
        }
        return out;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<ToolCraftingRecipe> getSerializer() {
        return SERIALIZER;
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
