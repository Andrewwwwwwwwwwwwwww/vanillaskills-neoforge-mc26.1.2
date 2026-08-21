package io.github.andrewwwwwwwwwwwwwww.vanillaskills.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Deletes vanilla recipes for blocks VanillaSkills has taken over.
 *
 * <p>2.0 claims {@code lodestone} as the Stable Skill Shard Block, so its vanilla recipe has to go —
 * otherwise anyone can craft the mod's endgame block out of eight chiseled stone bricks and an iron ingot.
 *
 * <p><b>A datapack cannot do this.</b> There is no "delete recipe" in the format: the closest you get is
 * overriding the file with a recipe that cannot be satisfied, and the first attempt here did exactly that
 * with a 3x3 of barriers. It worked, but the recipe book cheerfully displayed it — a barrier grid sitting
 * in the crafting list, which is worse than the problem. Dropping the holder outright removes it from
 * crafting, from the recipe book and from recipe unlocks in one go, silently.
 *
 * <p>{@code RecipeMap#create} is the single funnel every recipe map is built through, so filtering its
 * input catches the server's map and anything else that builds one.
 */
@Mixin(RecipeMap.class)
public class RecipeRemovalMixin {

    /** Vanilla recipe ids to drop. Keyed by recipe id, which is the file path under {@code data/…/recipe/}. */
    private static final Set<Identifier> VANILLASKILLS$REMOVED = Set.of(
            Identifier.fromNamespaceAndPath("minecraft", "lodestone"));

    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true, remap = false)
    private static Iterable<RecipeHolder<?>> vanillaskills$dropTakenOverRecipes(Iterable<RecipeHolder<?>> in) {
        if (in == null) return null;
        List<RecipeHolder<?>> out = new ArrayList<>();
        for (RecipeHolder<?> holder : in) {
            if (holder != null && VANILLASKILLS$REMOVED.contains(holder.id().identifier())) continue;
            out.add(holder);
        }
        return out;
    }
}
