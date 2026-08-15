package io.github.andrewwwwwwwwwwwwwww.vanillaskills.shield;

import com.mojang.serialization.MapCodec;
import io.github.andrewwwwwwwwwwwwwww.vanillaskills.armor.Alloys;
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
 * Infuses a plain shield with steel:
 *
 *   S H S     S = steel ingot
 *   S S S     H = shield
 *   . S .
 */
public class ShieldInfuseRecipe extends CustomRecipe {
    public static final ShieldInfuseRecipe INSTANCE = new ShieldInfuseRecipe();
    public static final RecipeSerializer<ShieldInfuseRecipe> SERIALIZER = new RecipeSerializer<>(
            MapCodec.unit(INSTANCE),
            StreamCodec.<RegistryFriendlyByteBuf, ShieldInfuseRecipe>unit(INSTANCE));

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        for (int i = 0; i < 9; i++) {
            ItemStack s = input.getItem(i);
            switch (i) {
                case 1 -> { if (!s.is(Items.SHIELD) || SteelShield.isSteelShield(s)) return false; }
                case 6, 8 -> { if (!s.isEmpty()) return false; }
                default -> { if (!Alloys.isSteelIngot(s)) return false; }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return SteelShield.create();
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<ShieldInfuseRecipe> getSerializer() {
        return SERIALIZER;
    }
}
